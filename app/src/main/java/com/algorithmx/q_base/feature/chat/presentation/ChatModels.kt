package com.algorithmx.q_base.feature.chat.presentation
import com.algorithmx.q_base.data.chat.ChatEntity
import com.algorithmx.q_base.data.chat.MessageEntity
import com.algorithmx.q_base.data.core.UserEntity

/**
 * UI model for a single chat item in the list.
 */
data class ChatUiModel(
    val chat: ChatEntity,
    val displayName: String,
    val latestMessage: MessageEntity? = null,
    val unreadCount: Int = 0
)

/**
 * State for the chat list screen.
 */
data class ChatListState(
    val chats: List<ChatUiModel> = emptyList(),
    val users: Map<String, UserEntity> = emptyMap(),
    val isLoading: Boolean = false
)

/**
 * State for the chat detail screen.
 */
data class ChatDetailState(
    val chat: ChatEntity? = null,
    val displayName: String = "",
    val messages: List<MessageEntity> = emptyList(),
    val participants: Map<String, UserEntity> = emptyMap(),
    val currentUserId: String = ""
)

/**
 * Navigation events emitted by the ChatViewModel.
 */
sealed class ChatNavEvent {
    data class NavigateToChatDetail(val chatId: String) : ChatNavEvent()
}