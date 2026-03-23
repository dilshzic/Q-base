package com.algorithmx.q_base.ui.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiGenerationScreen(
    collectionId: String,
    collectionName: String,
    onBack: () -> Unit,
    viewModel: AiViewModel = hiltViewModel()
) {
    var topic by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("5") }
    var selectedType by remember { mutableStateOf("SBA") }
    val uiState by viewModel.uiState.collectAsState()
    val previewResponse by viewModel.currentResponse.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Question Generator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Create a custom question collection using AI. Describe the topic in detail for better results.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = topic,
                onValueChange = { topic = it },
                label = { Text("Topic (e.g. Early signs of Heart Failure)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it },
                    label = { Text("Count") },
                    modifier = Modifier.weight(1f)
                )
                
                // Simplified type selector
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("SBA", "MCQ", "T/F").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.generateCollection(topic, count.toIntOrNull() ?: 5, selectedType, collectionId, collectionName)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = topic.isNotBlank() && uiState !is AiUiState.Loading
            ) {
                if (uiState is AiUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Generate Collection")
                }
            }

            when (val state = uiState) {
                is AiUiState.Success -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Preview Generated Questions", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            previewResponse?.questions?.forEach { question ->
                                Text("• ${question.stem}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.promoteResponse(collectionId, collectionName) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Save to Database")
                                }
                                OutlinedButton(
                                    onClick = { 
                                        viewModel.resetState()
                                        onBack() 
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Done")
                                }
                            }
                        }
                    }
                }
                is AiUiState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Error", style = MaterialTheme.typography.titleMedium)
                            Text(state.message)
                            Button(onClick = { viewModel.resetState() }) {
                                Text("Retry")
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
