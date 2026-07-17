package com.example.chologo.data.model

import com.google.firebase.Timestamp

data class RideNowRequest(

    // Request
    val requestId: String = "",

    // Passenger
    val passengerId: String = "",
    val passengerName: String = "",

    // Trip
    val pickup: String = "",
    val destination: String = "",
    val tripTime: String = "",
    val timeMinutes: Int = 0,
    val routeKey: String = "",

    // Ride status
    val status: String = RideNowStatus.SEARCHING,

    // Matched rider
    val matchedRideId: String = "",
    val matchedRiderId: String = "",
    val matchedRiderName: String = "",
    val matchedRiderPhone: String = "",

    // Ride lifecycle
    val acceptedAt: Timestamp? = null,
    val startedAt: Timestamp? = null,
    val completedAt: Timestamp? = null,

    // Cancellation / expiry
    val cancelledAt: Timestamp? = null,
    val expiredAt: Timestamp? = null,

    // Request lifecycle
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,

    // Feedback flags
    val riderRated: Boolean = false,
    val issueReported: Boolean = false,

    // Rating info
    val rating: Int = 0,
    val ratedAt: Timestamp? = null,

    // Report info
    val reportReason: String = "",
    val reportDetails: String = "",
    val reportedAt: Timestamp? = null
)