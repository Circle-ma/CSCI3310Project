package com.example.csci3310project

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TripView(
    authController: AuthController,
    firestoreRepository: FirestoreRepository,
    navController: NavController
) {
    var tripTitle by remember { mutableStateOf("") }
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var destination by remember { mutableStateOf("") }
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
        TextField(value = destination,
            onValueChange = { destination = it },
            label = { Text("Destination") })
        DateInput("Start Date", startDate) { newStartDate -> startDate = newStartDate }
        DateInput("End Date", endDate) { newEndDate -> endDate = newEndDate }
        Button(onClick = {
            val trip = Trip(
                title = tripTitle,
                startDate = startDate,
                endDate = endDate,
                destination = destination
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
fun TripDetailsView(
    tripId: String, firestoreRepository: FirestoreRepository, navController: NavController
) {
    var trip by remember { mutableStateOf<Trip?>(null) }
    val context = LocalContext.current

    // Trigger loading the trip details once when the view appears
    LaunchedEffect(key1 = tripId) {
        firestoreRepository.getTripDetails(tripId) { updatedTrip ->
            trip = updatedTrip
        }
    }

    trip?.let { currentTrip ->
        var editableTitle by remember { mutableStateOf(currentTrip.title) }
        var editableDestination by remember { mutableStateOf(currentTrip.destination) }
        var editableStartDate by remember { mutableLongStateOf(currentTrip.startDate) }
        var editableEndDate by remember { mutableLongStateOf(currentTrip.endDate) }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Trip: ${currentTrip.title}",
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                "Join Code: ${currentTrip.joinCode}",
                style = MaterialTheme.typography.bodyLarge,
            )
            MapWithMarkers(
                events = null,
                modifier = Modifier
                        .fillMaxSize()
                        .height(200.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            TextField(value = editableTitle,
                onValueChange = { editableTitle = it },
                label = { Text("Title") },
                singleLine = true
            )
            TextField(value = editableDestination,
                onValueChange = { editableDestination = it },
                label = { Text("Destination") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            DateInput("Start Date", editableStartDate) { newDate -> editableStartDate = newDate }
            DateInput("End Date", editableEndDate) { newDate -> editableEndDate = newDate }
            Spacer(modifier = Modifier.height(20.dp))
            Row {
                Button(
                    onClick = {
                        val updatedTrip = currentTrip.copy(
                            title = editableTitle,
                            destination = editableDestination,
                            startDate = editableStartDate,
                            endDate = editableEndDate
                        )
                        firestoreRepository.updateTrip(tripId, updatedTrip) { isSuccess ->
                            if (isSuccess) {
                                Toast.makeText(context, "Trip updated", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to update trip", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }, modifier = Modifier.weight(1f)
                ) {
                    Text("Save Changes")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        firestoreRepository.deleteTrip(tripId) { isSuccess ->
                            if (isSuccess) {
                                navController.popBackStack()
                                Toast.makeText(context, "Trip deleted", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to delete trip", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Delete Trip")
                }
            }
        }
    } ?: Text("Loading Trip Details...", style = MaterialTheme.typography.headlineSmall)
}


@Composable
fun DateInput(label: String, dateMillis: Long, onDateChanged: (Long) -> Unit) {
    val context = LocalContext.current
    var date by remember { mutableStateOf(Date(dateMillis)) }
    val dateString =
        remember(date) { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date) }

    // This will only create a new dialog when the `date` state changes, not on every recomposition
    val datePickerDialog = remember(date) {
        android.app.DatePickerDialog(
            context, { _, year, month, dayOfMonth ->
                val calendar = Calendar.getInstance()
                calendar.set(year, month, dayOfMonth)
                date = calendar.time
                onDateChanged(calendar.timeInMillis)
            }, date.year + 1900, // DatePickerDialog expects the year to start from 1900
            date.month, date.date
        )
    }

    Column {
        TextField(
            value = dateString,
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                Icon(Icons.Filled.DateRange,
                    contentDescription = "Select Date",
                    modifier = Modifier.clickable { datePickerDialog.show() })
            },
        )
    }
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
            Text("Join Code: ${trip.joinCode}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

