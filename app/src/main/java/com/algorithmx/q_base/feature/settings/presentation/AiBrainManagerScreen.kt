package com.algorithmx.q_base.feature.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.core.ai.brain.models.BrainTask
import com.algorithmx.q_base.core.designsystem.components.reusable.AiConfigSelector
import com.algorithmx.q_base.core.designsystem.components.reusable.UnifiedTopAppBar
import com.algorithmx.q_base.feature.settings.presentation.components.UsageStatsCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiBrainManagerScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val config by viewModel.brainConfig.collectAsStateWithLifecycle()
    val availableModels = viewModel.availableModels

    Scaffold(
        topBar = {
            UnifiedTopAppBar(
                title = "AI Brain Manager",
                currentUser = null,
                onProfileClick = {},
                showProfileIcon = false,
                isLarge = false,
                titleCentered = true,
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {


            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Master AI Freeze",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Globally disables all AI features across the app when enabled.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.isMasterAiFreeze,
                        onCheckedChange = { viewModel.toggleMasterAiFreeze(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.error,
                            checkedTrackColor = MaterialTheme.colorScheme.errorContainer,
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "CONFIGURE AI ENGINE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    // Create a dummy TaskConfig from the master model config
                    val masterConfig = com.algorithmx.q_base.core.ai.brain.models.TaskConfig(
                        modelName = config.modelName,
                        fallbackModelName = null, // Or derive if needed
                        systemPrompt = config.systemInstruction
                    )
                    
                    AiConfigSelector(
                        task = com.algorithmx.q_base.core.ai.brain.models.BrainTask.CHAT_BOT,
                        currentConfig = masterConfig,
                        availableModels = availableModels,
                        titleOverride = "Master Model Configuration",
                        onConfigChange = { newTaskConfig -> 
                            viewModel.updateMasterAiConfig(
                                newTaskConfig.modelName,
                                newTaskConfig.fallbackModelName,
                                newTaskConfig.systemPrompt
                            ) 
                        }
                    )
                }
            }
            
            /*item {
                Spacer(modifier = Modifier.height(24.dp))
                UsageStatsCard(
                    requests = config.totalRequests,
                    tokens = config.totalTokens
                )
            }*/
        }
    }
}