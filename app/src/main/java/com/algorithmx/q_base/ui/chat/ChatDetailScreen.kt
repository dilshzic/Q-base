package com.algorithmx.q_base.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.data.chat.MessageEntity
import com.algorithmx.q_base.data.chat.isAdmin
import com.algorithmx.q_base.data.collections.StudyCollection
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Report
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.Event
import com.algorithmx.q_base.ui.chat.components.*
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.rounded.RocketLaunch
import com.algorithmx.q_base.ui.components.reusable.ReportDialog
import com.algorithmx.q_base.ui.components.reusable.ProfileIconButton
import com.algorithmx.q_base.ui.state.AppAccessState
import com.algorithmx.q_base.ui.state.LocalAppAccessState
import kotlinx.coroutines.launch
import java.util.*

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
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val localCollections by viewModel.localStudyCollections.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var savedCollections by remember { mutableStateOf(setOf<String>()) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    var showSessionPicker by remember { mutableStateOf(false) } // Added for Shared Sessions
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
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(TopAppBarDefaults.pinnedScrollBehavior().nestedScrollConnection),
        contentWindowInsets = WindowInsets.statusBars, // Handle top bars, let bottom handle itself
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { state.chat?.chatId?.let { onHeaderClick(it) } },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = if (state.chat?.isGroup == true) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (state.chat?.isGroup == true) Icons.Rounded.Group else Icons.Rounded.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (state.chat?.isGroup == true) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = state.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isAiLoading) "Typing..."
                                    else if (state.chat?.isGroup == true) "${state.participants.size} participants"
                                    else if (state.chat?.isBlocked == true) "Blocked" else "Active now",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (state.chat?.isBlocked == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${accessStateLabel(appAccessState)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (appAccessState) {
                                        AppAccessState.OnlineReady, AppAccessState.GuestOnline -> MaterialTheme.colorScheme.primary
                                        AppAccessState.RestoringSession -> MaterialTheme.colorScheme.tertiary
                                        AppAccessState.SignedInOffline, AppAccessState.OfflineGuest -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.chat?.isGroup == true) {
                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                            val isLibraryMode by viewModel.isLibraryMode.collectAsStateWithLifecycle()
                            IconButton(onClick = { viewModel.toggleLibraryMode(!isLibraryMode) }) {
                                Icon(
                                    if (isLibraryMode) Icons.Rounded.ChatBubble else Icons.Rounded.FolderZip,
                                    contentDescription = if (isLibraryMode) "Show Messages" else "Open Shared Library"
                                )
                            }
                        }
                    }

                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Details") },
                            onClick = {
                                state.chat?.chatId?.let { onHeaderClick(it) }
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Clear Chat History") },
                            onClick = {
                                showClearConfirm = true
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Report Group") },
                            onClick = {
                                showReportGroupDialog = true
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Rounded.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Chat") },
                            onClick = {
                                showDeleteConfirm = true
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) }
                        )
                    }
                    ProfileIconButton(
                        user = currentUser,
                        onClick = onProfileClick
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        bottomBar = {
            val isLibraryMode by viewModel.isLibraryMode.collectAsStateWithLifecycle()
            if (state.chat?.isBlocked == true) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "You have blocked this contact. Unblock to send messages.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            } else if (!isLibraryMode) {
                Surface(
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .imePadding() // Pushes bar above keyboard
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(
                                    onClick = { showCollectionPicker = true },
                                    enabled = canSend
                                ) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = "Attach",
                                        tint = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                TextField(
                                    value = messageText,
                                    onValueChange = { messageText = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Message...", style = MaterialTheme.typography.bodyMedium) },
                                    maxLines = 5,
                                    colors = TextFieldDefaults.colors(
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        disabledIndicatorColor = Color.Transparent,
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent
                                    ),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                                
                                AnimatedVisibility(visible = messageText.isEmpty()) {
                                    IconButton(
                                        onClick = { showSessionPicker = true },
                                        enabled = canSend
                                    ) {
                                        Icon(
                                            Icons.Rounded.RocketLaunch,
                                            contentDescription = "Session",
                                            tint = if (canSend) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.26f),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                        
                        val isNotEmpty = messageText.isNotBlank()
                        AnimatedVisibility(
                            visible = isNotEmpty,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            IconButton(
                                onClick = {
                                    state.chat?.chatId?.let { 
                                        viewModel.sendMessage(it, messageText)
                                        messageText = ""
                                    }
                                },
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Rounded.Send, 
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
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
            if (isLibraryMode && state.chat?.isGroup == true) {
                val accessRequests by viewModel.accessRequests.collectAsStateWithLifecycle()
                val isAdmin = state.chat?.isAdmin(state.currentUserId ?: "") == true
                
                val sharedSessions by viewModel.sharedSessions.collectAsStateWithLifecycle()
                
                SharedLibraryView(
                    chatId = state.chat!!.chatId,
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
                    contentPadding = PaddingValues(bottom = 16.dp, start = 8.dp, end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp) // Handled by bubble padding
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
