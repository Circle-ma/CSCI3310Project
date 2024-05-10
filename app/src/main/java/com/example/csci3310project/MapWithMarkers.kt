package com.example.csci3310project

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PinConfig
import com.google.maps.android.compose.AdvancedMarker
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import java.time.Instant
import java.time.ZoneId

fun generateFakeEvents(): List<Event> {
    val events = mutableListOf<Event>()
    val locations = listOf(
        "Central", "Mong Kok", "Causeway Bay", "Tsim Sha Tsui", "Wan Chai", "Sham Shui Po"
    )

    val currentTimeMillis = System.currentTimeMillis()
    val length = locations.count()
    for (i in 1..length) {
        val event = Event(
            title = "Event $i",
            startTime = currentTimeMillis + (i * 3600000), // Start time 1 hour apart
            endTime = currentTimeMillis + ((i + 1) * 3600000), // End time 1 hour after start
            date = System.currentTimeMillis(),
            travelMethod = TravelMethod.PUBLIC_TRANSPORT,
            location = locations[i - 1] + ", Hong Kong" // Assigning location from the list
        )

        events.add(event)
    }
    return events
}

// Constants
private const val DEFAULT_ZOOM = 12f

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MapWithMarkers(destination: String, events: List<Event>?, modifier: Modifier, selectEventId: MutableState<String?>) {
    val TAG = "Map"
    var isMapLoaded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val eventsData = if (events == null)
    {
        generateFakeEvents()
    }else{
        events
    }
    val locations = remember { mutableListOf<LatLng>() }
    val points = remember{ mutableListOf<LatLng>() }
    val bounds = calculateCameraBounds(locations)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(bounds.center, DEFAULT_ZOOM)
    }
    LaunchedEffect(key1 = eventsData) {
        eventsData.forEach { event ->
            val location = event.location?.let { getLocationFromAddress(context, it, destination) }
            location?.let {
                locations.add(it)
            }
        }
        locations.zip(eventsData).windowed(2) { pair: List<Pair<LatLng, Event>> ->
            getDirection(pair[0].first, pair[1].first, pair[1].second.travelMethod.toString(), (pair[1].second.startTime / 1000).toInt()) { it: DirectionsResponse? ->
                Log.d(TAG, it.toString())
                if (it != null && it.routes.isNotEmpty()) {
                    val pointList = decodePolyline(it.routes.first().overview_polyline.points)
                    if(pointList.isNotEmpty())
                    {
                        points.addAll(pointList)
                    }
                }
            }
        }
        val newBounds = calculateCameraBounds(locations)
        cameraPositionState.position = CameraPosition.fromLatLngZoom(newBounds.center, DEFAULT_ZOOM)
    }
    if(selectEventId.value != null)
    {
        val e = eventsData.find{it.id == selectEventId.value}
        cameraPositionState.position = e?.location?.let { getLocationFromAddress(context, it, destination) }
            ?.let { CameraPosition.fromLatLngZoom(it, DEFAULT_ZOOM) }!!
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
            CreateMarkers(eventsData, locations)

            Polyline(
                points = points.toList(),
                clickable = true,
                color = Color.Blue,
                width = 5f,
                onClick = { polyline ->
                    // Handle polyline click event
                }
            )
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
                        .background(
                            Color(
                                ContextCompat.getColor(
                                    LocalContext.current,
                                    R.color.black
                                )
                            )
                        )
                        .wrapContentSize()
                )
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CreateMarkers(eventsData: List<Event>, locations: List<LatLng>) {
    var markerCount = 1
    val COLOR_LIST = listOf(
        Color(ContextCompat.getColor(LocalContext.current, R.color.red)),
        Color(ContextCompat.getColor(LocalContext.current, R.color.blue)),
        Color(ContextCompat.getColor(LocalContext.current, R.color.yellow)),
        Color(ContextCompat.getColor(LocalContext.current, R.color.green)),
        Color(ContextCompat.getColor(LocalContext.current, R.color.purple_200)),
        Color(ContextCompat.getColor(LocalContext.current, R.color.light_green)),
        Color(ContextCompat.getColor(LocalContext.current, R.color.purple_700)),
    )
    val colorMap: MutableMap<String, Color> = mutableMapOf()

    eventsData.zip(locations).forEach { (event, location) ->
        if (location != null) {
            val markerState = MarkerState(
                position = location
            )
            val date = Instant.ofEpochMilli(event.date).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            val markerColor = if (colorMap.containsKey(date))
            {
                colorMap[date]
            }else{
                colorMap[date] = COLOR_LIST[colorMap.count()%7]
                colorMap[date]
            }
            val glyphOne = PinConfig.Glyph(markerCount.toString(), android.graphics.Color.BLACK)
            val pinConfig = PinConfig.builder()
                .setBackgroundColor(markerColor!!.toArgb())
                .setBorderColor(android.graphics.Color.WHITE)
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
fun getLocationFromAddress(context: Context, strAddress: String, destination: String): LatLng? {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("LocationCache", Context.MODE_PRIVATE)
    val cachedLocation = sharedPreferences.getString(strAddress, null)

    if (cachedLocation != null) {
        val latLng = cachedLocation.split(",")
        return LatLng(latLng[0].toDouble(), latLng[1].toDouble())
    }

    val geocoder = Geocoder(context)
    val addresses: List<Address>?

    try {
        addresses = geocoder.getFromLocationName(strAddress + " ,$destination", 1)
        if (addresses == null || addresses.isEmpty()) {
            return null
        }
        val location = addresses[0]
        val latLng = LatLng(location.latitude, location.longitude)

        with(sharedPreferences.edit()) {
            putString(strAddress, "${latLng.latitude},${latLng.longitude}")
            apply()
        }

        return latLng
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return null
}

fun calculateCameraBounds(locations: List<LatLng>): LatLngBounds {
    val builder = LatLngBounds.Builder()
    if(locations.isEmpty())
    {
        builder.include(LatLng(22.396428, 114.109497))
    }
    locations.forEach { builder.include(it) }
    val bounds = builder.build()

    return bounds
}

fun decodePolyline(encodedPath: String): List<LatLng> {
    val poly = mutableListOf<LatLng>()
    var index = 0
    val len = encodedPath.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var result = 1
        var shift = 0
        var b: Int
        do {
            b = encodedPath[index++].toInt() - 63 - 1
            result += b shl shift
            shift += 5
        } while (b >= 0x1f)
        lat += if (result and 1 != 0) (result shr 1).inv() else result shr 1
        result = 1
        shift = 0
        do {
            b = encodedPath[index++].toInt() - 63 - 1
            result += b shl shift
            shift += 5
        } while (b >= 0x1f)
        lng += if (result and 1 != 0) (result shr 1).inv() else result shr 1
        val latLng = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
        poly.add(latLng)
    }
    return poly
}

