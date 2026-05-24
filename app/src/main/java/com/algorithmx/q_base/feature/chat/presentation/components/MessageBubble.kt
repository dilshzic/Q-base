package com.algorithmx.q_base.feature.chat.presentation.components
import android.content.ClipData
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.algorithmx.q_base.core.data.chat.MessageEntity
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.feature.chat.presentation.ChatViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AnimatedMessageItem(
    message: MessageEntity,
    isMine: Boolean,
    senderName: String?,
    showAvatar: Boolean,
    avatarUrl: String?,
    isSaved: Boolean,
    localCollections: List<StudyCollection>,
    onSaveCollection: (String) -> Unit,
    onJoinSession: (String) -> Unit,
    onReportMessage: () -> Unit,
    onProfileClick: (String) -> Unit,
    onDeleteChat: () -> Unit,
    isAiLoading: Boolean = false,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true
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
        MessageBubble(
            message, isMine, senderName, showAvatar, avatarUrl, 
            isSaved, localCollections, onSaveCollection, onJoinSession, 
            onReportMessage, onProfileClick, onDeleteChat, isAiLoading, isFirstInGroup, isLastInGroup
        )
    }
}

@Composable
fun MessageBubble(
    message: MessageEntity,
    isMine: Boolean,
    senderName: String?,
    showAvatar: Boolean,
    avatarUrl: String?,
    isSaved: Boolean,
    localCollections: List<StudyCollection>,
    onSaveCollection: (String) -> Unit,
    onJoinSession: (String) -> Unit,
    onReportMessage: () -> Unit,
    onProfileClick: (String) -> Unit,
    onDeleteChat: () -> Unit,
    isAiLoading: Boolean = false,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true
) {
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }
    val isAi = message.senderId == ChatViewModel.QBASE_AI_BOT_ID

    val bubbleShape = if (isMine) {
        RoundedCornerShape(
            topStart = 20.dp,
            topEnd = if (isFirstInGroup) 20.dp else 4.dp,
            bottomStart = 20.dp,
            bottomEnd = if (isLastInGroup) 4.dp else 4.dp
        ).let {
            if (isLastInGroup) it.copy(bottomEnd = CornerSize(20.dp)) else it
        }
    } else {
        RoundedCornerShape(
            topStart = if (isFirstInGroup) 20.dp else 4.dp,
            topEnd = 20.dp,
            bottomStart = if (isLastInGroup) 20.dp else 4.dp,
            bottomEnd = 20.dp
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(
            top = if (isFirstInGroup) 8.dp else 2.dp,
            bottom = if (isLastInGroup) 8.dp else 2.dp
        ),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        if (isAi && isFirstInGroup) {
            AIHeader(isAiLoading)
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (showAvatar && !isMine && isLastInGroup) {
                SenderAvatar(avatarUrl, onClick = { onProfileClick(message.senderId) })
            } else if (!isMine) {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                if (isFirstInGroup && !isMine && senderName != null) {
                    SenderNameLabel(senderName, onClick = { onProfileClick(message.senderId) })
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
                    shape = bubbleShape,
                    modifier = Modifier.widthIn(max = 280.dp)
                ) {
                    Column {
                        MessageContent(message, isMine, isAi, isSaved, onSaveCollection, onJoinSession, localCollections, onReportMessage, onDeleteChat, clipboard, scope)
                        if (isLastInGroup) {
                            MessageTimestampAndStatus(timeString, isMine, message.status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AIHeader(isAiLoading: Boolean) {
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
}

@Composable
private fun SenderAvatar(avatarUrl: String?, onClick: () -> Unit) {
    Box(modifier = Modifier.clickable { onClick() }) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 4.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 4.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
    Spacer(modifier = Modifier.width(8.dp))
}

@Composable
private fun SenderNameLabel(name: String, onClick: () -> Unit) {
    Text(
        text = name,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(start = 4.dp, bottom = 4.dp)
            .clickable { onClick() }
    )
}

@Composable
private fun MessageContent(
    message: MessageEntity,
    isMine: Boolean,
    isAi: Boolean,
    isSaved: Boolean,
    onSaveCollection: (String) -> Unit,
    onJoinSession: (String) -> Unit,
    localCollections: List<StudyCollection>,
    onReportMessage: () -> Unit,
    onDeleteChat: () -> Unit,
    clipboard: ClipboardManager,
    scope: CoroutineScope
) {
    var showDropdown by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Box(modifier = Modifier.clickable { showDropdown = true }) {
            when (message.type) {
                "COLLECTION", "SHARED_COLLECTION" -> CollectionBubbleContent(message.payload, isSaved, onSaveCollection, isMine)
                "SHARED_SESSION", "SESSION_INVITE" -> SessionBubbleContent(message.payload, onJoinSession, isMine)
                "FILE_TRANSFER" -> FileTransferBubbleContent(message.payload, localCollections, onSaveCollection, isMine)
                else -> {
                    if (message.decryptionStatus == "FAILED" || message.decryptionStatus == "DECRYPTION_ERROR") {
                        DecryptionErrorContent(message.decryptionStatus, isMine, onDeleteChat)
                    } else {
                        MarkdownText(
                            markdown = message.payload,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                lineHeight = 20.sp,
                                color = if (isMine) MaterialTheme.colorScheme.onPrimary 
                                        else if (isAi) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.padding(2.dp)
                        )
                    }
                }
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
                    onClick = {
                        scope.launch {
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(message.payload))
                        }
                        showDropdown = false
                    }
            )
        }
    }
}



@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun MessageBubblePreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Incoming Message
            MessageBubble(
                message = MessageEntity(
                    messageId = "1",
                    chatId = "chat1",
                    senderId = "other_user",
                    payload = "Hello! Have you checked the latest knowledge collection?",
                    timestamp = System.currentTimeMillis(),
                    status = "READ",
                    type = "TEXT"
                ),
                isMine = false,
                senderName = "Prof. Anderson",
                showAvatar = true,
                avatarUrl = null,
                isSaved = false,
                localCollections = emptyList(),
                onSaveCollection = {},
                onJoinSession = {},
                onReportMessage = {},
                onProfileClick = {},
                onDeleteChat = {}
            )

            // Outgoing Message
            MessageBubble(
                message = MessageEntity(
                    messageId = "2",
                    chatId = "chat1",
                    senderId = "my_user",
                    payload = "Yes, I'm reviewing it right now. Looks very comprehensive!",
                    timestamp = System.currentTimeMillis(),
                    status = "READ",
                    type = "TEXT"
                ),
                isMine = true,
                senderName = "Me",
                showAvatar = false,
                avatarUrl = null,
                isSaved = false,
                localCollections = emptyList(),
                onSaveCollection = {},
                onJoinSession = {},
                onReportMessage = {},
                onProfileClick = {},
                onDeleteChat = {}
            )

            // AI Message
            MessageBubble(
                message = MessageEntity(
                    messageId = "3",
                    chatId = "chat1",
                    senderId = ChatViewModel.QBASE_AI_BOT_ID,
                    payload = "I can help you analyze the specific questions in this collection. Would you like to start a practice session?",
                    timestamp = System.currentTimeMillis(),
                    status = "DELIVERED",
                    type = "TEXT"
                ),
                isMine = false,
                senderName = "Qbase AI",
                showAvatar = false,
                avatarUrl = null,
                isSaved = false,
                localCollections = emptyList(),
                onSaveCollection = {},
                onJoinSession = {},
                onReportMessage = {},
                onProfileClick = {},
                onDeleteChat = {},
                isAiLoading = false
            )
        }
    }
}