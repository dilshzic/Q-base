package com.algorithmx.q_base.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.ui.components.AiConfigSelector
import com.algorithmx.q_base.core_ai.brain.models.BrainTask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val config by viewModel.brainConfig.collectAsStateWithLifecycle()
    val dbSize by viewModel.dbSizeMb.collectAsStateWithLifecycle()
    
    var showClearDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
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
                SettingsSectionHeader("ACCOUNT")
                SettingsCard(
                    title = "My Profile",
                    subtitle = "Manage your information and friend code",
                    icon = Icons.Default.Person,
                    onClick = { /* Already handled by Profile button usually, but could link back */ }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionHeader("PREFERENCES")
                SettingsToggleCard(
                    title = "Push Notifications",
                    subtitle = "Stay updated on new messages and shared collections",
                    icon = Icons.Default.Notifications,
                    checked = config.notificationsEnabled,
                    onCheckedChange = { viewModel.updateNotifications(it) }
                )
                
                SettingsCard(
                    title = "App Theme",
                    subtitle = when(config.themeMode) {
                        "LIGHT" -> "Always Light"
                        "DARK" -> "Always Dark"
                        else -> "Follow System"
                    },
                    icon = Icons.Default.Palette,
                    onClick = { /* show Theme picker */ }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionHeader("AI ENGINE (CHATBOT)")
                AiConfigSelector(
                    task = BrainTask.CHAT_BOT,
                    currentConfig = config.taskConfigs[BrainTask.CHAT_BOT],
                    availableModels = viewModel.availableModels,
                    onConfigChange = { viewModel.saveTaskConfig(BrainTask.CHAT_BOT, it) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader("AI ENGINE (GENERATOR)")
                AiConfigSelector(
                    task = BrainTask.COLLECTION_GEN,
                    currentConfig = config.taskConfigs[BrainTask.COLLECTION_GEN],
                    availableModels = viewModel.availableModels,
                    onConfigChange = { viewModel.saveTaskConfig(BrainTask.COLLECTION_GEN, it) }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader("USAGE TELEMETRY")
                UsageStatsCard(
                    requests = config.totalRequests,
                    tokens = config.totalTokens
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionHeader("DATA & PRIVACY")
                SettingsCard(
                    title = "Local Database",
                    subtitle = "${String.format("%.2f", dbSize)} MB cached locally",
                    icon = Icons.Default.Storage,
                    onClick = { showClearDialog = true }
                )
                SettingsCard(
                    title = "Privacy Policy",
                    subtitle = "How we handle your data",
                    icon = Icons.Default.PrivacyTip,
                    onClick = { }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionHeader("SUPPORT")
                SettingsCard(
                    title = "Help Center",
                    subtitle = "Guides and troubleshooting",
                    icon = Icons.Default.Help,
                    onClick = { }
                )
                SettingsCard(
                    title = "Report a Bug",
                    subtitle = "Help us improve Q-Base",
                    icon = Icons.Default.BugReport,
                    onClick = { }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Q-BASE CORE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.outline
                        )
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

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Reset Application Data?") },
            text = { Text("This will delete all medical categories, questions, collections, sessions, and chat history. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData {
                            android.widget.Toast.makeText(context, "Data cleared. Please restart the app.", android.widget.Toast.LENGTH_LONG).show()
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
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsToggleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
fun UsageStatsCard(requests: Int, tokens: Int) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    requests.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Requests", style = MaterialTheme.typography.labelSmall)
            }
            VerticalDivider(modifier = Modifier.height(40.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    (tokens / 1000).toString() + "k",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("Tokens", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
