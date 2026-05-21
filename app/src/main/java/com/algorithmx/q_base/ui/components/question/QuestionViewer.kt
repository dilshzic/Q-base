package com.algorithmx.q_base.ui.components.question

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.collections.Question
import com.algorithmx.q_base.data.collections.QuestionOption
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import com.algorithmx.q_base.ui.theme.QbaseTheme

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
    onCheckAnswer: (() -> Unit)? = null,
    onPinToggled: () -> Unit = {},
    onAddToSet: () -> Unit = {},
    onAddToSession: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCopy: () -> Unit = {},
    showHeader: Boolean = true,
    isEditable: Boolean = true,
    onEditQuestion: (() -> Unit)? = null,
    onAskAi: (() -> Unit)? = null
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
            if (showHeader) {
                QuestionHeader(
                    question = question,
                    isAnswerRevealed = isAnswerRevealed,
                    selectedAnswers = selectedAnswers,
                    correctAnswers = correctAnswers,
                    options = options,
                    onPinToggled = onPinToggled,
                    onAddToSet = onAddToSet,
                    onAddToSession = onAddToSession,
                    onDelete = onDelete,
                    onCopy = onCopy,
                    isEditable = isEditable,
                    onEditQuestion = onEditQuestion
                )
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                // When header is hidden, we might still want the stem and type
                Text(
                    text = "Type: ${question.questionType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                dev.jeziellago.compose.markdowntext.MarkdownText(
                    markdown = question.stem,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
            
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

            if (onAskAi != null) {
                Spacer(modifier = Modifier.height(24.dp))
                FilledTonalButton(
                    onClick = onAskAi,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ask AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (onCheckAnswer != null && !isAnswerRevealed) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onCheckAnswer,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    contentPadding = PaddingValues(16.dp),
                    enabled = true
                ) {
                    Text(
                        text = "Check Answer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Extra padding at the bottom for better scrolling experience
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun QuestionViewerPreview() {
    val question = Question(
        questionId = "ai_1",
        collection = "AI & Technology",
        category = "Machine Learning",
        tags = "AI, LLM",
        questionType = "MTF",
        stem = "What does 'LLM' stand for in the context of Artificial Intelligence?"
    )
    val options = listOf(
        QuestionOption(questionId = "ai_1", optionLetter = "A", optionText = "Large Logical Model", optionExplanation = "Logical models refer to symbolic AI, not LLMs."),
        QuestionOption(questionId = "ai_1", optionLetter = "B", optionText = "Large Language Model", optionExplanation = "Correct! LLMs are trained on massive text datasets."),
        QuestionOption(questionId = "ai_1", optionLetter = "C", optionText = "Linear Learning Machine", optionExplanation = "This is a fabricated term."),
        QuestionOption(questionId = "ai_1", optionLetter = "D", optionText = "Limited Linguistic Memory", optionExplanation = "LLMs are characterized by vast, not limited, capabilities."),
        QuestionOption(questionId = "ai_1", optionLetter = "E", optionText = "Latent Layer Matrix", optionExplanation = "While matrices are used in neural networks, this is not what LLM stands for.")
    )

    QbaseTheme {
        QuestionViewer(
            question = question,
            options = options,
            selectedAnswers = listOf("B"),
            onOptionToggled = {},
            isAnswerRevealed = true,
            correctAnswers = listOf("B"),
            explanation = "LLM stands for **Large Language Model**. These models utilize deep learning techniques and massive datasets to understand, summarize, and generate text.",
            onCheckAnswer = {}
        )
    }
}