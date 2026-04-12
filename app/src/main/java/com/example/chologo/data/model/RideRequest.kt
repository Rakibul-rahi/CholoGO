package com.example.chologo.data.model

import com.google.firebase.Timestamp

data class RideRequest(
    val requestId: String = "",
    val userId: String = "",

    val passengerName: String = "",
    val pickup: String = "",
    val destination: String = "",
    val tripDirection: String = "",   // to_campus or to_home
    val tripTime: String = "",        // e.g. 8:30 AM

    val hour: Int = 0,
    val minute: Int = 0,
    val timeMinutes: Int = 0,         // e.g. 510
    val routeKey: String = "",        // e.g. to_campus|mirpur 12|aust gate
    val rideDate: String = "",        // e.g. 2026-04-06

    val status: String = "pending",   // pending, accepted, cancelled
    val createdAt: Timestamp? = null,

    val matchedRideId: String = "",
    val matchedRiderId: String = "",
    val matchedRiderName: String = "",
    val matchedRiderPhone: String = "",
    val matchedRideTime: String = "",
    val acceptedAt: Timestamp? = null,

    val rejectedByRiderIds: List<String> = emptyList()
)