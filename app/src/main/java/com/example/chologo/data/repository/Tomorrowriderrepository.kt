package com.example.chologo.data.repository

import com.example.chologo.data.model.Ride
import com.example.chologo.data.model.RideRequest
import com.example.chologo.data.model.buildRouteKey
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

    // TODO: replace with your actual Render URL once deployed.
    private val apiBaseUrl = "https://chologo-api.onrender.com"

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
     * one in place if it's still "active" (unmatched). Refuses (returns
     * Blocked, not an exception) if the existing ride for this direction is
     * already matched with a passenger - editing must not silently destroy
     * an active match. This mirrors what the rules now enforce server-side.
     */
    suspend fun upsertRiderRide(
        riderId: String,
        riderName: String,
        rideDate: String,
        tripDirection: String,
        pickup: String,
        destination: String,
        tripTime: String,
        timeMinutes: Int
    ): Result<TomorrowLegResult> {
        return try {
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
                if (existing.status != "active") {
                    return Result.success(
                        TomorrowLegResult.Blocked(
                            "Your ${tripDirection.readableDirection()} ride is already " +
                                    "matched with a passenger and can't be edited here."
                        )
                    )
                }

                ridesRef.document(existing.rideId)
                    .update(
                        mapOf(
                            "riderName" to riderName,
                            "pickup" to pickup,
                            "destination" to destination,
                            "tripTime" to tripTime,
                            "timeMinutes" to timeMinutes,
                            "routeKey" to routeKey,
                            "availableSeats" to 1,
                            "status" to "active"
                        )
                    )
                    .await()

                return Result.success(TomorrowLegResult.Saved(existing.rideId, isNew = false))
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
                availableSeats = 1,
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
     * A rider's own requests that have already been accepted, for this
     * date. Needed so the UI has a way to find which request is tied to a
     * given matched ride (the pending-requests listener alone stops
     * tracking a request the moment it's accepted).
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
            .whereEqualTo("status", "accepted")
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

                transaction.update(
                    rideDoc,
                    mapOf(
                        "availableSeats" to (ride.availableSeats + 1),
                        "status" to "active"
                    )
                )
            }.await()

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

            if (ride.status != "active") {
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
                if (existing.status != "pending") {
                    return Result.success(
                        TomorrowLegResult.Blocked(
                            "Your ${tripDirection.readableDirection()} request is already " +
                                    "accepted and can't be edited here."
                        )
                    )
                }

                rideRequestsRef.document(existing.requestId)
                    .update(
                        mapOf(
                            "passengerName" to passengerName,
                            "pickup" to pickup,
                            "destination" to destination,
                            "tripTime" to tripTime,
                            "hour" to hour,
                            "minute" to minute,
                            "timeMinutes" to timeMinutes,
                            "routeKey" to routeKey,
                            "status" to "pending"
                        )
                    )
                    .await()

                return Result.success(TomorrowLegResult.Saved(existing.requestId, isNew = false))
            }

            val docRef = rideRequestsRef.document()
            val request = RideRequest(
                requestId = docRef.id,
                userId = userId,
                passengerName = passengerName,
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