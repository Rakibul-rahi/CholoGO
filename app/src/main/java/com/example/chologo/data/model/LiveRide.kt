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

    // Same vehicle snapshot as Ride. Ride Now still matches one passenger at
    // a time (the whole flow is built around a single currentRequestId), so
    // availableSeats stays 1 here even for a car - the car details are
    // carried purely so the passenger knows what to look for.
    val vehicleType: String = "",
    val vehicleModel: String = "",
    val vehicleNumber: String = "",
    val vehicleColor: String = "",

    val availableSeats: Int = 1,
    val status: String = "active",
    val isLiveNow: Boolean = true,
    val isAvailable: Boolean = true,
    val currentRequestId: String = "",
    val createdAt: Timestamp? = null,
    val lastUpdatedAt: Timestamp? = null
)