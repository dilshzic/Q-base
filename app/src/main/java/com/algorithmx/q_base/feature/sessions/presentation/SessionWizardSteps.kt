package com.algorithmx.q_base.feature.sessions.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.collections.Question

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
            Card(
                onClick = { onSelect(category.name) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(category.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        category.description?.takeIf { it.isNotBlank() }?.let { description ->
                            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
fun SessionSetupStep(
    questions: List<Question>,
    selectedIds: Set<String>,
    lastRandomCount: Int?,
    order: String,
    timingType: String,
    timeLimitSeconds: Int,
    isAdminOnly: Boolean,
    onRandomSelect: (Int) -> Unit,
    onSelectAll: () -> Unit,
    onOrderChange: (String) -> Unit,
    onTimingChange: (String) -> Unit,
    onTimeLimitChange: (Int) -> Unit,
    onIsAdminOnlyChange: (Boolean) -> Unit,
    onLaunch: (String) -> Unit,
    onOpenCustomSelection: () -> Unit
) {
    var title by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Session Title (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        )

        // 1. QUESTION SELECTION
        Column {
            Text("QUESTION AMOUNT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 25, 50).forEach { count ->
                    val isSelected = lastRandomCount == count && selectedIds.size == count
                    FilterChip(
                        selected = isSelected,
                        onClick = { onRandomSelect(count) },
                        label = { Text("$count", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                        modifier = Modifier.weight(1f)
                    )
                }
                val isAllSelected = selectedIds.size == questions.size && questions.isNotEmpty()
                FilterChip(
                    selected = isAllSelected,
                    onClick = onSelectAll,
                    label = { Text("All", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${selectedIds.size} questions selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onOpenCustomSelection) {
                    Text("Custom Selection")
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // 2. ORDER
        Column {
            Text("QUESTION ORDER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ConfigToggle(selected = order == "SEQUENTIAL", label = "Sequential", icon = Icons.Rounded.FormatListNumbered, onClick = { onOrderChange("SEQUENTIAL") }, modifier = Modifier.weight(1f))
                ConfigToggle(selected = order == "RANDOM", label = "Random", icon = Icons.Rounded.Shuffle, onClick = { onOrderChange("RANDOM") }, modifier = Modifier.weight(1f))
            }
        }

        // 3. TIMING
        Column {
            Text("TIMING MODE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConfigToggle(selected = timingType == "NONE", label = "No Timer", icon = null, onClick = { onTimingChange("NONE") }, modifier = Modifier.weight(1f))
                ConfigToggle(selected = timingType == "TOTAL", label = "Total Time", icon = Icons.Rounded.HourglassEmpty, onClick = { onTimingChange("TOTAL") }, modifier = Modifier.weight(1f))
                ConfigToggle(selected = timingType == "PER_QUESTION", label = "Per Q", icon = Icons.Rounded.Timer, onClick = { onTimingChange("PER_QUESTION") }, modifier = Modifier.weight(1f))
            }
            
            if (timingType != "NONE") {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Limit:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("${timeLimitSeconds / 60}m", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                        Slider(
                            value = timeLimitSeconds.toFloat(),
                            onValueChange = { onTimeLimitChange(it.toInt()) },
                            valueRange = 30f..3600f,
                            steps = 59,
                            modifier = Modifier.weight(1f).padding(start = 16.dp)
                        )
                    }
                }
            }
        }

        // 4. ADMIN ONLY
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                    Text("Admin-Only Session", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Only group admins can answer or edit.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = isAdminOnly, onCheckedChange = onIsAdminOnlyChange)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { 
                val finalTitle = title.takeIf { it.isNotBlank() } ?: "Session ${java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(System.currentTimeMillis())}"
                onLaunch(finalTitle) 
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = selectedIds.isNotEmpty(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Launch Session", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
    Card(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) 
                 else androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomSelectionDialog(
    questions: List<Question>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text("Select Questions") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        TextButton(onClick = onSelectAll) { Text("All") }
                        TextButton(onClick = onDeselectAll) { Text("Clear") }
                    }
                )
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(questions) { question ->
                        val isSelected = selectedIds.contains(question.questionId)
                        Card(
                            onClick = { onToggle(question.questionId) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) 
                                else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(checked = isSelected, onCheckedChange = { onToggle(question.questionId) })
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = question.stem,
                                        maxLines = 3,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
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
            }
        }
    }
}