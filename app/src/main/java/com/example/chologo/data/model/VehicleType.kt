package com.example.chologo.data.model

/**
 * A rider shares either a bike (always exactly one pillion seat) or a car
 * (the rider picks how many seats they're opening up on each individual
 * trip). Vehicle choice lives on the rider's profile; the seat count does
 * not, because it changes trip to trip.
 *
 * Every rider who signed up before this existed has no vehicleType field on
 * their user/ride documents at all, which deserializes to "". normalize()
 * therefore treats anything that isn't explicitly "car" as a bike, so legacy
 * riders keep behaving exactly as they did - one seat, no car details.
 */
object VehicleType {

    const val BIKE = "bike"
    const val CAR = "car"

    /** A bike's seat count is not the rider's to choose. */
    const val BIKE_SEATS = 1

    const val MIN_CAR_SEATS = 1
    const val MAX_CAR_SEATS = 4

    fun normalize(rawVehicleType: String): String {
        return if (rawVehicleType.trim().equals(CAR, ignoreCase = true)) CAR else BIKE
    }

    fun isCar(rawVehicleType: String): Boolean {
        return normalize(rawVehicleType) == CAR
    }

    /**
     * The only place seat counts are decided. A bike is pinned to one seat
     * no matter what the caller passes; a car is clamped to a sane range so
     * neither a stale UI nor a hand-edited document can open up 0 or 99
     * seats.
     */
    fun resolveSeats(rawVehicleType: String, requestedSeats: Int): Int {
        return if (isCar(rawVehicleType)) {
            requestedSeats.coerceIn(MIN_CAR_SEATS, MAX_CAR_SEATS)
        } else {
            BIKE_SEATS
        }
    }

    fun label(rawVehicleType: String): String {
        return if (isCar(rawVehicleType)) "Car" else "Bike"
    }

    fun emoji(rawVehicleType: String): String {
        return if (isCar(rawVehicleType)) "🚗" else "🏍️"
    }

    /**
     * One-line "Toyota Axio · Dhaka Metro Ka 12-3456 · White" summary from
     * whichever car detail fields the rider actually filled in, or null when
     * there's nothing worth showing (always the case for a bike).
     */
    fun detailsSummary(
        rawVehicleType: String,
        model: String,
        number: String,
        color: String
    ): String? {
        if (!isCar(rawVehicleType)) return null

        val parts = listOf(model, color, number)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    }
}
