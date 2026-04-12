package com.example.chologo.data.model

import com.google.firebase.Timestamp

data class RideNowRequest(
    val requestId: String = "",

    // Passenger info
    val passengerId: String = "",
    val passengerName: String = "",

    // Trip info
    val pickup: String = "",
    val destination: String = "",
    val tripTime: String = "",
    val timeMinutes: Int = 0,
    val routeKey: String = "",

    // Status
    val status: String = RideNowStatus.SEARCHING,

    // Matching info
    val matchedRideId: String = "",
    val matchedRiderId: String = "",
    val matchedRiderName: String = "",
    val matchedRiderPhone: String = "",

    // Control timestamps
    val acceptedAt: Timestamp? = null,
    val cancelledAt: Timestamp? = null,

    // Lifecycle timestamps
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,

    // Optional extra timestamps
    val startedAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
    val expiredAt: Timestamp? = null
)