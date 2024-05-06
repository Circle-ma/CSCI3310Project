package com.example.csci3310project

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.util.Locale

fun generateFakeEvents(): List<Event> {
    val events = mutableListOf<Event>()
    val locations = listOf(
        "Central", "Mong Kok", "Causeway Bay", "Tsim Sha Tsui", "Wan Chai", "Sham Shui Po"
    )

    val currentTimeMillis = System.currentTimeMillis()
    for (i in 1..6) {
        val event = Event(
            title = "Event $i",
            startTime = currentTimeMillis + (i * 3600000), // Start time 1 hour apart
            endTime = currentTimeMillis + ((i + 1) * 3600000), // End time 1 hour after start
            date = System.currentTimeMillis(),
            location = locations[i - 1] // Assigning location from the list
        )
        events.add(event)
    }
    return events
}

// Constants
private val DEFAULT_LOCATION = LatLng(22.3193, 114.1694) // Default to Hong Kong
private const val DEFAULT_ZOOM = 10f
@Composable
fun MapWithMarkers(events: List<Event>?) {
    val context = LocalContext.current
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM)
    }
    val eventsData = if (events == null)
    {
        generateFakeEvents()
    }else{
        events
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        eventsData.forEach { event ->
            val location = event.location?.let { getLocationFromAddress(context, it) }
            location?.let {
                MarkerState(
                    position = it
                )
            }?.let {
                Marker(
                    state = it,
                    title = event.title
                )
            }
        }
    }
}

// You can use this function to get LatLng from address using Geocoder
fun getLocationFromAddress(context: Context, strAddress: String): LatLng? {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses: List<Address>?
    try {
        addresses = geocoder.getFromLocationName(strAddress, 1)
        if (addresses == null || addresses.isEmpty()) {
            return null
        }
        val location = addresses[0]
        return LatLng(location.latitude, location.longitude)
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
