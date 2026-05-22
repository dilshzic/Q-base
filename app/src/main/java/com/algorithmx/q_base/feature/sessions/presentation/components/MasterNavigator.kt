package com.algorithmx.q_base.feature.sessions.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algorithmx.q_base.feature.sessions.NavigatorDot
import com.algorithmx.q_base.feature.theme.warningOrange

@Composable
fun MasterNavigator(
    dots: List<NavigatorDot>,
    onQuestionClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Master Navigator",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Quickly jump between questions and review status.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(56.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(max = 450.dp)
        ) {
            itemsIndexed(dots) { index, dot ->
                val backgroundColor = when (dot.status) {
                    "ATTEMPTED" -> MaterialTheme.colorScheme.primaryContainer
                    "FLAGGED" -> warningOrange.copy(alpha = 0.1f)
                    "FINALIZED" -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
                
                val contentColor = when (dot.status) {
                    "ATTEMPTED" -> MaterialTheme.colorScheme.onPrimaryContainer
                    "FLAGGED" -> warningOrange
                    "FINALIZED" -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
                
                val borderColor = when {
                    dot.isSelected -> MaterialTheme.colorScheme.primary
                    dot.status == "FLAGGED" -> warningOrange
                    else -> MaterialTheme.colorScheme.outlineVariant
                }

                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            scaleX = if (dot.isSelected) { 1.1f } else 1f
                            scaleY = if (dot.isSelected) { 1.1f } else 1f
                        },
                    onClick = { onQuestionClick(index) },
                    shape = RoundedCornerShape(16.dp),
                    color = backgroundColor,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (dot.isSelected) 3.dp else 1.dp,
                        color = borderColor
                    ),
                    tonalElevation = if (dot.isSelected) 4.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (index + 1).toString(),
                            color = contentColor,
                            fontWeight = if (dot.isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}