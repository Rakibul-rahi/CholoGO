package com.example.chologo.data.model

import com.google.firebase.Timestamp

data class Ride(
    val rideId: String = "",
    val riderId: String = "",
    val riderName: String = "",
    val tripDirection: String = "",   // "to_campus" or "to_home"
    val pickup: String = "",
    val destination: String = "",
    val tripTime: String = "",        // e.g. "8:30 AM"
    val timeMinutes: Int = 0,         // e.g. 510
    val routeKey: String = "",        // e.g. "to_campus|mirpur 12|aust gate"
    val rideDate: String = "",        // e.g. "2026-04-06"

    // Snapshot of the rider's vehicle at the time this ride was saved, so a
    // matched passenger sees the car they were actually offered even if the
    // rider later edits their profile. Absent on pre-car rides, which
    // VehicleType.normalize() reads as "bike".
    val vehicleType: String = "",
    val vehicleModel: String = "",
    val vehicleNumber: String = "",
    val vehicleColor: String = "",

    // totalSeats is the capacity the rider opened up; availableSeats is
    // what's left after accepts. They start equal, and (totalSeats -
    // availableSeats) is how many passengers are already on board. Legacy
    // rides predate totalSeats and default to 1, matching their bike.
    val totalSeats: Int = 1,
    val availableSeats: Int = 1,
    val status: String = "active",    // active, full, cancelled
    val isTomorrowSetup: Boolean = false,
    val createdAt: Timestamp? = null
)

/**
 * How many passengers are already riding along. Derived rather than stored
 * so it can never drift out of sync with the seat counts the accept/cancel
 * transactions actually move.
 *
 * Legacy rides carry no totalSeats field, which defaults to 1 and matches
 * the single pillion seat they were created with, so this stays correct for
 * them: 1 seat, 0 available => 1 passenger taken.
 */
fun Ride.seatsTaken(): Int {
    return (totalSeats - availableSeats).coerceAtLeast(0)
}

/** Capacity as the rider set it, guarding against a malformed document. */
fun Ride.seatCapacity(): Int {
    return maxOf(totalSeats, availableSeats, 1)
}

/** Compact badge text for how much room is left, e.g. "2/3 left". */
fun Ride.seatSummary(): String {
    return "${availableSeats.coerceAtLeast(0)}/${seatCapacity()} left"
}
