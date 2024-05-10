package com.example.csci3310project

data class GeocodedWaypoint(
    val geocoder_status: String,
    val placeId: String,
    val types: List<String>
)

data class Bounds(
    val northeast: Location,
    val southwest: Location
)

data class Location(
    val lat: Double,
    val lng: Double
)

data class Distance(
    val text: String,
    val value: Int
)

data class Duration(
    val text: String,
    val value: Int
)

data class Step(
    val distance: Distance,
    val duration: Duration,
    val end_location: Location,
    val start_location: Location,
    val travel_mode: String,
    val html_instructions: String?,
    val polyline: Polyline?,
    val maneuver: String?
)

data class Leg(
    val distance: Distance,
    val duration: Duration,
    val end_address: String,
    val end_location: Location,
    val start_address: String,
    val start_location: Location,
    val steps: List<Step>,
    val trafficSpeedEntry: List<Any>,
    val viaWaypoint: List<Any>
)

data class Polyline(
    val points: String
)

data class OverviewPolyline(
    val points: String
)

data class Route(
    val bounds: Bounds,
    val copyrights: String,
    val legs: List<Leg>,
    val overview_polyline: OverviewPolyline,
    val summary: String,
    val warnings: List<Any>,
    val waypoint_order: List<Any>
)

data class DirectionsResponse(
    val geocoded_waypoints: List<GeocodedWaypoint>,
    val routes: List<Route>,
    val status: String
)
