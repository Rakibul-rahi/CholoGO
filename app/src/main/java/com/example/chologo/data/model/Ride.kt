package com.example.chologo.model

data class Ride(
    val rideId: String = "",
    val riderId: String = "",
    val riderName: String = "",
    val origin: String = "",
    val destination: String = "",
    val departureTime: String = "",
    val date: String = "",
    val seatsAvailable: Int = 1,
    val status: String = "active",
    val createdAt: Long = System.currentTimeMillis()
)