package com.example.chologo.data.repository

import com.example.chologo.data.model.RideRating
import com.example.chologo.data.model.RideReport
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class RideNowFeedbackRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val ratingsRef = db.collection("ride_ratings")
    private val reportsRef = db.collection("ride_reports")
    private val usersRef = db.collection("users")
    private val rideNowRequestsRef = db.collection("ride_now_requests")

    suspend fun submitRideRating(rating: RideRating): Result<String> {
        return try {
            if (rating.ratedBy.isBlank()) {
                return Result.failure(Exception("Rated by user is missing."))
            }

            if (rating.ratedTo.isBlank()) {
                return Result.failure(Exception("Rated user is missing."))
            }

            if (rating.requestId.isBlank()) {
                return Result.failure(Exception("Request ID is missing."))
            }

            if (rating.stars !in 1..5) {
                return Result.failure(Exception("Rating must be between 1 and 5."))
            }

            val existingRating = ratingsRef
                .whereEqualTo("requestId", rating.requestId)
                .whereEqualTo("ratedBy", rating.ratedBy)
                .limit(1)
                .get()
                .await()

            if (!existingRating.isEmpty) {
                return Result.failure(Exception("You already rated this ride."))
            }

            val ratingDoc = if (rating.ratingId.isBlank()) {
                ratingsRef.document()
            } else {
                ratingsRef.document(rating.ratingId)
            }

            val userDoc = usersRef.document(rating.ratedTo)
            val requestDoc = rideNowRequestsRef.document(rating.requestId)

            db.runTransaction { transaction ->
                val userSnapshot = transaction.get(userDoc)
                val requestSnapshot = transaction.get(requestDoc)

                val alreadyRated = requestSnapshot.getBoolean("riderRated") ?: false
                val issueReported = requestSnapshot.getBoolean("issueReported") ?: false

                if (alreadyRated) {
                    throw Exception("You already rated this ride.")
                }

                if (issueReported) {
                    throw Exception("You cannot rate after reporting an issue.")
                }

                val oldAverage = userSnapshot.getDouble("ratingAverage") ?: 0.0
                val oldCount = userSnapshot.getLong("ratingCount")?.toInt() ?: 0

                val newCount = oldCount + 1
                val newAverage = ((oldAverage * oldCount) + rating.stars) / newCount

                val ratingToSave = rating.copy(
                    ratingId = ratingDoc.id,
                    createdAt = rating.createdAt ?: Timestamp.now()
                )

                transaction.set(ratingDoc, ratingToSave)

                transaction.update(
                    userDoc,
                    mapOf(
                        "ratingAverage" to newAverage,
                        "ratingCount" to newCount
                    )
                )

                transaction.update(
                    requestDoc,
                    mapOf(
                        "riderRated" to true,
                        "rating" to rating.stars,
                        "ratedAt" to Timestamp.now()
                    )
                )
            }.await()

            Result.success(ratingDoc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitRideReport(report: RideReport): Result<String> {
        return try {
            if (report.reportedBy.isBlank()) {
                return Result.failure(Exception("Reporter user is missing."))
            }

            if (report.reportedUserId.isBlank()) {
                return Result.failure(Exception("Reported user is missing."))
            }

            if (report.requestId.isBlank()) {
                return Result.failure(Exception("Request ID is missing."))
            }

            if (report.reason.isBlank()) {
                return Result.failure(Exception("Report reason is required."))
            }

            val existingReport = reportsRef
                .whereEqualTo("requestId", report.requestId)
                .whereEqualTo("reportedBy", report.reportedBy)
                .limit(1)
                .get()
                .await()

            if (!existingReport.isEmpty) {
                return Result.failure(Exception("You already reported this ride."))
            }

            val reportDoc = if (report.reportId.isBlank()) {
                reportsRef.document()
            } else {
                reportsRef.document(report.reportId)
            }

            val requestDoc = rideNowRequestsRef.document(report.requestId)

            val reportToSave = report.copy(
                reportId = reportDoc.id,
                status = if (report.status.isBlank()) "pending" else report.status,
                createdAt = report.createdAt ?: Timestamp.now()
            )

            db.runTransaction { transaction ->
                val requestSnapshot = transaction.get(requestDoc)

                val alreadyReported = requestSnapshot.getBoolean("issueReported") ?: false
                val alreadyRated = requestSnapshot.getBoolean("riderRated") ?: false

                if (alreadyReported) {
                    throw Exception("You already reported this ride.")
                }

                if (alreadyRated) {
                    throw Exception("You cannot report after submitting a rating.")
                }

                transaction.set(reportDoc, reportToSave)

                transaction.update(
                    usersRef.document(report.reportedUserId),
                    "reportCount",
                    FieldValue.increment(1)
                )

                transaction.update(
                    requestDoc,
                    mapOf(
                        "issueReported" to true,
                        "reportReason" to report.reason,
                        "reportDetails" to report.details,
                        "reportedAt" to Timestamp.now()
                    )
                )
            }.await()

            Result.success(reportDoc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasUserRatedRide(
        requestId: String,
        ratedBy: String
    ): Boolean {
        return try {
            val snapshot = ratingsRef
                .whereEqualTo("requestId", requestId)
                .whereEqualTo("ratedBy", ratedBy)
                .limit(1)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (_: Exception) {
            false
        }
    }

    suspend fun hasUserReportedRide(
        requestId: String,
        reportedBy: String
    ): Boolean {
        return try {
            val snapshot = reportsRef
                .whereEqualTo("requestId", requestId)
                .whereEqualTo("reportedBy", reportedBy)
                .limit(1)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (_: Exception) {
            false
        }
    }
}