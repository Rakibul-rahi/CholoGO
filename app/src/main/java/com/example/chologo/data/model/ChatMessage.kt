package com.example.chologo.data.model

import com.google.firebase.Timestamp

/**
 * One line in the temporary in-ride chat between a passenger and their
 * matched rider. Stored at {ride collection}/{requestId}/messages/{id};
 * see firestore.rules ("In-ride chat") for who may read/post and when.
 */
data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val createdAt: Timestamp? = null
)

/**
 * Which ride flow a chat belongs to. Both flows hang the chat off their own
 * request document, so this is just the parent collection plus the name
 * the API server uses for it.
 */
enum class ChatFlow(val collection: String, val apiName: String) {
    RIDE_NOW("ride_now_requests", "now"),
    TOMORROW("ride_requests", "tomorrow");

    /** Statuses in which a new message may be posted (mirrors chatOpenStatuses() in the rules). */
    fun isChatOpen(status: String): Boolean = status in OPEN_STATUSES

    companion object {
        val OPEN_STATUSES = listOf(
            "accepted",
            "start_pending_confirmation",
            "ongoing",
            "end_pending_confirmation"
        )

        const val MAX_LENGTH = 500
    }
}
