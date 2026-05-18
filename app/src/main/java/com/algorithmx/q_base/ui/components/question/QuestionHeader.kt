package com.algorithmx.q_base.ui.components.question

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Report
import com.algorithmx.q_base.data.collections.Question
import com.algorithmx.q_base.data.collections.QuestionOption
import com.algorithmx.q_base.ui.theme.QbaseTheme

@Composable
fun QuestionHeader(
    question: Question,
    isAnswerRevealed: Boolean,
    selectedAnswers: List<String>,
    correctAnswers: List<String>,
    options: List<QuestionOption>,
    onPinToggled: () -> Unit = {},
    onAddToSet: () -> Unit = {},
    onAddToSession: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCopy: () -> Unit = {},
    onReport: () -> Unit = {},
    isEditable: Boolean = true,
    onEditQuestion: (() -> Unit)? = null
) {
    val type = question.questionType?.trim()?.uppercase()
    val isSBA = type == "SBA"
    val isMTF = type == "MTF" || type == "MCQ" || type == "T/F" || type == "MCQ1"

    var showMenu by remember { mutableStateOf(false) }

    // Metadata Pane
    val metadata = remember(question) {
        mutableListOf<String>().apply {
            question.category?.let { if (it.isNotBlank()) add(it) }
            question.tags?.let { if (it.isNotBlank()) add(it) }
        }
    }

    if (metadata.isNotEmpty() || !isAnswerRevealed) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp)
            ) {
                items(metadata) { info ->
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = info,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Quick Action Buttons aligned with tags
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onPinToggled, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (question.isPinned) Icons.Rounded.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        modifier = Modifier.size(18.dp),
                        tint = if (question.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                /*
                IconButton(onClick = onAddToSet, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.CollectionsBookmark,
                        contentDescription = "Add to Set",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onAddToSession, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = "Add to Session",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                */
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                if (isEditable) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = "More",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Report Content") },
                            onClick = {
                                showMenu = false
                                onReport()
                            },
                            leadingIcon = { Icon(Icons.Rounded.Report, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                        if (isEditable && onEditQuestion != null) {
                            DropdownMenuItem(
                                text = { Text("Edit Question") },
                                onClick = {
                                    showMenu = false
                                    onEditQuestion()
                                },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                            )
                        }
                    }
                }
            }
        }
    }

    val marks = remember(isAnswerRevealed, selectedAnswers, correctAnswers, isSBA, isMTF) {
        if (!isAnswerRevealed) 0f
        else {
            if (isSBA) {
                val userSelection = selectedAnswers.firstOrNull()
                val isCorrect = userSelection != null && correctAnswers.contains(userSelection)
                if (isCorrect) 1f else 0f
            } else if (isMTF) {
                var total = 0f
                options.forEach { option ->
                    val letter = option.optionLetter ?: ""
                    val userValue = when {
                        selectedAnswers.contains("${letter}_T") -> true
                        selectedAnswers.contains("${letter}_F") -> false
                        else -> null
                    }
                    val isActuallyTrue = correctAnswers.contains(letter)
                    
                    if (userValue != null) {
                        if (userValue == isActuallyTrue) total += 1 else total -= 1
                    }
                }
                total.coerceIn(0f, options.size.toFloat())
            } else 0f
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Type: ${question.questionType ?: "Unknown"}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        if (isAnswerRevealed) {
            val markBgColor = if (marks > 0) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            }
            val markTextColor = if (marks > 0) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }

            Surface(
                color = markBgColor,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Marks: $marks",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = markTextColor
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    dev.jeziellago.compose.markdowntext.MarkdownText(
        markdown = question.stem ?: "",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Preview(showBackground = true)
@Composable
fun QuestionHeaderPreview() {
    val question = Question(
        questionId = "1",
        collection = "AI",
        category = "General",
        tags = "LLM",
        questionType = "SBA",
        stem = "What does LLM stand for?"
    )
    val options = listOf(
        QuestionOption(questionId = "1", optionLetter = "A", optionText = "Large Language Model"),
        QuestionOption(questionId = "1", optionLetter = "B", optionText = "Logic Learning Machine")
    )
    
    QbaseTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            QuestionHeader(
                question = question,
                isAnswerRevealed = true,
                selectedAnswers = listOf("A"),
                correctAnswers = listOf("A"),
                options = options
            )
        }
    }
}
