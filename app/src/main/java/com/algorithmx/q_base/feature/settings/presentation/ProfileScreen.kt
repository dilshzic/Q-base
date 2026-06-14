package com.algorithmx.q_base.feature.settings.presentation

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.core.designsystem.components.reusable.UnifiedTopAppBar
import com.algorithmx.q_base.core.designsystem.theme.QbaseTheme
import com.algorithmx.q_base.core.state.AppAccessState
import com.algorithmx.q_base.core.state.LocalAppAccessState

import com.algorithmx.q_base.feature.settings.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val user by viewModel.userState.collectAsStateWithLifecycle()
    val hasSecureBackup by viewModel.hasSecureBackup.collectAsStateWithLifecycle()
    val appAccessState = LocalAppAccessState.current

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    ProfileContent(
        user = user,
        hasSecureBackup = hasSecureBackup,
        appAccessState = appAccessState,
        onBack = onBack,
        onNavigateToSettings = onNavigateToSettings,
        onLoggedOut = onLoggedOut,
        onCopyFriendCode = {
            clipboardManager.setText(AnnotatedString(it))
            android.widget.Toast.makeText(context, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
        },
        onShareFriendCode = { friendCode ->
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "Hey! Add me on Qbase! Here's my friend code: $friendCode")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share Friend Code")
            context.startActivity(shareIntent)
        },
        onRegenerateFriendCode = { viewModel.regenerateFriendCode() },
        onSignOut = { clearCollections, onComplete -> 
            viewModel.signOut(clearCollections) { onComplete() }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    user: com.algorithmx.q_base.core.data.UserEntity?,
    hasSecureBackup: Boolean,
    appAccessState: AppAccessState,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLoggedOut: () -> Unit,
    onCopyFriendCode: (String) -> Unit,
    onShareFriendCode: (String) -> Unit,
    onRegenerateFriendCode: () -> Unit,
    onSignOut: (Boolean, () -> Unit) -> Unit
) {
    val scrollState = rememberScrollState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var clearCollections by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    if (showBackupDialog) {
        SecureBackupDialog(onDismiss = { showBackupDialog = false })
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    if (!hasSecureBackup) {
                        Text(
                            text = "⚠️ WARNING: You do not have a Secure Chat Backup!\n\nLogging out will permanently destroy your decryption keys. You will lose access to unread messages and any queued messages from friends.",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        Text("Are you sure you want to log out? Chats and session history will be cleared from this device.")
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { clearCollections = !clearCollections }
                    ) {
                        Checkbox(
                            checked = clearCollections,
                            onCheckedChange = { clearCollections = it }
                        )
                        Text("Delete all collections and question content", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSignOut(clearCollections) { onLoggedOut() }
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Dynamic decorative gradient backdrops consistent with Qbase app design language
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        radius = 1200f
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                UnifiedTopAppBar(
                    title = "My Profile",
                    currentUser = null,
                    onProfileClick = {},
                    showProfileIcon = false,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Profile Avatar
                ProfileAvatar(
                    imageUrl = user?.profilePictureUrl,
                    modifier = Modifier.size(130.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Identity Block
                val displayName = user?.displayName?.takeIf { it.isNotBlank() } ?: "Knowledge Seeker"
                val email = user?.email?.takeIf { it.isNotBlank() } ?: "guest@qbase.io"

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                AssistChip(
                    onClick = {},
                    label = { Text("${appAccessStateLabel(appAccessState)}") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Circle,
                            contentDescription = null,
                            tint = appAccessStateColor(appAccessState)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Connect Section
                ProfileCardSection(title = "Connect") {
                    ProfilePropertyRow(
                        label = "My Friend Code",
                        value = user?.friendCode ?: "Click to generate",
                        isLink = true,
                        icon = Icons.Rounded.QrCode,
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onRegenerateFriendCode() }) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = "Regenerate")
                                }
                                IconButton(onClick = { user?.friendCode?.let { onCopyFriendCode(it) } }) {
                                    Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy")
                                }
                                IconButton(onClick = { user?.friendCode?.let { onShareFriendCode(it) } }) {
                                    Icon(Icons.Rounded.Share, contentDescription = "Share")
                                }
                            }
                        }
                    )
                }

                // Security Section
                ProfileCardSection(title = "Security & Encryption") {
                    ProfilePropertyRow(
                        label = "Encryption Key Backup",
                        value = if (hasSecureBackup) "Keys backed Up" else "⚠️ Backup Keys (Recommended)",
                        isLink = !hasSecureBackup,
                        icon = Icons.Rounded.Security,
                        valueColor = if (hasSecureBackup) null else MaterialTheme.colorScheme.error,
                        onClick = { showBackupDialog = true }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Logout Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .bounceClick { showLogoutDialog = true },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Logout, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Log Out from Device", 
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

private fun appAccessStateLabel(appAccessState: AppAccessState): String = when (appAccessState) {
    AppAccessState.Online -> "Online"
    AppAccessState.Offline -> "Offline"
    AppAccessState.NotLoggedIn -> "Not logged in"
}

@Composable
private fun appAccessStateColor(appAccessState: AppAccessState): Color = when (appAccessState) {
    AppAccessState.Online -> Color(0xFF2E7D32)
    AppAccessState.Offline -> MaterialTheme.colorScheme.error
    AppAccessState.NotLoggedIn -> MaterialTheme.colorScheme.tertiary
}

@Preview(showBackground = true)
@Composable
fun ProfileContentPreview() {
    QbaseTheme {
        ProfileContent(
            user = com.algorithmx.q_base.core.data.UserEntity(
                userId = "123",
                displayName = "John Doe",
                email = "john.doe@example.com",
                intro = "Passionate about learning and technology.",
                profilePictureUrl = "emoji:🎓",
                friendCode = "QBASE-1234",
                isPhotoVisible = true
            ),
            hasSecureBackup = false,
            appAccessState = AppAccessState.Online,
            onBack = {},
            onNavigateToSettings = {},
            onLoggedOut = {},
            onCopyFriendCode = {},
            onShareFriendCode = {},
            onRegenerateFriendCode = {},
            onSignOut = { _, _ -> }
        )
    }
}