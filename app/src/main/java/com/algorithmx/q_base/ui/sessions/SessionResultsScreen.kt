package com.algorithmx.q_base.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.ui.components.QuestionViewer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionResultsScreen(
    viewModel: SessionResultsViewModel = hiltViewModel(),
    onBackToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reviewState by viewModel.reviewQuestion.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Session Results") })
        }
    ) { padding ->
        when (val state = uiState) {
            is ResultsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ResultsUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your Score",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${state.score.toInt()}%",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = "Question Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Text(
                        text = "Tap a number to review explanations",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.align(Alignment.Start),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(40.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(state.attempts) { index, attempt ->
                            val color = when {
                                attempt.marksObtained >= 3f -> Color(0xFF2E7D32)
                                attempt.marksObtained > 0f -> Color(0xFFFFA500)
                                attempt.attemptStatus == "UNATTEMPTED" -> Color.LightGray
                                else -> Color(0xFFC62828)
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { viewModel.selectQuestionForReview(attempt) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    
                    Button(
                        onClick = onBackToHome,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Return to Home")
                    }
                }
            }
        }
    }

    if (reviewState != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearReview() },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question Review",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { viewModel.clearReview() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                reviewState?.let { review ->
                    QuestionViewer(
                        question = review.question,
                        options = review.options,
                        selectedAnswers = review.attempt.userSelectedAnswers.split(",").filter { it.isNotEmpty() },
                        onOptionToggled = { /* Read only in review */ },
                        isAnswerRevealed = true,
                        correctAnswers = review.answer?.correctAnswerString?.split(",") ?: emptyList(),
                        explanation = review.answer?.generalExplanation,
                        references = review.answer?.references
                    )
                }
            }
        }
    }
}
