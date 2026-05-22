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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.core_ai.brain.models.BrainTask
import com.algorithmx.q_base.feature.components.reusable.AiConfigSelector
import com.algorithmx.q_base.feature.components.reusable.UnifiedTopAppBar
import com.algorithmx.q_base.feature.settings.components.UsageStatsCard

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
                    AiConfigSelector(
                        task = BrainTask.COLLECTION_GEN,
                        currentConfig = config.taskConfigs[BrainTask.COLLECTION_GEN],
                        availableModels = availableModels,
                        onConfigChange = { viewModel.saveTaskConfig(BrainTask.COLLECTION_GEN, it) }
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    AiConfigSelector(
                        task = BrainTask.QUESTION_EXTRACTION,
                        currentConfig = config.taskConfigs[BrainTask.QUESTION_EXTRACTION],
                        availableModels = availableModels,
                        onConfigChange = { viewModel.saveTaskConfig(BrainTask.QUESTION_EXTRACTION, it) }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                UsageStatsCard(
                    requests = config.totalRequests,
                    tokens = config.totalTokens
                )
            }
        }
    }
}