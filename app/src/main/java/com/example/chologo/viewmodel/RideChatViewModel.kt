package com.example.chologo.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chologo.data.model.ChatFlow
import com.example.chologo.data.model.ChatMessage
import com.example.chologo.data.repository.RideChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RideChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val draft: String = "",
    val unreadCount: Int = 0,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Process-wide note of which chat thread is on screen right now, so the
 * FCM service can skip the "new message" notification for a thread the
 * user is already looking at.
 */
object ActiveChatTracker {
    @Volatile
    var openChatKey: String? = null

    fun keyFor(flow: String, requestId: String) = "$flow/$requestId"
}

class RideChatViewModel(
    private val flow: ChatFlow,
    private val requestId: String,
    private val myUid: String,
    private val prefs: SharedPreferences,
    private val repository: RideChatRepository = RideChatRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RideChatUiState())
    val uiState: StateFlow<RideChatUiState> = _uiState.asStateFlow()

    private val lastSeenKey = "last_seen_${flow.apiName}_$requestId"
    private var lastSeenMillis = prefs.getLong(lastSeenKey, 0L)
    private var isSheetOpen = false
    private var lastSentAtMillis = 0L

    init {
        viewModelScope.launch {
            repository.listenMessages(flow, requestId)
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Couldn't load chat.") }
                }
                .collect { messages ->
                    if (isSheetOpen) markSeen(messages)
                    _uiState.update {
                        it.copy(messages = messages, unreadCount = countUnread(messages))
                    }
                }
        }
    }

    fun onSheetVisibilityChanged(open: Boolean) {
        isSheetOpen = open
        ActiveChatTracker.openChatKey =
            if (open) ActiveChatTracker.keyFor(flow.apiName, requestId) else null

        if (open) {
            val messages = _uiState.value.messages
            markSeen(messages)
            _uiState.update { it.copy(unreadCount = countUnread(messages)) }
        }
    }

    fun onDraftChanged(text: String) {
        _uiState.update { it.copy(draft = text.take(ChatFlow.MAX_LENGTH)) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun sendDraft() = send(_uiState.value.draft)

    fun send(rawText: String) {
        val text = rawText.trim()
        if (text.isEmpty()) return

        // Light client-side throttle; the rules already cap message size.
        val now = System.currentTimeMillis()
        if (now - lastSentAtMillis < MIN_SEND_INTERVAL_MS) return
        lastSentAtMillis = now

        _uiState.update { it.copy(draft = "", isSending = true) }

        viewModelScope.launch {
            repository.sendMessage(flow, requestId, myUid, text)
                .onSuccess {
                    _uiState.update { it.copy(isSending = false) }
                    repository.notifyNewMessage(flow, requestId)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isSending = false,
                            // Hand the text back so it isn't lost.
                            draft = if (it.draft.isEmpty()) text else it.draft,
                            errorMessage = e.message ?: "Message not sent."
                        )
                    }
                }
        }
    }

    private fun markSeen(messages: List<ChatMessage>) {
        val newest = messages.maxOfOrNull { it.createdAt?.toDate()?.time ?: 0L } ?: return
        if (newest > lastSeenMillis) {
            lastSeenMillis = newest
            prefs.edit().putLong(lastSeenKey, newest).apply()
        }
    }

    private fun countUnread(messages: List<ChatMessage>): Int =
        messages.count {
            it.senderId != myUid && (it.createdAt?.toDate()?.time ?: 0L) > lastSeenMillis
        }

    override fun onCleared() {
        if (isSheetOpen) ActiveChatTracker.openChatKey = null
        super.onCleared()
    }

    companion object {
        private const val MIN_SEND_INTERVAL_MS = 800L
    }
}
