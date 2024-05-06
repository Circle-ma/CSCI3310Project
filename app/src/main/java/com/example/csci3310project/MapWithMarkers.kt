package com.example.csci3310project

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PinConfig
import com.google.maps.android.compose.AdvancedMarker
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

fun generateFakeEvents(): List<Event> {
    val events = mutableListOf<Event>()
    val locations = listOf(
        "Central", "Mong Kok", "Causeway Bay", "Tsim Sha Tsui", "Wan Chai", "Sham Shui Po"
    )
//    val locations2 = listOf(
//        "New Kelp City", "Tentacle Acres", "Bottom's Up", "Bass Vegas", "Far-Out-Ville"
//    )

    val currentTimeMillis = System.currentTimeMillis()
    val length = locations.count()
    for (i in 1..length) {
        val event = Event(
            title = "Event $i",
            startTime = currentTimeMillis + (i * 3600000), // Start time 1 hour apart
            endTime = currentTimeMillis + ((i + 1) * 3600000), // End time 1 hour after start
            date = System.currentTimeMillis(),
            location = locations[i - 1] + ", Hong Kong" // Assigning location from the list
        )
        events.add(event)
    }
    return events
}

// Constants
private val DEFAULT_LOCATION = LatLng(22.3193, 114.1694) // Default to Hong Kong
private const val DEFAULT_ZOOM = 10f
@Composable
fun MapWithMarkers(events: List<Event>?, modifier: Modifier) {
    val TAG = "Map"
    var isMapLoaded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val eventsData = if (events == null)
    {
        generateFakeEvents()
    }else{
        events
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM)
    }
    val markerClick: (Marker) -> Boolean = {
        Log.d(TAG, "${it.title} was clicked")
        cameraPositionState.projection?.let { projection ->
            Log.d(TAG, "The current projection is: $projection")
        }
        false
    }
    Box(
        modifier
    ) {
        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = cameraPositionState,
            onMapLoaded = {
                isMapLoaded = true
            },
            googleMapOptionsFactory = {
                GoogleMapOptions().mapId("DEMO_MAP_ID")
            },
        ) {
            CreateMarkers(context, eventsData)
        }
        if (!isMapLoaded) {
            AnimatedVisibility(
                modifier = Modifier
                    .matchParentSize(),
                visible = !isMapLoaded,
                enter = EnterTransition.None,
                exit = fadeOut()
            ) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .background(Color(ContextCompat.getColor(LocalContext.current, R.color.black)))
                        .wrapContentSize()
                )
            }
        }
    }
}

@Composable
fun CreateMarkers(context: Context, eventsData: List<Event>) {
    var markerCount = 1
    eventsData.forEach { event ->
        val location = event.location?.let { getLocationFromAddress(context, it) }
        location?.let {
            val markerState = MarkerState(
                position = it
            )
            val glyphOne = PinConfig.Glyph(markerCount.toString(), android.graphics.Color.BLACK)
            val pinConfig = PinConfig.builder()
                .setGlyph(glyphOne)
                .build()
            markerCount++
            AdvancedMarker(
                state = markerState,
                title = event.title,
                pinConfig = pinConfig
            )
        }
    }
}

// You can use this function to get LatLng from address using Geocoder
fun getLocationFromAddress(context: Context, strAddress: String): LatLng? {
    val maxResults = 1
    val geocoder = Geocoder(context)
    val addresses: List<Address>?
    val geocodeListener = @RequiresApi(33) object : Geocoder.GeocodeListener {
        override fun onGeocode(addresses: MutableList<Address>) {
            // do something with the addresses list
        }
    }

    if (Build.VERSION.SDK_INT >= 33) {
        // declare here the geocodeListener, as it requires Android API 33
//        geocoder.getFromLocationName(strAddress, maxResults, geocodeListener)
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
    } else {
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
    }
    return null
}
