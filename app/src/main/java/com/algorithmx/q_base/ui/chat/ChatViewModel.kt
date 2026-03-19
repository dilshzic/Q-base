package com.algorithmx.q_base.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.dao.ChatDao
import com.algorithmx.q_base.data.dao.MessageDao
import com.algorithmx.q_base.data.dao.UserDao
import com.algorithmx.q_base.data.entity.ChatEntity
import com.algorithmx.q_base.data.entity.MessageEntity
import com.algorithmx.q_base.data.entity.UserEntity
import com.algorithmx.q_base.data.repository.AuthRepository
import com.algorithmx.q_base.data.repository.SyncRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatListState(
    val chats: List<ChatEntity> = emptyList(),
    val users: Map<String, UserEntity> = emptyMap()
)

data class ChatDetailState(
    val chat: ChatEntity? = null,
    val messages: List<MessageEntity> = emptyList(),
    val participants: Map<String, UserEntity> = emptyMap(),
    val currentUserId: String = ""
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // State for the Inbox/Chat List
    val chatListState: StateFlow<ChatListState> = combine(
        chatDao.getAllChats(),
        userDao.getAllUsers()
    ) { chats, users ->
        ChatListState(
            chats = chats,
            users = users.associateBy { it.userId }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatListState())

    // State for a specific Chat Detail
    private val _currentChatId = MutableStateFlow<String?>(null)
    
    val chatDetailState: StateFlow<ChatDetailState> = _currentChatId.flatMapLatest { chatId ->
        if (chatId == null) flowOf(ChatDetailState())
        else {
            combine(
                flow { emit(chatDao.getChatById(chatId)) },
                messageDao.getMessagesForChat(chatId),
                userDao.getAllUsers()
            ) { chat, messages, users ->
                ChatDetailState(
                    chat = chat,
                    messages = messages,
                    participants = users.associateBy { it.userId },
                    currentUserId = currentUserId
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatDetailState())

    fun setChatId(chatId: String) {
        _currentChatId.value = chatId
        // Start syncing messages for this chat
        viewModelScope.launch {
            syncRepository.observeAndSyncMessages(chatId).collect()
        }
    }

    fun sendMessage(chatId: String, text: String, type: String = "TEXT") {
        val message = MessageEntity(
            messageId = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = currentUserId,
            payload = text,
            type = type,
            timestamp = System.currentTimeMillis()
        )
        
        viewModelScope.launch {
            // Save locally first (Offline-First)
            messageDao.insertMessage(message)
            // Sync to Firestore
            syncRepository.sendMessage(message)
        }
    }
}
