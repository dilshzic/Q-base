package com.algorithmx.q_base.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionOption

@Composable
fun QuestionViewer(
    question: Question,
    options: List<QuestionOption>,
    selectedAnswers: List<String>,
    onOptionToggled: (String) -> Unit,
    modifier: Modifier = Modifier,
    isAnswerRevealed: Boolean = false,
    correctAnswers: List<String> = emptyList(),
    explanation: String? = null,
    references: String? = null,
    onCheckAnswer: (() -> Unit)? = null
) {
    val scrollState = rememberScrollState()

    // Unified vertical layout for all screen sizes
    // Centered horizontally with a maximum width for readability on large displays
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 800.dp) // Limit width on large screens for better readability
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            QuestionHeader(
                question = question,
                isAnswerRevealed = isAnswerRevealed,
                selectedAnswers = selectedAnswers,
                correctAnswers = correctAnswers,
                options = options
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OptionsList(
                question = question,
                options = options,
                selectedAnswers = selectedAnswers,
                onOptionToggled = onOptionToggled,
                isAnswerRevealed = isAnswerRevealed,
                correctAnswers = correctAnswers,
                explanation = explanation,
                references = references,
                onCheckAnswer = onCheckAnswer
            )
            
            // Extra padding at the bottom for better scrolling experience
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
