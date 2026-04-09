package com.algorithmx.q_base.ui.content_import

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.data.collections.Question

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualBuilderScreen(
    targetId: String?,
    name: String? = null,
    onBack: () -> Unit,
    onAddQuestion: (String) -> Unit, // passes setId
    onEditQuestion: (String, String) -> Unit, // passes questionId, setId
    viewModel: ManualBuilderViewModel = hiltViewModel()
) {
    val questions by viewModel.questions.collectAsStateWithLifecycle()
    val targetName by viewModel.targetName.collectAsStateWithLifecycle()

    LaunchedEffect(targetId, name) {
        viewModel.initialize(targetId, name)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Manual Builder", style = MaterialTheme.typography.titleMedium)
                        Text(
                            targetName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = onBack,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Finish")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { 
                    viewModel.getTargetSetId()?.let { onAddQuestion(it) }
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Question") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { padding ->
        if (questions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.QuestionMark,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No questions added yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Tap the button below to start building.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(questions) { question ->
                    ManualQuestionItem(
                        question = question,
                        onClick = { 
                            viewModel.getTargetSetId()?.let { setId ->
                                onEditQuestion(question.questionId, setId)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualQuestionItem(
    question: Question,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = question.stem ?: "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        text = question.questionType ?: "SBA",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = question.category ?: "Manual Entry",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
