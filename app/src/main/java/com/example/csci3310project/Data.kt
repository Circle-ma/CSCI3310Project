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
    var participants: MutableList<String> = mutableListOf(),
    val destination: String = ""
)

fun generateJoinCode(): String {
    val allowedChars = ('A'..'Z') + ('0'..'9')
    return (1..6).map { allowedChars.random() }.joinToString("")
}



data class Event(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: Long,
    val startTime: Long,
    val endTime: Long,
    val location: String?
)
