package com.example.chologo.data.model

fun buildRouteKey(
    tripDirection: String,
    pickup: String,
    destination: String
): String {
    return "${tripDirection.trim().lowercase()}|${pickup.trim().lowercase()}|${destination.trim().lowercase()}"
}

fun toMinutes(hour: Int, minute: Int): Int {
    return hour * 60 + minute
}

fun isTimeClose(
    firstTimeMinutes: Int,
    secondTimeMinutes: Int,
    gapMinutes: Int = 30
): Boolean {
    return kotlin.math.abs(firstTimeMinutes - secondTimeMinutes) <= gapMinutes
}