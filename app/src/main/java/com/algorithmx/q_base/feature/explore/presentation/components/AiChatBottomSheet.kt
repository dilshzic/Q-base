package com.algorithmx.q_base.feature.explore.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.feature.explore.presentation.QuestionAiChatViewModel
import com.algorithmx.q_base.core.data.chat.MessageEntity
import com.algorithmx.q_base.feature.chat.presentation.components.AnimatedMessageItem
import com.algorithmx.q_base.feature.chat.presentation.components.ChatDetailBottomBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatBottomSheet(
    questionId: String,
    questionStem: String?,
    onDismiss: () -> Unit,
    viewModel: QuestionAiChatViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(questionId, questionStem) {
        viewModel.loadChatForQuestion(questionId, questionStem)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 12.dp)
            ) {
                Text("AI Chat", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = onDismiss) { Text("Close") }
            }

            // Question Preview
            if (!questionStem.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(
                        text = questionStem,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            // Messages
            val rawMessages by viewModel.messages.collectAsStateWithLifecycle()
            val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
            val listState = rememberLazyListState()

            // Map QuestionAiMessageEntity to MessageEntity for the UI component
            val messages = remember(rawMessages) {
                rawMessages.map { msg ->
                    MessageEntity(
                        messageId = msg.messageId,
                        chatId = msg.questionId,
                        senderId = if (msg.sender == "AI") "AI_BOT" else "USER",
                        payload = msg.payload,
                        type = "TEXT",
                        timestamp = msg.timestamp,
                        status = "SENT"
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                itemsIndexed(messages, key = { _, m -> m.messageId }) { index, message ->
                    val isMine = message.senderId == "USER"
                    val prev = if (index > 0) messages[index - 1] else null
                    val next = if (index < messages.size - 1) messages[index + 1] else null
                    val isFirst = prev == null || prev.senderId != message.senderId
                    val isLast = next == null || next.senderId != message.senderId

                    AnimatedMessageItem(
                        message = message,
                        isMine = isMine,
                        senderName = null,
                        showAvatar = !isMine && isLast,
                        avatarUrl = null,
                        isSaved = false,
                        localCollections = emptyList(),
                        onSaveCollection = {},
                        onJoinSession = {},
                        onReportMessage = {},
                        onProfileClick = {},
                        onDeleteChat = {},
                        isAiLoading = isAiLoading && !isMine && index == messages.size - 1,
                        isFirstInGroup = isFirst,
                        isLastInGroup = isLast
                    )
                }
            }

            // Input bar
            var messageText by remember { mutableStateOf("") }
            
            ChatDetailBottomBar(
                chat = null,
                isLibraryMode = false,
                canSend = true,
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onAttachClick = {},
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                        scope.launch { listState.animateScrollToItem(messages.size) }
                    }
                }
            )
        }
    }
}
