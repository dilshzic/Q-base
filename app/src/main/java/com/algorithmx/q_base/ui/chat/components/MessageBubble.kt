package com.algorithmx.q_base.ui.chat.components

import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import android.content.ClipData
import kotlinx.coroutines.launch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.algorithmx.q_base.R
import com.algorithmx.q_base.data.chat.MessageEntity
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.ui.chat.ChatViewModel
import dev.jeziellago.compose.markdowntext.MarkdownText
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
            onReportMessage, isAiLoading, isFirstInGroup, isLastInGroup
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
    isAiLoading: Boolean = false,
    isFirstInGroup: Boolean = true,
    isLastInGroup: Boolean = true
) {
    val clipboard = LocalClipboard.current
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
                SenderAvatar(avatarUrl)
            } else if (!isMine) {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                if (isFirstInGroup && !isMine && senderName != null) {
                    SenderNameLabel(senderName)
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
                    MessageContent(message, isMine, isAi, isSaved, onSaveCollection, onJoinSession, localCollections, onReportMessage, context)
                }
                
                if (isLastInGroup) {
                    MessageTimestampAndStatus(timeString, isMine, message.status)
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
private fun SenderAvatar(avatarUrl: String?) {
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
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .padding(start = 8.dp, bottom = 4.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
    Spacer(modifier = Modifier.width(8.dp))
}

@Composable
private fun SenderNameLabel(name: String) {
    Text(
        text = name,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
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
    context: Context
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
                        DecryptionErrorContent(message.decryptionStatus, isMine)
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
                        clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("Chat Message", message.payload)))
                    }
                    showDropdown = false 
                }
            )
        }
    }
}

@Composable
private fun DecryptionErrorContent(status: String, isMine: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(2.dp)) {
        Icon(
            if (status == "FAILED") Icons.Default.Lock else Icons.Default.Sync,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = when (status) {
                "FAILED" -> "Encrypted for a different session/key. If you just joined, you cannot see previous history."
                "DECRYPTION_ERROR" -> "Decryption failed. Secure session keys missing or invalid."
                else -> "Encountered decryption error"
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) 
                        else MaterialTheme.colorScheme.error
            )
        )
    }
}

@Composable
private fun MessageTimestampAndStatus(timeString: String, isMine: Boolean, status: String) {
    Spacer(modifier = Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 4.dp)) {
        Text(
            text = timeString,
            style = MaterialTheme.typography.labelSmall,
            color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) 
                    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
        )
        if (isMine) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = if (status == "READ" || status == "DELIVERED") Icons.Rounded.DoneAll else Icons.Rounded.Done,
                contentDescription = status,
                modifier = Modifier.size(12.dp),
                tint = if (status == "READ") Color(0xFF00E676) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun CollectionBubbleContent(payload: String, isSaved: Boolean, onSave: (String) -> Unit, isMine: Boolean) {
    val json = remember { kotlinx.serialization.json.Json { ignoreUnknownKeys = true } }
    val collection = remember(payload) {
        try {
            json.decodeFromString<com.algorithmx.q_base.core_ai.brain.models.AiCollectionResponse>(payload)
        } catch (e: Exception) { null }
    }

    if (collection != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CollectionsBookmark, null, Modifier.size(20.dp), if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(collection.collectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(collection.collectionDescription, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            Button(
                onClick = { if (!isSaved) onSave(payload) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaved,
                colors = if (isSaved) ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors()
            ) {
                Icon(if (isSaved) Icons.Rounded.CheckCircle else Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isSaved) "Saved" else "Save")
            }
        }
    }
}

@Composable
fun SessionBubbleContent(payload: String, onJoin: (String) -> Unit, isMine: Boolean) {
    val parts = payload.split("|")
    val sessionId = parts.getOrNull(0) ?: ""
    val sessionTitle = parts.getOrNull(1) ?: "Active Session"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.RocketLaunch, null, Modifier.size(20.dp), if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(sessionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text("Join this collaborative study session to track progress together.", style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = { onJoin(sessionId) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Join Session")
        }
    }
}

@Composable
fun FileTransferBubbleContent(payload: String, localCollections: List<StudyCollection>, onImport: (String) -> Unit, isMine: Boolean) {
    val parts = payload.split("|")
    val updatedAtRemote = parts.getOrNull(4)?.toLongOrNull() ?: 0L
    val collectionIdMetadata = parts.getOrNull(6)
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
            Icon(Icons.Rounded.FolderZip, null, Modifier.size(20.dp), if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(localCopy?.name ?: "Collection Shared", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            text = when(buttonState) {
                "IMPORT" -> "A question collection has been shared with you. Tap below to import it."
                "UPDATE" -> "A newer version of this collection is available. Tap below to update."
                else -> "You have the latest version of this collection in your library."
            },
            style = MaterialTheme.typography.bodySmall
        )
        Button(
            onClick = { if (buttonState != "UP_TO_DATE") onImport(payload) },
            modifier = Modifier.fillMaxWidth(),
            enabled = buttonState != "UP_TO_DATE"
        ) {
            Icon(when(buttonState) { "IMPORT" -> Icons.Rounded.CloudDownload; "UPDATE" -> Icons.Rounded.Update; else -> Icons.Rounded.CheckCircle }, null, Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(when(buttonState) { "IMPORT" -> "Import"; "UPDATE" -> "Update"; else -> "Up to date" })
        }
    }
}
