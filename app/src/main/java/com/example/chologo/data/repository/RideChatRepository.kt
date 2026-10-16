package com.example.chologo.data.repository

import com.example.chologo.data.model.ChatFlow
import com.example.chologo.data.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Temporary passenger <-> rider chat for a matched trip.
 *
 * Messages are plain Firestore docs under the ride's own request document,
 * so access control is entirely firestore.rules: only the two matched
 * people can read, and they can only post while the trip is locked in.
 * The API server pushes the other side on each new message and deletes
 * the whole thread 24 hours after the trip ends.
 */
class RideChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val apiBaseUrl = "https://chologo.onrender.com"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun messagesRef(flow: ChatFlow, requestId: String) =
        db.collection(flow.collection)
            .document(requestId)
            .collection("messages")

    fun listenMessages(flow: ChatFlow, requestId: String): Flow<List<ChatMessage>> =
        callbackFlow {
            val registration = messagesRef(flow, requestId)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(MESSAGE_LIMIT)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val messages = snapshot?.documents.orEmpty().map { doc ->
                        ChatMessage(
                            id = doc.id,
                            senderId = doc.getString("senderId").orEmpty(),
                            text = doc.getString("text").orEmpty(),
                            // Our own just-sent message has no server time
                            // yet; use the local estimate so it sorts and
                            // renders immediately instead of jumping.
                            createdAt = doc.getTimestamp(
                                "createdAt",
                                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                            )
                        )
                    }

                    trySend(messages)
                }

            awaitClose { registration.remove() }
        }

    suspend fun sendMessage(
        flow: ChatFlow,
        requestId: String,
        senderId: String,
        text: String
    ): Result<Unit> {
        return try {
            messagesRef(flow, requestId)
                .add(
                    mapOf(
                        "senderId" to senderId,
                        "text" to text,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Asks the API server to push the other side about a new message (it
     * also registers the thread for the 24h cleanup sweep). Best-effort:
     * the message itself is already saved, so a failure here only means a
     * missing notification.
     */
    suspend fun notifyNewMessage(flow: ChatFlow, requestId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val idToken = FirebaseAuth.getInstance().currentUser
                    ?.getIdToken(false)
                    ?.await()
                    ?.token
                    ?: throw Exception("Not authenticated.")

                val body = JSONObject()
                    .put("flow", flow.apiName)
                    .put("requestId", requestId)
                    .toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val httpRequest = Request.Builder()
                    .url("$apiBaseUrl/api/chat/notify")
                    .addHeader("Authorization", "Bearer $idToken")
                    .post(body)
                    .build()

                httpClient.newCall(httpRequest).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw Exception(
                            response.body?.string() ?: "Notify failed (${response.code})."
                        )
                    }
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    companion object {
        private const val MESSAGE_LIMIT = 200L
    }
}
