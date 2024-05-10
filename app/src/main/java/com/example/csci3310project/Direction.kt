package com.example.csci3310project

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Properties

val properties = Properties().apply {
    val localPropertiesFile = File("local.properties")
    if (localPropertiesFile.exists()) {
        load(FileInputStream(localPropertiesFile))
    }
}
val apiKey = BuildConfig.GOOGLE_MAPS_API_KEY
private val TAG = "Direction API"

fun getDirection(origin: LatLng, destination: LatLng, mode: String, arrivalTime: Int, callback: (DirectionsResponse?) -> Unit) {
    val url = if(mode == "transit"){
        "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&mode=$mode&key=$apiKey&arrival_time=$arrivalTime"
    }else{
        "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&mode=$mode&key=$apiKey"
    }
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            e.printStackTrace()
            callback(null)
        }

        override fun onResponse(call: Call, response: Response) {
            if (response.isSuccessful) {
                val jsonData = response.body()?.string()
                Log.d(TAG, jsonData.toString())
                val directionData = jsonData?.let { parseJsonToDirectionsResponse(it) }
                callback(directionData)
            } else {
                callback(null)
            }
        }
    })
}

fun parseJsonToDirectionsResponse(json: String): DirectionsResponse {
    val gson = Gson()
    return gson.fromJson(json, DirectionsResponse::class.java)
}

