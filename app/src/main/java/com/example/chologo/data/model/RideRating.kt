package com.example.chologo.data.model

import com.google.firebase.Timestamp

data class RideRating(
    val ratingId: String = "",

    val requestId: String = "",
    val rideId: String = "",

    val passengerId: String = "",
    val riderId: String = "",

    val ratedBy: String = "",
    val ratedTo: String = "",

    val stars: Int = 0,
    val comment: String = "",

    val createdAt: Timestamp? = null
)