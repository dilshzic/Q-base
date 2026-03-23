package com.algorithmx.q_base.ui.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.data.entity.MessageEntity
import com.algorithmx.q_base.data.entity.Collection as AppCollection
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Update
import com.algorithmx.q_base.ui.components.ReportDialog
import com.algorithmx.q_base.ui.components.ProfileIconButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import dev.jeziellago.compose.markdowntext.MarkdownText

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
    val totalUnreadCount by viewModel.totalUnreadCount.collectAsStateWithLifecycle()
    val localCollections by viewModel.localCollections.collectAsStateWithLifecycle()
    val isSharing by viewModel.isSharing.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedCollections = remember { mutableStateOf(setOf<String>()) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var reportingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var showReportGroupDialog by remember { mutableStateOf(false) }
    val collections by viewModel.allCollections.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.actionFeedback.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Expressive Scroll Behavior
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.nestedScroll(TopAppBarDefaults.pinnedScrollBehavior().nestedScrollConnection),
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
                            text = if (state.chat?.isGroup == true) "${state.participants.size} participants" 
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
            } else {
                Surface(
                    tonalElevation = 6.dp,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) // Expressive Shape
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .navigationBarsPadding()
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showCollectionPicker = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape),
                            enabled = !isSharing
                        ) {
                            if (isSharing) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Add, contentDescription = "Share Collection", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))

                        TextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(28.dp)),
                            placeholder = { Text("Message...", style = MaterialTheme.typography.bodyLarge) },
                            maxLines = 5,
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        val isNotEmpty = messageText.isNotBlank()
                        val buttonScale by animateFloatAsState(
                            targetValue = if (isNotEmpty) 1.1f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )

                        FilledIconButton(
                            onClick = {
                                if (isNotEmpty) {
                                    state.chat?.chatId?.let { 
                                        viewModel.sendMessage(it, messageText)
                                        messageText = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(52.dp)
                                .graphicsLayer {
                                    scaleX = buttonScale
                                    scaleY = buttonScale
                                },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = if (isNotEmpty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isNotEmpty) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send, 
                                contentDescription = "Send",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    )
 { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(state.messages) { index, message ->
                    if (message.type == "DB_CHANGE") {
                        SystemMessageItem(message.payload)
                    } else {
                        val isMine = message.senderId == state.currentUserId
                        val showSenderName = !isMine && (index == 0 || state.messages[index - 1].senderId != message.senderId)

                        AnimatedMessageItem(
                            message = message,
                            isMine = message.senderId == state.currentUserId,
                            senderName = state.participants[message.senderId]?.displayName,
                            isSaved = savedCollections.value.contains(message.payload),
                            localCollections = localCollections,
                            onSaveCollection = { payload ->
                                viewModel.addSharedCollection(payload)
                                savedCollections.value = savedCollections.value + payload
                            },
                            onJoinSession = { sessionId -> viewModel.shareSession(state.chat!!.chatId, sessionId) },
                            onReportMessage = {
                                reportingMessage = message
                            },
                            isAiLoading = isAiLoading && index == state.messages.size - 1 && message.senderId == ChatViewModel.QBASE_AI_BOT_ID
                        )
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
                        collections.forEach { collection: com.algorithmx.q_base.data.entity.Collection ->
                            item {
                                Surface(
                                    onClick = {
                                        state.chat?.chatId?.let { 
                                            viewModel.shareCollection(it, collection.collectionId)
                                        }
                                        // Assuming the user intended to close the picker after sharing
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
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.primary
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

@Composable
fun AnimatedMessageItem(
    message: MessageEntity,
    isMine: Boolean,
    senderName: String?,
    isSaved: Boolean,
    localCollections: List<com.algorithmx.q_base.data.entity.Collection>,
    onSaveCollection: (String) -> Unit,
    onJoinSession: (String) -> Unit,
    onReportMessage: () -> Unit,
    isAiLoading: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
 enter = slideInHorizontally(
            initialOffsetX = { if (isMine) it / 2 else -it / 2 },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(animationSpec = tween(300)),
    ) {
        MessageBubble(message, isMine, senderName, isSaved, localCollections, onSaveCollection, onJoinSession, onReportMessage, isAiLoading)
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isMine: Boolean,
    senderName: String?,
    isSaved: Boolean,
    localCollections: List<com.algorithmx.q_base.data.entity.Collection>,
    onSaveCollection: (String) -> Unit,
    onJoinSession: (String) -> Unit,
    onReportMessage: () -> Unit,
    isAiLoading: Boolean = false
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    val isAi = message.senderId == ChatViewModel.QBASE_AI_BOT_ID

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (isAi) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (isAiLoading) 1.3f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_scale"
                )
                
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp).graphicsLayer(scaleX = scale, scaleY = scale),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Qbase AI",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            senderName?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp, bottom = 4.dp, end = 12.dp)
                )
            }
        }
        
        Surface(
            color = when {
                isMine -> MaterialTheme.colorScheme.primary
                isAi -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = when {
                isMine -> MaterialTheme.colorScheme.onPrimary
                isAi -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSecondaryContainer
            },
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isMine) 20.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 20.dp
            ),
            tonalElevation = if (isMine) 2.dp else 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                var showDropdown by remember { mutableStateOf(false) }
                Box(modifier = Modifier.clickable { showDropdown = true }) {
                    if (message.type == "COLLECTION" || message.type == "SHARED_COLLECTION") {
                        CollectionBubbleContent(message.payload, isSaved, onSaveCollection, isMine)
                    } else if (message.type == "SHARED_SESSION" || message.type == "SESSION_INVITE") {
                        SessionBubbleContent(message.payload, onJoinSession, isMine)
                    } else if (message.type == "FILE_TRANSFER") {
                        FileTransferBubbleContent(message.payload, localCollections, onSaveCollection, isMine)
                    } else {
                        if (message.decryptionStatus == "FAILED" || message.decryptionStatus == "DECRYPTION_ERROR") {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Icon(
                                    if (message.decryptionStatus == "FAILED") Icons.Default.Lock else Icons.Default.Sync,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (message.decryptionStatus) {
                                        "FAILED" -> "Message from previous session unavailable"
                                        "DECRYPTION_ERROR" -> "Waiting for secure session keys..."
                                        else -> "Encountered decryption error"
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) 
                                                else MaterialTheme.colorScheme.error
                                    )
                                )
                            }
                        } else if (isAi) {
                            MarkdownText(
                                markdown = message.payload,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.padding(2.dp)
                            )
                        } else {
                            Text(
                                text = message.payload,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    lineHeight = 22.sp,
                                    color = if (isMine) MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                                ),
                                modifier = Modifier.padding(2.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        if (!isMine && !isAi) {
                            DropdownMenuItem(
                                text = { Text("Report Message") },
                                leadingIcon = { Icon(Icons.Rounded.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showDropdown = false
                                    onReportMessage()
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Copy Text") },
                            leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                            onClick = { showDropdown = false } // Mock copy
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End),
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) 
                            else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun CollectionBubbleContent(payload: String, isSaved: Boolean, onSave: (String) -> Unit, isMine: Boolean) {
    val json = remember { kotlinx.serialization.json.Json { ignoreUnknownKeys = true } }
    val collection = remember(payload) {
        try {
            json.decodeFromString<com.algorithmx.q_base.brain.models.AiCollectionResponse>(payload)
        } catch (e: Exception) {
            null
        }
    }

    if (collection != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.CollectionsBookmark,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = collection.collectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = collection.collectionDescription,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { if (!isSaved) onSave(payload) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaved,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = when {
                    isSaved -> ButtonDefaults.buttonColors(
                        containerColor = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) 
                                       else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                        contentColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
                    )
                    isMine -> ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onPrimary,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                    else -> ButtonDefaults.buttonColors()
                }
            ) {
                Icon(
                    if (isSaved) Icons.Rounded.CheckCircle else Icons.Default.Add, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (isSaved) "Saved to Collections" else "Save to My Collections", 
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    } else {
        Text("Invalid Collection Data", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun SessionBubbleContent(payload: String, onJoin: (String) -> Unit, isMine: Boolean) {
    // Payload format: sessionId|sessionTitle
    val parts = payload.split("|")
    val sessionId = parts.getOrNull(0) ?: ""
    val sessionTitle = parts.getOrNull(1) ?: "Active Session"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.RocketLaunch,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = sessionTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "Join this collaborative study session to track progress together.",
            style = MaterialTheme.typography.bodySmall,
            color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(
            onClick = { onJoin(sessionId) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary,
                contentColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Join Session", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun FileTransferBubbleContent(
    payload: String, 
    localCollections: List<com.algorithmx.q_base.data.entity.Collection>,
    onImport: (String) -> Unit,
    isMine: Boolean
) {
    // Payload format: url|E2EE_KEY|key|UPDATED_AT|timestamp|COLLECTION_ID|id
    val parts = payload.split("|")
    val updatedAtRemote = parts.getOrNull(4)?.toLongOrNull() ?: 0L
    val collectionIdMetadata = parts.getOrNull(6)
    
    // Use explicit id from metadata if available, otherwise fallback to URL (backward compatibility)
    val url = parts.getOrNull(0) ?: ""
    val collectionId = collectionIdMetadata ?: url.substringAfter("/files/").substringBefore("/download")
    
    val localCopy = localCollections.find { it.collectionId == collectionId }
    
    val buttonState = when {
        localCopy == null -> "IMPORT"
        updatedAtRemote > localCopy.updatedAt -> "UPDATE"
        else -> "UP_TO_DATE"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.FolderZip,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = localCopy?.name ?: "Collection Shared",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = when(buttonState) {
                "IMPORT" -> "A question collection has been shared with you. Tap below to import it."
                "UPDATE" -> "A newer version of this collection is available. Tap below to update."
                else -> "You have the latest version of this collection in your library."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = { if (buttonState != "UP_TO_DATE") onImport(payload) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            enabled = buttonState != "UP_TO_DATE",
            colors = when {
                buttonState == "IMPORT" -> ButtonDefaults.buttonColors(
                    containerColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primaryContainer, 
                    contentColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                )
                buttonState == "UPDATE" -> ButtonDefaults.buttonColors(
                    containerColor = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f) else MaterialTheme.colorScheme.tertiaryContainer, 
                    contentColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onTertiaryContainer
                )
                else -> ButtonDefaults.buttonColors(
                    containerColor = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), 
                    contentColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
                )
            }
        ) {
            Icon(
                when(buttonState) {
                    "IMPORT" -> Icons.Rounded.CloudDownload
                    "UPDATE" -> Icons.Rounded.Update
                    else -> Icons.Rounded.CheckCircle
                }, 
                contentDescription = null, 
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when(buttonState) {
                    "IMPORT" -> "Import to Library"
                    "UPDATE" -> "Update Collection"
                    else -> "You have updated collection"
                }, 
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
fun SystemMessageItem(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = CircleShape
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
