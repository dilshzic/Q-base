package com.algorithmx.q_base.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest

fun Modifier.bounceClick(onClick: () -> Unit = {}) = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounce"
    )
    
    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitFirstDown(false)
                    isPressed = true
                    val upEvent = waitForUpOrCancellation()
                    isPressed = false
                    if (upEvent != null) {
                        onClick()
                    }
                }
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val user by viewModel.userState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val hasSecureBackup by viewModel.hasSecureBackup.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    ProfileContent(
        user = user,
        stats = stats,
        hasSecureBackup = hasSecureBackup,
        onBack = onBack,
        onNavigateToSettings = onNavigateToSettings,
        onLoggedOut = onLoggedOut,
        onUpdateDisplayName = { viewModel.updateDisplayName(it) },
        onCopyFriendCode = {
            clipboardManager.setText(AnnotatedString(it))
            android.widget.Toast.makeText(context, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
        },
        onTogglePhotoVisibility = { viewModel.togglePhotoVisibility(it) },
        onSignOut = { clearCollections, onComplete -> 
            viewModel.signOut(clearCollections) { onComplete() }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    user: com.algorithmx.q_base.data.core.UserEntity?,
    stats: UserStats,
    hasSecureBackup: Boolean,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLoggedOut: () -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onCopyFriendCode: (String) -> Unit,
    onTogglePhotoVisibility: (Boolean) -> Unit,
    onSignOut: (Boolean, () -> Unit) -> Unit
) {
    val scrollState = rememberScrollState()
    var showLogoutDialog by remember { mutableStateOf(false) }
    var clearCollections by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    
    var isEditingName by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(user?.displayName ?: "") }

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

    if (isEditingName) {
        AlertDialog(
            onDismissRequest = { isEditingName = false },
            title = { Text("Edit Display Name", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Display Name") },
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDisplayName(editedName)
                        isEditingName = false
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { isEditingName = false }) {
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
        // Multi-colored radial glowing mesh overlays for premium styling consistent with Qbase
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
                com.algorithmx.q_base.ui.components.reusable.UnifiedTopAppBar(
                    title = "My Profile",
                    currentUser = null,
                    onProfileClick = {},
                    showProfileIcon = false,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Jetchat Parallax Offset Header Photo
                    ProfileParallaxHeader(
                        user = user,
                        scrollState = scrollState,
                        padding = padding
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Name and Position/Intro block matching Jetchat
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = user?.displayName ?: "Knowledge Seeker",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        
                        Text(
                            text = user?.email ?: "guest@qbase.io",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        user?.intro?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Clean divided Profile properties matching Jetchat
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        // Display Name
                        ProfilePropertyRow(
                            label = "Display Name",
                            value = user?.displayName ?: "Knowledge Seeker",
                            onClick = {
                                editedName = user?.displayName ?: ""
                                isEditingName = true
                            }
                        )

                        // Email
                        ProfilePropertyRow(
                            label = "Email Address",
                            value = user?.email ?: "guest@qbase.io"
                        )

                        // Friend Code (primary link color, copy action)
                        ProfilePropertyRow(
                            label = "Friend Code",
                            value = user?.friendCode ?: "Click to generate",
                            isLink = true,
                            onClick = { user?.friendCode?.let { onCopyFriendCode(it) } }
                        )

                        // Security Vault Status
                        ProfilePropertyRow(
                            label = "Cryptographic Backup Status",
                            value = if (hasSecureBackup) "Verified (Encrypted Vault)" else "⚠️ Action Required (Click to backup)",
                            isLink = !hasSecureBackup,
                            onClick = { showBackupDialog = true }
                        )

                        // Privacy Mode
                        ProfilePropertyToggleRow(
                            label = "Public Profile Mode",
                            valueText = if (user?.isPhotoVisible ?: true) "Visible to other users" else "Hidden from searches",
                            checked = user?.isPhotoVisible ?: true,
                            onToggle = onTogglePhotoVisibility
                        )

                        // Statistics
                        ProfilePropertyRow(
                            label = "Database Contribution Stats",
                            value = "Created Questions: ${stats.userCreatedQuestions}   |   Shared Materials: ${stats.sharedQuestions}"
                        )

                        // Achievements Section consistent with Jetchat Dividers
                        ProfileAchievementsProperty(stats = stats)

                        Spacer(modifier = Modifier.height(24.dp))

                        // Jetchat styled clean logout trigger
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .bounceClick { showLogoutDialog = true },
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Logout, 
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Log Out from Device", 
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(100.dp))
                }

                // Collapsible Jetchat-style Floating Action Button (FAB) at bottom right
                val isFabExtended by remember { derivedStateOf { scrollState.value == 0 } }
                ExtendedFloatingActionButton(
                    onClick = {
                        editedName = user?.displayName ?: ""
                        isEditingName = true
                    },
                    expanded = isFabExtended,
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Edit Profile") },
                    text = { Text("Edit Profile") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .navigationBarsPadding()
                )
            }
        }
    }
}

@Composable
fun ProfileParallaxHeader(
    user: com.algorithmx.q_base.data.core.UserEntity?,
    scrollState: androidx.compose.foundation.ScrollState,
    padding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(top = padding.calculateTopPadding()),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            if (user?.profilePictureUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(user.profilePictureUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun ProfilePropertyRow(
    label: String,
    value: String,
    isLink: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val isClickable = onClick != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isClickable) Modifier.clickable { onClick?.invoke() }
                else Modifier
            )
            .padding(vertical = 12.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isLink) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isLink) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun ProfilePropertyToggleRow(
    label: String,
    valueText: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun ProfileAchievementsProperty(stats: UserStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Unlocked Milestones",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BadgeBadge(
                name = "Creator",
                icon = Icons.Rounded.Star,
                unlocked = stats.userCreatedQuestions >= 5,
                color = Color(0xFFFFD700)
            )
            BadgeBadge(
                name = "Synergy",
                icon = Icons.Rounded.Group,
                unlocked = stats.sharedQuestions >= 5,
                color = Color(0xFFC0C0C0)
            )
            BadgeBadge(
                name = "Innovator",
                icon = Icons.Rounded.Lightbulb,
                unlocked = stats.userCreatedQuestions >= 15,
                color = Color(0xFFE5E4E2)
            )
        }
    }
}

@Composable
fun BadgeBadge(
    name: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    unlocked: Boolean,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(
                    if (unlocked) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    CircleShape
                )
                .border(
                    width = 1.5.dp,
                    color = if (unlocked) color else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = if (unlocked) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun ProfilePreview() {
    MaterialTheme {
        ProfileContent(
            user = com.algorithmx.q_base.data.core.UserEntity(
                userId = "123",
                displayName = "John Doe",
                email = "john.doe@example.com",
                intro = "Passionate learner.",
                profilePictureUrl = null,
                friendCode = "UOK-1234"
            ),
            stats = UserStats(
                userCreatedQuestions = 15,
                sharedQuestions = 42
            ),
            hasSecureBackup = true,
            onBack = {},
            onNavigateToSettings = {},
            onLoggedOut = {},
            onUpdateDisplayName = {},
            onCopyFriendCode = {},
            onTogglePhotoVisibility = {},
            onSignOut = { _, _ -> }
        )
    }
}
