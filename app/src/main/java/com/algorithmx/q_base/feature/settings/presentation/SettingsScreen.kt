package com.algorithmx.q_base.feature.settings.presentation

import android.content.Intent
import android.net.Uri
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
    onNavigateToAppTheme: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
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
        onNavigateToProfile = onNavigateToProfile,
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
    onNavigateToProfile: () -> Unit,
    onUpdateNotifications: (Boolean) -> Unit,
    onSaveTaskConfig: (BrainTask, com.algorithmx.q_base.core.ai.brain.models.TaskConfig) -> Unit,
    onClearAllData: (() -> Unit) -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }
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
                        onClick = onNavigateToProfile
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
                        subtitle = "Open our official Privacy Policy in browser",
                        icon = Icons.Rounded.PrivacyTip,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://dilshzic.github.io/Q-base/legal/privacy_policy.html"))
                            context.startActivity(intent)
                        }
                    )
                    SettingsCard(
                        title = "Terms of Service",
                        subtitle = "Open our official Terms of Service in browser",
                        icon = Icons.Rounded.Gavel,
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://dilshzic.github.io/Q-base/legal/terms_of_service.html"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
            
            item {
                SettingsSection(title = "SUPPORT") {
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

