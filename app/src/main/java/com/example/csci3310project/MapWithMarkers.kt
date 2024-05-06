package com.example.csci3310project

import android.content.Context
import android.location.Address
import android.location.Geocoder
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
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
            location = locations[i - 1] // Assigning location from the list
        )
        events.add(event)
    }
    return events
}

@Composable
fun MapWithMarkers(events: List<Event>?) {
    val context = LocalContext.current

    val eventsData = if (events == null)
    {
        generateFakeEvents()
    }else{
        events
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val mapView = MapView(context).apply {
                // Initialize Google Maps
                onCreate(null)
                getMapAsync { googleMap ->
                    // Add markers for each event location
                    eventsData.forEach { event ->
                        val location = event.location?.let { getLocationFromAddress(context, it) }
                        location?.let {
                            val latLng = LatLng(it.latitude, it.longitude)
                            googleMap.addMarker(
                                MarkerOptions()
                                    .position(latLng)
                                    .title(event.location)
                            )
                        }
                    }

                    // Move camera to the first marker
                    val firstEvent = eventsData.firstOrNull()
                    firstEvent?.let {
                        val firstLocation =
                            it.location?.let { it1 -> getLocationFromAddress(context, it1) }
                        firstLocation?.let {
                            val latLng = LatLng(it.latitude, it.longitude)
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
                        }
                    }
                }
            }
            mapView
        }
    )
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
