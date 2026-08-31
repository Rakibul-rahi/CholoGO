package com.example.chologo.data.model

import com.google.firebase.Timestamp

data class RideRequest(
    val requestId: String = "",
    val userId: String = "",

    val passengerName: String = "",
    val passengerPhone: String = "",
    val pickup: String = "",
    val destination: String = "",
    val tripDirection: String = "",   // to_campus or to_home
    val tripTime: String = "",        // e.g. 8:30 AM

    val hour: Int = 0,
    val minute: Int = 0,
    val timeMinutes: Int = 0,         // e.g. 510
    val routeKey: String = "",        // e.g. to_campus|mirpur 12|aust gate
    val rideDate: String = "",        // e.g. 2026-04-06

    val status: String = "pending",   // pending, accepted, cancelled, cancel_requested_by_passenger
    val createdAt: Timestamp? = null,

    val matchedRideId: String = "",
    val matchedRiderId: String = "",
    val matchedRiderName: String = "",
    val matchedRiderPhone: String = "",
    val matchedRideTime: String = "",
    val acceptedAt: Timestamp? = null,

    // Trip lifecycle - mirrors RideNowRequest's start/complete confirmation
    val rideStartedByRider: Boolean = false,
    val rideConfirmedByPassenger: Boolean = false,
    val rideEndedByRider: Boolean = false,
    val rideCompletedByPassenger: Boolean = false,
    val startedAt: Timestamp? = null,
    val completedAt: Timestamp? = null,

    // Rating / report
    val riderRated: Boolean = false,
    val rating: Int = 0,
    val ratedAt: Timestamp? = null,
    val issueReported: Boolean = false,
    val reportReason: String = "",
    val reportDetails: String = "",
    val reportedAt: Timestamp? = null,

    val rejectedByRiderIds: List<String> = emptyList(),

    // Cancellation tracking - populated whichever side initiates a cancel.
    val cancelledBy: String = "",
    val cancelledByRole: String = "",       // "rider" or "passenger"
    val cancellationReason: String = "",
    val cancelledAt: Timestamp? = null
)