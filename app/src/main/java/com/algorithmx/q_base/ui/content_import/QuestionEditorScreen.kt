package com.algorithmx.q_base.ui.content_import

import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionEditorScreen(
    questionId: String?,
    setId: String,
    onBack: () -> Unit,
    viewModel: QuestionEditorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    LaunchedEffect(questionId, setId) {
        viewModel.init(questionId, setId)
    }

    if (state.isSaved) {
        LaunchedEffect(Unit) { onBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (questionId == null) "New Question" else "Edit Question", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.getAiAssist() }) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI Assist", tint = MaterialTheme.colorScheme.primary)
                    }
                    Button(
                        onClick = { viewModel.saveQuestion() },
                        enabled = state.stem.isNotBlank() && !state.isSaving,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Save")
                        }
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
                Text("Question Stem", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.stem,
                    onValueChange = { viewModel.updateStem(it) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Enter the question clinical scenario or stem...") }
                )
            }

            item {
                Text("Options", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            items(state.options) { (letter, text) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = state.correctAnswer == letter,
                        onClick = { viewModel.updateCorrectAnswer(letter) }
                    )
                    Text(letter, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { viewModel.updateOption(letter, it) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        placeholder = { Text("Option $letter text") },
                        singleLine = true
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Explanation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = { viewModel.generateAiExplanation() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI Generate", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                if (state.temporaryAiExplanation != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Preview", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(state.temporaryAiExplanation!!, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { viewModel.discardAiExplanation() }) {
                                    Text("Discard", color = MaterialTheme.colorScheme.error)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.applyAiExplanation() },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Text("Apply & Replace")
                                }
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = state.explanation,
                    onValueChange = { viewModel.updateExplanation(it) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Explain why the answer is correct...") }
                )
            }

            item {
                Text("References", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.references,
                    onValueChange = { viewModel.updateReferences(it) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    placeholder = { Text("Source, Textbook, Year") }
                )
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (state.isAiLoading) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("AI is thinking...") },
            text = { 
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator() 
                }
            },
            confirmButton = {}
        )
    }

    if (state.aiSuggestions != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearAiSuggestions() },
            title = { Text("AI Suggestions") },
            text = { Text(state.aiSuggestions!!) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAiSuggestions() }) {
                    Text("Close")
                }
            }
        )
    }
}
