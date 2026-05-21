package com.algorithmx_q_base.feature.chat.presentation
import androidx.compose.animation.*
import androidx.compose.runtime.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

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

    ContactSelector(
        onUserSelected = { user ->
            viewModel.startNewChat(user.userId, user.displayName)
        },
        onBack = onBack,
        onProfileClick = onProfileClick,
        onNavigateToNewGroup = onNavigateToNewGroup,
        currentUser = currentUser,
        titleCentered = true
    )
}
