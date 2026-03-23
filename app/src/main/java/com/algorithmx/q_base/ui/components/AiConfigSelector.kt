package com.algorithmx.q_base.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.brain.models.BrainTask
import com.algorithmx.q_base.brain.models.TaskConfig

/**
 * A reusable UI component for selecting and configuring AI models for a specific BrainTask.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiConfigSelector(
    task: BrainTask,
    currentConfig: TaskConfig?,
    availableModels: List<String>,
    onConfigChange: (TaskConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var primaryModel by remember(currentConfig) { mutableStateOf(currentConfig?.modelName ?: availableModels.firstOrNull() ?: "") }
    var fallbackModel by remember(currentConfig) { mutableStateOf(currentConfig?.fallbackModelName ?: "") }
    var systemPrompt by remember(currentConfig) { mutableStateOf(currentConfig?.systemPrompt ?: "") }
    
    // Dropdown state
    var primaryExpanded by remember { mutableStateOf(false) }
    var fallbackExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "${task.displayName} Configuration",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // Primary Model Selector
        ExposedDropdownMenuBox(
            expanded = primaryExpanded,
            onExpandedChange = { primaryExpanded = !primaryExpanded }
        ) {
            OutlinedTextField(
                value = primaryModel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Primary Model") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = primaryExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = primaryExpanded,
                onDismissRequest = { primaryExpanded = false }
            ) {
                availableModels.forEach { model ->
                    DropdownMenuItem(
                        text = { Text(model) },
                        onClick = {
                            primaryModel = model
                            primaryExpanded = false
                            onConfigChange(TaskConfig(primaryModel, fallbackModel.takeIf { it.isNotBlank() }, systemPrompt))
                        }
                    )
                }
            }
        }

        // Fallback Model Selector
        ExposedDropdownMenuBox(
            expanded = fallbackExpanded,
            onExpandedChange = { fallbackExpanded = !fallbackExpanded }
        ) {
            OutlinedTextField(
                value = fallbackModel.ifBlank { "None (Recommended)" },
                onValueChange = {},
                readOnly = true,
                label = { Text("Fallback Model (Optional)") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fallbackExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = fallbackExpanded,
                onDismissRequest = { fallbackExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        fallbackModel = ""
                        fallbackExpanded = false
                        onConfigChange(TaskConfig(primaryModel, null, systemPrompt))
                    }
                )
                availableModels.forEach { model ->
                    if (model != primaryModel) {
                        DropdownMenuItem(
                            text = { Text(model) },
                            onClick = {
                                fallbackModel = model
                                fallbackExpanded = false
                                onConfigChange(TaskConfig(primaryModel, fallbackModel, systemPrompt))
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = systemPrompt,
            onValueChange = { 
                systemPrompt = it
                onConfigChange(TaskConfig(primaryModel, fallbackModel.takeIf { f -> f.isNotBlank() }, systemPrompt))
            },
            label = { Text("System Prompt Instructions") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6
        )
    }
}
