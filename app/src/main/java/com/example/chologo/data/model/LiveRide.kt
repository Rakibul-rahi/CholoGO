package com.example.chologo.data.model

import com.google.firebase.Timestamp

data class LiveRide(
    val rideId: String = "",
    val riderId: String = "",
    val riderName: String = "",
    val pickup: String = "",
    val destination: String = "",
    val tripDirection: String = "",
    val tripTime: String = "",
    val timeMinutes: Int = 0,
    val routeKey: String = "",
    val availableSeats: Int = 1,
    val status: String = "active",
    val isLiveNow: Boolean = true,
    val isAvailable: Boolean = true,
    val currentRequestId: String = "",
    val createdAt: Timestamp? = null,
    val lastUpdatedAt: Timestamp? = null
)