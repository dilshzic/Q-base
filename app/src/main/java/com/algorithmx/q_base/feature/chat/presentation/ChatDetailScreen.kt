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
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    onBack: () -> Unit,
    onHeaderClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onJoinSession: (String) -> Unit,
    onNavigateToCollection: (String) -> Unit,
    onDeleteAndRestart: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.chatDetailState.collectAsState()
    val chat = state.chat
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val localCollections by viewModel.localStudyCollections.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val accessRequests by viewModel.accessRequests.collectAsStateWithLifecycle()
    val groupReports by viewModel.groupReports.collectAsStateWithLifecycle()
    val messageReports by viewModel.messageReports.collectAsStateWithLifecycle()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var savedCollections by remember { mutableStateOf(setOf<String>()) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var reportingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var showReportGroupDialog by remember { mutableStateOf(false) }
    var pendingShareCollectionId by remember { mutableStateOf<String?>(null) }
    var shareAsAdminOnly by remember { mutableStateOf(false) }
    val collections by viewModel.allStudyCollections.collectAsStateWithLifecycle()
    val appAccessState = LocalAppAccessState.current
    val canSend by remember(state.chat?.chatId) {
        state.chat?.chatId?.let { viewModel.canSendToChat(it) } ?: kotlinx.coroutines.flow.flowOf(false)
    }.collectAsState(initial = false)

    val currentChatId by viewModel.currentChatId.collectAsStateWithLifecycle()
    val isLibraryMode by viewModel.isLibraryMode.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(isLibraryMode) {
        if (!isLibraryMode && selectedTab != 0) {
            selectedTab = 0
        } else if (isLibraryMode && selectedTab == 0) {
            selectedTab = 1
        }
    }
    
    BackHandler(enabled = isLibraryMode) {
        viewModel.toggleLibraryMode(false)
    }
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
            Column {
                ChatDetailTopBar(
                    displayName = state.displayName,
                    chat = state.chat,
                    participantsCount = state.chat?.participantIds?.split(",")?.size ?: 0,
                    isAiLoading = isAiLoading,
                    appAccessState = appAccessState,
                    currentUser = currentUser,
                    isLibraryMode = isLibraryMode,
                    onBack = {
                        if (isLibraryMode) {
                            viewModel.toggleLibraryMode(false)
                        } else {
                            onBack()
                        }
                    },
                    onHeaderClick = onHeaderClick,
                    onProfileClick = onProfileClick,
                    onToggleLibraryMode = { viewModel.toggleLibraryMode(it) },
                    onClearHistoryClick = { showClearConfirm = true },
                    onReportGroupClick = { showReportGroupDialog = true },
                    onDeleteChatClick = { showDeleteConfirm = true }
                )
                if (chat?.isGroup == true) {
                    val isAdmin = chat.isAdmin(state.currentUserId)
                    val badgeCount = accessRequests.size + groupReports.size + messageReports.size
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                viewModel.toggleLibraryMode(false)
                            },
                            text = { Text("Chat") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                viewModel.toggleLibraryMode(true)
                            },
                            text = { Text("Shared collections") }
                        )
                        if (isAdmin) {
                            Tab(
                                selected = selectedTab == 2,
                                onClick = {
                                    selectedTab = 2
                                    viewModel.toggleLibraryMode(true)
                                },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Admin")
                                        if (badgeCount > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Badge(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            ) {
                                                Text(text = badgeCount.toString(), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            ChatDetailBottomBar(
                chat = state.chat,
                isLibraryMode = isLibraryMode,
                canSend = canSend,
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onAttachClick = { showCollectionPicker = true },
                onSendClick = {
                    state.chat?.chatId?.let { 
                        viewModel.sendMessage(it, messageText)
                        messageText = ""
                    }
                }
            )
        }
    ) { padding ->
        val sharedCollections by viewModel.sharedCollections.collectAsStateWithLifecycle()
        val isLoadingCollections by viewModel.isLoadingCollections.collectAsStateWithLifecycle()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selectedTab == 2 && chat?.isGroup == true) {
                AdminReviewPanelView(
                    accessRequests = accessRequests,
                    groupReports = groupReports,
                    messageReports = messageReports,
                    onGrantAccess = { collId, reqId -> viewModel.grantAccess(collId, reqId) },
                    onDenyAccess = { collId, reqId -> viewModel.denyAccess(collId, reqId) },
                    onDismissGroupReport = { reportId -> viewModel.dismissGroupReport(reportId) },
                    onDismissMessageReport = { reportId -> viewModel.dismissMessageReport(reportId) },
                    onDeleteMessage = { msgId, reportId -> viewModel.deleteReportedMessage(msgId, reportId) }
                )
            } else if (selectedTab == 1 && chat?.isGroup == true) {
                val isAdmin = chat.isAdmin(state.currentUserId)
                SharedLibraryView(
                    chatId = chat.chatId,
                    collections = sharedCollections,
                    onImport = { payload -> viewModel.importSharedCollection(payload) },
                    onResend = { collectionId -> viewModel.resendCollection(collectionId) },
                    localCollections = localCollections,
                    isAdmin = isAdmin,
                    accessRequests = accessRequests,
                    onRequestAccess = { viewModel.requestAccess(it) },
                    onGrantAccess = { collId, reqId -> viewModel.grantAccess(collId, reqId) },
                    onDenyAccess = { collId, reqId -> viewModel.denyAccess(collId, reqId) },
                    onNavigateToCollection = onNavigateToCollection,
                    onDelete = { collectionId -> viewModel.deleteSharedCollection(collectionId) },
                    currentUserId = state.currentUserId,
                    isLoading = isLoadingCollections
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

    if (showCollectionPicker) {
        CollectionPickerSheet(
            collections = collections,
            onDismiss = { showCollectionPicker = false },
            onCollectionSelected = { collectionId ->
                pendingShareCollectionId = collectionId
                val selectedCollection = collections.find { it.collectionId == collectionId }
                shareAsAdminOnly = selectedCollection?.isAdminOnly == true
                showCollectionPicker = false
            }
        )
    }

    if (pendingShareCollectionId != null) {
        val selectedCollection = collections.find { it.collectionId == pendingShareCollectionId }
        AlertDialog(
            onDismissRequest = { pendingShareCollectionId = null },
            title = { Text("Share Collection") },
            text = {
                Column {
                    val warningText = if (state.chat?.isGroup == true) {
                        "This will be converted into a Shared collection and sent to this chat."
                    } else {
                        "This collection will be securely sent to this chat."
                    }
                    Text(
                        warningText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (state.chat?.isGroup == true) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = shareAsAdminOnly,
                                onCheckedChange = { shareAsAdminOnly = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Make Admin-Only", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val id = pendingShareCollectionId!!
                    state.chat?.chatId?.let { 
                        if (state.chat?.isGroup == true && selectedCollection?.isAdminOnly != shareAsAdminOnly) {
                            viewModel.updateCollectionAdminStatus(id, shareAsAdminOnly)
                        }
                        viewModel.shareCollection(it, id) 
                    }
                    pendingShareCollectionId = null
                }) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingShareCollectionId = null }) {
                    Text("Cancel")
                }
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