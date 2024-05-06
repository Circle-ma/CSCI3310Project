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
        trip.participants.add(userId)  // Add the creator to the participants list
        val newTripRef = db.collection("trips").document()
        trip.id = newTripRef.id  // Set the document ID as the trip ID
        newTripRef.set(trip).addOnSuccessListener { onComplete(true, newTripRef.id) }
            .addOnFailureListener { onComplete(false, null) }
    }

    fun getTripDetails(tripId: String, onUpdate: (Trip?) -> Unit) {
        db.collection("trips").document(tripId).addSnapshotListener { snapshot, e ->
            if (e != null) {
                onUpdate(null)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                onUpdate(snapshot.toObject(Trip::class.java))
            } else {
                onUpdate(null)
            }
        }
    }

    fun addEventToTrip(tripId: String, event: Event, onComplete: (Boolean) -> Unit) {
        db.collection("trips").document(tripId).update("events", FieldValue.arrayUnion(event))
            .addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }

    fun joinTrip(tripId: String, userId: String, onComplete: (Boolean) -> Unit) {
        db.collection("trips").document(tripId)
            .update("participants", FieldValue.arrayUnion(userId))
            .addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }
}
