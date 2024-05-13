package com.example.csci3310project

import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.example.csci3310project.ExchangeRates
import com.google.gson.Gson

class ExchangeRateManager(private val queue: RequestQueue) {
    private val gson = Gson()
    private val url = "https://v6.exchangerate-api.com/v6/" + BuildConfig.EXCHANGE_RATE_API_KEY + "/latest/"
    private val exchangeRatesCache = mutableMapOf<String, ExchangeRates>()

    fun getExchangeRates(baseCurrency: String, callback: (ExchangeRates) -> Unit) {
        if (exchangeRatesCache.containsKey(baseCurrency)) {
            callback(exchangeRatesCache[baseCurrency]!!)
        } else {
            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.GET,
                "$url$baseCurrency",
                null,
                { response ->
                    val exchangeRates = gson.fromJson(response.toString(), ExchangeRates::class.java)
                    exchangeRatesCache[baseCurrency] = exchangeRates
                    callback(exchangeRates)
                },
                { error ->
                    error.printStackTrace()
                    Log.e("ExchangeRateManager", "Error: ${error.networkResponse.statusCode}")
                }
            )

            queue.add(jsonObjectRequest)
        }
    }

    fun convertCurrency(
        exchangeRates: ExchangeRates,
        baseCurrency: String,
        targetCurrency: String,
        amount: Double,
        callback: (Double) -> Unit
    ) {
        val rate = exchangeRates.conversion_rates[baseCurrency] ?: 0.0
        callback(amount / rate)
    }

}