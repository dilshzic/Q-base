package com.algorithmx.q_base.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.algorithmx.q_base.ui.components.ProfileIconButton
import com.algorithmx.q_base.data.core.UserEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewChatScreen(
    onBack: () -> Unit,
    onChatStarted: (String) -> Unit,
    onNavigateToNewGroup: () -> Unit,
    onProfileClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is ChatNavEvent.NavigateToChatDetail -> onChatStarted(event.chatId)
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Message", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    ProfileIconButton(
                        user = currentUser,
                        onClick = onProfileClick
                    )
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // New Group Option
            Surface(
                onClick = onNavigateToNewGroup,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Rounded.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "New Group",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            ContactSelector(
                modifier = Modifier.weight(1f),
                onUserSelected = { user ->
                    viewModel.startNewChat(user.userId, user.displayName)
                    // Navigation is handled via LaunchedEffect above
                }
            )
        }
    }
}

@Composable
fun UserItem(user: UserEntity, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                user.friendCode?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
