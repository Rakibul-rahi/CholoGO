package com.example.chologo.model

data class RideRequest(
    val requestId: String = "",
    val rideId: String = "",
    val passengerId: String = "",
    val passengerName: String = "",
    val firstClassTime: String = "",
    val passengerLocation: String = "",
    val status: String = "pending",
    val createdAt: Long = System.currentTimeMillis()
)