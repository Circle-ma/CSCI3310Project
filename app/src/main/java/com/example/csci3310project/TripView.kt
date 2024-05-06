package com.example.csci3310project

import FirestoreRepository
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.text.ParseException

@Composable
fun TripView(firestoreRepository: FirestoreRepository) {
    var tripTitle by remember { mutableStateOf("") }
    var startDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var endDate by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = tripTitle,
            onValueChange = { tripTitle = it },
            label = { Text("Trip Title") }
        )
        Spacer(modifier = Modifier.height(16.dp))
        DateInput("Start Date", startDate) { newStartDate -> startDate = newStartDate }
        Spacer(modifier = Modifier.height(16.dp))
        DateInput("End Date", endDate) { newEndDate -> endDate = newEndDate }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val trip = Trip(
                title = tripTitle,
                startDate = startDate,
                endDate = endDate
            )
            firestoreRepository.addTrip(trip) { isSuccess ->
                if (isSuccess) {
                    Toast.makeText(context, "Trip added successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to add trip", Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Text("Create Trip")
        }
    }
}

@Composable
fun DateInput(label: String, dateMillis: Long, onDateChanged: (Long) -> Unit) {
    // This example uses a simple TextField, but you could integrate a date picker dialog
    var dateString by remember { mutableStateOf(DateFormat.getDateInstance().format(
        java.util.Date(
            dateMillis
        )
    )) }
    TextField(
        value = dateString,
        onValueChange = {
            dateString = it
            try {
                DateFormat.getDateInstance().parse(it)?.let { it1 -> onDateChanged(it1.time) }
            } catch (e: ParseException) {
                // Handle date parse error if necessary
            }
        },
        label = { Text(label) }
    )
}
