package com.algorithmx.q_base.core.designsystem.components.question

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.feature.theme.*

@Composable
fun TrueFalseToggle(
    isTrueSelected: Boolean?, // null = no selection, true = T, false = F
    onSelectionChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    isAnswerRevealed: Boolean = false,
    correctAnswer: Boolean? = null
) {
    val trueColor = if (isTrueSelected == true) successGreen else MaterialTheme.colorScheme.outlineVariant
    val falseColor = if (isTrueSelected == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // True Button
        val trueBorder = if (isAnswerRevealed && correctAnswer == true && isTrueSelected != true) BorderStroke(2.dp, MaterialTheme.colorScheme.error) else null
        
        Button(
            onClick = { onSelectionChange(true) },
            shape = CircleShape,
            modifier = Modifier.size(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = trueColor,
                contentColor = if (isTrueSelected == true) Color.White else MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = trueColor,
                disabledContentColor = if (isTrueSelected == true) Color.White else MaterialTheme.colorScheme.onSurface
            ),
            border = trueBorder,
            contentPadding = PaddingValues(0.dp),
            enabled = enabled
        ) {
            Text("T", fontWeight = FontWeight.Bold)
        }

        // False Button
        val falseBorder = if (isAnswerRevealed && correctAnswer == false && isTrueSelected != false) BorderStroke(2.dp, MaterialTheme.colorScheme.error) else null

        Button(
            onClick = { onSelectionChange(false) },
            shape = CircleShape,
            modifier = Modifier.size(40.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = falseColor,
                contentColor = if (isTrueSelected == false) Color.White else MaterialTheme.colorScheme.onSurface,
                disabledContainerColor = falseColor,
                disabledContentColor = if (isTrueSelected == false) Color.White else MaterialTheme.colorScheme.onSurface
            ),
            border = falseBorder,
            contentPadding = PaddingValues(0.dp),
            enabled = enabled
        ) {
            Text("F", fontWeight = FontWeight.Bold)
        }
    }
}