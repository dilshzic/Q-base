package com.algorithmx.q_base.ui.sessions

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionOption
import com.algorithmx.q_base.data.entity.SessionAttempt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    viewModel: ActiveSessionViewModel,
    onNavigateBack: () -> Unit
) {
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val options by viewModel.currentOptions.collectAsState()
    val attempts by viewModel.attempts.collectAsState()
    val currentIndex by viewModel.currentQuestionIndex.collectAsState()
    
    var showNavigator by remember { mutableStateOf(false) }
    val currentAttempt = attempts.getOrNull(currentIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Question ${currentIndex + 1} of ${attempts.size}") },
                actions = {
                    IconButton(onClick = { viewModel.toggleFlag() }) {
                        Icon(
                            imageVector = if (currentAttempt?.attemptStatus == "FLAGGED") Icons.Default.Flag else Icons.Default.OutlinedFlag,
                            contentDescription = "Flag Question",
                            tint = if (currentAttempt?.attemptStatus == "FLAGGED") Color(0xFFFFA500) else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { showNavigator = true }) {
                        Icon(Icons.Default.GridView, contentDescription = "Master Navigator")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { viewModel.navigateToQuestion(currentIndex - 1) },
                        enabled = currentIndex > 0
                    ) {
                        Text("Previous")
                    }
                    Button(
                        onClick = { viewModel.navigateToQuestion(currentIndex + 1) },
                        enabled = currentIndex < attempts.size - 1
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            currentQuestion?.let { question ->
                Text(
                    text = question.stem ?: "No question stem available",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                options.forEach { option ->
                    val isSelected = currentAttempt?.userSelectedAnswers?.split(",")?.contains(option.optionLetter ?: "") == true
                    
                    Surface(
                        onClick = { viewModel.onAnswerSelected(option.optionLetter ?: "") },
                        shape = MaterialTheme.shapes.medium,
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp, 
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (question.questionType == "SBA") {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null
                                )
                            } else {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = option.optionText ?: "")
                        }
                    }
                }
            }
        }
    }

    if (showNavigator) {
        ModalBottomSheet(onDismissRequest = { showNavigator = false }) {
            MasterNavigator(
                attempts = attempts,
                currentIndex = currentIndex,
                onQuestionClick = { index ->
                    viewModel.navigateToQuestion(index)
                    showNavigator = false
                }
            )
        }
    }
}

@Composable
fun MasterNavigator(
    attempts: List<SessionAttempt>,
    currentIndex: Int,
    onQuestionClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Master Navigator",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(48.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            itemsIndexed(attempts) { index, attempt ->
                val backgroundColor = when (attempt.attemptStatus) {
                    "ATTEMPTED" -> MaterialTheme.colorScheme.primary
                    "FLAGGED" -> Color(0xFFFFA500)
                    "FINALIZED" -> Color.DarkGray
                    else -> Color.Transparent
                }
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(backgroundColor)
                        .border(
                            width = if (index == currentIndex) 3.dp else 1.dp,
                            color = if (index == currentIndex) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable { onQuestionClick(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (index + 1).toString(),
                        color = if (attempt.attemptStatus == "UNATTEMPTED") 
                                    MaterialTheme.colorScheme.onSurface 
                                else Color.White,
                        fontWeight = if (index == currentIndex) FontWeight.ExtraBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
