package com.example.chologo.data.repository

import com.example.chologo.data.model.LiveRide
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

class RideNowLiveRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val liveRidesRef = db.collection("live_rides")

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

    /**
     * Finds any LiveRide documents for this rider still marked "active" in
     * Firestore and force-resets them to a clean inactive state, regardless
     * of what any client's local state currently believes.
     *
     * This exists so "Go Live" is always safe to tap: even if a previous
     * session left a document in a bad state (e.g. a stale currentRequestId
     * that was never cleared because the linked request died without
     * releasing it), calling this before creating a new LiveRide guarantees
     * the rider always starts clean - no manual Firestore edits required.
     *
     * Intentionally best-effort: callers should not treat failure here as
     * fatal to the overall goLiveAsRider flow (e.g. if offline), since
     * blocking "Go Live" entirely on this would just create a new way to
     * get stuck.
     */
    suspend fun forceReleaseStaleLiveRides(riderId: String): Result<Unit> {
        return try {
            val staleRides = liveRidesRef
                .whereEqualTo("riderId", riderId)
                .whereEqualTo("status", "active")
                .get()
                .await()

            if (staleRides.isEmpty) {
                return Result.success(Unit)
            }

            val now = Timestamp.now()
            val batch = db.batch()

            staleRides.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf(
                        "status" to "inactive",
                        "isLiveNow" to false,
                        "isAvailable" to false,
                        "currentRequestId" to "",
                        "lastUpdatedAt" to now
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
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
}