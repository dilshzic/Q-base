package com.algorithmx.q_base.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureBackupDialog(
    onDismiss: () -> Unit,
    viewModel: SecureBackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var passphrase by remember { mutableStateOf("") }
    
    // Check for success state and dismiss
    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            viewModel.clearState()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (state.hasBackup) "Restore Secure Backup" else "Setup Secure Backup")
            }
        },
        text = {
            Column {
                if (state.hasBackup) {
                    Text("A secure backup was found for your account. Enter your recovery passphrase to restore your encryption keys and unlock your message history.")
                } else {
                    Text("Create a secure backup of your encryption keys. This is required to access your message history if you log out or reinstall the app. Please enter a strong, memorable passphrase.")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (state.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (state.hasBackup) viewModel.restoreBackup(passphrase)
                    else viewModel.setupBackup(passphrase)
                },
                enabled = !state.isLoading && passphrase.length >= 6
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (state.hasBackup) "Restore" else "Save Backup")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isLoading) {
                Text("Cancel")
            }
        }
    )
}