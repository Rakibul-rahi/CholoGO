package com.example.chologo.data.repository

import com.example.chologo.data.model.RideNowRequest
import com.example.chologo.data.model.RideNowStatus
import com.example.chologo.data.model.RideRequest
import com.example.chologo.data.model.RideRequestStatus
import com.example.chologo.data.model.XpEvent
import com.example.chologo.data.model.XpRules
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

/**
 * Reads and writes the XP ledger.
 *
 * There is no "add XP" call here in the old sense, because there is no
 * counter to add to - users.xp is no longer writable by any client (see
 * firestore.rules) and is no longer read for display. A user's XP is the
 * sum of their xp_events rows, and this class is the only thing that
 * creates them.
 *
 * Every write goes to a derived document ID, so calling [awardXp] twice
 * for the same achievement is harmless: the second create hits an ID that
 * already exists and is rejected. That makes the whole class safe to call
 * speculatively, which is what [claimTripXpFor] relies on - it simply
 * re-offers every completed trip it can see and lets Firestore throw away
 * the ones already paid.
 */
class XpRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val xpEventsRef = db.collection("xp_events")
    private val rideRequestsRef = db.collection("ride_requests")
    private val rideNowRequestsRef = db.collection("ride_now_requests")

    /**
     * Writes one ledger row, if it isn't there already.
     *
     * Returns true only when this call is the one that created it, so
     * callers can decide whether to say "+12 XP" out loud. A false is the
     * normal, expected outcome for an already-paid achievement and is not
     * an error - neither is a rules rejection, which just means the
     * evidence didn't hold up.
     */
    suspend fun awardXp(
        userId: String,
        reason: String,
        role: String,
        sourceId: String,
        dedupeKey: String
    ): Boolean {
        if (userId.isBlank() || sourceId.isBlank() || dedupeKey.isBlank()) return false

        val amount = XpRules.amountFor(reason, role)
        if (amount <= 0L) return false

        val eventId = XpRules.eventId(userId, reason, dedupeKey)

        return try {
            val event = XpEvent(
                eventId = eventId,
                userId = userId,
                reason = reason,
                role = role,
                sourceId = sourceId,
                amount = amount,
                createdAt = Timestamp.now()
            )

            // set() on a document that already exists is an update, and
            // the rules forbid updating a row outright - so a repeat claim
            // fails here rather than overwriting anything. That failure is
            // how we learn this achievement was already paid, and it is
            // the dedupe mechanism, not an accident of it.
            xpEventsRef.document(eventId).set(event).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Live total for a user, recomputed from their rows on every change.
     *
     * A listener rather than a one-shot read so the level card moves the
     * instant an award lands, with no refresh callbacks threaded back up
     * through the UI. Rows are immutable and modest in number (a real
     * commuter earns at most three a day), so summing them client-side is
     * cheaper and simpler than an aggregation query, and it works from
     * the offline cache.
     */
    fun listenTotalXp(
        userId: String,
        onData: (Long) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return xpEventsRef
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val total = snapshot?.documents
                    ?.sumOf { it.getLong("amount") ?: 0L }
                    ?: 0L

                onData(total)
            }
    }

    /** One-shot total, for callers that don't want a listener. */
    suspend fun getTotalXp(userId: String): Long {
        return try {
            xpEventsRef
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .sumOf { it.getLong("amount") ?: 0L }
        } catch (_: Exception) {
            0L
        }
    }

    /**
     * Offers every completed trip this user was part of to the ledger and
     * returns how many were newly paid.
     *
     * This is the only place trip XP is claimed, and it runs on dashboard
     * load rather than at the moment a trip completes. That's deliberate:
     * only one side is present when a trip finishes (the passenger
     * confirms it), so an award-on-the-spot design always leaves the
     * other party's XP owed with nowhere to record the debt. Re-offering
     * everything instead means neither side can be missed, a trip
     * completed while the app was closed is picked up on next open, and
     * the whole thing self-heals after any failure - the derived IDs make
     * repetition free.
     */
    suspend fun claimTripXpFor(userId: String, isRider: Boolean): Int {
        if (userId.isBlank()) return 0

        val role = if (isRider) XpRules.ROLE_RIDER else XpRules.ROLE_PASSENGER

        return claimTomorrowTripXp(userId, isRider, role) +
                claimRideNowTripXp(userId, isRider, role)
    }

    private suspend fun claimTomorrowTripXp(
        userId: String,
        isRider: Boolean,
        role: String
    ): Int {
        val legs = try {
            rideRequestsRef
                .whereEqualTo(if (isRider) "matchedRiderId" else "userId", userId)
                .whereEqualTo("status", RideRequestStatus.COMPLETED)
                .limit(CLAIM_SWEEP_LIMIT)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(RideRequest::class.java)?.copy(requestId = doc.id)
                }
        } catch (_: Exception) {
            return 0
        }

        var awarded = 0

        legs.forEach { leg ->
            // A reported trip earns nothing for either side. Rules check
            // this too - it's repeated here only to save a round trip.
            if (leg.issueReported) return@forEach
            if (leg.rideDate.isBlank() || leg.tripDirection.isBlank()) return@forEach

            val didAward = awardXp(
                userId = userId,
                reason = XpRules.REASON_TOMORROW_TRIP,
                role = role,
                sourceId = leg.requestId,
                dedupeKey = XpRules.tomorrowTripDedupeKey(leg.rideDate, leg.tripDirection)
            )

            if (didAward) awarded++
        }

        return awarded
    }

    private suspend fun claimRideNowTripXp(
        userId: String,
        isRider: Boolean,
        role: String
    ): Int {
        val trips = try {
            rideNowRequestsRef
                .whereEqualTo(if (isRider) "matchedRiderId" else "passengerId", userId)
                .whereEqualTo("status", RideNowStatus.COMPLETED)
                .limit(CLAIM_SWEEP_LIMIT)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(RideNowRequest::class.java)?.copy(requestId = doc.id)
                }
        } catch (_: Exception) {
            return 0
        }

        var awarded = 0

        trips.forEach { trip ->
            if (trip.issueReported) return@forEach

            val dedupeKey = XpRules.rideNowTripDedupeKey(
                completedAt = trip.completedAt,
                routeKey = trip.routeKey
            ) ?: return@forEach

            val didAward = awardXp(
                userId = userId,
                reason = XpRules.REASON_RIDE_NOW_TRIP,
                role = role,
                sourceId = trip.requestId,
                dedupeKey = dedupeKey
            )

            if (didAward) awarded++
        }

        return awarded
    }

    private companion object {
        /**
         * Caps one sweep's reads. Anything beyond this is picked up on the
         * next dashboard load, and since already-paid trips are rejected
         * cheaply the backlog only ever shrinks.
         */
        const val CLAIM_SWEEP_LIMIT = 60L
    }
}
