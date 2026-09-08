package com.example.chologo.data.repository

import com.example.chologo.data.model.MissedRideAnswer
import com.example.chologo.data.model.Ride
import com.example.chologo.data.model.RideHistory
import com.example.chologo.data.model.RideRequest
import com.example.chologo.data.model.RideRequestStatus
import com.example.chologo.data.model.VehicleType
import com.example.chologo.data.model.answerFor
import com.example.chologo.data.model.buildRouteKey
import com.example.chologo.data.model.canStartTrip
import com.example.chologo.data.model.resolvedStatusAfter
import com.example.chologo.data.model.seatCapacity
import com.example.chologo.data.model.seatsTaken
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * A single leg (campus or home direction) of a create-or-update attempt.
 */
sealed class TomorrowLegResult {
    data class Saved(val docId: String, val isNew: Boolean) : TomorrowLegResult()
    data class Blocked(val reason: String) : TomorrowLegResult()
}

/**
 * Thrown from inside a transaction to signal an expected "can't edit this
 * right now" outcome (TomorrowLegResult.Blocked), as opposed to a real
 * failure (Result.failure) - lets upsertRiderRide/upsertPassengerRequest
 * re-check their "safe to overwrite" precondition transactionally without
 * losing the distinction between the two outcomes their callers expect.
 */
private class TomorrowLegBlockedException(message: String) : Exception(message)

/**
 * Data layer for the "Tomorrow" ride flow, rebuilt to match the current
 * firestore.rules exactly:
 *  - a matched ride/request can no longer be deleted or overwritten, so
 *    every save is an upsert that checks status before touching anything
 *  - accept only ever transitions pending -> accepted, self-matched
 *  - decline only ever appends to rejectedByRiderIds
 *  - always toObject() deserialization, never raw map access
 */
class TomorrowRideRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val ridesRef = db.collection("rides")
    private val rideRequestsRef = db.collection("ride_requests")
    private val rideHistoryRef = db.collection("ride_history")
    private val usersRef = db.collection("users")

    private val apiBaseUrl = "https://chologo.onrender.com"

    // Generous timeouts: Render's free tier sleeps after 15 min of
    // inactivity and can take 30-60s to wake up on the next request.
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // ---------- Rider: saved "Tomorrow" rides ----------

    fun listenRiderRides(
        riderId: String,
        rideDate: String,
        onData: (List<Ride>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return ridesRef
            .whereEqualTo("riderId", riderId)
            .whereEqualTo("rideDate", rideDate)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val rides = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(Ride::class.java)?.copy(rideId = doc.id)
                    }
                    ?.sortedBy { ride ->
                        when (ride.tripDirection.lowercase()) {
                            "to_campus" -> 0
                            "to_home" -> 1
                            else -> 2
                        }
                    }
                    ?: emptyList()

                onData(rides)
            }
    }

    /**
     * Creates a new ride for this direction/date, or updates the existing
     * one in place while nobody has claimed a seat on it yet. Refuses
     * (returns Blocked, not an exception) once any passenger has been
     * accepted - editing must not silently destroy an active match.
     *
     * Note the seat check is "has anyone been accepted", not "is the status
     * still active": a 4-seat car with one passenger on board is still
     * "active" (it has seats left to sell) but absolutely must not have its
     * route, time or capacity rewritten under that passenger. A bike hits
     * "full" on its first accept, so for bikes the two checks coincide -
     * which is why the old status-only check was sufficient before cars.
     */
    suspend fun upsertRiderRide(
        riderId: String,
        riderName: String,
        rideDate: String,
        tripDirection: String,
        pickup: String,
        destination: String,
        tripTime: String,
        timeMinutes: Int,
        vehicleType: String,
        vehicleModel: String,
        vehicleNumber: String,
        vehicleColor: String,
        requestedSeats: Int
    ): Result<TomorrowLegResult> {
        return try {
            val normalizedVehicleType = VehicleType.normalize(vehicleType)
            val seats = VehicleType.resolveSeats(normalizedVehicleType, requestedSeats)
            val existingDoc = ridesRef
                .whereEqualTo("riderId", riderId)
                .whereEqualTo("rideDate", rideDate)
                .whereEqualTo("tripDirection", tripDirection)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()

            val existing = existingDoc?.toObject(Ride::class.java)?.copy(rideId = existingDoc.id)
            val routeKey = buildRouteKey(tripDirection, pickup, destination)

            if (existing != null) {
                // Re-checked transactionally below, not just here: the
                // query above can't see a concurrent acceptRequest() that
                // lands in the gap between this read and the write that
                // follows it. A plain get()-then-update() here would let
                // such an accept be silently overwritten - seat counts
                // included - the instant after it was granted.
                return try {
                    db.runTransaction { transaction ->
                        val docRef = ridesRef.document(existing.rideId)
                        val fresh = transaction.get(docRef)
                            .toObject(Ride::class.java)
                            ?.copy(rideId = docRef.id)
                            ?: throw TomorrowLegBlockedException(
                                "Your ${tripDirection.readableDirection()} ride no longer exists."
                            )

                        if (fresh.status != "active" || fresh.seatsTaken() > 0) {
                            throw TomorrowLegBlockedException(
                                "Your ${tripDirection.readableDirection()} ride is already " +
                                        "matched with a passenger and can't be edited here."
                            )
                        }

                        transaction.update(
                            docRef,
                            mapOf(
                                "riderName" to riderName,
                                "pickup" to pickup,
                                "destination" to destination,
                                "tripTime" to tripTime,
                                "timeMinutes" to timeMinutes,
                                "routeKey" to routeKey,
                                "vehicleType" to normalizedVehicleType,
                                "vehicleModel" to vehicleModel,
                                "vehicleNumber" to vehicleNumber,
                                "vehicleColor" to vehicleColor,
                                // Safe to reset both outright: the
                                // transaction just confirmed zero seats
                                // taken, on the current server state.
                                "totalSeats" to seats,
                                "availableSeats" to seats,
                                "status" to "active"
                            )
                        )
                    }.await()

                    Result.success(TomorrowLegResult.Saved(existing.rideId, isNew = false))
                } catch (e: TomorrowLegBlockedException) {
                    Result.success(TomorrowLegResult.Blocked(e.message ?: "This ride can't be edited here."))
                }
            }

            val docRef = ridesRef.document()
            val ride = Ride(
                rideId = docRef.id,
                riderId = riderId,
                riderName = riderName,
                tripDirection = tripDirection,
                pickup = pickup,
                destination = destination,
                tripTime = tripTime,
                timeMinutes = timeMinutes,
                routeKey = routeKey,
                rideDate = rideDate,
                vehicleType = normalizedVehicleType,
                vehicleModel = vehicleModel,
                vehicleNumber = vehicleNumber,
                vehicleColor = vehicleColor,
                totalSeats = seats,
                availableSeats = seats,
                status = "active",
                isTomorrowSetup = true,
                createdAt = Timestamp.now()
            )

            docRef.set(ride).await()
            Result.success(TomorrowLegResult.Saved(docRef.id, isNew = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * A rider's own requests that have already been accepted (or are
     * further along the trip lifecycle - started/ongoing/completed), for
     * this date. Needed so the UI has a way to find which request is tied
     * to a given matched ride (the pending-requests listener alone stops
     * tracking a request the moment it's accepted). Must include every
     * lifecycle status, not just "accepted" - otherwise the rider's UI
     * loses track of the request the instant it moves to the next stage.
     */
    fun listenAcceptedRequestsForRider(
        riderId: String,
        rideDate: String,
        onData: (List<RideRequest>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return rideRequestsRef
            .whereEqualTo("matchedRiderId", riderId)
            .whereEqualTo("rideDate", rideDate)
            .whereIn("status", RideRequestStatus.ACTIVE_LIFECYCLE_STATUSES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(RideRequest::class.java)?.copy(requestId = doc.id)
                    }
                    ?: emptyList()

                onData(requests)
            }
    }

    /**
     * Cancels a request already matched to this rider, restoring the
     * seat on their own ride in the same transaction. Safe as a direct
     * client write because both documents belong to the rider - contrast
     * with a passenger-initiated cancellation, which needs a Cloud
     * Function since it would touch the rider's ride document instead.
     */
    suspend fun riderCancelAcceptedRide(
        rideId: String,
        requestId: String,
        riderId: String,
        reason: String
    ): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val rideDoc = ridesRef.document(rideId)
                val requestDoc = rideRequestsRef.document(requestId)

                val rideSnapshot = transaction.get(rideDoc)
                val requestSnapshot = transaction.get(requestDoc)

                if (!rideSnapshot.exists()) throw Exception("Ride not found.")
                if (!requestSnapshot.exists()) throw Exception("Request not found.")

                val ride = rideSnapshot.toObject(Ride::class.java)
                    ?: throw Exception("Invalid ride data.")
                val request = requestSnapshot.toObject(RideRequest::class.java)
                    ?: throw Exception("Invalid request data.")

                if (ride.riderId != riderId) throw Exception("Not your ride.")
                if (request.matchedRiderId != riderId) throw Exception("This request isn't matched to you.")
                if (request.status != "accepted") throw Exception("This request is no longer accepted.")

                val now = Timestamp.now()

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to "cancelled",
                        "cancelledBy" to riderId,
                        "cancelledByRole" to "rider",
                        "cancellationReason" to reason,
                        "cancelledAt" to now
                    )
                )

                // Give the seat back, but never above the capacity the rider
                // actually opened - otherwise repeated cancel/accept cycles
                // could inflate a 2-seat car into a 5-seat one.
                transaction.update(
                    rideDoc,
                    mapOf(
                        "availableSeats" to (ride.availableSeats + 1)
                            .coerceAtMost(ride.seatCapacity()),
                        "status" to "active"
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- Rider abandonment escape hatches ----------
    // Mirrors RideNowRequestRepository's riderCancelUnstartedTrip /
    // riderCloseUnconfirmedTrip. Without these, a Tomorrow leg that's moved
    // past ACCEPTED has no way out for either side except waiting out the
    // full missed-ride grace window (MissedRideWindow.GRACE_MINUTES), even
    // when it's obvious to the rider the passenger isn't coming or isn't
    // answering.

    /**
     * Lets the matched rider walk away from a trip the passenger never got
     * into: still START_PENDING_CONFIRMATION (the rider pressed Start and
     * the passenger never confirmed). Restores the seat, same as a normal
     * cancel.
     */
    suspend fun riderCancelUnstartedTrip(
        rideId: String,
        requestId: String,
        riderId: String,
        reason: String
    ): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val rideDoc = ridesRef.document(rideId)
                val requestDoc = rideRequestsRef.document(requestId)

                val rideSnapshot = transaction.get(rideDoc)
                val requestSnapshot = transaction.get(requestDoc)

                if (!rideSnapshot.exists()) throw Exception("Ride not found.")
                if (!requestSnapshot.exists()) throw Exception("Request not found.")

                val ride = rideSnapshot.toObject(Ride::class.java)
                    ?: throw Exception("Invalid ride data.")
                val request = requestSnapshot.toObject(RideRequest::class.java)
                    ?: throw Exception("Invalid request data.")

                if (request.matchedRiderId != riderId) {
                    throw Exception("This request isn't matched to you.")
                }

                if (request.status != RideRequestStatus.START_PENDING_CONFIRMATION) {
                    throw Exception("Only a trip that never started can be cancelled here.")
                }

                val now = Timestamp.now()

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to "cancelled",
                        "cancelledBy" to riderId,
                        "cancelledByRole" to "rider",
                        "cancellationReason" to reason,
                        "cancelledAt" to now
                    )
                )

                transaction.update(
                    rideDoc,
                    mapOf(
                        "availableSeats" to (ride.availableSeats + 1)
                            .coerceAtMost(ride.seatCapacity()),
                        "status" to "active"
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Closes a trip that was actually driven but which the passenger never
     * confirmed the end of - ONGOING or END_PENDING_CONFIRMATION with no
     * response. The outcome is UNVERIFIED, not COMPLETED: only the rider
     * ever vouched for it, so it earns no history entry, no rating, no XP.
     * Still restores the seat, since the leg is over either way.
     */
    suspend fun riderCloseUnconfirmedTrip(
        rideId: String,
        requestId: String,
        riderId: String
    ): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val rideDoc = ridesRef.document(rideId)
                val requestDoc = rideRequestsRef.document(requestId)

                val rideSnapshot = transaction.get(rideDoc)
                val requestSnapshot = transaction.get(requestDoc)

                if (!rideSnapshot.exists()) throw Exception("Ride not found.")
                if (!requestSnapshot.exists()) throw Exception("Request not found.")

                val ride = rideSnapshot.toObject(Ride::class.java)
                    ?: throw Exception("Invalid ride data.")
                val request = requestSnapshot.toObject(RideRequest::class.java)
                    ?: throw Exception("Invalid request data.")

                if (request.matchedRiderId != riderId) {
                    throw Exception("This request isn't matched to you.")
                }

                if (request.status != RideRequestStatus.ONGOING &&
                    request.status != RideRequestStatus.END_PENDING_CONFIRMATION
                ) {
                    throw Exception("Only a trip already under way can be closed here.")
                }

                val now = Timestamp.now()

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to RideRequestStatus.UNVERIFIED,
                        "closedByRole" to "rider",
                        "closedAt" to now
                    )
                )

                transaction.update(
                    rideDoc,
                    mapOf(
                        "availableSeats" to (ride.availableSeats + 1)
                            .coerceAtMost(ride.seatCapacity()),
                        "status" to "active"
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- Missed-trip reconciliation ----------

    /**
     * Every leg still sitting in an unfinished lifecycle status for this
     * user, across *all* dates - deliberately not filtered to one
     * rideDate like the listeners above.
     *
     * That date filter is exactly why a missed ride disappears today: the
     * Tomorrow tab only ever listens for tomorrow's key, so a leg booked
     * on Monday for Tuesday 8:00 AM becomes invisible the moment Tuesday
     * arrives, and can never be finished, rated, or cleared. This listener
     * is the way back to it. Callers narrow the result down to the
     * genuinely overdue ones with RideRequest.needsMissedRideReview().
     */
    fun listenPassengerUnfinishedLegs(
        userId: String,
        onData: (List<RideRequest>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return rideRequestsRef
            .whereEqualTo("userId", userId)
            .whereIn("status", RideRequestStatus.UNFINISHED_LIFECYCLE_STATUSES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                onData(snapshot.toRideRequests())
            }
    }

    /** Rider-side counterpart of [listenPassengerUnfinishedLegs]. */
    fun listenRiderUnfinishedLegs(
        riderId: String,
        onData: (List<RideRequest>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return rideRequestsRef
            .whereEqualTo("matchedRiderId", riderId)
            .whereIn("status", RideRequestStatus.UNFINISHED_LIFECYCLE_STATUSES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                onData(snapshot.toRideRequests())
            }
    }

    /**
     * Records one side's answer to "did this ride actually happen?" for a
     * leg that was never driven through the normal Start/Complete flow.
     *
     * The first answer only stores itself and leaves the status alone -
     * there is nothing to conclude from one voice. The second answer also
     * resolves the leg, and the outcome is derived by
     * RideRequest.resolvedStatusAfter() rather than being passed in, so
     * neither side can pick its own verdict:
     *   both "yes"  -> COMPLETED   (counts, and unlocks the XP claim)
     *   both "no"   -> NOT_COMPLETED
     *   disagreeing -> UNVERIFIED  (explicitly not finished; counts for
     *                               nobody)
     *
     * Answering is one-shot per side: a second attempt fails rather than
     * silently overwriting, which is also what firestore.rules enforces.
     */
    suspend fun submitMissedRideAnswer(
        requestId: String,
        userId: String,
        isRider: Boolean,
        answer: String
    ): Result<String> {
        if (answer != MissedRideAnswer.YES && answer != MissedRideAnswer.NO) {
            return Result.failure(Exception("Invalid answer."))
        }

        return try {
            val resolvedStatus = db.runTransaction { transaction ->
                val requestDoc = rideRequestsRef.document(requestId)
                val request = transaction.get(requestDoc)
                    .toObject(RideRequest::class.java)
                    ?: throw Exception("Invalid ride request data.")

                val ownerId = if (isRider) request.matchedRiderId else request.userId
                if (ownerId != userId) {
                    throw Exception("This trip isn't yours to confirm.")
                }

                if (request.status !in RideRequestStatus.UNFINISHED_LIFECYCLE_STATUSES) {
                    throw Exception("This trip has already been closed.")
                }

                if (request.answerFor(isRider).isNotBlank()) {
                    throw Exception("You already answered for this trip.")
                }

                val newStatus = request.resolvedStatusAfter(isRider, answer)
                val now = Timestamp.now()

                val updates = mutableMapOf<String, Any?>(
                    "status" to newStatus,
                    (if (isRider) "riderHappenedAnswer" else "passengerHappenedAnswer") to answer,
                    (if (isRider) "riderAnsweredAt" else "passengerAnsweredAt") to now
                )

                // completedAt is what the rest of the app reads as "when
                // did this trip end", and a leg reconciled all the way to
                // COMPLETED never got a real one.
                if (newStatus == RideRequestStatus.COMPLETED) {
                    updates["completedAt"] = now
                }

                transaction.update(requestDoc, updates)

                newStatus
            }.await()

            if (resolvedStatus == RideRequestStatus.COMPLETED) {
                val completedRequest = getRequestById(requestId)
                if (completedRequest != null) {
                    saveTomorrowToHistory(completedRequest)
                }
            }

            Result.success(resolvedStatus)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- Trip lifecycle (mirrors RideNowRequestRepository) ----------

    suspend fun riderStartTrip(requestId: String, riderId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val requestDoc = rideRequestsRef.document(requestId)
                val snapshot = transaction.get(requestDoc)

                val request = snapshot.toObject(RideRequest::class.java)
                    ?: throw Exception("Invalid ride request data.")

                if (request.matchedRiderId != riderId) {
                    throw Exception("This request isn't matched to you.")
                }

                if (request.status != RideRequestStatus.ACCEPTED) {
                    throw Exception("Trip can only be started after it is accepted.")
                }

                // Backstop for the UI's own canStartTrip() gate (RiderDashboardScreen) -
                // keeps a rider from locking the passenger into a trip hours
                // before the planned departure even if the client-side check
                // is bypassed or stale.
                if (!request.canStartTrip()) {
                    throw Exception(
                        "You can only start this trip within 1 hour of the scheduled time."
                    )
                }

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to RideRequestStatus.START_PENDING_CONFIRMATION,
                        "rideStartedByRider" to true,
                        "rideConfirmedByPassenger" to false,
                        "startedAt" to null
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun passengerConfirmTripStarted(requestId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val requestDoc = rideRequestsRef.document(requestId)
                val snapshot = transaction.get(requestDoc)

                val request = snapshot.toObject(RideRequest::class.java)
                    ?: throw Exception("Invalid ride request data.")

                if (request.status != RideRequestStatus.START_PENDING_CONFIRMATION) {
                    throw Exception("Ride is not waiting for start confirmation.")
                }

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to RideRequestStatus.ONGOING,
                        "rideConfirmedByPassenger" to true,
                        "startedAt" to Timestamp.now()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun passengerRejectTripStarted(requestId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val requestDoc = rideRequestsRef.document(requestId)
                val snapshot = transaction.get(requestDoc)

                val request = snapshot.toObject(RideRequest::class.java)
                    ?: throw Exception("Invalid ride request data.")

                if (request.status != RideRequestStatus.START_PENDING_CONFIRMATION) {
                    throw Exception("Ride is not waiting for start confirmation.")
                }

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to RideRequestStatus.ACCEPTED,
                        "rideStartedByRider" to false,
                        "rideConfirmedByPassenger" to false,
                        "startedAt" to null
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun riderRequestTripCompletion(requestId: String, riderId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val requestDoc = rideRequestsRef.document(requestId)
                val snapshot = transaction.get(requestDoc)

                val request = snapshot.toObject(RideRequest::class.java)
                    ?: throw Exception("Invalid ride request data.")

                if (request.matchedRiderId != riderId) {
                    throw Exception("This request isn't matched to you.")
                }

                if (request.status != RideRequestStatus.ONGOING) {
                    throw Exception("Only an ongoing trip can be marked for completion.")
                }

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to RideRequestStatus.END_PENDING_CONFIRMATION,
                        "rideEndedByRider" to true,
                        "rideCompletedByPassenger" to false,
                        "completedAt" to null
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun passengerConfirmTripCompleted(requestId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val requestDoc = rideRequestsRef.document(requestId)
                val snapshot = transaction.get(requestDoc)

                val request = snapshot.toObject(RideRequest::class.java)
                    ?: throw Exception("Invalid ride request data.")

                if (request.status != RideRequestStatus.END_PENDING_CONFIRMATION) {
                    throw Exception("Ride is not waiting for completion confirmation.")
                }

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to RideRequestStatus.COMPLETED,
                        "rideCompletedByPassenger" to true,
                        "completedAt" to Timestamp.now()
                    )
                )

                if (request.matchedRiderId.isNotBlank()) {
                    transaction.update(
                        usersRef.document(request.matchedRiderId),
                        mapOf(
                            "completedRideCount" to FieldValue.increment(1),
                            // Lets firestore.rules' isValidCompletedRideBump()
                            // verify this bump against a real ride tying the
                            // passenger to the rider being credited.
                            "lastCompletedRideEvidenceId" to requestId
                        )
                    )
                }
            }.await()

            val completedRequest = getRequestById(requestId)
            if (completedRequest != null) {
                saveTomorrowToHistory(completedRequest)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRequestById(requestId: String): RideRequest? {
        return try {
            val snapshot = rideRequestsRef.document(requestId).get().await()
            snapshot.toObject(RideRequest::class.java)?.copy(requestId = snapshot.id)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Saves a completed Tomorrow leg to the shared ride_history collection
     * - the same one Ride Now writes to, so RideHistoryScreen reads both
     * ride types from one place. Without this, a completed Tomorrow leg
     * never appeared in Ride History for either side: the collection only
     * ever received "ride_now" rows.
     *
     * Called once a leg reaches COMPLETED, whether through the normal
     * Start/Complete lifecycle (passengerConfirmTripCompleted) or the
     * missed-ride reconciliation path (submitMissedRideAnswer) - both call
     * sites pass the just-updated request, so this never needs its own
     * read, and each only ever calls it once per leg (their own
     * transactions gate on the leg's prior status, so a retry after
     * success fails the precondition before reaching this call).
     */
    suspend fun saveTomorrowToHistory(request: RideRequest): Result<Unit> {
        return try {
            val historyDoc = rideHistoryRef.document()

            val history = RideHistory(
                historyId = historyDoc.id,
                rideType = "tomorrow",

                requestId = request.requestId,

                passengerId = request.userId,
                passengerName = request.passengerName,

                riderId = request.matchedRiderId,
                riderName = request.matchedRiderName,
                riderPhone = request.matchedRiderPhone,

                pickup = request.pickup,
                destination = request.destination,
                tripTime = request.tripTime,
                timeMinutes = request.timeMinutes,

                status = "completed",
                createdAt = request.createdAt,
                completedAt = request.completedAt ?: Timestamp.now()
            )

            historyDoc.set(history).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRide(rideId: String, riderId: String): Result<Unit> {
        return try {
            val snapshot = ridesRef.document(rideId).get().await()
            val ride = snapshot.toObject(Ride::class.java)
                ?: throw Exception("Ride not found.")

            if (ride.riderId != riderId) {
                throw Exception("You can only remove your own rides.")
            }

            // Same reasoning as upsertRiderRide: a part-full car is still
            // "active" but already has a passenger counting on it.
            if (ride.status != "active" || ride.seatsTaken() > 0) {
                throw Exception(
                    "This ride is already matched with a passenger and can't be removed here."
                )
            }

            ridesRef.document(rideId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- Passenger: saved "Tomorrow" requests ----------

    fun listenPassengerRequests(
        userId: String,
        rideDate: String,
        onData: (List<RideRequest>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return rideRequestsRef
            .whereEqualTo("userId", userId)
            .whereEqualTo("rideDate", rideDate)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(RideRequest::class.java)?.copy(requestId = doc.id)
                    }
                    ?.sortedBy { request ->
                        when (request.tripDirection.lowercase()) {
                            "to_campus" -> 0
                            "to_home" -> 1
                            else -> 2
                        }
                    }
                    ?: emptyList()

                onData(requests)
            }
    }

    /**
     * Same upsert-not-overwrite pattern as upsertRiderRide, for a
     * passenger's request in one direction.
     */
    suspend fun upsertPassengerRequest(
        userId: String,
        passengerName: String,
        passengerPhone: String,
        rideDate: String,
        tripDirection: String,
        pickup: String,
        destination: String,
        tripTime: String,
        hour: Int,
        minute: Int,
        timeMinutes: Int
    ): Result<TomorrowLegResult> {
        return try {
            val existingDoc = rideRequestsRef
                .whereEqualTo("userId", userId)
                .whereEqualTo("rideDate", rideDate)
                .whereEqualTo("tripDirection", tripDirection)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()

            val existing = existingDoc?.toObject(RideRequest::class.java)
                ?.copy(requestId = existingDoc.id)
            val routeKey = buildRouteKey(tripDirection, pickup, destination)

            if (existing != null) {
                // Re-checked transactionally below - see the matching note
                // in upsertRiderRide above. A concurrent acceptRequest()
                // landing between the query above and a plain update()
                // here could otherwise be silently reverted back to
                // "pending" with its match info wiped, right after the
                // rider accepted it.
                return try {
                    db.runTransaction { transaction ->
                        val docRef = rideRequestsRef.document(existing.requestId)
                        val fresh = transaction.get(docRef)
                            .toObject(RideRequest::class.java)
                            ?.copy(requestId = docRef.id)
                            ?: throw TomorrowLegBlockedException(
                                "Your ${tripDirection.readableDirection()} request no longer exists."
                            )

                        if (fresh.status != "pending" && fresh.status != "cancelled") {
                            throw TomorrowLegBlockedException(
                                "Your ${tripDirection.readableDirection()} request is already " +
                                        "accepted and can't be edited here."
                            )
                        }

                        // "pending" is resubmitted in place. "cancelled"
                        // (e.g. the rider backed out after accepting) is
                        // also resubmitted in place, rather than blocked
                        // forever - otherwise the leg can never return to
                        // "pending", so no other rider can ever see or
                        // match it again. Clear the stale match info from
                        // the previous rider so the doc looks like a fresh
                        // request.
                        transaction.update(
                            docRef,
                            mapOf(
                                "passengerName" to passengerName,
                                "passengerPhone" to passengerPhone,
                                "pickup" to pickup,
                                "destination" to destination,
                                "tripTime" to tripTime,
                                "hour" to hour,
                                "minute" to minute,
                                "timeMinutes" to timeMinutes,
                                "routeKey" to routeKey,
                                "status" to "pending",
                                "matchedRideId" to "",
                                "matchedRiderId" to "",
                                "matchedRiderName" to "",
                                "matchedRiderPhone" to "",
                                "matchedRideTime" to "",
                                "matchedVehicleType" to "",
                                "matchedVehicleModel" to "",
                                "matchedVehicleNumber" to "",
                                "matchedVehicleColor" to "",
                                "acceptedAt" to null,
                                "cancelledBy" to "",
                                "cancelledByRole" to "",
                                "cancellationReason" to "",
                                "cancelledAt" to null
                            )
                        )
                    }.await()

                    Result.success(TomorrowLegResult.Saved(existing.requestId, isNew = false))
                } catch (e: TomorrowLegBlockedException) {
                    Result.success(TomorrowLegResult.Blocked(e.message ?: "This request can't be edited here."))
                }
            }

            val docRef = rideRequestsRef.document()
            val request = RideRequest(
                requestId = docRef.id,
                userId = userId,
                passengerName = passengerName,
                passengerPhone = passengerPhone,
                pickup = pickup,
                destination = destination,
                tripDirection = tripDirection,
                tripTime = tripTime,
                hour = hour,
                minute = minute,
                timeMinutes = timeMinutes,
                routeKey = routeKey,
                rideDate = rideDate,
                status = "pending",
                createdAt = Timestamp.now()
            )

            docRef.set(request).await()
            Result.success(TomorrowLegResult.Saved(docRef.id, isNew = true))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancels a passenger's own accepted request via the standalone REST
     * server (see /server in the project root), which verifies the
     * caller's Firebase ID token and does the full cancellation - request
     * status + seat restore - in one transaction. Replaces the earlier
     * Cloud-Function-based two-step version: this is synchronous, so the
     * cancellation is fully finalized by the time this call returns.
     */
    suspend fun requestPassengerCancellation(
        requestId: String,
        userId: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val idToken = FirebaseAuth.getInstance().currentUser
                ?.getIdToken(false)
                ?.await()
                ?.token
                ?: throw Exception("Not authenticated.")

            val payload = JSONObject().apply {
                put("requestId", requestId)
                put("reason", reason)
            }

            val body = payload.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val httpRequest = Request.Builder()
                .url("$apiBaseUrl/api/tomorrow/cancel-request")
                .addHeader("Authorization", "Bearer $idToken")
                .post(body)
                .build()

            httpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    throw Exception(errorBody ?: "Cancellation failed (${response.code}).")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Shared shape for the two notify-* endpoints below - both take just
     * {requestId} and a Bearer token, same as requestPassengerCancellation.
     * Best-effort by design: callers fire these after their own real
     * Firestore write already succeeded and don't surface failures here to
     * the UI - push delivery isn't critical-path.
     */
    private suspend fun postNotifyRequest(
        path: String,
        requestId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val idToken = FirebaseAuth.getInstance().currentUser
                ?.getIdToken(false)
                ?.await()
                ?.token
                ?: throw Exception("Not authenticated.")

            val payload = JSONObject().apply {
                put("requestId", requestId)
            }

            val body = payload.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val httpRequest = Request.Builder()
                .url("$apiBaseUrl$path")
                .addHeader("Authorization", "Bearer $idToken")
                .post(body)
                .build()

            httpClient.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string()
                    throw Exception(errorBody ?: "Notify failed (${response.code}).")
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Pushes the passenger once the rider's accept transaction has landed. */
    suspend fun notifyPassengerAccepted(requestId: String): Result<Unit> =
        postNotifyRequest("/api/tomorrow/notify-accepted", requestId)

    /** Pushes any rider whose saved ride matches this newly (re)submitted request. */
    suspend fun notifyMatchingRiders(requestId: String): Result<Unit> =
        postNotifyRequest("/api/tomorrow/notify-match", requestId)

    suspend fun deleteRequest(requestId: String, userId: String): Result<Unit> {
        return try {
            val snapshot = rideRequestsRef.document(requestId).get().await()
            val request = snapshot.toObject(RideRequest::class.java)
                ?: throw Exception("Request not found.")

            if (request.userId != userId) {
                throw Exception("You can only remove your own requests.")
            }

            if (request.status != "pending") {
                throw Exception(
                    "This request is already accepted and can't be removed here."
                )
            }

            rideRequestsRef.document(requestId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ---------- Matching ----------

    /**
     * All currently "active" rides for a date, for the passenger side to
     * match against their own pending requests. Filtering to available
     * seats and route/time proximity happens in the ViewModel, same as the
     * rider-side matching already does.
     */
    fun listenActiveRidesForDate(
        rideDate: String,
        onData: (List<Ride>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return ridesRef
            .whereEqualTo("rideDate", rideDate)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val rides = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(Ride::class.java)?.copy(rideId = doc.id)
                    }
                    ?: emptyList()

                onData(rides)
            }
    }

    /**
     * All currently "pending" requests for a date, for the rider side to
     * match against their own saved rides.
     */
    fun listenPendingRequests(
        rideDate: String,
        onData: (List<RideRequest>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return rideRequestsRef
            .whereEqualTo("rideDate", rideDate)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(RideRequest::class.java)?.copy(requestId = doc.id)
                    }
                    ?: emptyList()

                onData(requests)
            }
    }

    suspend fun declineRequest(requestId: String, riderId: String): Result<Unit> {
        return try {
            rideRequestsRef.document(requestId)
                .update("rejectedByRiderIds", FieldValue.arrayUnion(riderId))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptRequest(
        rideId: String,
        requestId: String,
        riderId: String,
        riderName: String,
        riderPhone: String
    ): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val rideDoc = ridesRef.document(rideId)
                val requestDoc = rideRequestsRef.document(requestId)

                val rideSnapshot = transaction.get(rideDoc)
                val requestSnapshot = transaction.get(requestDoc)

                if (!rideSnapshot.exists()) throw Exception("Ride not found.")
                if (!requestSnapshot.exists()) throw Exception("Request not found.")

                val ride = rideSnapshot.toObject(Ride::class.java)
                    ?: throw Exception("Invalid ride data.")

                val request = requestSnapshot.toObject(RideRequest::class.java)
                    ?: throw Exception("Invalid request data.")

                if (ride.riderId != riderId) throw Exception("Not your ride.")
                if (ride.status != "active") throw Exception("Ride no longer active.")
                if (ride.availableSeats <= 0) throw Exception("No seats available.")
                if (request.status != "pending") throw Exception("Request already handled.")

                val newSeats = ride.availableSeats - 1
                val now = Timestamp.now()

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to "accepted",
                        "matchedRideId" to ride.rideId,
                        "matchedRiderId" to riderId,
                        "matchedRiderName" to riderName,
                        "matchedRiderPhone" to riderPhone,
                        "matchedRideTime" to ride.tripTime,
                        // Copied off the ride so the passenger can see which
                        // vehicle to look for without reading the rider's
                        // own document.
                        "matchedVehicleType" to VehicleType.normalize(ride.vehicleType),
                        "matchedVehicleModel" to ride.vehicleModel,
                        "matchedVehicleNumber" to ride.vehicleNumber,
                        "matchedVehicleColor" to ride.vehicleColor,
                        "acceptedAt" to now
                    )
                )

                transaction.update(
                    rideDoc,
                    mapOf(
                        "availableSeats" to newSeats,
                        "status" to if (newSeats <= 0) "full" else "active"
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

private fun String.readableDirection(): String {
    return when (this) {
        "to_campus" -> "campus"
        "to_home" -> "return"
        else -> this
    }
}

/**
 * Standard snapshot -> RideRequest mapping used by the missed-ride
 * listeners: always toObject(), with the document id stamped back on
 * since requestId isn't stored inside the document itself.
 */
private fun QuerySnapshot?.toRideRequests(): List<RideRequest> {
    return this?.documents
        ?.mapNotNull { doc ->
            doc.toObject(RideRequest::class.java)?.copy(requestId = doc.id)
        }
        ?: emptyList()
}
