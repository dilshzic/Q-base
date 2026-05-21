package com.algorithmx.q_base.feature.chat.presentation.components
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.chat.ChatEntity
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.ui.components.reusable.ProfileIconButton
import com.algorithmx.q_base.ui.state.AppAccessState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DateHeader(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailTopBar(
    displayName: String,
    chat: ChatEntity?,
    participantsCount: Int,
    isAiLoading: Boolean,
    appAccessState: AppAccessState,
    currentUser: UserEntity?,
    isLibraryMode: Boolean,
    onBack: () -> Unit,
    onHeaderClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    onToggleLibraryMode: (Boolean) -> Unit,
    onClearHistoryClick: () -> Unit,
    onReportGroupClick: () -> Unit,
    onDeleteChatClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { chat?.chatId?.let { onHeaderClick(it) } },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = if (chat?.isGroup == true) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (chat?.isGroup == true) Icons.Rounded.Group else Icons.Rounded.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (chat?.isGroup == true) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isAiLoading) "Typing..."
                            else if (chat?.isGroup == true) "$participantsCount participants"
                            else if (chat?.isBlocked == true) "Blocked" else "Active now",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (chat?.isBlocked == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
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
            if (chat?.isGroup == true) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                    IconButton(onClick = { onToggleLibraryMode(!isLibraryMode) }) {
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
                        chat?.chatId?.let { onHeaderClick(it) }
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Rounded.Info, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Clear Chat History") },
                    onClick = {
                        onClearHistoryClick()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Report Group") },
                    onClick = {
                        onReportGroupClick()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Rounded.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
                DropdownMenuItem(
                    text = { Text("Delete Chat") },
                    onClick = {
                        onDeleteChatClick()
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
}

@Composable
fun ChatDetailBottomBar(
    chat: ChatEntity?,
    isLibraryMode: Boolean,
    canSend: Boolean,
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onAttachClick: () -> Unit,
    onSessionClick: () -> Unit,
    onSendClick: () -> Unit
) {
    if (chat?.isBlocked == true) {
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
                .imePadding()
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
                            onClick = onAttachClick,
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
                            onValueChange = onMessageTextChange,
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
                                onClick = onSessionClick,
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
                        onClick = onSendClick,
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
