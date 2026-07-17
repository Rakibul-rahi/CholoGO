package com.example.chologo.data.repository

import com.example.chologo.data.model.LiveRide
import com.example.chologo.data.model.RideHistory
import com.example.chologo.data.model.RideNowRequest
import com.example.chologo.data.model.RideNowStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class RideNowRequestRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val liveRidesRef = db.collection("live_rides")
    private val rideNowRequestsRef = db.collection("ride_now_requests")
    private val rideHistoryRef = db.collection("ride_history")

    suspend fun createRideNowRequest(request: RideNowRequest): Result<String> {
        return try {
            val blockingStatuses = listOf(
                RideNowStatus.SEARCHING,
                RideNowStatus.NOTIFIED,
                RideNowStatus.ACCEPTED,
                RideNowStatus.START_PENDING_CONFIRMATION,
                RideNowStatus.ONGOING,
                RideNowStatus.END_PENDING_CONFIRMATION
            )

            val existingRequest = rideNowRequestsRef
                .whereEqualTo("passengerId", request.passengerId)
                .whereIn("status", blockingStatuses)
                .limit(1)
                .get()
                .await()

            if (!existingRequest.isEmpty) {
                return Result.failure(Exception("You already have an active ride request."))
            }

            val docRef = if (request.requestId.isBlank()) {
                rideNowRequestsRef.document()
            } else {
                rideNowRequestsRef.document(request.requestId)
            }

            val now = Timestamp.now()

            val requestToSave = request.copy(
                requestId = docRef.id,
                status = RideNowStatus.SEARCHING,
                createdAt = request.createdAt ?: now,
                expiresAt = request.expiresAt ?: Timestamp(now.seconds + 120, 0)
            )

            docRef.set(requestToSave).await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelRideNowRequest(requestId: String): Result<Unit> {
        return try {
            rideNowRequestsRef.document(requestId)
                .update(
                    mapOf(
                        "status" to RideNowStatus.CANCELLED,
                        "cancelledAt" to Timestamp.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelAcceptedRideNowTrip(
        requestId: String,
        liveRideId: String
    ): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val requestDoc = rideNowRequestsRef.document(requestId)
                val rideDoc = liveRidesRef.document(liveRideId)

                val requestSnapshot = transaction.get(requestDoc)

                if (!requestSnapshot.exists()) {
                    throw Exception("Ride request not found.")
                }

                val request = requestSnapshot.toObject(RideNowRequest::class.java)
                    ?: throw Exception("Invalid ride request data.")

                if (request.status == RideNowStatus.COMPLETED) {
                    throw Exception("Completed ride cannot be cancelled.")
                }

                val now = Timestamp.now()

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to RideNowStatus.CANCELLED,
                        "cancelledAt" to now
                    )
                )

                transaction.update(
                    rideDoc,
                    mapOf(
                        "isAvailable" to true,
                        "currentRequestId" to "",
                        "lastUpdatedAt" to now
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun listenForMatchingRequests(
        routeKey: String,
        onData: (List<RideNowRequest>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return rideNowRequestsRef
            .whereEqualTo("routeKey", routeKey)
            .whereEqualTo("status", RideNowStatus.SEARCHING)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val requests = value?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(RideNowRequest::class.java)?.copy(
                            requestId = doc.id
                        )
                    }
                    ?: emptyList()

                onData(requests)
            }
    }

    fun listenToPassengerRequest(
        requestId: String,
        onData: (RideNowRequest?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return rideNowRequestsRef.document(requestId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val request = snapshot?.toObject(RideNowRequest::class.java)?.copy(
                    requestId = snapshot.id
                )

                onData(request)
            }
    }

    fun listenPassengerActiveRide(
        passengerId: String,
        onData: (RideNowRequest?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return rideNowRequestsRef
            .whereEqualTo("passengerId", passengerId)
            .whereIn(
                "status",
                listOf(
                    RideNowStatus.SEARCHING,
                    RideNowStatus.NOTIFIED,
                    RideNowStatus.ACCEPTED,
                    RideNowStatus.START_PENDING_CONFIRMATION,
                    RideNowStatus.ONGOING,
                    RideNowStatus.END_PENDING_CONFIRMATION
                )
            )
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val request = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(RideNowRequest::class.java)?.copy(
                            requestId = doc.id
                        )
                    }
                    ?.firstOrNull()

                onData(request)
            }
    }

    suspend fun acceptRideNowRequest(
        requestId: String,
        liveRideId: String,
        riderId: String,
        riderName: String,
        riderPhone: String
    ): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val requestDoc = rideNowRequestsRef.document(requestId)
                val rideDoc = liveRidesRef.document(liveRideId)

                val requestSnapshot = transaction.get(requestDoc)
                val rideSnapshot = transaction.get(rideDoc)

                if (!requestSnapshot.exists()) throw Exception("Ride now request not found.")
                if (!rideSnapshot.exists()) throw Exception("Live ride not found.")

                val request = requestSnapshot.toObject(RideNowRequest::class.java)
                    ?: throw Exception("Invalid request data.")

                // IMPORTANT: use toObject(), not rideSnapshot.data (raw map).
                // Firestore strips the "is" prefix from Kotlin boolean field
                // names when it serializes a document - isAvailable is
                // stored as "available", isLiveNow as "liveNow". Reading
                // those via raw map keys like ride["isAvailable"] always
                // returns null (since no such key exists), which made this
                // check throw "This rider is no longer available" on every
                // single accept attempt, regardless of the document's real
                // state. toObject() correctly reverses that field-name
                // mapping back onto the LiveRide data class properties.
                val ride = rideSnapshot.toObject(LiveRide::class.java)
                    ?: throw Exception("Invalid live ride data.")

                val now = Timestamp.now()
                val expiresAtSeconds = request.expiresAt?.seconds
                val isExpired = expiresAtSeconds != null && expiresAtSeconds <= now.seconds
                val isRideBusy = ride.currentRequestId.isNotBlank()

                if (request.status != RideNowStatus.SEARCHING) {
                    throw Exception("This request was already taken or closed.")
                }

                if (isExpired) {
                    transaction.update(
                        requestDoc,
                        mapOf(
                            "status" to RideNowStatus.EXPIRED,
                            "expiredAt" to now
                        )
                    )
                    throw Exception("This request already expired.")
                }

                if (ride.status != "active" ||
                    !ride.isLiveNow ||
                    !ride.isAvailable
                ) {
                    throw Exception("This rider is no longer available.")
                }

                if (isRideBusy) {
                    throw Exception("This rider already has an active request.")
                }

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to RideNowStatus.ACCEPTED,
                        "matchedRideId" to liveRideId,
                        "matchedRiderId" to riderId,
                        "matchedRiderName" to riderName,
                        "matchedRiderPhone" to riderPhone,
                        "acceptedAt" to now,
                        "rideStartedByRider" to false,
                        "rideConfirmedByPassenger" to false,
                        "rideEndedByRider" to false,
                        "rideCompletedByPassenger" to false,
                        "startedAt" to null,
                        "completedAt" to null
                    )
                )

                transaction.update(
                    rideDoc,
                    mapOf(
                        "isAvailable" to false,
                        "currentRequestId" to requestId,
                        "lastUpdatedAt" to now
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun riderStartRideNowTrip(requestId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val requestDoc = rideNowRequestsRef.document(requestId)
                val snapshot = transaction.get(requestDoc)

                val request = snapshot.toObject(RideNowRequest::class.java)
                    ?: throw Exception("Invalid ride request data.")

                if (request.status != RideNowStatus.ACCEPTED) {
                    throw Exception("Trip can only be started after it is accepted.")
                }

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to RideNowStatus.START_PENDING_CONFIRMATION,
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

    suspend fun passengerConfirmRideNowStarted(requestId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val requestDoc = rideNowRequestsRef.document(requestId)
                val snapshot = transaction.get(requestDoc)

                val request = snapshot.toObject(RideNowRequest::class.java)
                    ?: throw Exception("Invalid ride request data.")

                if (request.status != RideNowStatus.START_PENDING_CONFIRMATION) {
                    throw Exception("Ride is not waiting for start confirmation.")
                }

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to RideNowStatus.ONGOING,
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

    suspend fun passengerRejectRideNowStarted(requestId: String): Result<Unit> {
        return try {
            rideNowRequestsRef.document(requestId)
                .update(
                    mapOf(
                        "status" to RideNowStatus.ACCEPTED,
                        "rideStartedByRider" to false,
                        "rideConfirmedByPassenger" to false,
                        "startedAt" to null
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun riderRequestRideNowCompletion(requestId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val requestDoc = rideNowRequestsRef.document(requestId)
                val snapshot = transaction.get(requestDoc)

                val request = snapshot.toObject(RideNowRequest::class.java)
                    ?: throw Exception("Invalid ride request data.")

                if (request.status != RideNowStatus.ONGOING) {
                    throw Exception("Only an ongoing trip can be marked for completion.")
                }

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to RideNowStatus.END_PENDING_CONFIRMATION,
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

    suspend fun passengerConfirmRideNowCompleted(
        requestId: String,
        liveRideId: String
    ): Result<Unit> {
        return try {
            val completedAt = Timestamp.now()

            db.runTransaction { transaction ->
                val requestDoc = rideNowRequestsRef.document(requestId)
                val rideDoc = liveRidesRef.document(liveRideId)

                val requestSnapshot = transaction.get(requestDoc)

                val request = requestSnapshot.toObject(RideNowRequest::class.java)
                    ?: throw Exception("Invalid ride request data.")

                if (request.status != RideNowStatus.END_PENDING_CONFIRMATION) {
                    throw Exception("Ride is not waiting for completion confirmation.")
                }

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to RideNowStatus.COMPLETED,
                        "rideCompletedByPassenger" to true,
                        "completedAt" to completedAt
                    )
                )

                transaction.update(
                    rideDoc,
                    mapOf(
                        "isAvailable" to true,
                        "currentRequestId" to "",
                        "lastUpdatedAt" to completedAt
                    )
                )
            }.await()

            val completedRequest = getRideNowRequestById(requestId)

            if (completedRequest != null) {
                saveRideNowToHistory(completedRequest)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reports an issue on a Ride Now trip and releases the matched rider's
     * LiveRide so they can be matched again.
     *
     * Validates inside the transaction, matching every other lifecycle
     * function in this file:
     *  - the request actually exists
     *  - the request isn't already in a finished/terminal state (prevents
     *    re-releasing a LiveRide that's already been reassigned to a new
     *    request since this one closed)
     *  - the given liveRideId actually matches the request's matchedRideId
     *    (prevents a stale/incorrect liveRideId passed by the caller from
     *    releasing a *different* rider's currently active trip)
     */
    suspend fun passengerReportRideNowIssue(
        requestId: String,
        liveRideId: String
    ): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val requestDoc = rideNowRequestsRef.document(requestId)
                val rideDoc = liveRidesRef.document(liveRideId)

                val requestSnapshot = transaction.get(requestDoc)

                if (!requestSnapshot.exists()) {
                    throw Exception("Ride request not found.")
                }

                val request = requestSnapshot.toObject(RideNowRequest::class.java)
                    ?: throw Exception("Invalid ride request data.")

                if (request.status in RideNowStatus.FINISHED_STATUSES) {
                    throw Exception("This ride is already closed.")
                }

                if (request.matchedRideId.isNotBlank() && request.matchedRideId != liveRideId) {
                    throw Exception("This live ride does not match the reported request.")
                }

                val now = Timestamp.now()

                transaction.update(
                    requestDoc,
                    mapOf(
                        "status" to RideNowStatus.ISSUE_REPORTED,
                        "issueReportedAt" to now
                    )
                )

                transaction.update(
                    rideDoc,
                    mapOf(
                        "isAvailable" to true,
                        "currentRequestId" to "",
                        "lastUpdatedAt" to now
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startRideNowTrip(requestId: String): Result<Unit> {
        return riderStartRideNowTrip(requestId)
    }

    suspend fun completeRideNowTrip(
        requestId: String,
        liveRideId: String
    ): Result<Unit> {
        // liveRideId is intentionally unused here: the LiveRide is released
        // only when the passenger confirms completion
        // (passengerConfirmRideNowCompleted), not when the rider requests it.
        return riderRequestRideNowCompletion(requestId)
    }

    suspend fun expireRequestIfNeeded(requestId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val requestDoc = rideNowRequestsRef.document(requestId)
                val snapshot = transaction.get(requestDoc)

                val request = snapshot.toObject(RideNowRequest::class.java)
                    ?: throw Exception("Invalid request data.")

                val now = Timestamp.now()
                val expiresAtSeconds = request.expiresAt?.seconds

                val shouldExpire =
                    request.status == RideNowStatus.SEARCHING &&
                            expiresAtSeconds != null &&
                            expiresAtSeconds <= now.seconds

                if (shouldExpire) {
                    transaction.update(
                        requestDoc,
                        mapOf(
                            "status" to RideNowStatus.EXPIRED,
                            "expiredAt" to now
                        )
                    )
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveRideNowToHistory(request: RideNowRequest): Result<Unit> {
        return try {
            val historyDoc = rideHistoryRef.document()

            val history = RideHistory(
                historyId = historyDoc.id,
                rideType = "ride_now",

                requestId = request.requestId,

                passengerId = request.passengerId,
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

    fun listenPassengerRideHistory(
        passengerId: String,
        onData: (List<RideHistory>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return rideHistoryRef
            .whereEqualTo("passengerId", passengerId)
            .orderBy("completedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val historyList = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(RideHistory::class.java)?.copy(
                            historyId = doc.id
                        )
                    }
                    ?: emptyList()

                onData(historyList)
            }
    }

    fun listenRiderRideHistory(
        riderId: String,
        onData: (List<RideHistory>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return rideHistoryRef
            .whereEqualTo("riderId", riderId)
            .orderBy("completedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val historyList = snapshot?.documents
                    ?.mapNotNull { doc ->
                        doc.toObject(RideHistory::class.java)?.copy(
                            historyId = doc.id
                        )
                    }
                    ?: emptyList()

                onData(historyList)
            }
    }

    suspend fun getRideNowRequestById(requestId: String): RideNowRequest? {
        return try {
            val snapshot = rideNowRequestsRef.document(requestId).get().await()
            snapshot.toObject(RideNowRequest::class.java)?.copy(
                requestId = snapshot.id
            )
        } catch (_: Exception) {
            null
        }
    }
}