package com.algorithmx.q_base.feature.auth.presentation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestoreBackupScreen(
    userId: String,
    onRestoreSuccess: () -> Unit,
    onStartFresh: () -> Unit,
    viewModel: RestoreBackupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var passphrase by remember { mutableStateOf("") }
    var showStartFreshDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    LaunchedEffect(state.isRestored, state.isFreshStart) {
        if (state.isRestored) {
            Toast.makeText(context, "Keys restored successfully!", Toast.LENGTH_SHORT).show()
            onRestoreSuccess()
        } else if (state.isFreshStart) {
            Toast.makeText(context, "Started fresh. Cloud keys removed.", Toast.LENGTH_SHORT).show()
            onStartFresh()
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) {
            Toast.makeText(context, state.error, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    if (showStartFreshDialog) {
        AlertDialog(
            onDismissRequest = { showStartFreshDialog = false },
            title = { Text("Start Fresh?") },
            text = { Text("If you forgot your passphrase, you can start fresh. Your cloud backup will be discarded and new keys will be generated. You will lose access to any old encrypted messages. Continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showStartFreshDialog = false
                        viewModel.startFresh(userId)
                    }
                ) {
                    Text("Yes, start fresh", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartFreshDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Encrypted Backup Found",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "We found an end-to-end encryption key backup on the cloud. Enter your passphrase to decrypt and restore it.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text("Passphrase") },
                leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                shape = RoundedCornerShape(16.dp)
            )

            Button(
                onClick = { viewModel.restoreBackup(userId, passphrase) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp)),
                enabled = passphrase.isNotBlank() && !state.isLoading
            ) {
                if (state.isLoading && !state.isFreshStart) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Unlock Backup",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { showStartFreshDialog = true },
                enabled = !state.isLoading
            ) {
                Text("I forgot my passphrase (Start Fresh)", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
