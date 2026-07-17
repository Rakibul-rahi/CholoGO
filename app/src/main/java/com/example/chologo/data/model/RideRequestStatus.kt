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
}