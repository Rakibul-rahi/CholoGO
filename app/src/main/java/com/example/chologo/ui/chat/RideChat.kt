package com.example.chologo.ui.chat

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.chologo.data.model.ChatFlow
import com.example.chologo.data.model.ChatMessage
import com.example.chologo.ui.passenger.AccentBlue
import com.example.chologo.ui.passenger.CardBase
import com.example.chologo.ui.passenger.CardElevated
import com.example.chologo.ui.passenger.TextHigh
import com.example.chologo.ui.passenger.TextLow
import com.example.chologo.ui.passenger.TextMed
import com.example.chologo.viewmodel.RideChatViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Locale

private val QUICK_REPLIES = listOf(
    "I'm here",
    "On my way",
    "2 min away",
    "Where exactly are you?",
    "OK 👍"
)

/**
 * "Message {name}" button with an unread badge; opens the in-ride chat
 * sheet. Drop it into any card for a matched trip. When the trip is no
 * longer in a chat-open status ([status]), the thread stays readable but
 * the input is replaced by a read-only notice.
 */
@Composable
fun RideChatButton(
    flow: ChatFlow,
    requestId: String,
    status: String,
    otherPartyName: String,
    modifier: Modifier = Modifier
) {
    val myUid = FirebaseAuth.getInstance().currentUser?.uid
    if (requestId.isBlank() || myUid == null) return

    val context = LocalContext.current
    val chatViewModel: RideChatViewModel = viewModel(
        key = "ride_chat_${flow.apiName}_$requestId",
        factory = viewModelFactory {
            initializer {
                RideChatViewModel(
                    flow = flow,
                    requestId = requestId,
                    myUid = myUid,
                    prefs = context.applicationContext
                        .getSharedPreferences("ride_chat", Context.MODE_PRIVATE)
                )
            }
        }
    )
    val uiState by chatViewModel.uiState.collectAsState()
    var showSheet by rememberSaveable(requestId) { mutableStateOf(false) }

    val accent = AccentBlue
    val name = otherPartyName.ifBlank { "your ride partner" }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
            .clickable { showSheet = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BadgedBox(
                badge = {
                    if (uiState.unreadCount > 0) {
                        Badge { Text(uiState.unreadCount.coerceAtMost(99).toString()) }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = if (flow.isChatOpen(status)) "Message $name" else "View chat",
                color = accent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
        }
    }

    if (showSheet) {
        RideChatSheet(
            viewModel = chatViewModel,
            myUid = myUid,
            otherPartyName = name,
            isOpen = flow.isChatOpen(status),
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RideChatSheet(
    viewModel: RideChatViewModel,
    myUid: String,
    otherPartyName: String,
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        viewModel.onSheetVisibilityChanged(true)
        onDispose { viewModel.onSheetVisibilityChanged(false) }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBase
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = otherPartyName,
                color = TextHigh,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = "Messages are deleted 24 hours after the trip ends.",
                color = TextLow,
                fontSize = 12.sp
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item {
                        Text(
                            text = "No messages yet. Say hi or let them know where you are.",
                            color = TextMed,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                }

                items(uiState.messages, key = { it.id }) { message ->
                    ChatBubble(message = message, isMine = message.senderId == myUid)
                }
            }

            if (isOpen) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(QUICK_REPLIES) { reply ->
                        Text(
                            text = reply,
                            color = TextHigh,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(CardElevated)
                                .clickable { viewModel.send(reply) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.draft,
                        onValueChange = viewModel::onDraftChanged,
                        placeholder = { Text("Type a message", color = TextLow) },
                        maxLines = 4,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextHigh,
                            unfocusedTextColor = TextHigh
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = viewModel::sendDraft,
                        enabled = uiState.draft.isNotBlank()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (uiState.draft.isNotBlank()) AccentBlue else TextLow
                        )
                    }
                }
            } else {
                Text(
                    text = "This trip has ended, so the chat is read-only.",
                    color = TextMed,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, isMine: Boolean) {
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMine) 16.dp else 4.dp,
                        bottomEnd = if (isMine) 4.dp else 16.dp
                    )
                )
                .background(if (isMine) AccentBlue.copy(alpha = 0.18f) else CardElevated)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = message.text, color = TextHigh, fontSize = 14.sp)
            message.createdAt?.let {
                Text(
                    text = timeFormat.format(it.toDate()),
                    color = TextLow,
                    fontSize = 10.sp
                )
            }
        }
    }
}
