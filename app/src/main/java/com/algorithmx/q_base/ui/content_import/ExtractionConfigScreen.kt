package com.algorithmx.q_base.ui.content_import

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractionConfigScreen(
    extractedText: String,
    targetId: String?,
    onBack: () -> Unit,
    onStartGeneration: (ExtractionConfigData) -> Unit
) {
    var count by remember { mutableFloatStateOf(10f) }
    var selectedTypes by remember { mutableStateOf(setOf("SBA")) }
    var stemLength by remember { mutableStateOf("Medium") }
    var customInstructions by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configure AI Extraction", fontWeight = FontWeight.Bold) },
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
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // Extracted Text Preview
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DocumentScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Source Content", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        Text("${extractedText.length} chars", style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        extractedText.take(150) + if(extractedText.length > 150) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Target Context
            if (targetId != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LibraryAddCheck, contentDescription = null, tint = Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Appending to specified collection", fontWeight = FontWeight.Medium, color = Color(0xFF4CAF50))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FiberNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Will create a new collection", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                }
            }

            HorizontalDivider()

            // Configuration Options
            
            // 1. Count
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Target Question Count", fontWeight = FontWeight.Bold)
                    Text("${count.toInt()}", fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = count,
                    onValueChange = { count = it },
                    valueRange = 1f..50f,
                    steps = 49
                )
            }

            // 2. Types
            Column {
                Text("Question Type", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val types = listOf("SBA", "True/False", "Short Answer")
                    types.forEach { type ->
                        FilterChip(
                            selected = selectedTypes.contains(type),
                            onClick = {
                                val current = selectedTypes.toMutableSet()
                                if (current.contains(type) && current.size > 1) {
                                    current.remove(type)
                                } else {
                                    current.add(type)
                                }
                                selectedTypes = current
                            },
                            label = { Text(type) }
                        )
                    }
                }
            }

            // 3. Stem Length
            Column {
                Text("Question Stem Detail Level", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val lengths = listOf("Short", "Medium", "Long (Clinical)")
                    lengths.forEach { len ->
                        OutlinedButton(
                            onClick = { stemLength = len },
                            colors = if (stemLength == len) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) else ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text(len)
                        }
                    }
                }
            }

            // 4. Instructions
            Column {
                Text("Custom Instructions for AI", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = customInstructions,
                    onValueChange = { customInstructions = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("E.g., Include detailed explanations for incorrect distractors, focus mostly on treatments...") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val data = ExtractionConfigData(
                            count = count.toInt(),
                            types = selectedTypes.toList(),
                            stemLength = stemLength,
                            customInstructions = customInstructions,
                            isResearchMode = true
                        )
                        onStartGeneration(data)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.BatchPrediction, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Research & Plan (Iterative)", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val data = ExtractionConfigData(
                            count = count.toInt(),
                            types = selectedTypes.toList(),
                            stemLength = stemLength,
                            customInstructions = customInstructions,
                            isResearchMode = false
                        )
                        onStartGeneration(data)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Quick Generate")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
