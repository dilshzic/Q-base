package com.algorithmx.q_base.ui.chat

import android.util.Log

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.core_ai.brain.models.AiCollectionResponse
import com.algorithmx.q_base.data.chat.ChatDao
import com.algorithmx.q_base.data.chat.ChatEntity
import com.algorithmx.q_base.data.chat.isAdmin
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.algorithmx.q_base.data.util.MockDownloader
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject


import com.algorithmx.q_base.util.NetworkMonitor

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
    private val mockDownloader: MockDownloader,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun canSendToChat(chatId: String): Flow<Boolean> {
        return combine(
            chatDao.getChatByIdFlow(chatId),
            isOnline
        ) { chat, online ->
            if (chat == null) false
            else {
                val isAi = chat.participantIds.contains(QBASE_AI_BOT_ID)
                isAi || online
            }
        }
    }


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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

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

    val currentUserId: String
        get() = authRepository.currentUserId ?: ""

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
                    if (chat != null && !chat.isAdmin(currentUserId ?: "") && chat.isGroup) {
                        val currentParticipants = chat.participantIds.split(",").toMutableList()
                        currentParticipants.remove(currentUserId)
                        val updatedParticipants = currentParticipants.joinToString(",")
                        chatDao.deleteChatById(chatId)
                        messageDao.deleteMessagesByChatId(chatId)
                        syncRepository.removeParticipantFromRemote(chatId, currentUserId ?: "")
                    } else {
                        chatDao.deleteChatById(chatId)
                        messageDao.deleteMessagesByChatId(chatId)
                        syncRepository.deleteChatOnRemote(chatId)
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
        chatDao.getChatSummaries(),
        userDao.getAllUsers()
    ) { summaries, users ->
        val userMap = users.associateBy { it.userId }
        val chatUiModels = summaries.mapNotNull { summary ->
            // Resolve Display Name
            val otherParticipantId = summary.participantIds.split(",")
                .firstOrNull { it != currentUserId && it.isNotEmpty() }
            val otherUser = userMap[otherParticipantId]

            // Account-Level Ban/Block mechanism: Client-Side Hiding
            if ((!summary.isGroup && otherUser?.isBanned == true) || summary.isBlocked) {
                return@mapNotNull null
            }

            val resolvedName = (if (summary.isGroup) {
                summary.chatName
            } else {
                otherUser?.displayName ?: summary.chatName
            }) ?: "Chat"

            ChatUiModel(
                chat = ChatEntity(
                    chatId = summary.chatId,
                    chatName = summary.chatName,
                    isGroup = summary.isGroup,
                    participantIds = summary.participantIds,
                    unreadCount = summary.unreadCount,
                    isBlocked = summary.isBlocked
                ),
                displayName = resolvedName,
                latestMessage = if (summary.lastMessageTimestamp != null) {
                    MessageEntity(
                        messageId = "", // Not needed for UI list
                        chatId = summary.chatId,
                        senderId = "", // Not needed for UI list
                        payload = summary.lastMessagePayload ?: "",
                        type = summary.lastMessageType ?: "TEXT",
                        timestamp = summary.lastMessageTimestamp ?: 0L
                    )
                } else null,
                unreadCount = summary.unreadCount
            )
        }.sortedByDescending { it.latestMessage?.timestamp ?: 0L }

        ChatListState(
            chats = chatUiModels,
            users = userMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatListState())
    
    // State for the Blocked List
    val blockedChatsState: StateFlow<List<ChatUiModel>> = combine(
        chatDao.getChatSummaries(),
        userDao.getAllUsers()
    ) { summaries, users ->
        val userMap = users.associateBy { it.userId }
        summaries.filter { it.isBlocked }.map { summary ->
            val otherParticipantId = summary.participantIds.split(",")
                .firstOrNull { it != currentUserId && it.isNotEmpty() }
            val otherUser = userMap[otherParticipantId]
            val resolvedName = (if (summary.isGroup) {
                summary.chatName
            } else {
                otherUser?.displayName ?: summary.chatName
            }) ?: "Chat"

            ChatUiModel(
                chat = ChatEntity(
                    chatId = summary.chatId,
                    chatName = summary.chatName,
                    isGroup = summary.isGroup,
                    participantIds = summary.participantIds,
                    unreadCount = summary.unreadCount,
                    isBlocked = summary.isBlocked
                ),
                displayName = resolvedName,
                latestMessage = if (summary.lastMessageTimestamp != null) {
                    MessageEntity(
                        messageId = "",
                        chatId = summary.chatId,
                        senderId = "",
                        payload = summary.lastMessagePayload ?: "",
                        type = summary.lastMessageType ?: "TEXT",
                        timestamp = summary.lastMessageTimestamp ?: 0L
                    )
                } else null,
                unreadCount = summary.unreadCount
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()
    
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
            if (chat.isGroup && !chat.isAdmin(currentUserId ?: "")) {
                _actionFeedback.emit("Only an admin can add participants")
                return@launch
            }

            val currentParticipants = chat.participantIds
            val updatedParticipants = if (currentParticipants.isEmpty()) userId else "$currentParticipants,$userId"
            
            chatDao.updateParticipants(chatId, updatedParticipants)
            syncRepository.addParticipantToRemote(chatId, userId)
            
            // System message for participant added
            sendMessage(chatId, "added a new participant", type = "DB_CHANGE")
            
            _actionFeedback.emit("Participant added successfully")
        }
    }

    fun removeParticipant(chatId: String, userId: String) {
        viewModelScope.launch {
            val chat = chatDao.getChatById(chatId) ?: return@launch
            
            // Admin Check
            if (!chat.isAdmin(currentUserId ?: "")) {
                _actionFeedback.emit("Only an admin can remove participants")
                return@launch
            }

            val currentParticipants = chat.participantIds.split(",").toMutableList()
            if (currentParticipants.contains(userId)) {
                currentParticipants.remove(userId)
                val updatedParticipants = currentParticipants.joinToString(",")
                
                val currentAdmins = chat.adminIds.split(",").filter { it.isNotBlank() }.toMutableList()
                currentAdmins.remove(userId)
                val updatedAdmins = currentAdmins.joinToString(",")

                chatDao.insertChat(chat.copy(
                    participantIds = updatedParticipants,
                    adminIds = updatedAdmins
                ))
                syncRepository.removeParticipantFromRemote(chatId, userId)
                
                sendMessage(chatId, "removed a participant", type = "DB_CHANGE")
                _actionFeedback.emit("Participant removed successfully")
            }
        }
    }

    fun promoteParticipantToAdmin(chatId: String, userId: String) {
        viewModelScope.launch {
            val chat = chatDao.getChatById(chatId) ?: return@launch
            
            // Admin Check
            if (!chat.isAdmin(currentUserId ?: "")) {
                _actionFeedback.emit("Only an admin can promote members")
                return@launch
            }

            val currentAdmins = chat.adminIds.split(",").filter { it.isNotBlank() }.toMutableList()
            if (!currentAdmins.contains(userId)) {
                currentAdmins.add(userId)
                val updatedAdmins = currentAdmins.joinToString(",")
                
                chatDao.insertChat(chat.copy(adminIds = updatedAdmins))
                syncRepository.promoteParticipantToAdminOnRemote(chatId, userId)
                
                sendMessage(chatId, "promoted a member to admin", type = "DB_CHANGE")
                _actionFeedback.emit("Participant promoted to Admin successfully")
            }
        }
    }

    fun demoteAdmin(chatId: String, userId: String) {
        viewModelScope.launch {
            val chat = chatDao.getChatById(chatId) ?: return@launch
            
            // Admin Check
            if (!chat.isAdmin(currentUserId ?: "")) {
                _actionFeedback.emit("Only an admin can demote admins")
                return@launch
            }

            val currentAdmins = chat.adminIds.split(",").filter { it.isNotBlank() }.toMutableList()
            if (currentAdmins.contains(userId)) {
                currentAdmins.remove(userId)
                val updatedAdmins = currentAdmins.joinToString(",")
                
                chatDao.insertChat(chat.copy(adminIds = updatedAdmins))
                syncRepository.demoteAdminOnRemote(chatId, userId)
                
                sendMessage(chatId, "demoted an admin to member", type = "DB_CHANGE")
                _actionFeedback.emit("Admin demoted to member successfully")
            }
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
                messageDao.deleteMessagesByChatId(chatId)
                syncRepository.deleteChatOnRemote(chatId)
            } else {
                chatDao.deleteChatById(chatId)
                messageDao.deleteMessagesByChatId(chatId)
                syncRepository.removeParticipantFromRemote(chatId, currentUserId ?: "")
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

    fun syncChatsFromRemote() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                syncRepository.syncUserChatsFromRemote()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error syncing remote chats", e)
            } finally {
                _isRefreshing.value = false
            }
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
                val remainder = parts[1]
                val key = remainder.substringBefore("|UPDATED_AT|")
                val isAdminOnly = remainder.contains("|ADMIN_ONLY|true")
                val groupId = remainder.substringAfter("|GROUP_ID|", "").substringBefore("|")
                val collectionId = remainder.substringAfter("|COLLECTION_ID|", "").substringBefore("|")
                
                _actionFeedback.emit("Downloading and importing collection...")
                
                val result = mockDownloader.downloadAndImportMock(
                    url = url, 
                    symmetricKeyBase64 = key,
                    sharedWithGroupId = groupId.ifBlank { null },
                    isAdminOnly = isAdminOnly
                )
                if (result.isSuccess) {
                    _actionFeedback.emit("Collection imported successfully to your library!")
                    
                    if (collectionId.isNotEmpty()) {
                        try {
                            syncRepository.acknowledgeCollectionDownload(collectionId)
                            Log.d("ChatViewModel", "Acknowledged collection download for $collectionId")
                        } catch (ae: Exception) {
                            Log.e("ChatViewModel", "Failed to acknowledge download receipt", ae)
                        }
                    }
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("ChatViewModel", "Import failed: $error")
                    if (error.contains("404") || error.contains("not found") || error.contains("NotFound")) {
                        _actionFeedback.emit("Collection ZIP has been garbage-collected (zero-retention policy). Please request an admin or owner to Re-upload/Resend this collection.")
                    } else {
                        _actionFeedback.emit("Import failed: $error")
                    }
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
                
                // RESTRICTION: Non-sharable if admin-only and user is not admin
                if (collection.isAdminOnly) {
                    if (!chat.isAdmin(currentUserId ?: "")) {
                        _actionFeedback.emit("Cannot share: This collection is restricted to group admins only.")
                        _isSharing.value = false
                        return@launch
                    }
                }

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
                        "symmetricKey" to symmetricKey,
                        "updatedAt" to updatedAt,
                        "sharedBy" to currentUserId,
                        "timestamp" to System.currentTimeMillis(),
                        "isAdminOnly" to collection.isAdminOnly
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

    fun resendCollection(collectionId: String) {
        val chatId = _currentChatId.value ?: return
        viewModelScope.launch {
            try {
                _actionFeedback.emit("Re-packaging and re-uploading collection...")
                val collection = collectionDao.getStudyCollectionByIdOnce(collectionId)
                    ?: throw Exception("Collection not found in local library")
                
                val zipFile = mockExporter.exportCollection(collectionId)
                    ?: throw Exception("Failed to export collection")
                
                val (downloadUrl, symmetricKey) = syncRepository.uploadQuestionBankZip(zipFile)
                val updatedAt = collection.updatedAt
                
                val metadata = hashMapOf(
                    "collectionId" to collectionId,
                    "name" to collection.name,
                    "description" to (collection.description ?: ""),
                    "downloadUrl" to downloadUrl,
                    "symmetricKey" to symmetricKey,
                    "updatedAt" to updatedAt,
                    "sharedBy" to (currentUserId ?: ""),
                    "timestamp" to System.currentTimeMillis(),
                    "isAdminOnly" to collection.isAdminOnly
                )
                syncRepository.shareCollectionToGroup(chatId, metadata)
                mockExporter.cleanup(zipFile)
                _actionFeedback.emit("Collection resent and re-uploaded successfully!")
                sendMessage(chatId, "re-uploaded and updated collection in group library: ${collection.name}", type = "DB_CHANGE")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Resend failed", e)
                _actionFeedback.emit("Resend failed: ${e.message}")
            }
        }
    }

    fun shareSession(chatId: String, sessionId: String) {
        viewModelScope.launch {
            _isAiLoading.value = true // Show loading
            try {
                val session = sessionDao.getSessionById(sessionId) ?: throw Exception("Session not found")
                val sessionTitle = session.title
                
                val chat = chatDao.getChatById(chatId) ?: throw Exception("Chat not found")
                
                // RESTRICTION: Non-sharable if admin-only and user is not admin
                if (session.isAdminOnly) {
                    if (!chat.isAdmin(currentUserId ?: "")) {
                        _actionFeedback.emit("Cannot share: This session is restricted to group admins only.")
                        return@launch
                    }
                }
                
                _actionFeedback.emit("Exporting and zipping study session...")
                val zipFile = mockExporter.exportSession(sessionId) ?: throw Exception("Failed to export session")
                
                _actionFeedback.emit("Uploading encrypted session...")
                val (downloadUrl, symmetricKey) = syncRepository.uploadQuestionBankZip(zipFile)
                
                if (chat.isGroup) {
                    syncRepository.addSharedSessionToGroup(chatId, sessionId, sessionTitle, downloadUrl, symmetricKey)
                    sendMessage(chatId, "shared a study session: $sessionTitle", type = "DB_CHANGE")
                } else {
                    syncRepository.sendSessionInvite(chatId, sessionId, sessionTitle, downloadUrl, symmetricKey)
                }
                
                mockExporter.cleanup(zipFile)
                _actionFeedback.emit("Session shared successfully")
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to share session", e)
                _actionFeedback.emit("Failed to share session: ${e.message}")
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun joinSession(sessionIdOrPayload: String, onSessionImported: (String) -> Unit) {
        viewModelScope.launch {
            _isAiLoading.value = true
            try {
                var downloadUrl: String? = null
                var symmetricKey: String? = null
                var sessionId: String = sessionIdOrPayload
                
                if (sessionIdOrPayload.contains("|E2EE_KEY|")) {
                    val parts = sessionIdOrPayload.split("|E2EE_KEY|")
                    downloadUrl = parts.getOrNull(0)
                    val remainder = parts.getOrNull(1) ?: ""
                    symmetricKey = remainder.substringBefore("|SESSION_ID|")
                    sessionId = remainder.substringAfter("|SESSION_ID|", "").substringBefore("|TITLE|")
                } else {
                    // Try finding in sharedSessions list
                    val sharedSessionsList = _sharedSessions.value
                    val matchingSession = sharedSessionsList.find { (it["sessionId"] as? String) == sessionIdOrPayload }
                    if (matchingSession != null) {
                        downloadUrl = matchingSession["downloadUrl"] as? String
                        symmetricKey = matchingSession["symmetricKey"] as? String
                        sessionId = sessionIdOrPayload
                    }
                }
                
                if (!downloadUrl.isNullOrBlank()) {
                    _actionFeedback.emit("Downloading and joining study session...")
                    val result = mockDownloader.downloadAndImportSession(downloadUrl, symmetricKey)
                    if (result.isSuccess) {
                        val importedId = result.getOrNull() ?: sessionId
                        _actionFeedback.emit("Session joined successfully!")
                        onSessionImported(importedId)
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Unknown error"
                        _actionFeedback.emit("Failed to join session: $error")
                    }
                } else {
                    val existing = sessionDao.getSessionById(sessionId)
                    if (existing != null) {
                        onSessionImported(sessionId)
                    } else {
                        _actionFeedback.emit("Session data not found locally or on the cloud.")
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error joining session", e)
                _actionFeedback.emit("Failed to join session: ${e.message}")
            } finally {
                _isAiLoading.value = false
            }
        }
    }

    fun startNewChat(userId: String, userName: String) {
        viewModelScope.launch {
            val uid = authRepository.currentUserId ?: return@launch
            // Check for existing P2P chat locally or on the cloud
            val existingChat = syncRepository.findExistingP2PChat(uid, userId)
            
            if (existingChat != null) {
                _currentChatId.value = existingChat.chatId
                _actionFeedback.emit("Opening existing conversation")
                _navigationEvents.emit(ChatNavEvent.NavigateToChatDetail(existingChat.chatId))
                if (userId != QBASE_AI_BOT_ID) {
                    viewModelScope.launch {
                        try {
                            syncRepository.createChatOnRemote(existingChat)
                        } catch (_: Exception) {}
                    }
                }
                return@launch
            }

            val chatId = UUID.randomUUID().toString()
            val newChat = ChatEntity(
                chatId = chatId,
                chatName = userName,
                isGroup = false,
                participantIds = "$uid,$userId",
                adminIds = uid
            )
            chatDao.insertChat(newChat)
            
            // Log onto Firestore as well (don't sync AI chats to global chat list if they are local)
            if (userId != QBASE_AI_BOT_ID) {
                syncRepository.createChatOnRemote(newChat)
            }
            
            val initialMessage = if (userId == QBASE_AI_BOT_ID) 
                "Hello! I am Qbase AI. How can I assist you with your studies today?"
                else "Hi! I'd like to start a professional conversation."
            
            sendMessage(chatId, initialMessage, senderId = if (userId == QBASE_AI_BOT_ID) QBASE_AI_BOT_ID else uid)
            _currentChatId.value = chatId
            _navigationEvents.emit(ChatNavEvent.NavigateToChatDetail(chatId))
        }
    }

    fun startNewGroup(participantIds: List<String>, groupName: String) {
        viewModelScope.launch {
            val uid = authRepository.currentUserId ?: return@launch
            val chatId = UUID.randomUUID().toString()
            val allParticipants = (participantIds + uid).filter { it.isNotBlank() }.distinct().joinToString(",")
            
            val newChat = ChatEntity(
                chatId = chatId,
                chatName = groupName,
                isGroup = true,
                participantIds = allParticipants,
                adminIds = uid
            )
            
            chatDao.insertChat(newChat)
            syncRepository.createChatOnRemote(newChat)
            
            sendMessage(chatId, "created the group \"$groupName\"", type = "DB_CHANGE", senderId = uid)
            _currentChatId.value = chatId
            _navigationEvents.emit(ChatNavEvent.NavigateToChatDetail(chatId))
        }
    }

    fun startAiChat() {
        startNewChat(QBASE_AI_BOT_ID, "Qbase AI")
    }

    fun deleteChat(chatId: String) {
        syncRepository.deleteChatAndMessagesGlobally(chatId)
    }

    fun clearChatMessages(chatId: String) {
        viewModelScope.launch {
            try {
                messageDao.deleteMessagesByChatId(chatId)
                syncRepository.clearChatMessagesOnRemote(chatId)
                _actionFeedback.emit("Chat history cleared")
            } catch (e: Exception) {
                _actionFeedback.emit("Failed to clear chat: ${e.message}")
            }
        }
    }

    fun sendMessage(chatId: String, text: String, type: String = "TEXT", senderId: String = currentUserId) {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        
        viewModelScope.launch {
            val chat = chatDao.getChatById(chatId)
            val isAiChat = chat?.participantIds?.contains(QBASE_AI_BOT_ID) == true
            
            val isOffline = !isAiChat && !isOnline.value
            
            val message = MessageEntity(
                messageId = messageId,
                chatId = chatId,
                senderId = senderId,
                payload = text,
                type = type,
                timestamp = timestamp,
                status = if (isOffline) "PENDING" else "SENT"
            )
            
            messageDao.insertMessage(message)
            
            if (!isAiChat) {
                if (isOffline) {
                    _actionFeedback.emit("Offline: Message queued.")
                    return@launch
                }
                
                try {
                    syncRepository.sendMessage(message)
                } catch (e: Exception) {
                    if (e.message?.contains("missing encryption keys") == true || e.message?.contains("Security abort") == true || e.message?.contains("failed to encrypt") == true) {
                        messageDao.updateMessageStatus(message.messageId, "FAILED")
                        _actionFeedback.emit("Message failed: Missing security keys.")
                    } else {
                        // Generic network/timeout error during sending
                        messageDao.updateMessageStatus(message.messageId, "PENDING")
                        _actionFeedback.emit("Network error: Message queued.")
                    }
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
