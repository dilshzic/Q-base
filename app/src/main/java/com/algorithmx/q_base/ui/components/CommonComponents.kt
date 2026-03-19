package com.algorithmx.q_base.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.entity.MasterCategory
import com.algorithmx.q_base.data.entity.QuestionCollection
import com.algorithmx.q_base.data.entity.StudySession
import java.util.Locale

@Composable
fun CategoryCard(
    category: MasterCategory,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SessionCard(
    session: StudySession,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = session.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Started: ${java.text.SimpleDateFormat("MMM dd", Locale.getDefault()).format(session.createdTimestamp)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun SessionListItem(
    session: StudySession,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = session.title.ifEmpty { "Mock Session: ${session.sessionId.take(5).uppercase()}" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val score = String.format(Locale.getDefault(), "%.0f", session.scoreAchieved)
                Text(
                    text = "Score: $score%",
                    color = if (session.scoreAchieved >= 50) Color(0xFF2E7D32) else Color.Red,
                    fontWeight = FontWeight.Bold
                )
                Text(" | ", color = Color.Gray)
                Text("Started: ${java.text.SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(session.createdTimestamp)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun CollectionItem(collection: QuestionCollection) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(collection.title) },
            supportingContent = { Text(collection.description ?: "No description", maxLines = 1) },
            leadingContent = { Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) }
        )
    }
}

@Composable
fun SectionHeader(title: String, onActionClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text("See All")
            }
        }
    }
}
