package com.example.csci3310project

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreRepository(private val db: FirebaseFirestore) {
    fun getUserTrips(userId: String, onUpdate: (List<Trip>) -> Unit) {
        db.collection("trips").whereArrayContains("participantsID", userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onUpdate(emptyList())
                    return@addSnapshotListener
                }
                val trips =
                    snapshot?.documents?.mapNotNull { it.toObject(Trip::class.java) } ?: emptyList()
                onUpdate(trips)
            }
    }

    fun addTrip(trip: Trip, userID: String, userName: String, onComplete: (Boolean, String?) -> Unit) {
        trip.participantsID.add(userID)
        trip.participants.add(userName)  // Automatically add the creator to the participants list
        trip.joinCode = generateJoinCode()  // Generate a unique join code for the trip
        val newTripRef = db.collection("trips").document(trip.id)
        newTripRef.set(trip).addOnSuccessListener {
            onComplete(true, newTripRef.id)
        }.addOnFailureListener {
            onComplete(false, null)
        }
    }

    fun joinTripByCode(joinCode: String, userName: String, userId: String, onComplete: (Boolean) -> Unit) {
        db.collection("trips").whereEqualTo("joinCode", joinCode).limit(1).get()
            .addOnSuccessListener { querySnapshot ->
                val tripDocument = querySnapshot.documents.firstOrNull()
                tripDocument?.let {
                    val tripRef = db.collection("trips").document(it.id)
                    tripRef.update(
                        mapOf(
                            "participants" to FieldValue.arrayUnion(userName),
                            "participantsID" to FieldValue.arrayUnion(userId)
                        )
                    ).addOnSuccessListener { onComplete(true) }
                      .addOnFailureListener { onComplete(false) }
                } ?: onComplete(false)
            }.addOnFailureListener {
                onComplete(false)
            }
    }

    fun getTripDetails(tripId: String, onUpdate: (Trip?) -> Unit) {
        db.collection("trips").document(tripId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                onUpdate(null)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                onUpdate(snapshot.toObject(Trip::class.java))
            }
        }
    }

    fun updateTrip(tripId: String, updatedTrip: Trip, onComplete: (Boolean) -> Unit) {
        db.collection("trips").document(tripId).set(updatedTrip).addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun deleteTrip(tripId: String, onComplete: (Boolean) -> Unit) {
        db.collection("trips").document(tripId).delete().addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun addEventToTrip(tripId: String, event: Event, onComplete: (Boolean) -> Unit) {
        db.collection("trips").document(tripId).update("events", FieldValue.arrayUnion(event))
            .addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }

    fun updateEventInTrip(tripId: String, event: Event, onComplete: (Boolean) -> Unit) {
        val tripRef = db.collection("trips").document(tripId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(tripRef)
            val trip = snapshot.toObject(Trip::class.java)
            trip?.events?.find { it.id == event.id }?.apply {
                title = event.title
                date = event.date
                startTime = event.startTime
                endTime = event.endTime
                location = event.location
                travelMethod = event.travelMethod
            }
            if (trip != null) {
                transaction.set(tripRef, trip)
            }
            true
        }.addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }

    fun getEventDetails(tripId: String, eventId: String, onComplete: (Event?) -> Unit) {
        val tripRef = db.collection("trips").document(tripId)
        tripRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val trip = document.toObject(Trip::class.java)
                val event = trip?.events?.find { it.id == eventId }
                onComplete(event)
            } else {
                onComplete(null) // Trip does not exist
            }
        }.addOnFailureListener {
            onComplete(null) // Handle failure
        }
    }

    fun deleteEventFromTrip(tripId: String, eventId: String, onComplete: (Boolean) -> Unit) {
        val tripRef = db.collection("trips").document(tripId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(tripRef)
            val trip = snapshot.toObject(Trip::class.java)
            trip?.events?.removeIf { it.id == eventId }
            if (trip != null) {
                transaction.set(tripRef, trip)
            }
            true
        }.addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }

    fun addExpenseToTrip(tripId: String, expense: Expense, onComplete: (Boolean) -> Unit) {
        val tripRef = db.collection("trips").document(tripId)
    
        db.runTransaction { transaction ->
            val trip = transaction.get(tripRef).toObject(Trip::class.java)
            trip?.expenses = (trip?.expenses.orEmpty() + expense).toMutableList()
            for (transaction in expense.transactions) {
                val creditorName = transaction.creditorName
                val debtorName = transaction.debtorName
                val amount = transaction.amount
                val existingTransaction = trip?.transactions?.find { it.creditorName == creditorName && it.debtorName == debtorName }
                if (existingTransaction != null) {
                    existingTransaction.amount += amount
                } else {
                    trip?.transactions?.add(ExpenseTransaction(creditorName, debtorName, amount))
                }
            }
            transaction.set(tripRef, trip!!)
        }.addOnSuccessListener {
            onComplete(true)
        }.addOnFailureListener {
            onComplete(false)
        }
    }

    fun updateExpenseInTrip(tripId: String, originalExpense: Expense, expense: Expense, onComplete: (Boolean) -> Unit) {
        val tripRef = db.collection("trips").document(tripId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(tripRef)
            val trip = snapshot.toObject(Trip::class.java)
            trip?.expenses?.find { it.id == expense.id }?.apply {
                title = expense.title
                date = expense.date
                amount = expense.amount
                payer = expense.payer
                transactions = expense.transactions
            }
    
            // Update the transactions
            originalExpense.transactions.forEach { originalTransaction ->
                val existingTransaction = trip?.transactions?.find { it.creditorName == originalTransaction.creditorName && it.debtorName == originalTransaction.debtorName }
                existingTransaction?.let {
                    it.amount -= originalTransaction.amount
                    if (it.amount <= 0) {
                        trip.transactions.remove(it)
                    }
                }
            }
    
            expense.transactions.forEach { newTransaction ->
                val existingTransaction = trip?.transactions?.find { it.creditorName == newTransaction.creditorName && it.debtorName == newTransaction.debtorName }
                if (existingTransaction != null) {
                    existingTransaction.amount += newTransaction.amount
                } else {
                    trip?.transactions?.add(ExpenseTransaction(newTransaction.creditorName, newTransaction.debtorName, newTransaction.amount))
                }
            }
    
            if (trip != null) {
                transaction.set(tripRef, trip)
            }
            true
        }.addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getExpenseDetails(tripId: String, expenseId: String, onComplete: (Expense?) -> Unit) {
        val tripRef = db.collection("trips").document(tripId)
        tripRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val trip = document.toObject(Trip::class.java)
                val expense = trip?.expenses?.find { it.id == expenseId }
                onComplete(expense)
            } else {
                onComplete(null) // Trip does not exist
            }
        }.addOnFailureListener {
            onComplete(null) // Handle failure
        }
    }

    fun deleteExpenseFromTrip(tripId: String, expenseId: String, onComplete: (Boolean) -> Unit) {
        val tripRef = db.collection("trips").document(tripId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(tripRef)
            val trip = snapshot.toObject(Trip::class.java)
            val expense = trip?.expenses?.find { it.id == expenseId }
            expense?.let {
                // Remove the expense
                trip.expenses.remove(it)
    
                // Update the transactions
                it.transactions.forEach { expenseTransaction ->
                    val existingTransaction = trip.transactions.find { transaction ->
                        transaction.creditorName == expenseTransaction.creditorName && transaction.debtorName == expenseTransaction.debtorName
                    }
                    existingTransaction?.let { transaction ->
                        transaction.amount -= expenseTransaction.amount
                        if (transaction.amount <= 0) {
                            trip.transactions.remove(transaction)
                        }
                    }
                }
            }
            if (trip != null) {
                transaction.set(tripRef, trip)
            }
            true
        }.addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getTripParticipants(tripId: String, onComplete: (List<String>) -> Unit) {
        db.collection("trips").document(tripId).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val trip = document.toObject(Trip::class.java)
                val participants = trip?.participants ?: emptyList()
                db.collection("users").whereIn("id", participants).get()
                    .addOnSuccessListener {
                        Log.d("FirestoreRepository", "Participants: $participants")
                        onComplete(participants)
                    }.addOnFailureListener {
                        onComplete(emptyList())
                    }
            } else {
                onComplete(emptyList()) // Trip does not exist
            }
        }.addOnFailureListener {
            onComplete(emptyList()) // Handle failure
        }
    }

}
