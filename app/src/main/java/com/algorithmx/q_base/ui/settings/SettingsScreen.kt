package com.algorithmx.q_base.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.algorithmx.q_base.core_ai.brain.models.BrainTask
import com.algorithmx.q_base.ui.components.reusable.AiConfigSelector

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
    config: com.algorithmx.q_base.core_ai.brain.models.StoredBrainConfig,
    dbSize: Double,
    availableModels: List<String>,
    onBack: () -> Unit,
    onNavigateToBrainManager: () -> Unit,
    onNavigateToAppTheme: () -> Unit,
    onUpdateNotifications: (Boolean) -> Unit,
    onSaveTaskConfig: (BrainTask, com.algorithmx.q_base.core_ai.brain.models.TaskConfig) -> Unit,
    onClearAllData: (() -> Unit) -> Unit
) {
    var showClearDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            com.algorithmx.q_base.ui.components.reusable.UnifiedTopAppBar(
                title = "Settings",
                currentUser = null,
                onProfileClick = {},
                showProfileIcon = false,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    icon = Icons.Default.Palette,
                    onClick = onNavigateToAppTheme
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SettingsSectionHeader("AI ENGINE")
                SettingsCard(
                    title = "AI Brain Manager",
                    subtitle = "Configure ${config.provider.name} (${config.modelName})",
                    icon = Icons.Default.Psychology,
                    onClick = onNavigateToBrainManager
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
                    icon = Icons.AutoMirrored.Filled.Help,
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
            text = { Text("This will delete all categories, questions, collections, sessions, and chat history. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAllData {
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

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun SettingsPreview() {
    MaterialTheme {
        SettingsContent(
            config = com.algorithmx.q_base.core_ai.brain.models.StoredBrainConfig(
                provider = com.algorithmx.androidmodules.coreai.brain.models.BrainProvider.GEMINI,
                modelName = "gemini-1.5-flash",
                systemInstruction = "",
                totalRequests = 120,
                totalTokens = 45000,
                themeMode = "SYSTEM",
                notificationsEnabled = true
            ),
            dbSize = 12.45,
            availableModels = listOf("gemini-1.5-pro", "gemini-1.5-flash", "llama-3-70b"),
            onBack = {},
            onNavigateToBrainManager = {},
            onNavigateToAppTheme = {},
            onUpdateNotifications = {},
            onSaveTaskConfig = { _, _ -> },
            onClearAllData = {}
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
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .padding(top = 8.dp)
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
        onClick = { onCheckedChange(!checked) },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
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
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun UsageStatsCard(requests: Int, tokens: Int) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = requests.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Requests",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Box(modifier = Modifier.width(1.dp).height(40.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (tokens > 1000) "${tokens / 1000}k" else tokens.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Tokens",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
