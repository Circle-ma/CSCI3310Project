package com.example.csci3310project

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.DateFormat
import java.text.ParseException

@Composable
fun TripView(
    authController: AuthController,
    firestoreRepository: FirestoreRepository,
    navController: NavController
) {
    var tripTitle by remember { mutableStateOf("") }
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current
    val userId = authController.user?.id ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(value = tripTitle,
            onValueChange = { tripTitle = it },
            label = { Text("Trip Title") })
        DateInput("Start Date", startDate) { newStartDate -> startDate = newStartDate }
        DateInput("End Date", endDate) { newEndDate -> endDate = newEndDate }
        Button(onClick = {
            val trip = Trip(
                title = tripTitle, startDate = startDate, endDate = endDate
            )
            firestoreRepository.addTrip(trip, userId) { isSuccess, tripId ->
                if (isSuccess && tripId != null) {
                    Toast.makeText(context, "Trip created. ID: $tripId", Toast.LENGTH_LONG).show()
                    navController.navigate("tripDetails/$tripId")
                } else {
                    Toast.makeText(context, "Failed to create trip", Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Text("Create Trip")
        }
    }
}


@Composable
fun TripDetailsView(tripId: String, firestoreRepository: FirestoreRepository) {
    var trip by remember { mutableStateOf<Trip?>(null) }

    LaunchedEffect(key1 = tripId) {
        firestoreRepository.getTripDetails(tripId) { updatedTrip ->
            trip = updatedTrip
        }
    }

    trip?.let { currentTrip ->
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Trip Title: ${currentTrip.title}", style = MaterialTheme.typography.headlineSmall)
            currentTrip.events.forEach { event ->
                EventCard(event)
            }
            Button(onClick = { /* Add Event Logic */ }) {
                Text("Add New Event")
            }
        }
    } ?: Text("Loading Trip Details...", style = MaterialTheme.typography.labelSmall)
}


@Composable
fun DateInput(label: String, dateMillis: Long, onDateChanged: (Long) -> Unit) {
    // This example uses a simple TextField, but you could integrate a date picker dialog
    var dateString by remember {
        mutableStateOf(
            DateFormat.getDateInstance().format(
                java.util.Date(
                    dateMillis
                )
            )
        )
    }
    TextField(value = dateString, onValueChange = {
        dateString = it
        try {
            DateFormat.getDateInstance().parse(it)?.let { it1 -> onDateChanged(it1.time) }
        } catch (e: ParseException) {
            // Handle date parse error if necessary
        }
    }, label = { Text(label) })
}


@Composable
fun EventCard(event: Event) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Event: ${event.title}", style = MaterialTheme.typography.bodyMedium)
            // Include other event details and edit functionalities
        }
    }
}

@Composable
fun TripsListView(
    firestoreRepository: FirestoreRepository, userId: String, onTripSelected: (String) -> Unit
) {
    var trips by remember { mutableStateOf<List<Trip>>(emptyList()) }

    LaunchedEffect(key1 = userId) {
        firestoreRepository.getUserTrips(userId) { updatedTrips ->
            trips = updatedTrips
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(trips) { trip ->
            TripItem(trip, onTripSelected)
        }
    }
}

@Composable
fun TripItem(trip: Trip, onTripSelected: (String) -> Unit) {
    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp)
        .clickable { onTripSelected(trip.id) }) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Trip: ${trip.title}", style = MaterialTheme.typography.headlineSmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ID: ${trip.id}", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.width(8.dp))
            }
        }
    }
}
