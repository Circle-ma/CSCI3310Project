package com.example.csci3310project

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreRepository(private val db: FirebaseFirestore) {
    fun getUserTrips(userId: String, onUpdate: (List<Trip>) -> Unit) {
        db.collection("trips").whereArrayContains("participants", userId)
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

    fun addTrip(trip: Trip, userId: String, onComplete: (Boolean, String?) -> Unit) {
        trip.participants.add(userId)  // Automatically add the creator to the participants list
        trip.joinCode = generateJoinCode()  // Generate a unique join code for the trip
        val newTripRef = db.collection("trips").document(trip.id)
        newTripRef.set(trip).addOnSuccessListener {
            onComplete(true, newTripRef.id)
        }.addOnFailureListener {
            onComplete(false, null)
        }
    }

    fun joinTripByCode(joinCode: String, userId: String, onComplete: (Boolean) -> Unit) {
        db.collection("trips").whereEqualTo("joinCode", joinCode).limit(1).get()
            .addOnSuccessListener { querySnapshot ->
                val tripDocument = querySnapshot.documents.firstOrNull()
                tripDocument?.let {
                    db.collection("trips").document(it.id)
                        .update("participants", FieldValue.arrayUnion(userId))
                        .addOnSuccessListener { onComplete(true) }
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
    private fun generateJoinCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..6).map { allowedChars.random() }.joinToString("")
    }
}
