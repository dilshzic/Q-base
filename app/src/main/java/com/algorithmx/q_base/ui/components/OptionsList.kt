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
import androidx.compose.ui.graphics.graphicsLayer
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
            enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                ),
                shape = MaterialTheme.shapes.extraLarge, // Expressive M3 Shape
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "General Explanation",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = explanation ?: "No general explanation available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    
                    if (!references.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "References",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
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
            
            // The database stores correct letters (e.g., "A", "C"). 
            // For MTF: presence in correctAnswers means TRUE, absence means FALSE.
            val isActuallyCorrect = correctAnswers.contains(letter)
            val isActuallyIncorrect = !isActuallyCorrect

            // Expressive Color Tones based on User Requirements
            val targetBackgroundColor = when {
                !isAnswerRevealed -> {
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surface
                }
                isMTF -> {
                    when {
                        // T answer user T OR F answer user F -> Green
                        (isTrueSelected == true && isActuallyCorrect) || 
                        (isTrueSelected == false && isActuallyIncorrect) -> {
                            if (isDarkMode) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFF1F8E9)
                        }
                        // Difference (T answer user F OR F answer user T) -> Red
                        (isTrueSelected != null && ((isTrueSelected == true && isActuallyIncorrect) || (isTrueSelected == false && isActuallyCorrect))) -> {
                            if (isDarkMode) Color(0xFFB71C1C).copy(alpha = 0.2f) else Color(0xFFFFEBEE)
                        }
                        // Any decision missed (T or F) -> Yellow
                        (isTrueSelected == null) -> {
                            if (isDarkMode) Color(0xFFF9A825).copy(alpha = 0.2f) else Color(0xFFFFFDE7)
                        }
                        else -> MaterialTheme.colorScheme.surface
                    }
                }
                else -> { // SBA / MCQ
                    when {
                        isSelected && isActuallyCorrect -> {
                            if (isDarkMode) Color(0xFF1B5E20).copy(alpha = 0.2f) else Color(0xFFF1F8E9)
                        }
                        isSelected && !isActuallyCorrect -> {
                            if (isDarkMode) Color(0xFFB71C1C).copy(alpha = 0.2f) else Color(0xFFFFEBEE)
                        }
                        !isSelected && isActuallyCorrect -> {
                            if (isDarkMode) Color(0xFFF9A825).copy(alpha = 0.2f) else Color(0xFFFFFDE7)
                        }
                        else -> MaterialTheme.colorScheme.surface
                    }
                }
            }

            val targetBorderColor = when {
                !isAnswerRevealed -> {
                    if (isSelected) MaterialTheme.colorScheme.primary 
                    else MaterialTheme.colorScheme.outlineVariant
                }
                isMTF -> {
                    when {
                        (isTrueSelected == true && isActuallyCorrect) || (isTrueSelected == false && isActuallyIncorrect) -> Color(0xFF388E3C)
                        (isTrueSelected != null && ((isTrueSelected == true && isActuallyIncorrect) || (isTrueSelected == false && isActuallyCorrect))) -> Color(0xFFD32F2F)
                        (isTrueSelected == null) -> Color(0xFFFBC02D)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                }
                else -> {
                    when {
                        isSelected && isActuallyCorrect -> Color(0xFF388E3C)
                        isSelected && !isActuallyCorrect -> Color(0xFFD32F2F)
                        !isSelected && isActuallyCorrect -> Color(0xFFFBC02D)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                }
            }

            val animatedBgColor by animateColorAsState(
                targetValue = targetBackgroundColor,
                animationSpec = tween(durationMillis = 400)
            )
            val animatedBorderColor by animateColorAsState(
                targetValue = targetBorderColor,
                animationSpec = tween(durationMillis = 400)
            )

            val scale by animateFloatAsState(
                targetValue = if (isSelected && !isAnswerRevealed) 1.02f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
            )

            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                Surface(
                    onClick = { if (!isAnswerRevealed && !isMTF) onOptionToggled(letter) },
                    shape = MaterialTheme.shapes.large, // Expressive Radius
                    color = animatedBgColor,
                    border = BorderStroke(if (isSelected || isAnswerRevealed) 2.dp else 1.dp, animatedBorderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        },
                    tonalElevation = if (isSelected) 4.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            isMTF -> {
                                TrueFalseToggle(
                                    isTrueSelected = isTrueSelected,
                                    onSelectionChange = { selected -> 
                                        onOptionToggled("${letter}_${if (selected) "T" else "F"}") 
                                    },
                                    enabled = !isAnswerRevealed,
                                    isAnswerRevealed = isAnswerRevealed,
                                    correctAnswer = isActuallyCorrect // if isActuallyCorrect is true, then T is correct.
                                )
                            }
                            isSBA -> {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = if (isAnswerRevealed && isActuallyCorrect) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            else -> {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = if (isAnswerRevealed && isActuallyCorrect) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${option.optionLetter}. ${option.optionText}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )

                        AnimatedVisibility(
                            visible = isAnswerRevealed,
                            enter = scaleIn(initialScale = 0.5f) + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            val userPickedCorrect = if (isMTF) {
                                (isTrueSelected == true && isActuallyCorrect) || (isTrueSelected == false && isActuallyIncorrect)
                            } else {
                                isSelected && isActuallyCorrect
                            }

                            val userPickedIncorrect = if (isMTF) {
                                (isTrueSelected == true && isActuallyIncorrect) || (isTrueSelected == false && isActuallyCorrect)
                            } else {
                                isSelected && !isActuallyCorrect
                            }

                            val userMissedCorrect = if (isMTF) {
                                isTrueSelected == null && isActuallyCorrect
                            } else {
                                !isSelected && isActuallyCorrect
                            }

                            if (userPickedCorrect) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Correct",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(28.dp)
                                )
                            } else if (userPickedIncorrect) {
                                Icon(
                                    Icons.Default.Cancel,
                                    contentDescription = "Incorrect",
                                    tint = Color(0xFFF44336),
                                    modifier = Modifier.size(28.dp)
                                )
                            } else if (userMissedCorrect) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = "Missed",
                                    tint = Color(0xFFFFEB3B),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
                
                // Show option explanation with Expressive Tonal Container
                AnimatedVisibility(
                    visible = isAnswerRevealed,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Icon(
                                Icons.Default.Info, 
                                contentDescription = null, 
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (!option.optionExplanation.isNullOrBlank()) option.optionExplanation else "No specific explanation for this option.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }
    }
}
