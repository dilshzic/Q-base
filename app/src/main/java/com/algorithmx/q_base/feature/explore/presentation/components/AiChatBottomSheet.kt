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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.feature.chat.presentation.ChatViewModel
import com.algorithmx.q_base.feature.chat.presentation.sendMessage
import com.algorithmx.q_base.feature.chat.presentation.startAiChat
import com.algorithmx.q_base.feature.chat.presentation.components.AnimatedMessageItem
import com.algorithmx.q_base.feature.chat.presentation.components.ChatDetailBottomBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatBottomSheet(
    questionStem: String?,
    onDismiss: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(questionStem) {
        // Ensure the AI chat exists and is selected
        viewModel.startAiChat()
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

            // Messages
            val state by viewModel.chatDetailState.collectAsStateWithLifecycle()
            val messages = state.messages
            val listState = rememberLazyListState()

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                itemsIndexed(messages, key = { _, m -> m.messageId }) { index, message ->
                    val isMine = message.senderId == viewModel.currentUserId
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
                        isAiLoading = false,
                        isFirstInGroup = isFirst,
                        isLastInGroup = isLast
                    )
                }
            }

            // Input bar
            var messageText by remember { mutableStateOf(questionStem ?: "") }
            val canSendFlow = remember(state.chat?.chatId) {
                state.chat?.chatId?.let { viewModel.canSendToChat(it) } ?: kotlinx.coroutines.flow.flowOf(false)
            }
            val canSend by canSendFlow.collectAsState(initial = true)

            ChatDetailBottomBar(
                chat = state.chat,
                isLibraryMode = false,
                canSend = canSend,
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onAttachClick = {},
                onSessionClick = {},
                onSendClick = {
                    state.chat?.chatId?.let { chatId ->
                        viewModel.sendMessage(chatId, messageText)
                        messageText = ""
                        scope.launch { listState.animateScrollToItem(state.messages.size) }
                    }
                }
            )
        }
    }
}
