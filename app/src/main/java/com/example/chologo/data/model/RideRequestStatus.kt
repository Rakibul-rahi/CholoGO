package com.example.chologo.data.model

object RideRequestStatus {

    /** Passenger created a request and is waiting for a rider to accept. */
    const val PENDING = "pending"

    /** A rider has accepted this request. */
    const val ACCEPTED = "accepted"

    /** Cancelled by either side. Check cancelledByRole for who. */
    const val CANCELLED = "cancelled"

    /**
     * Passenger asked to cancel an already-accepted request. This is a
     * transitional state: restoring the rider's seat requires writing to a
     * document the passenger doesn't own, so a Cloud Function finishes the
     * job and moves this to CANCELLED. See TomorrowRideRepository.
     */
    const val CANCEL_REQUESTED_BY_PASSENGER = "cancel_requested_by_passenger"

    /** Rider pressed Start Trip. Waiting for passenger confirmation. */
    const val START_PENDING_CONFIRMATION = "start_pending_confirmation"

    /** Passenger confirmed the trip started. Ride is currently active. */
    const val ONGOING = "ongoing"

    /** Rider pressed Trip Completed. Waiting for passenger confirmation. */
    const val END_PENDING_CONFIRMATION = "end_pending_confirmation"

    /** Passenger confirmed safe arrival. */
    const val COMPLETED = "completed"

    /** Statuses in which a matched trip is still live/in-progress. */
    val ACTIVE_LIFECYCLE_STATUSES = listOf(
        ACCEPTED,
        START_PENDING_CONFIRMATION,
        ONGOING,
        END_PENDING_CONFIRMATION,
        COMPLETED
    )
}