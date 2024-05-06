package com.example.csci3310project

import java.util.UUID

data class User(val id: String, val name: String)

data class Trip(
    var id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val startDate: Long = System.currentTimeMillis(),  // Using current time as default
    val endDate: Long = System.currentTimeMillis(),
    val events: MutableList<Event> = mutableListOf(),
    var participants: MutableList<String> = mutableListOf()
)


data class Event(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String?
)
