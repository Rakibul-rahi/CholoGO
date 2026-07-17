package com.example.chologo.data.model

import com.google.firebase.Timestamp

data class RideReport(
    val reportId: String = "",

    val requestId: String = "",
    val rideId: String = "",

    val passengerId: String = "",
    val riderId: String = "",

    val reportedBy: String = "",
    val reportedUserId: String = "",

    val reason: String = "",
    val details: String = "",

    val status: String = "pending",

    val createdAt: Timestamp? = null
)