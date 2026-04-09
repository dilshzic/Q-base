package com.algorithmx.q_base.ui.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.rounded.PushPin
import com.algorithmx.q_base.data.collections.Question
import com.algorithmx.q_base.data.collections.QuestionOption

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
    onAskAi: () -> Unit = {}
) {
    val isDarkMode = isSystemInDarkTheme()
    val type = question.questionType?.trim()?.uppercase()
    val isSBA = type == "SBA"
    val isMTF = type == "MTF" || type == "MCQ" || type == "T/F" || type == "MCQ1"

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
                        imageVector = if (question.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        modifier = Modifier.size(18.dp),
                        tint = if (question.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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
                        imageVector = Icons.Rounded.PlaylistAdd,
                        contentDescription = "Add to Session",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
                IconButton(onClick = onAskAi, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = "Ask AI",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
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
                if (isCorrect) 1f else 0f // SBA is usually 1 mark or weighted, following +1 logic
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
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        if (isAnswerRevealed) {
            val markBgColor = if (marks > 0) {
                if (isDarkMode) Color(0xFF1B5E20) else Color(0xFFE8F5E9)
            } else {
                if (isDarkMode) Color(0xFFB71C1C) else Color(0xFFFFEBEE)
            }
            val markTextColor = if (marks > 0) {
                if (isDarkMode) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
            } else {
                if (isDarkMode) Color(0xFFEF9A9A) else Color(0xFFC62828)
            }

            Surface(
                color = markBgColor,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Marks: $marks",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = markTextColor
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = question.stem ?: "",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium
    )
}
