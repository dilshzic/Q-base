package com.algorithmx.q_base.feature.chat.presentation
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.core.data.chat.ChatLocalDataSource
import com.algorithmx.q_base.core.data.chat.ChatEntity
import com.algorithmx.q_base.core.data.chat.isAdmin
// Message flows provided by ChatLocalDataSource
import com.algorithmx.q_base.core.data.chat.MessageEntity
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.core.data.UserDao
import com.algorithmx.q_base.core.data.UserEntity
import com.algorithmx.q_base.core.ai.data.AiRepository
import com.algorithmx.q_base.core.data.auth.AuthRepository
import com.algorithmx.q_base.core.data.auth.ProfileRepository
import com.algorithmx.q_base.feature.sessions.data.SessionDao
import com.algorithmx.q_base.feature.sessions.data.StudySession
import com.algorithmx.q_base.sync.orchestration.SyncRepository
import com.algorithmx.q_base.core.data.util.MockExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.algorithmx.q_base.core.data.util.MockDownloader
import kotlinx.serialization.json.Json
import javax.inject.Inject
import com.algorithmx.q_base.util.NetworkMonitor

@HiltViewModel
class ChatViewModel @Inject constructor(
    internal val chatLocalDataSource: ChatLocalDataSource,
    internal val userDao: UserDao,
    internal val syncRepository: SyncRepository,
    internal val authRepository: AuthRepository,
    internal val profileRepository: ProfileRepository,
    internal val aiRepository: AiRepository,
    internal val collectionDao: CollectionDao,
    internal val sessionDao: SessionDao,
    internal val mockExporter: MockExporter,
    internal val mockDownloader: MockDownloader,
    internal val networkMonitor: NetworkMonitor
) : ViewModel() {
    private var messageSyncJob: Job? = null
    private var groupLibraryJob: Job? = null
    private var sharedSessionsJob: Job? = null
    private var accessRequestsJob: Job? = null
    private var keyPrefetchJob: Job? = null
    private var keyRefreshJob: Job? = null

    internal val json = Json { ignoreUnknownKeys = true }
    
    internal val _actionFeedback = MutableSharedFlow<String>()
    val actionFeedback = _actionFeedback.asSharedFlow()
    
    internal val _accessRequests = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val accessRequests = _accessRequests.asStateFlow()

    internal val _isSharing = MutableStateFlow(false)
    val isSharing = _isSharing.asStateFlow()

    internal val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    internal val _isAiLoading = MutableStateFlow(false)
    val isAiLoading = _isAiLoading.asStateFlow()

    internal val _sharedCollections = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val sharedCollections: StateFlow<List<Map<String, Any>>> = _sharedCollections.asStateFlow()

    internal val _sharedSessions = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val sharedSessions: StateFlow<List<Map<String, Any>>> = _sharedSessions.asStateFlow()

    internal val _isLibraryMode = MutableStateFlow(false)
    val isLibraryMode = _isLibraryMode.asStateFlow()

    internal val _selectedChatIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedChatIds = _selectedChatIds.asStateFlow()

    internal val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    internal val _currentChatId = MutableStateFlow<String?>(null)
    val currentChatId: StateFlow<String?> = _currentChatId.asStateFlow()
    
    internal val _navigationEvents = MutableSharedFlow<ChatNavEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val currentUserId: String
        get() = authRepository.currentUserId ?: ""

    // State for user selection
    val allUsers: StateFlow<List<UserEntity>> = userDao.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val localStudyCollections: StateFlow<List<StudyCollection>> = collectionDao.getAllStudyCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<StudySession>> = sessionDao.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<UserEntity?> = authRepository.currentUser
        .flatMapLatest { authUser ->
            if (authUser == null) flowOf<UserEntity?>(null)
            else userDao.getCurrentUser(authUser.uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalUnreadCount = chatLocalDataSource.getTotalUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allStudyCollections: StateFlow<List<StudyCollection>> = collectionDao.getAllStudyCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // State for the Inbox/Chat List
    val chatListState: StateFlow<ChatListState> = combine(
        chatLocalDataSource.getChatSummaries(),
        userDao.getAllUsers()
    ) { summaries, users ->
        val userMap = users.associateBy { it.userId }
        val chatUiModels = summaries.mapNotNull { summary ->
            val otherParticipantId = summary.participantIds.split(",")
                .firstOrNull { it != currentUserId && it.isNotEmpty() }
            val otherUser = userMap[otherParticipantId]

            if ((!summary.isGroup && otherUser?.isBanned == true) || summary.isBlocked) {
                return@mapNotNull null
            }

            val resolvedName = if (summary.isGroup) {
                summary.chatName?.takeIf { it.isNotBlank() } ?: "Chat"
            } else {
                otherUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: summary.chatName?.takeIf { it.isNotBlank() }
                    ?: "Chat"
            }

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
        }.sortedByDescending { it.latestMessage?.timestamp ?: 0L }

        ChatListState(
            chats = chatUiModels,
            users = userMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ChatListState())
    
    val blockedChatsState: StateFlow<List<ChatUiModel>> = combine(
        chatLocalDataSource.getChatSummaries(),
        userDao.getAllUsers()
    ) { summaries, users ->
        val userMap = users.associateBy { it.userId }
        summaries.filter { it.isBlocked }.map { summary ->
            val otherParticipantId = summary.participantIds.split(",")
                .firstOrNull { it != currentUserId && it.isNotEmpty() }
            val otherUser = userMap[otherParticipantId]
            val resolvedName = if (summary.isGroup) {
                summary.chatName?.takeIf { it.isNotBlank() } ?: "Chat"
            } else {
                otherUser?.displayName?.takeIf { it.isNotBlank() }
                    ?: summary.chatName?.takeIf { it.isNotBlank() }
                    ?: "Chat"
            }

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

    @OptIn(ExperimentalCoroutinesApi::class)
    val chatDetailState: StateFlow<ChatDetailState> = _currentChatId.flatMapLatest { chatId ->
        if (chatId == null) flowOf(ChatDetailState())
        else {
            combine(
                chatLocalDataSource.getChatByIdFlow(chatId),
                chatLocalDataSource.getMessagesForChat(chatId),
                userDao.getAllUsers()
            ) { chat, messages, users ->
                val userMap = users.associateBy { it.userId }
                val resolvedName = if (chat?.isGroup == true) {
                    chat.chatName?.takeIf { it.isNotBlank() } ?: "Chat"
                } else {
                    val otherParticipantId = chat?.participantIds?.split(",")
                        ?.firstOrNull { it != currentUserId && it.isNotEmpty() }
                    userMap[otherParticipantId]?.displayName?.takeIf { it.isNotBlank() }
                        ?: chat?.chatName?.takeIf { it.isNotBlank() }
                        ?: "Chat"
                }

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

    init {
        viewModelScope.launch {
            if (userDao.getUserById(QBASE_AI_BOT_ID) == null) {
                userDao.insertUser(
                    UserEntity(
                        userId = QBASE_AI_BOT_ID,
                        displayName = "Qbase AI",
                        email = null,
                        intro = "Qbase Official AI Assistant",
                        profilePictureUrl = null,
                        friendCode = "AI-BOT",
                        publicKey = null,
                        isBanned = false,
                        isPhotoVisible = true
                    )
                )
            }
        }
        viewModelScope.launch {
            combine(
                authRepository.currentUser,
                isOnline
            ) { user, online ->
                user != null && online
            }
            .distinctUntilChanged()
            .collect { shouldSync ->
                if (shouldSync) {
                    syncChatsFromRemote()
                }
            }
        }
    }

    fun canSendToChat(chatId: String): Flow<Boolean> {
        return combine(
            chatLocalDataSource.getChatByIdFlow(chatId),
            isOnline
        ) { chat, online ->
            if (chat == null) false
            else {
                val isAi = chat.participantIds
                    .split(",")
                    .any { it.trim() == QBASE_AI_BOT_ID }
                isAi || online
            }
        }
    }

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

    fun toggleLibraryMode(enabled: Boolean) {
        _isLibraryMode.value = enabled
    }

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
                    val chat = chatLocalDataSource.getChatById(chatId)
                    if (chat != null && !chat.isAdmin(currentUserId) && chat.isGroup) {
                        val currentParticipants = chat.participantIds.split(",").toMutableList()
                        currentParticipants.remove(currentUserId)
                        chatLocalDataSource.deleteChatById(chatId)
                        chatLocalDataSource.deleteMessagesByChatId(chatId)
                        syncRepository.removeParticipantFromRemote(chatId, currentUserId)
                    } else {
                        chatLocalDataSource.deleteChatById(chatId)
                        chatLocalDataSource.deleteMessagesByChatId(chatId)
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
        if (_currentChatId.value == chatId) return

        messageSyncJob?.cancel()
        groupLibraryJob?.cancel()
        sharedSessionsJob?.cancel()
        accessRequestsJob?.cancel()

        _currentChatId.value = chatId
        prefetchChatParticipantProfiles(chatId)
        viewModelScope.launch {
            chatLocalDataSource.clearUnreadCount(chatId)
            
            messageSyncJob = syncRepository.observeAndSyncMessages(chatId)
                .catch { e -> Log.e("ChatViewModel", "Error syncing messages for $chatId: ${e.message}") }
                .launchIn(viewModelScope)
                
            val chat = chatLocalDataSource.getChatById(chatId)
            if (chat?.isGroup == true) {
                groupLibraryJob = viewModelScope.launch {
                    syncRepository.observeGroupLibrary(chatId)
                        .collect { _sharedCollections.value = it }
                }
                sharedSessionsJob = viewModelScope.launch {
                    syncRepository.observeSharedSessions(chatId)
                        .collect { _sharedSessions.value = it }
                }
                accessRequestsJob = viewModelScope.launch {
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

    internal fun prefetchChatParticipantProfiles(chatId: String) {
        keyPrefetchJob?.cancel()
        keyPrefetchJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            refreshChatParticipantProfiles(chatId)
        }
    }

    internal fun refreshMissingKeysForChat(chatId: String) {
        keyRefreshJob?.cancel()
        keyRefreshJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            refreshChatParticipantProfiles(chatId)
            try {
                syncRepository.flushQueue()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to flush message queue after key refresh", e)
            }
        }
    }

    internal suspend fun refreshChatParticipantProfiles(chatId: String) {
        val chat = chatLocalDataSource.getChatById(chatId) ?: return
        chat.participantIds
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .forEach { participantId ->
                try {
                    profileRepository.syncUserProfile(participantId)
                } catch (e: Exception) {
                    Log.w("ChatViewModel", "Background profile refresh failed for $participantId", e)
                }
            }
    }

    companion object {
        const val QBASE_AI_BOT_ID = "qbase_ai_bot"
    }
}