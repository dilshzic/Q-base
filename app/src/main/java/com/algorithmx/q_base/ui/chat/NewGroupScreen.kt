package com.algorithmx.q_base.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.ui.components.reusable.UnifiedTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewGroupScreen(
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit,
    onProfileClick: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is ChatNavEvent.NavigateToChatDetail -> onGroupCreated(event.chatId)
            }
        }
    }

    var step by remember { mutableStateOf(1) } // 1: Select Participants, 2: Name Group
    var selectedParticipants by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var groupName by remember { mutableStateOf("") }

    // Intercept OS back to go to previous step before exiting wizard
    androidx.activity.compose.BackHandler(enabled = step > 1) {
        step = 1
    }

    Scaffold(
        topBar = {
            UnifiedTopAppBar(
                title = if (step == 1) "Select Participants" else "Group Details",
                currentUser = currentUser,
                onProfileClick = onProfileClick,
                isLarge = false,
                titleCentered = true,
                navigationIcon = {
                    IconButton(onClick = {
                        if (step == 2) step = 1 else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val stepProgress by androidx.compose.animation.core.animateFloatAsState(
                targetValue = step.toFloat() / 2f,
                animationSpec = androidx.compose.animation.core.tween(400), label = "progress"
            )
            LinearProgressIndicator(
                progress = { stepProgress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                                slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                                slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "Group Creation Step",
                modifier = Modifier.weight(1f)
            ) { targetStep ->
                when (targetStep) {
                    1 -> {
                        ContactSelector(
                            multiSelectMode = true,
                            onUsersSelected = {
                                selectedParticipants = it
                                step = 2
                            }
                        )
                    }
                    2 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                modifier = Modifier.size(80.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Rounded.Group,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            OutlinedTextField(
                                value = groupName,
                                onValueChange = { groupName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Group Name") },
                                placeholder = { Text("Enter a descriptive name") },
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                "${selectedParticipants.size + 1} participants (including you)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                            Button(
                                onClick = {
                                    viewModel.startNewGroup(
                                        participantIds = selectedParticipants.map { it.userId },
                                        groupName = groupName
                                    )
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                enabled = groupName.isNotBlank(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create Group")
                            }
                        }
                    }
                }
            }
        }
    }
}
