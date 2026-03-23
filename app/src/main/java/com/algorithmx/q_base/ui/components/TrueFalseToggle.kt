package com.algorithmx.q_base.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TrueFalseToggle(
    isTrueSelected: Boolean?, // null = no selection, true = T, false = F
    onSelectionChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    isAnswerRevealed: Boolean = false,
    correctAnswer: Boolean? = null
) {
    val trueColor = if (isTrueSelected == true) Color(0xFF4CAF50) else Color.LightGray
    val falseColor = if (isTrueSelected == false) Color(0xFFF44336) else Color.LightGray

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // True Button
        val trueBorder = if (isAnswerRevealed && correctAnswer == true && isTrueSelected != true) BorderStroke(2.dp, Color.Red) else null
        
        Button(
            onClick = { onSelectionChange(true) },
            shape = CircleShape,
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = trueColor,
                contentColor = Color.White,
                disabledContainerColor = trueColor,
                disabledContentColor = Color.White
            ),
            border = trueBorder,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            enabled = enabled
        ) {
            Text("T", fontWeight = FontWeight.Bold)
        }

        // False Button
        val falseBorder = if (isAnswerRevealed && correctAnswer == false && isTrueSelected != false) BorderStroke(2.dp, Color.Red) else null

        Button(
            onClick = { onSelectionChange(false) },
            shape = CircleShape,
            modifier = Modifier.height(36.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = falseColor,
                contentColor = Color.White,
                disabledContainerColor = falseColor,
                disabledContentColor = Color.White
            ),
            border = falseBorder,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            enabled = enabled
        ) {
            Text("F", fontWeight = FontWeight.Bold)
        }
    }
}
