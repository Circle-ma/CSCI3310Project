package com.example.csci3310project

import android.app.TimePickerDialog
import android.util.Log
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.csci3310project.ui.theme.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.lang.Math.min

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
                        Spacer(modifier = Modifier.height(30.dp))

                        val data = mutableMapOf<String, Int>()
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(onClick = { navController.navigate("addExpense/$tripId") }) {
                                Text("Add Expense")
                            }
                            Spacer(modifier = Modifier.width(20.dp))
                            Button(onClick = { navController.navigate("expensesReport/$tripId") }) {
                                Text("View Expenses Report")
                            }
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
            ExpenseTypeIcon(expenseType = expense.type)
            Spacer(modifier = Modifier.width(16.dp))
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
                // if this expense's transactions involve current user, show the text that says "You owe ${amount} to ${creditorName}"
                // if this expense's payer is current user, show the text that says "${creditorName} owes you ${amount}"
                val user = Firebase.auth.currentUser
                val transactions = expense.transactions
//                val userTransactions = transactions.filter { it.debtorName == user?.displayName ?: "" || it.creditorName == user?.displayName ?: "" }
                transactions.forEach { transaction ->
                    val creditorName = transaction.creditorName
                    val debtorName = transaction.debtorName
                    val amount = transaction.amount
                    if (user != null) {
                        if (creditorName == user.displayName && debtorName == user.displayName || expense.isSettled == true) {
                            Text(
                                text = "Settled",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                        }else if (creditorName == user.displayName) {
                            Text(
                                text = "$debtorName owes you $amount",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Teal500
                            )
                        } else if (debtorName == user.displayName) {
                            Text(
                                text = "You owe $amount to $creditorName",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Red
                            )
                        } else {
                            Text(
                                text = "Not involved",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
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
fun ExpenseTypeIcon(expenseType: ExpenseType) {
    when (expenseType) {
        ExpenseType.FOOD -> Icon(imageVector = Icons.Default.Fastfood, contentDescription = "Food", modifier = Modifier.size(48.dp))
        ExpenseType.ENTERTAINMENT -> Icon(imageVector = Icons.Default.MusicNote, contentDescription = "Entertainment", modifier = Modifier.size(48.dp))
        ExpenseType.TRANSPORTATION -> Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = "Transportation", modifier = Modifier.size(48.dp))
        ExpenseType.OTHER -> Icon(imageVector = Icons.Default.Receipt, contentDescription = "Other", modifier = Modifier.size(48.dp))
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
    var expenseType by remember { mutableStateOf(ExpenseType.OTHER) }
    var debtors by remember { mutableStateOf<List<String>>(emptyList()) }
    var debtorAmounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showDebtorDropdown by remember { mutableStateOf(false) }
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
        Text("Add Expense", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        TextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title") })
        DropdownMenuForExpenseType(
            selectedExpenseType = ExpenseType.OTHER,
            onExpenseTypeSelected = { expenseType = it })
        TextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") })
        DateInput("Date", date) { newDate -> date = newDate }
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column (
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                DropdownMenuForPayer(
                    participants = participants,
                    selectedParticipant = payer,
                    onParticipantSelected = { payer = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showDebtorDropdown = true }) {
                    Text("Add Debtor")
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (showDebtorDropdown) {
                    DropdownMenuForDebtor(
                        participants = participants,
                        selectedParticipant = "",
                        onParticipantSelected = { selectedDebtor ->
                            if (selectedDebtor !in debtors) {
                                debtors = debtors + selectedDebtor
                            }
                            showDebtorDropdown = false
                        }
                    )
                }
                debtors.forEach { debtor ->
                    Row {
                        TextField(
                            value = debtorAmounts[debtor] ?: "",
                            onValueChange = { debtorAmounts = debtorAmounts + (debtor to it) },
                            label = { Text("Amount owed by $debtor") }
                        )
                        IconButton(onClick = {
                            debtors = debtors - debtor
                            debtorAmounts = debtorAmounts - debtor
                        }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Debtor", tint = Color.Red)
                        }
                    }
                }
            }
        }
        Button(onClick = {
            val transactions = debtors.map { debtor ->
                ExpenseTransaction(creditorName = payer, debtorName = debtor, amount = debtorAmounts[debtor]?.toDouble() ?: 0.0)
            }
            val newExpense = Expense(
                title = title,
                date = date,
                amount = amount.toDouble(),
                payer = payer,
                type = expenseType,
                transactions = transactions.toMutableList()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuForDebtor(
    participants: List<String>,
    selectedParticipant: String,
    onParticipantSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(selectedParticipant) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = selectedOptionText,
            onValueChange = {},
            label = { Text("Add Debtor") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            participants.forEach { participant ->
                DropdownMenuItem(
                    text = { Text(participant) },
                    onClick = {
                        selectedOptionText = participant
                        onParticipantSelected(participant)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuForPayer(
    participants: List<String>,
    selectedParticipant: String,
    onParticipantSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(selectedParticipant) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = selectedOptionText,
            onValueChange = {},
            label = { Text("Paid by") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            participants.forEach { participant ->
                DropdownMenuItem(
                    text = { Text(participant) },
                    onClick = {
                        selectedOptionText = participant
                        onParticipantSelected(participant)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuForExpenseType(
    selectedExpenseType: ExpenseType,
    onExpenseTypeSelected: (ExpenseType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedOptionText by remember { mutableStateOf(selectedExpenseType.name) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        TextField(
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = selectedOptionText,
            onValueChange = {},
            label = { Text("Expense Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ExpenseType.values().forEach { expenseType ->
                DropdownMenuItem(
                    text = { Text(expenseType.name) },
                    onClick = {
                        selectedOptionText = expenseType.name
                        onExpenseTypeSelected(expenseType)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
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
    var participants by remember { mutableStateOf<List<String>>(emptyList()) }
    var expense by remember { mutableStateOf<Expense?>(null) }
    var title by remember { mutableStateOf("") }
    var date by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var amount by remember { mutableStateOf("") }
    var payer by remember { mutableStateOf("") }
    var expenseType by remember { mutableStateOf(ExpenseType.OTHER) }
    var debtors by remember { mutableStateOf<List<String>>(emptyList()) }
    var debtorAmounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showDebtorDropdown by remember { mutableStateOf(false) }
    var originalExpense by remember { mutableStateOf<Expense?>(null) }
    val context = LocalContext.current

    LaunchedEffect(key1 = expenseId) {
        firestoreRepository.getTripParticipants(tripId) { fetchedParticipants ->
            participants = fetchedParticipants
        }

        firestoreRepository.getExpenseDetails(tripId, expenseId) { fetchedExpense ->
            expense = fetchedExpense
            if (fetchedExpense != null) {
                title = fetchedExpense.title
                date = fetchedExpense.date
                amount = fetchedExpense.amount.toString()
                payer = fetchedExpense.payer
                expenseType = fetchedExpense.type
                debtors = fetchedExpense.transactions.map { it.debtorName }
                debtorAmounts = fetchedExpense.transactions.associate { it.debtorName to it.amount.toString() }
                originalExpense = fetchedExpense
            }
        }
    }

    expense?.let {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Edit Expense", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            TextField(value = title, onValueChange = { title = it }, label = { Text("Expense Title") })
            DropdownMenuForExpenseType(
                selectedExpenseType = expenseType,
                onExpenseTypeSelected = { expenseType = it })
            TextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") })
            DateInput("Date", date) { newDate -> date = newDate }
            Spacer(modifier = Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column (
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ){
                    DropdownMenuForPayer(
                        participants = participants,
                        selectedParticipant = payer,
                        onParticipantSelected = { payer = it }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showDebtorDropdown = true }) {
                        Text("Add Debtor")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (showDebtorDropdown) {
                        DropdownMenuForDebtor(
                            participants = participants,
                            selectedParticipant = "",
                            onParticipantSelected = { selectedDebtor ->
                                if (selectedDebtor !in debtors) {
                                    debtors = debtors + selectedDebtor
                                }
                                showDebtorDropdown = false
                            }
                        )
                    }
                    debtors.forEach { debtor ->
                        Row {
                            TextField(
                                value = debtorAmounts[debtor] ?: "",
                                onValueChange = { debtorAmounts = debtorAmounts + (debtor to it) },
                                label = { Text("Amount owed by $debtor") }
                            )
                            IconButton(onClick = {
                                debtors = debtors - debtor
                                debtorAmounts = debtorAmounts - debtor
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Debtor", tint = Color.Red)
                            }
                        }
                    }
                }
            }
            Button(onClick = {
                val transactions = debtors.map { debtor ->
                    ExpenseTransaction(creditorName = payer, debtorName = debtor, amount = debtorAmounts[debtor]?.toDouble() ?: 0.0)
                }
                val updatedExpense = expense?.copy(
                    title = title,
                    date = date,
                    amount = amount.toDouble(),
                    payer = payer,
                    type = expenseType,
                    transactions = transactions.toMutableList()
                )
                updatedExpense?.let {
                    originalExpense?.let { it1 ->
                        firestoreRepository.updateExpenseInTrip(tripId, it1, it) { isSuccess ->
                            if (isSuccess) {
                                Toast.makeText(context, "Expense updated", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            } else {
                                Toast.makeText(context, "Failed to update expense", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Save Changes")
            }
        }
    } ?: Text("Loading Expense Details...", style = MaterialTheme.typography.headlineSmall)
}

@Composable
fun PieChart(
    data: Map<String, Int>,
    radiusOuter: Dp = 140.dp,
    chartBarWidth: Dp = 35.dp,
    animDuration: Int = 1000,
) {

    val totalSum = data.values.sum()
    val floatValue = mutableListOf<Float>()

    data.values.forEachIndexed { index, values ->
        floatValue.add(index, 360 * values.toFloat() / totalSum.toFloat())
    }

    val colors = listOf(
        Purple200,
        Purple500,
        Teal200,
        Purple700,
        Blue
    )

    var animationPlayed by remember { mutableStateOf(false) }

    var lastValue = 0f

    val animateSize by animateFloatAsState(
        targetValue = if (animationPlayed) radiusOuter.value * 2f else 0f,
        animationSpec = tween(
            durationMillis = animDuration,
            delayMillis = 0,
            easing = LinearOutSlowInEasing
        )
    )

    val animateRotation by animateFloatAsState(
        targetValue = if (animationPlayed) 90f * 11f else 0f,
        animationSpec = tween(
            durationMillis = animDuration,
            delayMillis = 0,
            easing = LinearOutSlowInEasing
        )
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(animateSize.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(radiusOuter * 2f)
                    .rotate(animateRotation)
            ) {
                floatValue.forEachIndexed { index, value ->
                    drawArc(
                        color = colors[index],
                        lastValue,
                        value,
                        useCenter = false,
                        style = Stroke(chartBarWidth.toPx(), cap = StrokeCap.Butt)
                    )
                    lastValue += value
                }
            }
        }
        DetailsPieChart(
            data = data,
            colors = colors
        )

    }

}

@Composable
fun DetailsPieChart(
    data: Map<String, Int>,
    colors: List<Color>
) {
    Column(
        modifier = Modifier
            .padding(top = 80.dp)
            .fillMaxWidth()
    ) {
        data.values.forEachIndexed { index, value ->
            DetailsPieChartItem(
                data = Pair(data.keys.elementAt(index), value),
                color = colors[index]
            )
        }

    }
}

@Composable
fun DetailsPieChartItem(
    data: Pair<String, Int>,
    height: Dp = 45.dp,
    color: Color
) {

    Surface(
        modifier = Modifier
            .padding(vertical = 10.dp, horizontal = 40.dp),
        color = Color.Transparent
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .background(
                        color = color,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .size(height)
            )

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.padding(start = 15.dp),
                    text = data.first,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    color = Color.Black
                )
                Text(
                    modifier = Modifier.padding(start = 15.dp),
                    text = data.second.toString(),
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    color = Color.Gray
                )
            }

        }

    }
}

@Composable
fun ExpenseReportView(
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
                    "Trip: ${currentTrip.title} (${currentTrip.destination}) Expenses Report",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Period: ${formatDate(currentTrip.startDate)} - ${formatDate(currentTrip.endDate)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(30.dp))

                val data = mutableMapOf<String, Int>()
                currentTrip.expenses.forEach { expense ->
                    data[expense.type.name] = data.getOrDefault(expense.type.name, 0) + expense.amount.toInt()
                }
                PieChart(data = data)

                // Use trip.transactions to calculate the total balance of the trip, and display the result for who owes who how much using Card
                val transactions = currentTrip.transactions
                val balance = mutableMapOf<String, Double>()
                transactions.forEach { transaction ->
                    val creditorName = transaction.creditorName
                    val debtorName = transaction.debtorName
                    val amount = transaction.amount
                    balance[creditorName] = (balance[creditorName] ?: 0.0) + amount
                    balance[debtorName] = (balance[debtorName] ?: 0.0) - amount
                }

                val debts = mutableListOf<String>()
                balance.forEach { (name, amount) ->
                    if (amount < 0) {
                        val debtors = balance.filter { it.value > 0 && it.value >= -amount }
                        for (debtor in debtors) {
                            val payment = min(debtor.value, -amount)
                            debts.add("$name owes ${debtor.key} $payment")
                            balance[debtor.key] = debtor.value - payment
                            balance[name] = amount + payment
                            if (balance[name] == 0.0) break
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Trip Balance", style = MaterialTheme.typography.headlineSmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        debts.forEach { debt ->
                            Text(
                                text = debt,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }   
            }
        }
    } ?: Text("Loading Expenses Report...", style = MaterialTheme.typography.headlineSmall)
}
