package com.algorithmx.q_base.feature.chat.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.activity.compose.BackHandler
import com.algorithmx.q_base.core.data.UserEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddParticipantDialog(
    onDismiss: () -> Unit,
    onUserSelected: (String) -> Unit,
    availableUsers: List<UserEntity>
) {
    BackHandler(onBack = onDismiss)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text("Add Participant") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Close")
                    }
                }
            )
            Box(modifier = Modifier.weight(1f)) {
                ContactSelector(
                    onUserSelected = { user ->
                        onUserSelected(user.userId)
                        onDismiss()
                    }
                )
            }
        }
    }
}