package com.algorithmx.q_base.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionOption

@Composable
fun TrueFalseToggle(
    isTrueSelected: Boolean?, // null = no selection, true = T, false = F
    onSelectionChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // True Button
        Button(
            onClick = { onSelectionChange(true) },
            shape = CircleShape,
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTrueSelected == true) Color(0xFF4CAF50) else Color.LightGray
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            enabled = enabled
        ) {
            Text("T", color = Color.White, fontWeight = FontWeight.Bold)
        }

        // False Button
        Button(
            onClick = { onSelectionChange(false) },
            shape = CircleShape,
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTrueSelected == false) Color(0xFFF44336) else Color.LightGray
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            enabled = enabled
        ) {
            Text("F", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

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
    val isSBA = question.questionType == "SBA"
    val isMTF = question.questionType == "MTF"
    val isDarkMode = isSystemInDarkTheme()

    // Marking Logic
    val marks = remember(isAnswerRevealed, selectedAnswers, correctAnswers, isSBA, isMTF) {
        if (!isAnswerRevealed) 0f
        else {
            if (isSBA) {
                val userSelection = selectedAnswers.firstOrNull()
                val isCorrect = userSelection != null && correctAnswers.contains(userSelection)
                if (isCorrect) 3f else 0f
            } else if (isMTF) {
                var total = 0f
                options.forEach { option ->
                    val letter = option.optionLetter ?: ""
                    val userIsT = selectedAnswers.contains("${letter}_T")
                    val userIsF = selectedAnswers.contains("${letter}_F")
                    val correctIsT = correctAnswers.contains("${letter}_T")
                    val correctIsF = correctAnswers.contains("${letter}_F")

                    if (userIsT || userIsF) {
                        val isCorrect = (userIsT && correctIsT) || (userIsF && correctIsF)
                        if (isCorrect) total += 1 else total -= 1
                    }
                }
                total.coerceIn(0f, 5f)
            } else 0f
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
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

        Spacer(modifier = Modifier.height(24.dp))

        options.forEach { option ->
            val letter = option.optionLetter ?: ""
            
            val isTrueSelected = if (isMTF) {
                when {
                    selectedAnswers.contains("${letter}_T") -> true
                    selectedAnswers.contains("${letter}_F") -> false
                    else -> null
                }
            } else null
            
            val isSelected = if (isMTF) isTrueSelected != null else selectedAnswers.contains(letter)
            
            val isCorrect = if (isMTF) {
                val correctIsT = correctAnswers.contains("${letter}_T")
                val correctIsF = correctAnswers.contains("${letter}_F")
                (isTrueSelected == true && correctIsT) || (isTrueSelected == false && correctIsF)
            } else {
                correctAnswers.contains(letter)
            }

            // Theme-aware colors for options
            val backgroundColor = when {
                !isAnswerRevealed -> {
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surface
                }
                isCorrect -> {
                    if (isDarkMode) Color(0xFF1B5E20).copy(alpha = 0.6f) // Darker Green
                    else Color(0xFFE8F5E9) // Soft Green
                }
                isSelected && !isCorrect -> {
                    if (isDarkMode) Color(0xFFB71C1C).copy(alpha = 0.6f) // Darker Red
                    else Color(0xFFFFEBEE) // Soft Red
                }
                else -> MaterialTheme.colorScheme.surface
            }

            val borderColor = when {
                !isAnswerRevealed -> {
                    if (isSelected) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.outlineVariant
                }
                isCorrect -> {
                    if (isDarkMode) Color(0xFF81C784) else Color(0xFF4CAF50)
                }
                isSelected && !isCorrect -> {
                    if (isDarkMode) Color(0xFFE57373) else Color(0xFFF44336)
                }
                else -> MaterialTheme.colorScheme.outlineVariant
            }

            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Surface(
                    onClick = { if (!isAnswerRevealed && !isMTF) onOptionToggled(letter) },
                    shape = MaterialTheme.shapes.medium,
                    color = backgroundColor,
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            isMTF -> {
                                TrueFalseToggle(
                                    isTrueSelected = isTrueSelected,
                                    onSelectionChange = { onOptionToggled("${letter}_${if (it) "T" else "F"}") },
                                    enabled = !isAnswerRevealed
                                )
                            }
                            isSBA -> {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = if (isAnswerRevealed && isCorrect) {
                                            if (isDarkMode) Color(0xFF81C784) else Color(0xFF4CAF50)
                                        } else MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            else -> {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = if (isAnswerRevealed && isCorrect) {
                                            if (isDarkMode) Color(0xFF81C784) else Color(0xFF4CAF50)
                                        } else MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${option.optionLetter}. ${option.optionText}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // Per-option explanation
                AnimatedVisibility(visible = isAnswerRevealed && !option.optionExplanation.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Row(modifier = Modifier.padding(8.dp)) {
                            Icon(
                                Icons.Default.Info, 
                                contentDescription = null, 
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option.optionExplanation ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        if (onCheckAnswer != null && !isAnswerRevealed) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onCheckAnswer,
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedAnswers.isNotEmpty()
            ) {
                Text("Check Answer")
            }
        }

        if (isAnswerRevealed) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "General Explanation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = explanation ?: "No general explanation available.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    if (!references.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "References",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = references,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}
