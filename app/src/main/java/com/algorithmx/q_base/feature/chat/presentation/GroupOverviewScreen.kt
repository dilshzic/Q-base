package com.algorithmx.q_base.feature.chat.presentation
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.data.chat.isAdmin
import com.algorithmx.q_base.ui.components.reusable.UnifiedTopAppBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupOverviewScreen(
    chatId: String,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val state by viewModel.chatDetailState.collectAsState()
    val chat = state.chat
    val participants = chat?.participantIds?.split(",")?.mapNotNull { state.participants[it] } ?: emptyList()

    var showReportDialog by remember { mutableStateOf(false) }
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    
    val allUsers by viewModel.allUsers.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.actionFeedback.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            UnifiedTopAppBar(
                title = "Group Info",
                currentUser = currentUser,
                onProfileClick = onProfileClick,
                isLarge = false,
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
                // Group Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(120.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 4.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Rounded.Group,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = chat?.chatName ?: "Group Chat",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${participants.size} Participants",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            item {
                // Group Actions Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val isAdmin = chat?.isAdmin(viewModel.currentUserId ?: "") == true
                    
                    ActionButton(
                        icon = Icons.Rounded.PersonAdd,
                        label = "Add",
                        onClick = { 
                            if (isAdmin) showAddDialog = true 
                            else coroutineScope.launch { snackbarHostState.showSnackbar("Only admins can add members") }
                        }
                    )
                    ActionButton(
                        icon = if (chat?.isMuted == true) Icons.Rounded.NotificationsOff else Icons.Rounded.Notifications,
                        label = if (chat?.isMuted == true) "Unmute" else "Mute",
                        onClick = { chat?.let { viewModel.toggleMute(it.chatId, !it.isMuted) } }
                    )
                    ActionButton(
                        icon = Icons.Rounded.Edit,
                        label = "Edit",
                        onClick = { 
                            if (isAdmin) { /* Edit logic */ } 
                            else coroutineScope.launch { snackbarHostState.showSnackbar("Only admins can edit group info") }
                        }
                    )
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Text(
                    text = "Participants",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }

            items(participants) { participant ->
                val isAdminOfGroup = chat?.isAdmin(participant.userId) == true
                val isCurrentUserAdmin = chat?.isAdmin(viewModel.currentUserId ?: "") == true
                val isSelf = participant.userId == viewModel.currentUserId
                
                var showActionDialog by remember { mutableStateOf(false) }
                
                ParticipantItem(
                    user = participant,
                    isAdmin = isAdminOfGroup,
                    onClick = {
                        if (isCurrentUserAdmin && !isSelf) {
                            showActionDialog = true
                        }
                    }
                )
                
                if (showActionDialog) {
                    AlertDialog(
                        onDismissRequest = { showActionDialog = false },
                        title = { Text(text = participant.displayName, fontWeight = FontWeight.Bold) },
                        text = { Text("Select an administrative action for this participant.") },
                        confirmButton = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                if (isAdminOfGroup) {
                                    Button(
                                        onClick = {
                                            viewModel.demoteAdmin(chatId, participant.userId)
                                            showActionDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Demote from Admin")
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            viewModel.promoteParticipantToAdmin(chatId, participant.userId)
                                            showActionDialog = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Promote to Admin")
                                    }
                                }
                                
                                Button(
                                    onClick = {
                                        viewModel.removeParticipant(chatId, participant.userId)
                                        showActionDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Kick from Group")
                                }
                                
                                TextButton(
                                    onClick = { showActionDialog = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cancel")
                                }
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                Column(modifier = Modifier.padding(16.dp)) {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Rounded.ExitToApp,
                        title = "Leave Group",
                        subtitle = "Remove yourself from this group",
                        isDestructive = true,
                        onClick = { showLeaveDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.DeleteSweep,
                        title = "Clear Chat History",
                        subtitle = "Delete all messages in this group",
                        onClick = { chat?.let { viewModel.clearChatMessages(it.chatId) } }
                    )
                    SettingsItem(
                        icon = Icons.Rounded.Report,
                        title = "Report Group",
                        subtitle = "Report inappropriate content or behavior",
                        isDestructive = true,
                        onClick = { showReportDialog = true }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        val currentParticipants = chat?.participantIds?.split(",") ?: emptyList()
        val usersToAdd = allUsers.filter { it.userId !in currentParticipants }
        
        AddParticipantDialog(
            onDismiss = { showAddDialog = false },
            onUserSelected = { userId ->
                chat?.let { viewModel.addParticipant(it.chatId, userId) }
                showAddDialog = false
            },
            availableUsers = usersToAdd
        )
    }

    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onConfirm = { reason ->
                chat?.let { viewModel.reportGroup(it.chatId, reason) }
                showReportDialog = false
            }
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave Group?") },
            text = { Text("Are you sure you want to leave this group? You will no longer receive messages.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        chat?.let { viewModel.leaveGroup(it.chatId) }
                        showLeaveDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ParticipantItem(user: UserEntity, isAdmin: Boolean, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = user.friendCode,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        if (isAdmin) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "Admin",
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}