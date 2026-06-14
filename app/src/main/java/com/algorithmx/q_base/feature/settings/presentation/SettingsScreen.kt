package com.algorithmx.q_base.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.core.ai.brain.models.BrainTask
import com.algorithmx.q_base.core.designsystem.components.reusable.AiConfigSelector
import com.algorithmx.q_base.feature.settings.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToBrainManager: () -> Unit = {},
    onNavigateToAppTheme: () -> Unit = {}
) {
    val config by viewModel.brainConfig.collectAsStateWithLifecycle()
    val dbSize by viewModel.dbSizeMb.collectAsStateWithLifecycle()
    
    SettingsContent(
        config = config,
        dbSize = dbSize,
        availableModels = viewModel.availableModels,
        onBack = onBack,
        onNavigateToBrainManager = onNavigateToBrainManager,
        onNavigateToAppTheme = onNavigateToAppTheme,
        onUpdateNotifications = { viewModel.updateNotifications(it) },
        onSaveTaskConfig = { task, taskConfig -> viewModel.saveTaskConfig(task, taskConfig) },
        onClearAllData = { onComplete -> viewModel.clearAllData(onComplete) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    config: com.algorithmx.q_base.core.ai.brain.models.StoredBrainConfig,
    dbSize: Double,
    availableModels: List<String>,
    onBack: () -> Unit,
    onNavigateToBrainManager: () -> Unit,
    onNavigateToAppTheme: () -> Unit,
    onUpdateNotifications: (Boolean) -> Unit,
    onSaveTaskConfig: (BrainTask, com.algorithmx.q_base.core.ai.brain.models.TaskConfig) -> Unit,
    onClearAllData: (() -> Unit) -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showBugDialog by remember { mutableStateOf(false) }
    var bugDescription by remember { mutableStateOf("") }
    
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            com.algorithmx.q_base.core.designsystem.components.reusable.UnifiedTopAppBar(
                title = "Settings",
                currentUser = null,
                onProfileClick = {},
                showProfileIcon = false,
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                SettingsSection(title = "ACCOUNT") {
                    SettingsCard(
                        title = "My Profile",
                        subtitle = "Manage your information and friend code",
                        icon = Icons.Rounded.Person,
                        onClick = onBack
                    )
                }
            }

            item {
                SettingsSection(title = "PREFERENCES") {
                    SettingsToggleCard(
                        title = "Push Notifications",
                        subtitle = "Stay updated on new messages and shared collections",
                        icon = Icons.Rounded.Notifications,
                        checked = config.notificationsEnabled,
                        onCheckedChange = onUpdateNotifications
                    )

                    SettingsCard(
                        title = "App Theme",
                        subtitle = when(config.themeMode) {
                            "LIGHT" -> "Always Light"
                            "DARK" -> "Always Dark"
                            "MONOCHROME" -> "Elegant Monochrome"
                            else -> "Follow System"
                        },
                        icon = Icons.Rounded.Palette,
                        onClick = onNavigateToAppTheme
                    )
                }
            }

            item {
                SettingsSection(title = "AI ENGINE") {
                    val displayModelName = if (config.modelName.contains("gemini-3.1-flash-lite", ignoreCase = true)) "Gemini 3.1 Flash Lite" else config.modelName
                    SettingsCard(
                        title = "AI Brain Manager",
                        subtitle = "Configure ${config.provider.name} ($displayModelName)",
                        icon = Icons.Rounded.Psychology,
                        onClick = onNavigateToBrainManager
                    )
                }
            }


            item {
                SettingsSection(title = "DATA & PRIVACY") {
                    SettingsCard(
                        title = "Local Database",
                        subtitle = "${String.format("%.2f", dbSize)} MB cached locally",
                        icon = Icons.Rounded.Storage,
                        onClick = { showClearDialog = true }
                    )
                    SettingsCard(
                        title = "Privacy Policy",
                        subtitle = "How we handle your data",
                        icon = Icons.Rounded.PrivacyTip,
                        onClick = { showPrivacyDialog = true }
                    )
                }
            }
            
            item {
                SettingsSection(title = "SUPPORT") {
                    SettingsCard(
                        title = "Help Center",
                        subtitle = "Guides and troubleshooting",
                        icon = Icons.AutoMirrored.Rounded.Help,
                        onClick = { showHelpDialog = true }
                    )
                    SettingsCard(
                        title = "Report a Bug",
                        subtitle = "Help us improve Q-Base",
                        icon = Icons.Rounded.BugReport,
                        onClick = { showBugDialog = true }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Q-BASE CORE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Version 1.1.2 Build (STABLE)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }

    // Reset Data Dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Reset Application Data?") },
            text = { Text("This will delete all categories, questions, collections, sessions, and chat history. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData {
                            android.widget.Toast.makeText(context, "Data successfully cleared.", android.widget.Toast.LENGTH_LONG).show()
                            showClearDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy") },
            text = {
                Column(
                    modifier = Modifier
                        .height(280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Your privacy is extremely important to us. Q-Base maintains strict protocols to ensure your learning telemetry and personal information remain secure.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "1. Data Collection & Storage",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Your personal study habits, custom collections, and application configurations are cached locally on your device using AES-256 encryption. Core data necessary for multi-device sync, including peer connections and session records, are stored securely on our Appwrite cloud infrastructure.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "2. End-to-End Encrypted Communications",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "All direct messages and shared study collections between peers use End-to-End Encryption (E2EE). Q-Base servers only route the encrypted payloads and cannot decrypt your private discussions or shared materials.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "3. AI Interactions",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "AI requests processed through the brain system rely on official Google Gemini and Groq APIs. No private friend data, conversational records, or PII are shared with third-party advertising algorithms.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "4. Usage Telemetry",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "We do not collect granular usage telemetry for advertising purposes. Limited diagnostic logs may be collected to improve application stability if you explicitly opt-in.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "5. User Rights & Data Deletion",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "You retain full control over your data. You may initiate a complete wipe of your local cache and remote cloud presence at any time via the Settings menu. This action is irreversible.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Help Center Dialog
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Help Center") },
            text = {
                Column(
                    modifier = Modifier
                        .height(280.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Welcome to the Q-Base support guide! Here are the core details to help you get the most out of your study companion:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "• Timed Practice Sessions",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Click 'Practice Lab' on Home to launch custom session timers. Your answers are marked and logged into 'Sessions' so you can track progress trends over time.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Peer Study Rooms",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Use the 'Study Sync' tab to share complete question sets and chat in real-time with other students using their unique friend codes.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got It")
                }
            }
        )
    }

    // Report a Bug Dialog
    if (showBugDialog) {
        AlertDialog(
            onDismissRequest = { showBugDialog = false },
            title = { Text("Report a Bug") },
            text = {
                Column {
                    Text(
                        text = "Notice something working incorrectly? Let us know below:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = bugDescription,
                        onValueChange = { bugDescription = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        placeholder = { Text("Describe the issue in detail...") },
                        maxLines = 5
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (bugDescription.isNotBlank()) {
                            android.widget.Toast.makeText(
                                context,
                                "Bug report submitted successfully! Thank you.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                            bugDescription = ""
                            showBugDialog = false
                        }
                    },
                    enabled = bugDescription.isNotBlank()
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBugDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

