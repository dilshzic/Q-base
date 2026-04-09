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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
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
import com.algorithmx.q_base.ui.components.ReportDialog
import com.algorithmx.q_base.ui.components.ProfileIconButton
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    onBack: () -> Unit,
    onHeaderClick: (String) -> Unit,
    onProfileClick: () -> Unit,
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { state.chat?.chatId?.let { onHeaderClick(it) } }
                    ) {
                        Text(
                            text = state.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAiLoading) "Typing..."
                                   else if (state.chat?.isGroup == true) "${state.participants.size} participants" 
                                   else if (state.chat?.isBlocked == true) "Blocked" else "Active now",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.chat?.isBlocked == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
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
                                IconButton(onClick = { showCollectionPicker = true }) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Attach",
                                        tint = MaterialTheme.colorScheme.primary,
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
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                                
                                AnimatedVisibility(visible = messageText.isEmpty()) {
                                    IconButton(onClick = { showSessionPicker = true }) {
                                        Icon(
                                            Icons.Rounded.RocketLaunch,
                                            contentDescription = "Session",
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
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
                                    Icons.AutoMirrored.Filled.Send, 
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
                val isAdmin = state.chat?.adminId == state.currentUserId
                
                val sharedSessions by viewModel.sharedSessions.collectAsStateWithLifecycle()
                
                SharedLibraryView(
                    collections = sharedCollections,
                    sessions = sharedSessions,
                    onImport = { payload -> viewModel.importSharedCollection(payload) },
                    onJoinSession = { sessionId -> viewModel.shareSession(state.chat!!.chatId, sessionId) },
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
                                        onJoinSession = { sessionId -> viewModel.shareSession(state.chat!!.chatId, sessionId) },
                                        onReportMessage = {
                                            reportingMessage = message
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
        ModalBottomSheet(
            onDismissRequest = { showSessionPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Share Session",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(24.dp)
                )

                if (sessions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No sessions to share", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        sessions.forEach { session ->
                            item {
                                Surface(
                                    onClick = {
                                        state.chat?.chatId?.let { 
                                            viewModel.shareSession(it, session.sessionId)
                                        }
                                        showSessionPicker = false
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(session.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("Active Session", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showCollectionPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCollectionPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Share Collection",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(24.dp)
                )

                if (collections.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No collections to share", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        collections.forEach { collection: StudyCollection ->
                            item {
                                Surface(
                                    onClick = {
                                        state.chat?.chatId?.let { 
                                            viewModel.shareCollection(it, collection.collectionId)
                                        }
                                        showCollectionPicker = false
                                    },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.Transparent
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.CollectionsBookmark, 
                                            tint = MaterialTheme.colorScheme.primary,
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(collection.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            collection.description?.let { desc: String -> 
                                                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1) 
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Chat History?") },
            text = { Text("This will permanently remove all messages from this chat for all participants.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.chat?.chatId?.let { viewModel.clearChatMessages(it) }
                        showClearConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Chat?") },
            text = { Text("This will permanently delete this chat and its history for everyone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.chat?.chatId?.let { viewModel.deleteChat(it) }
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    reportingMessage?.let { message ->
        ReportDialog(
            itemType = "Message",
            itemName = message.payload.take(20) + if(message.payload.length > 20) "..." else "",
            onDismiss = { reportingMessage = null },
            onConfirm = { reason ->
                viewModel.reportMessage(message, reason)
                reportingMessage = null
            }
        )
    }

    if (showReportGroupDialog) {
        state.chat?.let { chat ->
            ReportDialog(
                itemType = "Group",
                itemName = state.displayName,
                onDismiss = { showReportGroupDialog = false },
                onConfirm = { reason ->
                    viewModel.reportGroup(chat.chatId, reason)
                    showReportGroupDialog = false
                }
            )
        }
    }
}

private fun formatDateRelatively(timestamp: Long): String {
    val now = Calendar.getInstance()
    val messageDate = Calendar.getInstance().apply { timeInMillis = timestamp }
    
    return when {
        isSameDay(now, messageDate) -> "Today"
        isYesterday(now, messageDate) -> "Yesterday"
        else -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isYesterday(now: Calendar, date: Calendar): Boolean {
    val yesterday = Calendar.getInstance().apply { 
        timeInMillis = now.timeInMillis
        add(Calendar.DAY_OF_YEAR, -1)
    }
    return isSameDay(yesterday, date)
}
