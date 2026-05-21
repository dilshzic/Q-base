package com.algorithmx.q_base.feature.chat.presentation
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.algorithmx.q_base.data.chat.ChatEntity
import com.algorithmx.q_base.data.chat.MessageEntity
import com.algorithmx.q_base.ui.components.reusable.ProfileIconButton
import com.algorithmx.q_base.ui.state.AppAccessState
import com.algorithmx.q_base.ui.state.LocalAppAccessState
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import com.algorithmx.q_base.ui.components.reusable.UnifiedTopAppBar
import com.algorithmx.q_base.ui.chat.components.GuestConnectView
import com.algorithmx.q_base.ui.chat.components.EmptyConnectView
import com.algorithmx.q_base.ui.chat.components.AnimatedChatItem

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ChatListScreen(
    onChatClick: (String) -> Unit,
    onNewChat: () -> Unit,
    onNavigateToBlockedList: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val state by viewModel.chatListState.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedChatIds by viewModel.selectedChatIds.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val appAccessState = LocalAppAccessState.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is ChatNavEvent.NavigateToChatDetail -> onChatClick(event.chatId)
            }
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            viewModel.syncChatsFromRemote()
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                UnifiedTopAppBar(
                    title = "${selectedChatIds.size} selected",
                    currentUser = null,
                    onProfileClick = {},
                    showProfileIcon = false,
                    isLarge = false,
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear Selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Selected")
                        }
                    }
                )
            } else {
                UnifiedTopAppBar(
                    title = "Connect",
                    subtitle = "Connect with Groups and People",
                    currentUser = currentUser,
                    onProfileClick = onProfileClick,
                    actions = {
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Blocked List") },
                                    onClick = { showMenu = false; onNavigateToBlockedList() },
                                    leadingIcon = { Icon(Icons.Rounded.Block, contentDescription = null) }
                                )
                            }
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (currentUser != null) {
                ExtendedFloatingActionButton(
                    onClick = onNewChat,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Message")
                }
            }
        }
    ) { padding ->
        if (appAccessState is AppAccessState.RestoringSession) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Restoring your session…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (currentUser == null) {
            GuestConnectView(
                modifier = Modifier.padding(padding)
            )
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.syncChatsFromRemote() },
                modifier = Modifier.padding(padding)
            ) {
                if (state.chats.isEmpty() && !state.isLoading) {
                    EmptyConnectView(modifier = Modifier.fillMaxSize())
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        /*
                        item {
                            AiChatQuickAction(onClick = { viewModel.startAiChat() })
                        }
                        */
                        
                        itemsIndexed(state.chats) { index, chatUi ->
                            val isSelected = selectedChatIds.contains(chatUi.chat.chatId)
                            AnimatedChatItem(
                                chatUi = chatUi,
                                index = index,
                                isSelected = isSelected,
                                selectionMode = isSelectionMode,
                                onClick = { 
                                    if (isSelectionMode) viewModel.toggleChatSelection(chatUi.chat.chatId)
                                    else onChatClick(chatUi.chat.chatId) 
                                },
                                onLongClick = { viewModel.toggleChatSelection(chatUi.chat.chatId) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete selected chats?") },
            text = { Text("This will permanently remove ${selectedChatIds.size} conversations from your list and the cloud.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedChats()
                        showDeleteConfirm = false
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
}


