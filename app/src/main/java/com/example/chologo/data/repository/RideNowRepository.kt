package com.example.chologo.repository

import com.example.chologo.data.model.LiveRide
import com.example.chologo.data.model.RideNowRequest
import com.example.chologo.data.model.RideNowStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class RideNowRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val liveRidesRef = db.collection("live_rides")
    private val rideNowRequestsRef = db.collection("ride_now_requests")

    suspend fun goLiveAsRider(liveRide: LiveRide): Result<String> {
        return try {
            val docRef = if (liveRide.rideId.isBlank()) {
                liveRidesRef.document()
            } else {
                liveRidesRef.document(liveRide.rideId)
            }

            val now = Timestamp.now()

            val rideToSave = liveRide.copy(
                rideId = docRef.id,
                createdAt = liveRide.createdAt ?: now,
                lastUpdatedAt = now,
                status = "active",
                isLiveNow = true,
                isAvailable = true,
                currentRequestId = ""
            )

            docRef.set(rideToSave).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopLiveRide(rideId: String): Result<Unit> {
        return try {
            liveRidesRef.document(rideId)
                .update(
                    mapOf(
                        "status" to "inactive",
                        "isLiveNow" to false,
                        "isAvailable" to false,
                        "currentRequestId" to "",
                        "lastUpdatedAt" to Timestamp.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRideNowRequest(request: RideNowRequest): Result<String> {
        return try {
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

    fun listenToLiveRide(
        rideId: String,
        onData: (LiveRide?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration {
        return liveRidesRef.document(rideId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(error)
                    return@addSnapshotListener
                }

                val ride = snapshot?.toObject(LiveRide::class.java)?.copy(
                    rideId = snapshot.id
                )

                onData(ride)
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

                if (!requestSnapshot.exists()) {
                    throw Exception("Ride now request not found.")
                }

                if (!rideSnapshot.exists()) {
                    throw Exception("Live ride not found.")
                }

                val request = requestSnapshot.toObject(RideNowRequest::class.java)
                    ?: throw Exception("Invalid request data.")

                val ride = rideSnapshot.toObject(LiveRide::class.java)
                    ?: throw Exception("Invalid live ride data.")

                val now = Timestamp.now()
                val expiresAtSeconds = request.expiresAt?.seconds
                val isExpired = expiresAtSeconds != null && expiresAtSeconds <= now.seconds
                val isRideBusy = !ride.currentRequestId.isNullOrBlank()

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

                if (ride.status != "active" || ride.isLiveNow != true || ride.isAvailable != true) {
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
                        "acceptedAt" to now
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

    suspend fun startRideNowTrip(
        requestId: String
    ): Result<Unit> {
        return try {
            rideNowRequestsRef.document(requestId)
                .update(
                    mapOf(
                        "status" to RideNowStatus.ONGOING,
                        "startedAt" to Timestamp.now()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun completeRideNowTrip(
        requestId: String,
        liveRideId: String
    ): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val now = Timestamp.now()

                transaction.update(
                    rideNowRequestsRef.document(requestId),
                    mapOf(
                        "status" to RideNowStatus.COMPLETED,
                        "completedAt" to now
                    )
                )

                transaction.update(
                    liveRidesRef.document(liveRideId),
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

    suspend fun expireRequestIfNeeded(requestId: String): Result<Unit> {
        return try {
            db.runTransaction { transaction ->
                val requestDoc = rideNowRequestsRef.document(requestId)
                val snapshot = transaction.get(requestDoc)

                if (!snapshot.exists()) {
                    throw Exception("Request not found.")
                }

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

    suspend fun getLiveRideById(rideId: String): LiveRide? {
        return try {
            val snapshot = liveRidesRef.document(rideId).get().await()
            snapshot.toObject(LiveRide::class.java)?.copy(
                rideId = snapshot.id
            )
        } catch (_: Exception) {
            null
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