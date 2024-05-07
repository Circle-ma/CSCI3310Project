package com.example.csci3310project

import android.app.TimePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
                    // Navigate to TripDetailsView and remove only this TripView from the back stack
                    navController.navigate("tripDetails/$tripId") {
                        // Pop only the TripView off the stack
                        popUpTo(navController.currentBackStackEntry?.destination?.route ?: "tripView") {
                            inclusive = true // This ensures TripView is removed from the stack
                        }
                    }
                } else {
                    Toast.makeText(context, "Failed to create trip", Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Text("Create Trip")
        }

    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TripDetailsView(
    tripId: String, firestoreRepository: FirestoreRepository, navController: NavController
) {
    var trip by remember { mutableStateOf<Trip?>(null) }
    val context = LocalContext.current

    // Fetch trip details once when the view appears
    LaunchedEffect(key1 = tripId) {
        firestoreRepository.getTripDetails(tripId) { updatedTrip ->
            trip = updatedTrip
        }
    }

    trip?.let { currentTrip ->
        val tripStartDate =
            Instant.ofEpochMilli(currentTrip.startDate).atZone(ZoneId.systemDefault()).toLocalDate()
        LazyColumn(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
        ) {
            item {
                Text(
                    "Trip: ${currentTrip.title} (${currentTrip.destination})",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Period: ${formatDate(currentTrip.startDate)} - ${formatDate(currentTrip.endDate)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    onClick = { navController.navigate("editTrip/$tripId") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Edit Trip Details")
                }
                MapWithMarkers(
                    events = currentTrip.events,
                    modifier = Modifier
                        .fillMaxSize()
                        .height(200.dp),
                    destination = currentTrip.destination
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Grouping events by LocalDate and sorting by start time within each day
            val groupedEvents = currentTrip.events.groupBy { event ->
                Instant.ofEpochMilli(event.date).atZone(ZoneId.systemDefault()).toLocalDate()
            }.mapValues { (_, events) ->
                events.sortedBy { it.startTime }
            }.toList().sortedBy { it.first }  // Sort by LocalDate to ensure order

            groupedEvents.forEach { (date, events) ->
                val dayNumber =
                    ChronoUnit.DAYS.between(tripStartDate, date) + 1  // Calculate the day number
                item {
                    Text(
                        "Day $dayNumber: ${date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
                items(events) { event ->
                    EventItem(event = event,
                        onEdit = { navController.navigate("editEvent/${event.id}/${tripId}") },
                        onDelete = {
                            firestoreRepository.deleteEventFromTrip(tripId, event.id) { isSuccess ->
                                if (isSuccess) {
                                    Toast.makeText(context, "Event deleted", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    Toast.makeText(
                                        context, "Failed to delete event", Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        })
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = { navController.navigate("addEvent/$tripId") }) {
                    Text("Add Event")
                }
                Spacer(modifier = Modifier.height(20.dp))
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
                    }, modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Trip")
                }
            }
        }
    } ?: Text("Loading Trip Details...", style = MaterialTheme.typography.headlineSmall)
}


@Composable
fun EventItem(event: Event, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title, style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "From ${formatTime(event.startTime)} to ${formatTime(event.endTime)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${formatDate(event.date)}, ${event.location}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Travel by: ${event.travelMethod.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onEdit() }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Event")
            }
            IconButton(onClick = { onDelete() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Event",
                    tint = Color.Red
                )
            }
        }
    }
}

fun formatTime(timeMillis: Long): String {
    val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return formatter.format(Date(timeMillis))
}

@Composable
fun AddEventView(
    tripId: String, firestoreRepository: FirestoreRepository, navController: NavController
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var startTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var endTime by remember { mutableLongStateOf(System.currentTimeMillis() + 3600000) } // +1 hour
    var location by remember { mutableStateOf("") }
    var travelMethod by remember { mutableStateOf(TravelMethod.WALK) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(value = title, onValueChange = { title = it }, label = { Text("Event Title") })
        DateInput("Event Date", date) { newDate -> date = newDate }
        TimeInput("Start Time", startTime) { newStartTime -> startTime = newStartTime }
        TimeInput("End Time", endTime) { newEndTime -> endTime = newEndTime }
        TextField(value = location, onValueChange = { location = it }, label = { Text("Location") })
        DropdownMenuForTravelMethod(travelMethod) { newMethod -> travelMethod = newMethod }

        Button(onClick = {
            val newEvent = Event(
                title = title,
                date = date,
                startTime = startTime,
                endTime = endTime,
                location = location,
                travelMethod = travelMethod
            )
            firestoreRepository.addEventToTrip(tripId, newEvent) { isSuccess ->
                if (isSuccess) {
                    Toast.makeText(context, "Event added", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                } else {
                    Toast.makeText(context, "Error adding event", Toast.LENGTH_SHORT).show()
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Add Event")
        }
    }
}

@Composable
fun DropdownMenuForTravelMethod(selectedMethod: TravelMethod, onSelect: (TravelMethod) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val methods = TravelMethod.entries.toTypedArray()  // Correct way to get all values from an enum

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopStart)
    ) {
        Text(
            text = "Travel method: $selectedMethod",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true },
            textAlign = TextAlign.Left
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            methods.forEach { method ->
                DropdownMenuItem({ Text(text = method.name) }, onClick = {
                    onSelect(method)
                    expanded = false
                })
            }
        }
    }
}


@Composable
fun EditEventView(
    eventId: String,
    tripId: String,
    firestoreRepository: FirestoreRepository,
    navController: NavController
) {
    var event by remember { mutableStateOf<Event?>(null) }
    val context = LocalContext.current

    LaunchedEffect(key1 = eventId) {
        firestoreRepository.getEventDetails(tripId, eventId) { fetchedEvent ->
            event = fetchedEvent
        }
    }

    event?.let { currentEvent ->
        var title by remember { mutableStateOf(currentEvent.title) }
        var date by remember { mutableLongStateOf(currentEvent.date) }
        var startTime by remember { mutableLongStateOf(currentEvent.startTime) }
        var endTime by remember { mutableLongStateOf(currentEvent.endTime) }
        var location by remember { mutableStateOf(currentEvent.location ?: "") }
        var travelMethod by remember { mutableStateOf(currentEvent.travelMethod) }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(value = title,
                onValueChange = { title = it },
                label = { Text("Event Title") })
            DateInput("Event Date", date) { newDate -> date = newDate }
            TimeInput("Start Time", startTime) { newStartTime -> startTime = newStartTime }
            TimeInput("End Time", endTime) { newEndTime -> endTime = newEndTime }
            TextField(value = location,
                onValueChange = { location = it },
                label = { Text("Location") })
            DropdownMenuForTravelMethod(travelMethod) { newMethod -> travelMethod = newMethod }

            Button(onClick = {
                val updatedEvent = currentEvent.copy(
                    title = title,
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    travelMethod = travelMethod
                )
                firestoreRepository.updateEventInTrip(tripId, updatedEvent) { isSuccess ->
                    if (isSuccess) {
                        Toast.makeText(context, "Event updated", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } else {
                        Toast.makeText(context, "Failed to update event", Toast.LENGTH_SHORT).show()
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Save Changes")
            }
        }
    } ?: Text("Loading Event Details...", style = MaterialTheme.typography.headlineSmall)
}

@Composable
fun TimeInput(label: String, timeMillis: Long, onTimeChanged: (Long) -> Unit) {
    val context = LocalContext.current
    var time by remember { mutableStateOf(Date(timeMillis)) }
    val timeString = remember(time) { SimpleDateFormat("h:mm a", Locale.getDefault()).format(time) }

    val timePickerDialog = remember(time) {
        TimePickerDialog(
            context, { _, hourOfDay, minute ->
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                time = calendar.time
                onTimeChanged(calendar.timeInMillis)
            }, time.hours, time.minutes, false
        )
    }

    TextField(
        value = timeString,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        trailingIcon = {
            Icon(Icons.Filled.AccessTime,
                contentDescription = "Select Time",
                modifier = Modifier.clickable { timePickerDialog.show() })
        },
    )
}


fun formatDate(timestamp: Long): String {
    // Use a date formatter to convert timestamp to a human-readable format
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return formatter.format(Date(timestamp))
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
fun EditTripDetailsView(
    tripId: String, firestoreRepository: FirestoreRepository, navController: NavController
) {
    var trip by remember { mutableStateOf<Trip?>(null) }
    val context = LocalContext.current

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
            TextField(value = editableTitle,
                onValueChange = { editableTitle = it },
                label = { Text("Title") })
            TextField(value = editableDestination,
                onValueChange = { editableDestination = it },
                label = { Text("Destination") })
            DateInput("Start Date", editableStartDate) { newDate -> editableStartDate = newDate }
            DateInput("End Date", editableEndDate) { newDate -> editableEndDate = newDate }

            Spacer(modifier = Modifier.height(20.dp))
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
                            navController.popBackStack()
                            Toast.makeText(context, "Trip updated successfully", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            Toast.makeText(context, "Failed to update trip", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }, modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Changes")
            }
        }
    } ?: Text("Loading Trip Details...", style = MaterialTheme.typography.bodyLarge)
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
            Text("Destination: ${trip.destination}", style = MaterialTheme.typography.bodySmall)
            Text("Join Code: ${trip.joinCode}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

