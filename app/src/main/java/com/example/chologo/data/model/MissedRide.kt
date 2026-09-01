package com.example.chologo.data.model

import java.util.Calendar

/**
 * Detection rules for a Tomorrow leg whose departure time has come and
 * gone while the trip lifecycle was never driven to an end.
 *
 * The common case this exists for: a passenger books on Monday for an
 * 8:00 AM Tuesday ride, the ride genuinely happens, but neither side ever
 * opens the app to press Start Trip / Trip Completed. Without this the
 * request sits at "accepted" forever - invisible (the Tomorrow tab only
 * ever listens for *tomorrow's* date), unrateable, and permanently
 * blocking that leg from being re-planned.
 *
 * Rather than waiting for a lifecycle that will never arrive, the leg is
 * reconstructed after the fact: whichever side opens the app first is
 * asked whether the ride happened, and the other side validates. See
 * RideRequest.riderHappenedAnswer / passengerHappenedAnswer.
 */
object MissedRideWindow {

    /**
     * How long after the scheduled departure a still-unfinished leg is
     * treated as missed rather than merely running late. Three hours
     * comfortably clears any realistic campus commute (plus traffic), so
     * a trip that's genuinely in progress is never interrupted by a
     * "did this happen?" prompt.
     */
    const val GRACE_MINUTES = 180

    /**
     * Wall-clock millis of [rideDate] ("yyyy-MM-dd") at [timeMinutes]
     * minutes past midnight, both interpreted in the device's local
     * timezone - the same convention TomorrowRideReminderScheduler uses.
     *
     * Returns null for a malformed date, which callers read as "can't
     * judge this one", never as "it's overdue".
     */
    fun departureMillis(rideDate: String, timeMinutes: Int): Long? {
        val parts = rideDate.split("-")
        if (parts.size != 3) return null

        val year = parts[0].toIntOrNull() ?: return null
        val month = parts[1].toIntOrNull() ?: return null
        val day = parts[2].toIntOrNull() ?: return null

        return try {
            Calendar.getInstance().apply {
                set(year, month - 1, day, timeMinutes / 60, timeMinutes % 60, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * True when this leg is matched, unfinished, and its departure is more
 * than [MissedRideWindow.GRACE_MINUTES] in the past - i.e. it needs the
 * after-the-fact "did this ride happen?" review.
 */
fun RideRequest.needsMissedRideReview(
    nowMillis: Long = System.currentTimeMillis()
): Boolean {
    if (status !in RideRequestStatus.UNFINISHED_LIFECYCLE_STATUSES) return false

    val departure = MissedRideWindow.departureMillis(rideDate, timeMinutes) ?: return false

    return nowMillis - departure > MissedRideWindow.GRACE_MINUTES * 60_000L
}

/** This side's stored answer, or "" if they haven't been asked yet. */
fun RideRequest.answerFor(isRider: Boolean): String {
    return if (isRider) riderHappenedAnswer else passengerHappenedAnswer
}

/** The other side's stored answer, or "" while they haven't answered. */
fun RideRequest.otherAnswerFor(isRider: Boolean): String {
    return if (isRider) passengerHappenedAnswer else riderHappenedAnswer
}

/**
 * The status a missed leg lands on once [ownAnswer] is recorded, given
 * what the other side already said. Shared by the repository (which
 * writes it) and firestore.rules (which re-derives the same thing) so
 * the two can never drift.
 */
fun RideRequest.resolvedStatusAfter(isRider: Boolean, ownAnswer: String): String {
    val other = otherAnswerFor(isRider)

    return when {
        other.isBlank() -> status               // still waiting on the other side
        other != ownAnswer -> RideRequestStatus.UNVERIFIED
        ownAnswer == "yes" -> RideRequestStatus.COMPLETED
        else -> RideRequestStatus.NOT_COMPLETED
    }
}

/** Answer values accepted by both the client and firestore.rules. */
object MissedRideAnswer {
    const val YES = "yes"
    const val NO = "no"
}
