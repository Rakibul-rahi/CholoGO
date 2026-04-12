package com.example.chologo.data.model

import com.google.firebase.Timestamp

data class Ride(
    val rideId: String = "",
    val riderId: String = "",
    val riderName: String = "",
    val tripDirection: String = "",   // "to_campus" or "to_home"
    val pickup: String = "",
    val destination: String = "",
    val tripTime: String = "",        // e.g. "8:30 AM"
    val timeMinutes: Int = 0,         // e.g. 510
    val routeKey: String = "",        // e.g. "to_campus|mirpur 12|aust gate"
    val rideDate: String = "",        // e.g. "2026-04-06"
    val availableSeats: Int = 1,
    val status: String = "active",    // active, full, cancelled
    val isTomorrowSetup: Boolean = false,
    val createdAt: Timestamp? = null
)