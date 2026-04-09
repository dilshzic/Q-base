package com.algorithmx.q_base.ui.chat

import android.util.Log

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.core_ai.brain.models.AiCollectionResponse
import com.algorithmx.q_base.data.chat.ChatDao
import com.algorithmx.q_base.data.chat.ChatEntity
import com.algorithmx.q_base.data.chat.MessageDao
import com.algorithmx.q_base.data.chat.MessageEntity
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.data.core.UserDao
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.data.ai.AiRepository
import com.algorithmx.q_base.data.auth.AuthRepository
import com.algorithmx.q_base.data.sessions.SessionDao
import com.algorithmx.q_base.data.sessions.StudySession
import com.algorithmx.q_base.data.sync.SyncRepository
import com.algorithmx.q_base.data.util.MockExporter
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.algorithmx.q_base.data.util.MockDownloader
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

data class ChatUiModel(
    val chat: ChatEntity,
    val displayName: String, // Resolved name
    val latestMessage: MessageEntity? = null,
    val unreadCount: Int = 0
)

data class ChatListState(
    val chats: List<ChatUiModel> = emptyList(),
    val users: Map<String, UserEntity> = emptyMap()
)

data class ChatDetailState(
    val chat: ChatEntity? = null,
    val displayName: String = "", // Resolved name
    val messages: List<MessageEntity> = emptyList(),
    val participants: Map<String, UserEntity> = emptyMap(),
    val currentUserId: String = ""
)

sealed class ChatNavEvent {
    data class NavigateToChatDetail(val chatId: String) : ChatNavEvent()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val aiRepository: AiRepository,
    private val collectionDao: CollectionDao,
    private val sessionDao: SessionDao,
    private val mockExporter: MockExporter,
    private val mockDownloader: MockDownloader
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }
    
    private val _actionFeedback = MutableSharedFlow<String>()
    val actionFeedback = _actionFeedback.asSharedFlow()
    
    private val _accessRequests = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val accessRequests = _accessRequests.asStateFlow()

    fun requestAccess(collectionId: String) {
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch {
            try {
                syncRepository.requestCollectionAccess(chatId, collectionId)
                _actionFeedback.emit("Access request sent to group admins")
            } catch (e: Exception) {
                _actionFeedback.emit("Failed to send request: ${e.message}")
            }
        }
    }

    fun grantAccess(collectionId: String, requesterId: String) {
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch {
            try {
                syncRepository.grantCollectionAccess(chatId, collectionId, requesterId)
                _actionFeedback.emit("Access granted!")
            } catch (e: Exception) {
                _actionFeedback.emit("Failed to grant access: ${e.message}")
            }
        }
    }

    private val _isSharing = MutableStateFlow(false)
    val isSharing = _isSharing.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    private val _sharedCollections = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val sharedCollections: StateFlow<List<Map<String, Any>>> = _sharedCollections.asStateFlow()

    private val _sharedSessions = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val sharedSessions: StateFlow<List<Map<String, Any>>> = _sharedSessions.asStateFlow()

    private val _isLibraryMode = MutableStateFlow(false)
    val isLibraryMode = _isLibraryMode.asStateFlow()

    fun toggleLibraryMode(enabled: Boolean) {
        _isLibraryMode.value = enabled
    }

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    companion object {
        const val QBASE_AI_BOT_ID = "qbase_ai_bot"
    }

    // State for user selection
    val allUsers: StateFlow<List<UserEntity>> = userDao.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localStudyCollections: StateFlow<List<StudyCollection>> = collectionDao.getAllStudyCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<StudySession>> = sessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<UserEntity?> = authRepository.currentUser
        .flatMapLatest { firebaseUser ->
            if (firebaseUser == null) flowOf<UserEntity?>(null)
            else userDao.getCurrentUser(firebaseUser.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalUnreadCount = chatDao.getTotalUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Selection State
    private val _selectedChatIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedChatIds = _selectedChatIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    fun toggleChatSelection(chatId: String) {
        val current = _selectedChatIds.value
        if (current.contains(chatId)) {
            val newSet = current - chatId
            _selectedChatIds.value = newSet
            if (newSet.isEmpty()) _isSelectionMode.value = false
        } else {
            _selectedChatIds.value = current + chatId
            _isSelectionMode.value = true
        }
    }

    fun clearSelection() {
        _selectedChatIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun deleteSelectedChats() {
        val idsToDelete = _selectedChatIds.value.toList()
        viewModelScope.launch {
            try {
                idsToDelete.forEach { chatId ->
                    val chat = chatDao.getChatById(chatId)
                    if (chat != null && chat.adminId != currentUserId && chat.isGroup) {
                        val currentParticipants = chat.participantIds.split(",").toMutableList()
                        currentParticipants.remove(currentUserId)
                        val updatedParticipants = currentParticipants.joinToString(",")
                        chatDao.deleteChatById(chatId)
                        syncRepository.removeParticipantFromFirestore(chatId, currentUserId ?: "")
                    } else {
                        chatDao.deleteChatById(chatId)
                        syncRepository.deleteChatOnFirestore(chatId)
                    }
                }
                _actionFeedback.emit("Deleted ${idsToDelete.size} chats")
                clearSelection()
            } catch (e: Exception) {
                _actionFeedback.emit("Error deleting chats: ${e.message}")
            }
        }
    }

    val allStudyCollections: StateFlow<List<StudyCollection>> = collectionDao.getAllStudyCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // State for the Inbox/Chat List
    val chatListState: StateFlow<ChatListState> = combine(
        chatDao.getAllChats(),
        userDao.getAllUsers(),
        messageDao.getAllMessages() 
    ) { chats, users, allMessages ->
        val userMap = users.associateBy { it.userId }
        val chatUiModels = chats.mapNotNull { chat ->
            val chatMessages = allMessages.filter { it.chatId == chat.chatId }
            
            // Resolve Display Name
            val otherParticipantId = chat.participantIds.split(",")
                .firstOrNull { it != currentUserId && it.isNotEmpty() }
            val otherUser = userMap[otherParticipantId]

            // Account-Level Ban/Block mechanism: Client-Side Hiding
            if ((!chat.isGroup && otherUser?.isBanned == true) || chat.isBlocked) {
                return@mapNotNull null
            }

            val resolvedName = (if (chat.isGroup) {
                chat.chatName
            } else {
                otherUser?.displayName ?: chat.chatName
            }) ?: "Chat"

            ChatUiModel(
                chat = chat,
                displayName = resolvedName,
                latestMessage = chatMessages.maxByOrNull { it.timestamp },
                unreadCount = 0 
            )
        }.sortedByDescending { it.latestMessage?.timestamp ?: 0L }

        ChatListState(
            chats = chatUiModels,
            users = userMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatListState())
    
    // State for the Blocked List
    val blockedChatsState: StateFlow<List<ChatUiModel>> = combine(
        chatDao.getAllChats(),
        userDao.getAllUsers(),
        messageDao.getAllMessages()
    ) { chats, users, allMessages ->
        val userMap = users.associateBy { it.userId }
        chats.filter { it.isBlocked }.map { chat ->
            val chatMessages = allMessages.filter { it.chatId == chat.chatId }
            val otherParticipantId = chat.participantIds.split(",")
                .firstOrNull { it != currentUserId && it.isNotEmpty() }
            val otherUser = userMap[otherParticipantId]
            val resolvedName = (if (chat.isGroup) {
                chat.chatName
            } else {
                otherUser?.displayName ?: chat.chatName
            }) ?: "Chat"

            ChatUiModel(
                chat = chat,
                displayName = resolvedName,
                latestMessage = chatMessages.maxByOrNull { it.timestamp },
                unreadCount = 0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentChatId = MutableStateFlow<String?>(null)
    
    private val _navigationEvents = MutableSharedFlow<ChatNavEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()
    
    @OptIn(ExperimentalCoroutinesApi::class)
    val chatDetailState: StateFlow<ChatDetailState> = _currentChatId.flatMapLatest { chatId ->
        if (chatId == null) flowOf(ChatDetailState())
        else {
            combine(
                chatDao.getChatByIdFlow(chatId),
                messageDao.getMessagesForChat(chatId),
                userDao.getAllUsers()
            ) { chat, messages, users ->
                val userMap = users.associateBy { it.userId }
                val resolvedName = (if (chat?.isGroup == true) {
                    chat.chatName
                } else {
                    val otherParticipantId = chat?.participantIds?.split(",")
                        ?.firstOrNull { it != currentUserId && it.isNotEmpty() }
                    userMap[otherParticipantId]?.displayName ?: chat?.chatName
                }) ?: "Chat"

                ChatDetailState(
                    chat = chat,
                    displayName = resolvedName,
                    messages = messages,
                    participants = userMap,
                    currentUserId = currentUserId
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatDetailState())

    fun addParticipant(chatId: String, userId: String) {
        viewModelScope.launch {
            val chat = chatDao.getChatById(chatId) ?: return@launch
            
            // Admin Check
            if (chat.isGroup && chat.adminId != currentUserId) {
                _actionFeedback.emit("Only the admin can add participants")
                return@launch
            }

            val currentParticipants = chat.participantIds
            val updatedParticipants = if (currentParticipants.isEmpty()) userId else "$currentParticipants,$userId"
            
            chatDao.updateParticipants(chatId, updatedParticipants)
            syncRepository.addParticipantToFirestore(chatId, userId)
            
            // System message for participant added
            sendMessage(chatId, "added a new participant", type = "DB_CHANGE")
            
            _actionFeedback.emit("Participant added successfully")
        }
    }

    fun leaveGroup(chatId: String) {
        viewModelScope.launch {
            val chat = chatDao.getChatById(chatId) ?: return@launch
            val currentParticipants = chat.participantIds.split(",").toMutableList()
            currentParticipants.remove(currentUserId)
            val updatedParticipants = currentParticipants.joinToString(",")
            
            if (updatedParticipants.isEmpty()) {
                chatDao.deleteChatById(chatId)
                syncRepository.deleteChatOnFirestore(chatId)
            } else {
                chatDao.updateParticipants(chatId, updatedParticipants)
                syncRepository.removeParticipantFromFirestore(chatId, currentUserId ?: "")
            }
            _actionFeedback.emit("You left the group")
        }
    }

    fun reportGroup(chatId: String, reason: String) {
        viewModelScope.launch {
            try {
                val chat = chatDao.getChatById(chatId) ?: return@launch
                syncRepository.reportGroup(chat, reason)
                chatDao.updateReportedStatus(chatId, true)
                _actionFeedback.emit("Group reported successfully.")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to report group", e)
                _actionFeedback.emit("Failed to report group: ${e.message}")
            }
        }
    }

    fun reportUser(userId: String, reason: String) {
        viewModelScope.launch {
            try {
                val user = userDao.getUserById(userId) ?: return@launch
                syncRepository.reportUser(user, reason)
                _actionFeedback.emit("User reported successfully.")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to report user", e)
                _actionFeedback.emit("Failed to report user: ${e.message}")
            }
        }
    }

    fun reportMessage(message: MessageEntity, reason: String) {
        viewModelScope.launch {
            try {
                syncRepository.reportMessage(message, reason)
                _actionFeedback.emit("Message reported. Our moderation team will review this interaction.")
            } catch (e: Exception) {
                _actionFeedback.emit("Failed to report message: ${e.message}")
            }
        }
    }

    fun toggleMute(chatId: String, isMuted: Boolean) {
        viewModelScope.launch {
            chatDao.updateMutedStatus(chatId, isMuted)
            val chat = chatDao.getChatById(chatId)
            val label = if (chat?.isGroup == true) "Group" else "Chat"
            _actionFeedback.emit(if (isMuted) "$label muted" else "$label unmuted")
        }
    }

    fun setChatId(chatId: String) {
        _currentChatId.value = chatId
        viewModelScope.launch {
            chatDao.clearUnreadCount(chatId)
            
            // Observe messages
            val syncJob = syncRepository.observeAndSyncMessages(chatId)
                .catch { e -> Log.e("ChatViewModel", "Error syncing messages for $chatId: ${e.message}") }
                .launchIn(viewModelScope)
                
            // Observe group library and access requests if applicable
            val chat = chatDao.getChatById(chatId)
            if (chat?.isGroup == true) {
                viewModelScope.launch {
                    syncRepository.observeGroupLibrary(chatId)
                        .collect { _sharedCollections.value = it }
                }
                viewModelScope.launch {
                    syncRepository.observeSharedSessions(chatId)
                        .collect { _sharedSessions.value = it }
                }
                viewModelScope.launch {
                    syncRepository.observeAccessRequests(chatId)
                        .collect { _accessRequests.value = it }
                }
            } else {
                _sharedCollections.value = emptyList()
                _sharedSessions.value = emptyList()
                _accessRequests.value = emptyList()
            }
        }
    }


    fun addSharedCollection(jsonPayload: String) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Attempting to add AI collection from JSON")
                val response = json.decodeFromString<AiCollectionResponse>(jsonPayload)
                
                // Save to Cache first (optional but keeps a record)
                val responseId = UUID.randomUUID().toString()
                aiRepository.getAiResponseById(responseId) // Just to ensure visibility if we were using a different flow
                
                // Since this is already triggered by a "Save" button in the bubble, it represents explicit consent.
                aiRepository.saveAsCollection(response)
                
                _actionFeedback.emit("New Collection '${response.collectionTitle}' added to your library.")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to add AI collection", e)
                _actionFeedback.emit("Failed to add collection: ${e.message}")
            }
        }
    }

    fun importSharedCollection(payload: String) {
        viewModelScope.launch {
            try {
                Log.d("ChatViewModel", "Attempting to import shared collection file")
                // Payload format: url|E2EE_KEY|symmetricKey
                val parts = payload.split("|E2EE_KEY|")
                if (parts.size < 2) {
                    _actionFeedback.emit("Invalid sharing payload format")
                    return@launch
                }
                
                val url = parts[0]
                val key = parts[1].substringBefore("|UPDATED_AT|")
                
                _actionFeedback.emit("Downloading and importing collection...")
                
                val result = mockDownloader.downloadAndImportMock(url, key)
                if (result.isSuccess) {
                    _actionFeedback.emit("Collection imported successfully to your library!")
                    
                    // Cleanup: Delete the temporary file from Appwrite
                    try {
                        val fileId = url.substringAfter("/files/").substringBefore("/download")
                        if (fileId.isNotEmpty() && fileId != url) {
                            syncRepository.deleteQuestionBankZip(fileId)
                            Log.d("ChatViewModel", "Deleted temporary file $fileId from Appwrite")
                        }
                    } catch (cleanupEx: Exception) {
                        Log.e("ChatViewModel", "Failed to cleanup shared file", cleanupEx)
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("ChatViewModel", "Import failed: $error")
                    _actionFeedback.emit("Import failed: $error")
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Import exception", e)
                _actionFeedback.emit("Import error: ${e.message}")
            }
        }
    }

    fun shareCollection(chatId: String, collectionId: String) {
        viewModelScope.launch {
            val chat = chatDao.getChatById(chatId) ?: return@launch
            _isSharing.value = true
            try {
                val collection = collectionDao.getStudyCollectionByIdOnce(collectionId) ?: throw Exception("Collection not found")
                val zipFile = mockExporter.exportCollection(collectionId) ?: throw Exception("Failed to export collection")
                
                val (downloadUrl, symmetricKey) = syncRepository.uploadQuestionBankZip(zipFile)
                val updatedAt = collection.updatedAt
                
                if (chat.isGroup) {
                    // DIFFERENT MECHANISM FOR GROUPS: Persistent Library
                    val metadata = hashMapOf(
                        "collectionId" to collectionId,
                        "name" to collection.name,
                        "description" to (collection.description ?: ""),
                        "downloadUrl" to downloadUrl,
                        "symmetricKey" to symmetricKey, // This is already encrypted via encryptFileContent logic? No, symmetricKey is the key to decrypt the ZIP.
                        // Actually, in the current app, the ZIP symmetric key IS the E2EE key for that file.
                        "updatedAt" to updatedAt,
                        "sharedBy" to currentUserId,
                        "timestamp" to System.currentTimeMillis()
                    )
                    syncRepository.shareCollectionToGroup(chatId, metadata)
                    sendMessage(chatId, "shared a collection to the group library: ${collection.name}", type = "DB_CHANGE")
                } else {
                    // P2P: Message-based (Ephemeral)
                    sendMessage(chatId, "$downloadUrl|E2EE_KEY|$symmetricKey|UPDATED_AT|$updatedAt|COLLECTION_ID|$collectionId", type = "FILE_TRANSFER")
                }
                
                mockExporter.cleanup(zipFile)
                _actionFeedback.emit("Collection shared successfully")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Sharing failed", e)
                _actionFeedback.emit("Sharing failed: ${e.message}")
            } finally {
                _isSharing.value = false
            }
        }
    }

    fun shareSession(chatId: String, sessionId: String) {
        viewModelScope.launch {
            try {
                _isAiLoading.value = true // Show loading
                val session = sessionDao.getSessionById(sessionId)
                val sessionTitle = session?.title ?: "Study Session"
                
                val chat = chatDao.getChatById(chatId)
                if (chat?.isGroup == true) {
                    syncRepository.addSharedSessionToGroup(chatId, sessionId, sessionTitle)
                    sendMessage(chatId, "shared a study session: $sessionTitle", type = "DB_CHANGE")
                } else {
                    syncRepository.sendSessionInvite(chatId, sessionId, sessionTitle)
                }
                _actionFeedback.emit("Session shared successfully")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to share session", e)
                _actionFeedback.emit("Failed to share session: ${e.message}")
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun startNewChat(userId: String, userName: String) {
        viewModelScope.launch {
            // Check for existing P2P chat
            val ids1 = "$currentUserId,$userId"
            val ids2 = "$userId,$currentUserId"
            val existingChat = chatDao.getP2PChat(ids1, ids2)
            
            if (existingChat != null) {
                _currentChatId.value = existingChat.chatId
                _actionFeedback.emit("Opening existing conversation")
                return@launch
            }

            val chatId = UUID.randomUUID().toString()
            val newChat = ChatEntity(
                chatId = chatId,
                chatName = userName,
                isGroup = false,
                participantIds = ids1,
                adminId = currentUserId
            )
            chatDao.insertChat(newChat)
            
            // Log onto Firestore as well (don't sync AI chats to global chat list if they are local)
            if (userId != QBASE_AI_BOT_ID) {
                syncRepository.createChatOnFirestore(newChat)
            }
            
            val initialMessage = if (userId == QBASE_AI_BOT_ID) 
                "Hello! I am Qbase AI. How can I assist you with your studies today?"
                else "Hi! I'd like to start a professional conversation."
            
            sendMessage(chatId, initialMessage, senderId = if (userId == QBASE_AI_BOT_ID) QBASE_AI_BOT_ID else currentUserId)
            _currentChatId.value = chatId
            _navigationEvents.emit(ChatNavEvent.NavigateToChatDetail(chatId))
        }
    }

    fun startNewGroup(participantIds: List<String>, groupName: String) {
        viewModelScope.launch {
            val chatId = UUID.randomUUID().toString()
            val allParticipants = (participantIds + currentUserId).distinct().joinToString(",")
            
            val newChat = ChatEntity(
                chatId = chatId,
                chatName = groupName,
                isGroup = true,
                participantIds = allParticipants,
                adminId = currentUserId
            )
            
            chatDao.insertChat(newChat)
            syncRepository.createChatOnFirestore(newChat)
            
            sendMessage(chatId, "created the group \"$groupName\"", type = "DB_CHANGE")
            _currentChatId.value = chatId
            _navigationEvents.emit(ChatNavEvent.NavigateToChatDetail(chatId))
        }
    }

    fun startAiChat() {
        startNewChat(QBASE_AI_BOT_ID, "Qbase AI")
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch {
            try {
                val chat = chatDao.getChatById(chatId)
                if (chat != null && chat.adminId != currentUserId && chat.isGroup) {
                    val currentParticipants = chat.participantIds.split(",").toMutableList()
                    currentParticipants.remove(currentUserId)
                    val updatedParticipants = currentParticipants.joinToString(",")
                    chatDao.deleteChatById(chatId)
                    syncRepository.removeParticipantFromFirestore(chatId, currentUserId ?: "")
                } else {
                    chatDao.deleteChatById(chatId)
                    syncRepository.deleteChatOnFirestore(chatId)
                }
                _actionFeedback.emit("Chat deleted successfully")
            } catch (e: Exception) {
                _actionFeedback.emit("Failed to delete chat: ${e.message}")
            }
        }
    }

    fun clearChatMessages(chatId: String) {
        viewModelScope.launch {
            try {
                messageDao.deleteMessagesByChatId(chatId)
                syncRepository.clearChatMessagesOnFirestore(chatId)
                _actionFeedback.emit("Chat history cleared")
            } catch (e: Exception) {
                _actionFeedback.emit("Failed to clear chat: ${e.message}")
            }
        }
    }

    fun sendMessage(chatId: String, text: String, type: String = "TEXT", senderId: String = currentUserId) {
        val message = MessageEntity(
            messageId = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = senderId,
            payload = text,
            type = type,
            timestamp = System.currentTimeMillis()
        )
        
        viewModelScope.launch {
            messageDao.insertMessage(message)
            
            // Only sync to Firestore if it's not a message from/to the local AI bot
            // Note: If both users are human, currentUserId and senderId are same.
            // If it's a bot message being inserted, senderId is botId.
            val chat = chatDao.getChatById(chatId)
            val isAiChat = chat?.participantIds?.contains(QBASE_AI_BOT_ID) == true
            
            if (!isAiChat) {
                try {
                    syncRepository.sendMessage(message)
                } catch (e: Exception) {
                    // Remove the optimistic local message if encryption/sync fails
                    messageDao.deleteMessage(message)
                    _actionFeedback.emit("Message not sent: ${e.message ?: "Unknown error"}")
                }
            } else if (senderId == currentUserId) {
                // Trigger AI response if user sent a message to the bot
                handleAiChatResponse(chatId, text)
            }
        }
    }

    private fun handleAiChatResponse(chatId: String, userMessage: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                _actionFeedback.emit("Qbase AI is thinking...")
                val result = aiRepository.getAiAssistance(userMessage)
                result.onSuccess { reply ->
                    sendMessage(chatId, reply, senderId = QBASE_AI_BOT_ID)
                }.onFailure { e ->
                    sendMessage(chatId, "I'm sorry, I encountered an error: ${e.message}", senderId = QBASE_AI_BOT_ID)
                }
            } catch (e: Exception) {
                _actionFeedback.emit("AI Error: ${e.message}")
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun toggleBlock(chatId: String, isBlocked: Boolean) {
        viewModelScope.launch {
            chatDao.updateBlockedStatus(chatId, isBlocked)
            val chat = chatDao.getChatById(chatId)
            val label = if (chat?.isGroup == true) "Group" else "Chat"
            _actionFeedback.emit(if (isBlocked) "$label blocked" else "$label unblocked")
        }
    }
}
