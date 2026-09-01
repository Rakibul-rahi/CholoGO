package com.example.chologo.data.model

object RideNowStatus {

    /**
     * Passenger created a request and is waiting
     * for a matching rider.
     */
    const val SEARCHING = "searching"

    /**
     * Optional state when rider has been notified
     * but has not accepted yet.
     */
    const val NOTIFIED = "notified"

    /**
     * Rider accepted the request.
     */
    const val ACCEPTED = "accepted"

    /**
     * Rider pressed Start Ride.
     * Waiting for passenger confirmation.
     */
    const val START_PENDING_CONFIRMATION =
        "start_pending_confirmation"

    /**
     * Passenger confirmed ride start.
     * Ride is currently active.
     */
    const val ONGOING = "ongoing"

    /**
     * Rider pressed Complete Ride.
     * Waiting for passenger confirmation.
     */
    const val END_PENDING_CONFIRMATION =
        "end_pending_confirmation"

    /**
     * Passenger confirmed successful completion.
     */
    const val COMPLETED = "completed"

    /**
     * Ride cancelled by passenger or rider.
     */
    const val CANCELLED = "cancelled"

    /**
     * Passenger request expired before matching.
     */
    const val EXPIRED = "expired"

    /**
     * Passenger submitted a report after ride.
     */
    const val ISSUE_REPORTED = "issue_reported"

    /**
     * The rider closed a trip the passenger never confirmed - they pressed
     * Ride Completed (or the trip was already ongoing) and the passenger
     * simply stopped responding. Terminal, and deliberately not COMPLETED:
     * only one side ever vouched for it, so it earns no history entry and
     * no rating. Mirrors RideRequestStatus.UNVERIFIED on the Tomorrow side.
     */
    const val UNVERIFIED = "unverified"

    /**
     * Helper list for active rides.
     */
    val ACTIVE_STATUSES = listOf(
        SEARCHING,
        NOTIFIED,
        ACCEPTED,
        START_PENDING_CONFIRMATION,
        ONGOING,
        END_PENDING_CONFIRMATION
    )

    /**
     * Helper list for finished rides.
     */
    val FINISHED_STATUSES = listOf(
        COMPLETED,
        CANCELLED,
        EXPIRED,
        ISSUE_REPORTED,
        UNVERIFIED
    )

    /**
     * Statuses where a rider has already committed to a passenger. These
     * are the ones that can strand either side if the other stops
     * responding, so they're what the abandonment escape hatches
     * (RideNowRequestRepository.riderCancelUnstartedTrip /
     * riderCloseUnconfirmedTrip) and the stale sweep operate on.
     */
    val MATCHED_STATUSES = listOf(
        ACCEPTED,
        START_PENDING_CONFIRMATION,
        ONGOING,
        END_PENDING_CONFIRMATION
    )
}