package com.example.chologo.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "",
    val university: String = "AUST",
    val studentId: String = "",
    val homeLocation: String = "",
    val xp: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0,
    val reportCount: Int = 0
)