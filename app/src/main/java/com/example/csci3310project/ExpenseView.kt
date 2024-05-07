package com.example.csci3310project

import android.app.TimePickerDialog
import android.widget.Toast
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ExpenseDetailsView(
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
                LazyColumn(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    item {
                        Text(
                            "Trip: ${currentTrip.title} (${currentTrip.destination}) Expenses",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Text(
                            "Period: ${formatDate(currentTrip.startDate)} - ${formatDate(currentTrip.endDate)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    items(currentTrip.expenses) { expense ->
                        ExpenseItem(expense = expense,
                            onExpenseEdit = {
                                navController.navigate("editExpense/${expense.id}/$tripId")
                            },
                            onExpenseDelete = {
                                firestoreRepository.deleteExpenseFromTrip(tripId, expense.id) { isSuccess ->
                                    if (isSuccess) {
                                        Toast.makeText(context, "Expense deleted", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to delete expense", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
        
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(onClick = { navController.navigate("addExpense/$tripId") }) {
                            Text("Add Expense")
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            } ?: Text("Loading Expenses Details...", style = MaterialTheme.typography.headlineSmall)
}

@Composable
fun ExpenseItem(expense: Expense, onExpenseEdit: () -> Unit, onExpenseDelete: () -> Unit) {
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
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = expense.title, style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "Amount: ${expense.amount}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    // text = "Date: ${formatDate(expense.date)}",
                    text = "Date: ${formatDate(expense.date)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                // Text(
                //     text = "Payer: ${expense.payer}",
                //     style = MaterialTheme.typography.bodyMedium
                // )
                //    Text(
                //        text = "Category: ${expense.category}",
                //        style = MaterialTheme.typography.bodyMedium
                //    )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { onExpenseEdit() }) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Expense")
            }
            IconButton(onClick = { onExpenseDelete() }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Expense",
                    tint = Color.Red
                )
            }
        }
   }
}

@Composable
fun AddExpenseView(
   tripId: String, firestoreRepository: FirestoreRepository, navController: NavController
) {
    var participants by remember { mutableStateOf<List<String>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var amount by remember { mutableStateOf("") }
    var payer by remember { mutableStateOf("") }
    val context = LocalContext.current

        LaunchedEffect(key1 = tripId) {
            firestoreRepository.getTripParticipants(tripId) { fetchedParticipants ->
                participants = fetchedParticipants
            }
        }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title") })
        TextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") })
        DateInput("Date", date) { newDate -> date = newDate }
        DropdownMenuForParticipants(
            participants = participants,
            selectedParticipant = participants.firstOrNull() ?: "No User",
            onParticipantSelected = { payer = it }
        )

        Button(onClick = {
            val newExpense = Expense(
                title = title,
                date = date,
                amount = amount.toDouble(),
                payer = payer
            )
            firestoreRepository.addExpenseToTrip(tripId, newExpense) { isSuccess ->
                if (isSuccess) {
                    Toast.makeText(context, "Expense added", Toast.LENGTH_SHORT).show()
                    navController.popBackStack()
                } else {
                    Toast.makeText(context, "Error adding expense", Toast.LENGTH_SHORT).show()
                }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Add Expense")
        }
    }
}

@Composable
fun DropdownMenuForParticipants(
   participants: List<String>,
   selectedParticipant: String,
   onParticipantSelected: (String) -> Unit
) {
   var expanded by remember { mutableStateOf(false) }

   Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopStart)
    )  {
       Text(
           text = "Payer: $selectedParticipant",
           modifier = Modifier
               .clickable { expanded = true }
               .padding(16.dp)
       )
       DropdownMenu(
           expanded = expanded,
           onDismissRequest = { expanded = false }
       ) {
           participants.forEach { participant ->
               DropdownMenuItem({ Text(text = participant) }, onClick = {
                   onParticipantSelected(participant)
                   expanded = false
               })
           }
       }
   }
}

@Composable
fun EditExpenseView(
    expenseId: String,
    tripId: String,
    firestoreRepository: FirestoreRepository,
    navController: NavController
) {
    var payer by remember { mutableStateOf("") }
    var participants by remember { mutableStateOf<List<String>>(emptyList()) }
    var expense by remember { mutableStateOf<Expense?>(null) }
    val context = LocalContext.current

    LaunchedEffect(key1 = expenseId) {
        firestoreRepository.getTripParticipants(tripId) { fetchedParticipants ->
            participants = fetchedParticipants
        }

        firestoreRepository.getExpenseDetails(tripId, expenseId) { fetchedExpense ->
            expense = fetchedExpense
        }
    }

    expense?.let { currentExpense ->
        var title by remember { mutableStateOf(currentExpense.title) }
        var date by remember { mutableLongStateOf(currentExpense.date) }
        var amount by remember { mutableStateOf(currentExpense.amount.toString()) }
        var payer by remember { mutableStateOf(currentExpense.payer) }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(value = title,
                onValueChange = { title = it },
                label = { Text("Expense Title") })
            TextField(value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") })
            DateInput("Date", date) { newDate -> date = newDate }

            DropdownMenuForParticipants(
                participants = participants,
                selectedParticipant = participants.firstOrNull() ?: "No User",
                onParticipantSelected = { payer = it }
            )

            Button(onClick = {
                val updatedExpense = currentExpense.copy(
                    title = title,
                    date = date,
                    amount = amount.toDouble(),
                    payer = payer
                )
                firestoreRepository.updateExpenseInTrip(tripId, updatedExpense) { isSuccess ->
                    if (isSuccess) {
                        Toast.makeText(context, "Expense updated", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } else {
                        Toast.makeText(context, "Failed to update expense", Toast.LENGTH_SHORT).show()
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Save Changes")
            }
        }
    } ?: Text("Loading Expense Details...", style = MaterialTheme.typography.headlineSmall)
}