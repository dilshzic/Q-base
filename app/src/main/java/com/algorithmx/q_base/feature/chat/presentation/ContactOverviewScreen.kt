package com.algorithmx.q_base.feature.chat.presentation
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

import androidx.compose.foundation.clickable
import com.algorithmx.q_base.core.data.UserEntity
import com.algorithmx.q_base.core.data.chat.ChatEntity
import com.algorithmx.q_base.core.designsystem.components.reusable.ProfileIconButton
import com.algorithmx.q_base.core.designsystem.components.reusable.ReportDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactOverviewScreen(
    chatId: String,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val state by viewModel.chatDetailState.collectAsState()
    
    var fallbackChat by remember { mutableStateOf<ChatEntity?>(null) }
    LaunchedEffect(chatId) {
        if (state.chat == null) {
            fallbackChat = viewModel.chatLocalDataSource.getChatById(chatId)
        }
    }
    
    val chat = state.chat ?: fallbackChat
    val otherUser = if (chat?.isGroup == false) {
        val otherId = chat.participantIds.split(",").firstOrNull { it != state.currentUserId }
        state.participants[otherId]
    } else null

    var showReportDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.actionFeedback.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            com.algorithmx.q_base.core.designsystem.components.reusable.UnifiedTopAppBar(
                title = "Contact Info",
                currentUser = currentUser,
                onProfileClick = onProfileClick,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                // Profile Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Vibrant Gradient Background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(120.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = otherUser?.displayName ?: chat?.chatName ?: "User",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                    }
                }
            }

            item {
                // Main Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ActionButton(
                        icon = Icons.AutoMirrored.Rounded.Chat,
                        label = "Message",
                        onClick = onBack
                    )
                    ActionButton(
                        icon = if (chat?.isMuted == true) Icons.Rounded.NotificationsOff else Icons.Rounded.Notifications,
                        label = if (chat?.isMuted == true) "Unmute" else "Mute",
                        onClick = { viewModel.toggleMute(chatId, !(chat?.isMuted ?: false)) }
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                // Settings List
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsItem(
                        icon = Icons.Rounded.Block,
                        title = if (chat?.isBlocked == true) "Unblock User" else "Block User",
                        subtitle = "Stop receiving messages from this person",
                        isDestructive = true,
                        onClick = { viewModel.toggleBlock(chatId, !(chat?.isBlocked ?: false)) }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.Report,
                        title = "Report User",
                        subtitle = "Report suspicious or abusive behavior",
                        isDestructive = true,
                        onClick = { showReportDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "Clear Chat History",
                        subtitle = "Delete all messages in this chat",
                        onClick = { viewModel.clearChatMessages(chatId) }
                    )
                }
            }
        }
    }

    if (showReportDialog) {
        ReportDialog(
            itemType = "User",
            itemName = otherUser?.displayName ?: chat?.chatName ?: "User",
            onDismiss = { showReportDialog = false },
            onConfirm = { reason ->
                otherUser?.let { viewModel.reportUser(it.userId, reason) }
                showReportDialog = false
            }
        )
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { 
            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
            onClick() 
        }
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Icon(icon, contentDescription = null)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
