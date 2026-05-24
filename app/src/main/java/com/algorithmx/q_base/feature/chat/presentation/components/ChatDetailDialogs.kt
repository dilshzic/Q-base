package com.algorithmx.q_base.feature.chat.presentation.components
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.feature.sessions.data.StudySession
import com.algorithmx.q_base.core.data.chat.MessageEntity
import com.algorithmx.q_base.core.designsystem.components.reusable.ReportDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionPickerSheet(
    sessions: List<StudySession>,
    onDismiss: () -> Unit,
    onSessionSelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Share Session",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(24.dp)
            )

            if (sessions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No sessions to share", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    sessions.forEach { session ->
                        item {
                            Surface(
                                onClick = { onSessionSelected(session.sessionId) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(session.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("Active Session", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionPickerSheet(
    collections: List<StudyCollection>,
    onDismiss: () -> Unit,
    onCollectionSelected: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Share Collection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(24.dp)
            )

            if (collections.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("No collections to share", color = MaterialTheme.colorScheme.outline)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    collections.forEach { collection ->
                        item {
                            Surface(
                                onClick = { onCollectionSelected(collection.collectionId) },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Transparent
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.CollectionsBookmark, 
                                        tint = MaterialTheme.colorScheme.primary,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(collection.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        collection.description?.let { desc: String -> 
                                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, maxLines = 1) 
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatDetailConfirmDialogs(
    showClearConfirm: Boolean,
    onClearDismiss: () -> Unit,
    onClearConfirm: () -> Unit,
    showDeleteConfirm: Boolean,
    onDeleteDismiss: () -> Unit,
    onDeleteConfirm: () -> Unit,
    reportingMessage: MessageEntity?,
    onReportMessageDismiss: () -> Unit,
    onReportMessageConfirm: (String) -> Unit,
    showReportGroupDialog: Boolean,
    groupName: String,
    onReportGroupDismiss: () -> Unit,
    onReportGroupConfirm: (String) -> Unit
) {
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = onClearDismiss,
            title = { Text("Clear Chat History?") },
            text = { Text("This will permanently remove all messages from this chat for all participants.") },
            confirmButton = {
                TextButton(
                    onClick = onClearConfirm,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = onClearDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = onDeleteDismiss,
            title = { Text("Delete Chat?") },
            text = { Text("This will permanently delete this chat and its history for everyone.") },
            confirmButton = {
                TextButton(
                    onClick = onDeleteConfirm,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    reportingMessage?.let { message ->
        ReportDialog(
            itemType = "Message",
            itemName = message.payload.take(20) + if(message.payload.length > 20) "..." else "",
            onDismiss = onReportMessageDismiss,
            onConfirm = onReportMessageConfirm
        )
    }

    if (showReportGroupDialog) {
        ReportDialog(
            itemType = "Group",
            itemName = groupName,
            onDismiss = onReportGroupDismiss,
            onConfirm = onReportGroupConfirm
        )
    }
}