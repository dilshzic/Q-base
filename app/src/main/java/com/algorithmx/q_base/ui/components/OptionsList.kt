package com.algorithmx.q_base.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
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
fun OptionsList(
    question: Question,
    options: List<QuestionOption>,
    selectedAnswers: List<String>,
    onOptionToggled: (String) -> Unit,
    isAnswerRevealed: Boolean,
    correctAnswers: List<String>,
    explanation: String?,
    references: String?,
    onCheckAnswer: (() -> Unit)?
) {
    val isDarkMode = isSystemInDarkTheme()
    val type = question.questionType?.trim()?.uppercase()
    val isSBA = type == "SBA"
    val isMTF = type == "MTF" || type == "MCQ" || type == "T/F" || type == "MCQ1"

    Column {
        // Expressive Animation for General Explanation
        AnimatedVisibility(
            visible = isAnswerRevealed,
            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
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

            val targetBackgroundColor = when {
                !isAnswerRevealed -> {
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surface
                }
                isCorrect -> {
                    if (isDarkMode) Color(0xFF1B5E20).copy(alpha = 0.6f)
                    else Color(0xFFE8F5E9)
                }
                isSelected && !isCorrect -> {
                    if (isDarkMode) Color(0xFFB71C1C).copy(alpha = 0.6f)
                    else Color(0xFFFFEBEE)
                }
                else -> MaterialTheme.colorScheme.surface
            }

            val targetBorderColor = when {
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

            val animatedBgColor by animateColorAsState(
                targetValue = targetBackgroundColor,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
            val animatedBorderColor by animateColorAsState(
                targetValue = targetBorderColor,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )

            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Surface(
                    onClick = { if (!isAnswerRevealed && !isMTF) onOptionToggled(letter) },
                    shape = MaterialTheme.shapes.medium,
                    color = animatedBgColor,
                    border = BorderStroke(1.dp, animatedBorderColor),
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
                                    onSelectionChange = { selected -> 
                                        onOptionToggled("${letter}_${if (selected) "T" else "F"}") 
                                    },
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
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )

                        AnimatedVisibility(
                            visible = isAnswerRevealed,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            if (isCorrect) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Correct",
                                    tint = if (isDarkMode) Color(0xFF81C784) else Color(0xFF4CAF50)
                                )
                            } else if (isSelected) {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = "Incorrect",
                                    tint = if (isDarkMode) Color(0xFFE57373) else Color(0xFFF44336)
                                )
                            }
                        }
                    }
                }
                
                // Show option explanation or placeholder with Animation
                AnimatedVisibility(
                    visible = isAnswerRevealed,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
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
                                text = if (!option.optionExplanation.isNullOrBlank()) option.optionExplanation else "Explanation not available for this option.",
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
                enabled = true
            ) {
                Text("Check Answer")
            }
        }
    }
}
