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
    var expenses: MutableList<Expense> = mutableListOf(),
    var participantsID: MutableList<String> = mutableListOf(),
    var participants: MutableList<String> = mutableListOf(),
    val destination: String = "",
    var transactions: MutableList<ExpenseTransaction> = mutableListOf()
)

fun generateJoinCode(): String {
    val allowedChars = ('A'..'Z') + ('0'..'9')
    return (1..6).map { allowedChars.random() }.joinToString("")
}


enum class TravelMethod {
    WALK, CAR, PUBLIC_TRANSPORT
}

data class Event(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var date: Long = System.currentTimeMillis(),
    var startTime: Long = System.currentTimeMillis(),
    var endTime: Long = System.currentTimeMillis() + 3600000, // Default to one hour later
    var location: String? = null,
    var travelMethod: TravelMethod = TravelMethod.WALK // Assume WALK as default
)

data class ExpenseTransaction(
    val creditorName: String = "",
    val debtorName: String = "",
    var currency: Currency = Currency.HKD,
    var amount: Double = 0.0
)

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    var title: String = "",
    var date: Long = System.currentTimeMillis(),
    var currency: Currency = Currency.HKD,
    var amount: Double = 0.0,
    var payer: String = "",
    var type: ExpenseType = ExpenseType.OTHER,
    var transactions: MutableList<ExpenseTransaction> = mutableListOf(),
    var isSettled: Boolean = false
)

enum class ExpenseType {
    FOOD, ENTERTAINMENT, TRANSPORTATION, OTHER
}

data class ExchangeRates(
    val base_code: String = "",
    val conversion_rates: Map<String, Double> = emptyMap()
)
  
enum class Currency {
    USD, AED, AFN, ALL, AMD, ANG, AOA, ARS, AUD, AWG, AZN, BAM, BBD, BDT, BGN, BHD, BIF, BMD, BND, BOB, BRL, BSD, BTN, BWP, BYN, BZD, CAD, CDF, CHF, CLP, CNY, COP, CRC, CUP, CVE, CZK, DJF, DKK, DOP, DZD, EGP, ERN, ETB, EUR, FJD, FKP, FOK, GBP, GEL, GGP, GHS, GIP, GMD, GNF, GTQ, GYD, HKD, HNL, HRK, HTG, HUF, IDR, ILS, IMP, INR, IQD, IRR, ISK, JEP, JMD, JOD, JPY, KES, KGS, KHR, KID, KMF, KRW, KWD, KYD, KZT, LAK, LBP, LKR, LRD, LSL, LYD, MAD, MDL, MGA, MKD, MMK, MNT, MOP, MRU, MUR, MVR, MWK, MXN, MYR, MZN, NAD, NGN, NIO, NOK, NPR, NZD, OMR, PAB, PEN, PGK, PHP, PKR, PLN, PYG, QAR, RON, RSD, RUB, RWF, SAR, SBD, SCR, SDG, SEK, SGD, SHP, SLE, SLL, SOS, SRD, SSP, STN, SYP, SZL, THB, TJS, TMT, TND, TOP, TRY, TTD, TVD, TWD, TZS, UAH, UGX, UYU, UZS, VES, VND, VUV, WST, XAF, XCD, XDR, XOF, XPF, YER, ZAR, ZMW, ZWL
}