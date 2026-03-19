package com.algorithmx.q_base.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.ui.components.QuestionViewer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    viewModel: ActiveSessionViewModel,
    onNavigateBack: () -> Unit,
    onViewResults: (String) -> Unit
) {
    val currentQuestion by viewModel.currentQuestion.collectAsStateWithLifecycle()
    val options by viewModel.currentOptions.collectAsStateWithLifecycle()
    val attempts by viewModel.attempts.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val timerText by viewModel.timerDisplay.collectAsStateWithLifecycle()
    val navDots by viewModel.navigatorDots.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val currentAnswer by viewModel.currentAnswer.collectAsStateWithLifecycle()
    
    var showNavigator by remember { mutableStateOf(false) }
    val currentAttempt = attempts.getOrNull(currentIndex)
    val isCompleted = session?.isCompleted == true

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                TopAppBar(
                    title = {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            if (isCompleted) {
                                Text(text = "Review Mode", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Text(text = "Time: $timerText", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (!isCompleted) {
                            TextButton(onClick = { 
                                viewModel.submitSession()
                            }) {
                                Text("[ Submit ]", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            IconButton(onClick = { onViewResults(viewModel.getSessionId()) }) {
                                Icon(Icons.Default.Assessment, contentDescription = "View Results")
                            }
                        }
                    }
                )
                
                // Inline Master Navigator Section
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showNavigator = true }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Master Navigator", 
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown, 
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(navDots) { _, dot ->
                            StatusDot(
                                status = dot.status,
                                isSelected = dot.isSelected,
                                onClick = { viewModel.navigateToQuestion(dot.index) }
                            )
                        }
                    }
                }
                HorizontalDivider()
            }
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
                    TextButton(
                        onClick = { viewModel.navigateToQuestion(currentIndex - 1) },
                        enabled = currentIndex > 0
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous")
                    }
                    TextButton(
                        onClick = { viewModel.navigateToQuestion(currentIndex + 1) },
                        enabled = currentIndex < attempts.size - 1
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            currentQuestion?.let { question ->
                Text(
                    text = "Question ${currentIndex + 1} of ${attempts.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                QuestionViewer(
                    question = question,
                    options = options,
                    selectedAnswers = currentAttempt?.userSelectedAnswers?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
                    onOptionToggled = { viewModel.onAnswerSelected(it) },
                    isAnswerRevealed = isCompleted,
                    correctAnswers = currentAnswer?.correctAnswerString?.split(",") ?: emptyList(),
                    explanation = currentAnswer?.generalExplanation,
                    references = currentAnswer?.references
                )
            }
        }
    }

    if (showNavigator) {
        ModalBottomSheet(onDismissRequest = { showNavigator = false }) {
            MasterNavigator(
                dots = navDots,
                onQuestionClick = { index ->
                    viewModel.navigateToQuestion(index)
                    showNavigator = false
                }
            )
        }
    }
}

@Composable
fun StatusDot(status: String, isSelected: Boolean, onClick: () -> Unit) {
    val color = when (status) {
        "ATTEMPTED" -> Color(0xFF2196F3)
        "FLAGGED" -> Color(0xFFFFA500)
        "FINALIZED" -> Color.DarkGray
        else -> Color(0xFFE0E0E0)
    }
    
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier
            )
            .clickable { onClick() }
    )
}

@Composable
fun MasterNavigator(
    dots: List<NavigatorDot>,
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
            itemsIndexed(dots) { index, dot ->
                val backgroundColor = when (dot.status) {
                    "ATTEMPTED" -> Color(0xFF2196F3)
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
                            width = if (dot.isSelected) 3.dp else 1.dp,
                            color = if (dot.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable { onQuestionClick(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (index + 1).toString(),
                        color = if (dot.status == "UNATTEMPTED") 
                                    MaterialTheme.colorScheme.onSurface 
                                else Color.White,
                        fontWeight = if (dot.isSelected) FontWeight.ExtraBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
