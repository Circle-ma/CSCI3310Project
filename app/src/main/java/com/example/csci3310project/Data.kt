package com.example.csci3310project

import java.util.UUID

data class User(val name: String)
data class Trip(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startDate: Long, // Using timestamps for dates
    val endDate: Long,
    val events: List<Event> = listOf(),
    val participants: List<String> = listOf() // User IDs of participants
)

data class Event(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String?
)
