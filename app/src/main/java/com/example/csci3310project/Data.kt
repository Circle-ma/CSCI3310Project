package com.example.csci3310project

import java.util.UUID

data class User(val id: String, val name: String)

data class Trip(
    var id: String = UUID.randomUUID().toString(),
    var joinCode: String = generateJoinCode(), // New attribute for join code
    val title: String = "",
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis(),
    val events: MutableList<Event> = mutableListOf(),
    var expenses: MutableList<Expense> = mutableListOf(),
    var participantsID: MutableList<String> = mutableListOf(),
    var participants: MutableList<String> = mutableListOf(),
    val destination: String = "",
    var transactions: MutableList<ExpenseTransaction> = mutableListOf()
)

fun generateJoinCode(): String {
    val allowedChars = ('A'..'Z') + ('0'..'9')
    return (1..6).map { allowedChars.random() }.joinToString("")
}


enum class TravelMethod {
    WALK, CAR, PUBLIC_TRANSPORT
}

data class Event(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var date: Long = System.currentTimeMillis(),
    var startTime: Long = System.currentTimeMillis(),
    var endTime: Long = System.currentTimeMillis() + 3600000, // Default to one hour later
    var location: String? = null,
    var travelMethod: TravelMethod = TravelMethod.WALK // Assume WALK as default
)

data class ExpenseTransaction(
    val creditorName: String = "",
    val debtorName: String = "",
    var amount: Double = 0.0
)

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var date: Long = System.currentTimeMillis(),
    var amount: Double = 0.0,
    var payer: String = "",
    var type: ExpenseType = ExpenseType.OTHER,
    var transactions: MutableList<ExpenseTransaction> = mutableListOf(),
    var isSettled: Boolean = false
)

enum class ExpenseType {
    FOOD, ENTERTAINMENT, TRANSPORTATION, OTHER
}
