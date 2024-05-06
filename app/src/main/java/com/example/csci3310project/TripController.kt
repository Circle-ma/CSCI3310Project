import com.example.csci3310project.Event
import com.example.csci3310project.Trip
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class FirestoreRepository(private val db: FirebaseFirestore) {

    fun addTrip(trip: Trip, onComplete: (Boolean) -> Unit) {
        db.collection("trips").document(trip.id).set(trip).addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun addEventToTrip(tripId: String, event: Event, onComplete: (Boolean) -> Unit) {
        db.collection("trips").document(tripId).update("events", FieldValue.arrayUnion(event))
            .addOnSuccessListener { onComplete(true) }.addOnFailureListener { onComplete(false) }
    }

    // Add more methods as needed for updating and deleting trips/events
}
