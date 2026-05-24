package com.algorithmx.q_base.feature.chat.presentation
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.core.data.chat.MessageEntity
import com.algorithmx.q_base.core.data.chat.isAdmin
import com.algorithmx.q_base.feature.chat.presentation.components.*
import com.algorithmx.q_base.core.state.LocalAppAccessState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    onBack: () -> Unit,
    onHeaderClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onJoinSession: (String) -> Unit,
    onDeleteAndRestart: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.chatDetailState.collectAsState()
    val chat = state.chat
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val localCollections by viewModel.localStudyCollections.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var savedCollections by remember { mutableStateOf(setOf<String>()) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    var showSessionPicker by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var reportingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var showReportGroupDialog by remember { mutableStateOf(false) }
    val collections by viewModel.allStudyCollections.collectAsStateWithLifecycle()
    val appAccessState = LocalAppAccessState.current
    val canSend by remember(state.chat?.chatId) {
        state.chat?.chatId?.let { viewModel.canSendToChat(it) } ?: kotlinx.coroutines.flow.flowOf(false)
    }.collectAsState(initial = false)

    val currentChatId by viewModel.currentChatId.collectAsStateWithLifecycle()
    var hasLoadedActiveChat by remember(currentChatId) { mutableStateOf(false) }

    LaunchedEffect(state.chat, currentChatId) {
        if (state.chat != null) {
            hasLoadedActiveChat = true
        } else if (currentChatId != null && hasLoadedActiveChat) {
            onBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.actionFeedback.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Keyboard Visibility Awareness for Scrolling
    val density = LocalDensity.current
    val imeInsets = WindowInsets.ime
    val isKeyboardOpen by remember {
        derivedStateOf { imeInsets.getBottom(density) > 0 }
    }
    
    LaunchedEffect(isKeyboardOpen) {
        if (isKeyboardOpen && state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            val isLibraryMode by viewModel.isLibraryMode.collectAsStateWithLifecycle()
            ChatDetailTopBar(
                displayName = state.displayName,
                chat = state.chat,
                participantsCount = state.participants.size,
                isAiLoading = isAiLoading,
                appAccessState = appAccessState,
                currentUser = currentUser,
                isLibraryMode = isLibraryMode,
                onBack = onBack,
                onHeaderClick = onHeaderClick,
                onProfileClick = onProfileClick,
                onToggleLibraryMode = { viewModel.toggleLibraryMode(it) },
                onClearHistoryClick = { showClearConfirm = true },
                onReportGroupClick = { showReportGroupDialog = true },
                onDeleteChatClick = { showDeleteConfirm = true }
            )
        },
        bottomBar = {
            val isLibraryMode by viewModel.isLibraryMode.collectAsStateWithLifecycle()
            ChatDetailBottomBar(
                chat = state.chat,
                isLibraryMode = isLibraryMode,
                canSend = canSend,
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onAttachClick = { showCollectionPicker = true },
                onSessionClick = { showSessionPicker = true },
                onSendClick = {
                    state.chat?.chatId?.let { 
                        viewModel.sendMessage(it, messageText)
                        messageText = ""
                    }
                }
            )
        }
    ) { padding ->
        val isLibraryMode by viewModel.isLibraryMode.collectAsStateWithLifecycle()
        val sharedCollections by viewModel.sharedCollections.collectAsStateWithLifecycle()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLibraryMode && chat?.isGroup == true) {
                val accessRequests by viewModel.accessRequests.collectAsStateWithLifecycle()
                val isAdmin = chat.isAdmin(state.currentUserId)
                val sharedSessions by viewModel.sharedSessions.collectAsStateWithLifecycle()
                
                SharedLibraryView(
                    chatId = chat.chatId,
                    collections = sharedCollections,
                    sessions = sharedSessions,
                    onImport = { payload -> viewModel.importSharedCollection(payload) },
                    onJoinSession = { sessionId -> viewModel.joinSession(sessionId, onJoinSession) },
                    onResend = { collectionId -> viewModel.resendCollection(collectionId) },
                    localCollections = localCollections,
                    isAdmin = isAdmin,
                    accessRequests = accessRequests,
                    onRequestAccess = { viewModel.requestAccess(it) },
                    onGrantAccess = { collId, reqId -> viewModel.grantAccess(collId, reqId) }
                )
            } else {
                val messagesByDate = remember(state.messages) {
                    state.messages.groupBy { message ->
                        formatDateRelatively(message.timestamp)
                    }
                }
 
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp, start = 8.dp, end = 8.dp)
                ) {
                    messagesByDate.forEach { (date, messages) ->
                        stickyHeader(key = date) {
                            DateHeader(date)
                        }
                        
                        itemsIndexed(
                            items = messages,
                            key = { _, msg -> msg.messageId }
                        ) { index, message ->
                            if (message.type == "DB_CHANGE") {
                                SystemMessageItem(message.payload)
                            } else {
                                val isMine = message.senderId == state.currentUserId
                                val prevMessage = if (index > 0) messages[index - 1] else null
                                val nextMessage = if (index < messages.size - 1) messages[index + 1] else null
                                
                                val isFirstInGroup = prevMessage == null || prevMessage.senderId != message.senderId
                                val isLastInGroup = nextMessage == null || nextMessage.senderId != message.senderId
                                
                                val showAvatar = !isMine && isLastInGroup
                                val showSenderName = (state.chat?.isGroup == true) && isFirstInGroup && !isMine
 
                                Box(modifier = Modifier.animateItem()) {
                                    AnimatedMessageItem(
                                        message = message,
                                        isMine = isMine,
                                        senderName = if (showSenderName) state.participants[message.senderId]?.displayName else null,
                                        showAvatar = showAvatar,
                                        avatarUrl = if (showAvatar) state.participants[message.senderId]?.profilePictureUrl else null,
                                        isSaved = savedCollections.contains(message.payload),
                                        localCollections = localCollections,
                                        onSaveCollection = { payload ->
                                            viewModel.addSharedCollection(payload)
                                            savedCollections += payload
                                        },
                                        onJoinSession = { payload -> viewModel.joinSession(payload, onJoinSession) },
                                        onReportMessage = {
                                            reportingMessage = message
                                        },
                                        onProfileClick = { _ -> state.chat?.chatId?.let { onHeaderClick(it) } },
                                        onDeleteChat = { 
                                            state.chat?.chatId?.let { chatId ->
                                                viewModel.deleteChat(chatId)
                                                onDeleteAndRestart()
                                            }
                                        },
                                        isAiLoading = isAiLoading && index == state.messages.size - 1 && message.senderId == ChatViewModel.QBASE_AI_BOT_ID,
                                        isFirstInGroup = isFirstInGroup,
                                        isLastInGroup = isLastInGroup
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scroll to Bottom FAB
            val showScrollToBottom by remember {
                derivedStateOf { listState.firstVisibleItemIndex > 5 }
            }
            if (showScrollToBottom) {
                SmallFloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (state.messages.isNotEmpty()) {
                                listState.animateScrollToItem(state.messages.size - 1)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Scroll to bottom")
                }
            }
        }
    }
    
    if (showSessionPicker) {
        val sessions by viewModel.allSessions.collectAsStateWithLifecycle()
        SessionPickerSheet(
            sessions = sessions,
            onDismiss = { showSessionPicker = false },
            onSessionSelected = { sessionId ->
                state.chat?.chatId?.let { viewModel.shareSession(it, sessionId) }
                showSessionPicker = false
            }
        )
    }
    
    if (showCollectionPicker) {
        CollectionPickerSheet(
            collections = collections,
            onDismiss = { showCollectionPicker = false },
            onCollectionSelected = { collectionId ->
                state.chat?.chatId?.let { viewModel.shareCollection(it, collectionId) }
                showCollectionPicker = false
            }
        )
    }

    ChatDetailConfirmDialogs(
        showClearConfirm = showClearConfirm,
        onClearDismiss = { showClearConfirm = false },
        onClearConfirm = {
            state.chat?.chatId?.let { viewModel.clearChatMessages(it) }
            showClearConfirm = false
        },
        showDeleteConfirm = showDeleteConfirm,
        onDeleteDismiss = { showDeleteConfirm = false },
        onDeleteConfirm = {
            state.chat?.chatId?.let { viewModel.deleteChat(it) }
            showDeleteConfirm = false
            onDeleteAndRestart()
        },
        reportingMessage = reportingMessage,
        onReportMessageDismiss = { reportingMessage = null },
        onReportMessageConfirm = { reason ->
            reportingMessage?.let { viewModel.reportMessage(it, reason) }
            reportingMessage = null
        },
        showReportGroupDialog = showReportGroupDialog,
        groupName = state.displayName,
        onReportGroupDismiss = { showReportGroupDialog = false },
        onReportGroupConfirm = { reason ->
            state.chat?.let { viewModel.reportGroup(it.chatId, reason) }
            showReportGroupDialog = false
        }
    )
}