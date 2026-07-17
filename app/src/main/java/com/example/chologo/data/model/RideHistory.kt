package com.example.chologo.data.model

import com.google.firebase.Timestamp

data class RideHistory(
    val historyId: String = "",

    val rideType: String = "", // "ride_now" or "tomorrow"
    val requestId: String = "",

    val passengerId: String = "",
    val passengerName: String = "",

    val riderId: String = "",
    val riderName: String = "",
    val riderPhone: String = "",

    val pickup: String = "",
    val destination: String = "",

    val tripTime: String = "",
    val timeMinutes: Int = 0,

    val status: String = "completed",

    val createdAt: Timestamp? = null,
    val completedAt: Timestamp? = null
)