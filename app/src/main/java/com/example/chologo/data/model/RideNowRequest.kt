package com.example.chologo.data.model

import com.google.firebase.Timestamp

data class RideNowRequest(

    // Request
    val requestId: String = "",

    // Passenger
    val passengerId: String = "",
    val passengerName: String = "",
    val passengerPhone: String = "",

    // Trip
    val pickup: String = "",
    val destination: String = "",
    val tripTime: String = "",
    val timeMinutes: Int = 0,
    val routeKey: String = "",

    // Ride status
    val status: String = RideNowStatus.SEARCHING,

    // Matched rider
    val matchedRideId: String = "",
    val matchedRiderId: String = "",
    val matchedRiderName: String = "",
    val matchedRiderPhone: String = "",

    // Denormalized off the matched LiveRide at accept time - see the same
    // fields on RideRequest.
    val matchedVehicleType: String = "",
    val matchedVehicleModel: String = "",
    val matchedVehicleNumber: String = "",
    val matchedVehicleColor: String = "",

    // Ride lifecycle
    val acceptedAt: Timestamp? = null,
    val startedAt: Timestamp? = null,
    val completedAt: Timestamp? = null,

    // Cancellation / expiry
    val cancelledAt: Timestamp? = null,
    val expiredAt: Timestamp? = null,

    // Abandonment handling. Set when one side closes a trip the other
    // never responded to - see RideNowStatus.UNVERIFIED and
    // RideNowRequestRepository.riderCloseUnconfirmedTrip. "rider",
    // "passenger", or "system" for the stale sweep.
    val closedByRole: String = "",
    val closedAt: Timestamp? = null,

    // Request lifecycle
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,

    // Feedback flags
    val riderRated: Boolean = false,
    val issueReported: Boolean = false,

    // Rating info. "riderRated"/"rating"/"ratedAt" are the passenger's
    // rating of the rider; "passengerRated"/"passengerRating"/
    // "passengerRatedAt" are the mirror of that in the other direction, the
    // rider's rating of the passenger - two independent one-shot slots on
    // the same completed trip.
    val rating: Int = 0,
    val ratedAt: Timestamp? = null,
    val passengerRated: Boolean = false,
    val passengerRating: Int = 0,
    val passengerRatedAt: Timestamp? = null,

    // Report info
    val reportReason: String = "",
    val reportDetails: String = "",
    val reportedAt: Timestamp? = null
)

/**
 * How long a matched Ride Now trip may sit untouched before either app
 * treats it as abandoned rather than in progress.
 *
 * Two separate windows, because they guard against different mistakes:
 *  - [RIDER_ESCAPE_MINUTES] is how long a rider must wait before the
 *    "passenger isn't responding" buttons appear. Short, because the
 *    rider is standing there right now.
 *  - [STALE_HOURS] is the backstop sweep that runs when either app opens.
 *    Long, because it fires with nobody watching and must never kill a
 *    trip that's genuinely still happening.
 */
object RideNowAbandonment {
    const val RIDER_ESCAPE_MINUTES = 10
    const val STALE_HOURS = 6
}

/**
 * The most recent moment either side actually did something on this
 * request. Used to age a trip rather than createdAt alone, so a long but
 * genuinely progressing trip is never swept away mid-ride.
 */
fun RideNowRequest.lastProgressSeconds(): Long? {
    return listOfNotNull(completedAt, startedAt, acceptedAt, createdAt)
        .maxOfOrNull { it.seconds }
}

/**
 * True when this request is matched but has shown no progress for
 * [RideNowAbandonment.STALE_HOURS] - it was abandoned by both sides and
 * is now only serving to block them from making new ones.
 */
fun RideNowRequest.isAbandoned(nowSeconds: Long): Boolean {
    if (status !in RideNowStatus.MATCHED_STATUSES) return false
    val last = lastProgressSeconds() ?: return false
    return nowSeconds - last > RideNowAbandonment.STALE_HOURS * 60L * 60L
}

/**
 * True once the rider has waited long enough on an unresponsive passenger
 * for the manual escape hatches to be offered.
 */
fun RideNowRequest.riderMayForceClose(nowSeconds: Long): Boolean {
    if (status !in RideNowStatus.MATCHED_STATUSES) return false
    val last = lastProgressSeconds() ?: return false
    return nowSeconds - last > RideNowAbandonment.RIDER_ESCAPE_MINUTES * 60L
}
