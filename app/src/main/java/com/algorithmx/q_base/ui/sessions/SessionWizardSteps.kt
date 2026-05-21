package com.algorithmx.q_base.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.collections.StudyCollection

@Composable
fun CategoryStep(
    categories: List<StudyCollection>,
    onSelect: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories) { category ->
            Surface(
                onClick = { onSelect(category.name) },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(category.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (!category.description.isNullOrEmpty()) {
                            Text(category.description!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
fun QuestionSelectionStep(
    questions: List<com.algorithmx.q_base.data.collections.Question>,
    selectedIds: Set<String>,
    lastRandomCount: Int?,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onRandomSelect: (Int) -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                "Quick Select",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 25, 50).forEach { count ->
                    val isSelected = lastRandomCount == count
                    FilterChip(
                        selected = isSelected,
                        onClick = { onRandomSelect(count) },
                        label = { Text("$count") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onSelectAll, modifier = Modifier.weight(1f)) {
                Text("Select All (${questions.size})")
            }
            TextButton(onClick = onDeselectAll, modifier = Modifier.weight(1f)) {
                Text("Clear Selection")
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            items(questions) { question ->
                val isSelected = selectedIds.contains(question.questionId)
                Surface(
                    onClick = { onToggle(question.questionId) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = { onToggle(question.questionId) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = question.stem ?: "Untitled Question",
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = question.questionType ?: "SBA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
            enabled = selectedIds.isNotEmpty(),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Configure Session (${selectedIds.size})", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ConfigurationStep(
    order: String,
    timingType: String,
    timeLimitSeconds: Int,
    onOrderChange: (String) -> Unit,
    onTimingChange: (String) -> Unit,
    onTimeLimitChange: (Int) -> Unit,
    selectedCount: Int,
    isAdminOnly: Boolean,
    onIsAdminOnlyChange: (Boolean) -> Unit,
    onLaunch: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Session Title") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        )

        Column {
            Text("QUESTION ORDER", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConfigToggle(selected = order == "SEQUENTIAL", label = "Sequential", icon = Icons.Rounded.History, onClick = { onOrderChange("SEQUENTIAL") }, modifier = Modifier.weight(1f))
                ConfigToggle(selected = order == "RANDOM", label = "Random", icon = Icons.Rounded.AutoAwesome, onClick = { onOrderChange("RANDOM") }, modifier = Modifier.weight(1f))
            }
        }

        Column {
            Text("TIMING MODE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConfigToggle(selected = timingType == "NONE", label = "No Timer", icon = null, onClick = { onTimingChange("NONE") }, modifier = Modifier.weight(1f))
                ConfigToggle(selected = timingType == "TOTAL", label = "Total Time", icon = Icons.Rounded.Timer, onClick = { onTimingChange("TOTAL") }, modifier = Modifier.weight(1f))
                ConfigToggle(selected = timingType == "PER_QUESTION", label = "Per Question", icon = Icons.Rounded.Timer, onClick = { onTimingChange("PER_QUESTION") }, modifier = Modifier.weight(1f))
            }
            
            if (timingType != "NONE") {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Time Limit: ${timeLimitSeconds / 60}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Slider(
                        value = timeLimitSeconds.toFloat(),
                        onValueChange = { onTimeLimitChange(it.toInt()) },
                        valueRange = 30f..3600f,
                        steps = 59,
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Admin-Only Session",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Only group admins can answer, submit, or edit attempts.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isAdminOnly,
                onCheckedChange = onIsAdminOnlyChange
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { 
                val finalTitle = title.takeIf { it.isNotBlank() } ?: "Exam Session ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(System.currentTimeMillis())}"
                onLaunch(finalTitle) 
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = selectedCount > 0,
            shape = MaterialTheme.shapes.large
        ) {
            Text("Launch $selectedCount Questions", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ConfigToggle(
    selected: Boolean,
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}