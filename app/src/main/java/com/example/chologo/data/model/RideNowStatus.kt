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
        ISSUE_REPORTED
    )
}