package com.algorithmx.q_base.feature.chat.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminReviewPanelView(
    accessRequests: List<Map<String, Any>>,
    groupReports: List<Map<String, Any>>,
    messageReports: List<Map<String, Any>>,
    onGrantAccess: (String, String) -> Unit,
    onDenyAccess: (String, String) -> Unit,
    onDismissGroupReport: (String) -> Unit,
    onDismissMessageReport: (String) -> Unit,
    onDeleteMessage: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalCount = accessRequests.size + groupReports.size + messageReports.size

    if (totalCount == 0) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "All Clear!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No pending access requests or moderation reports for this group.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "Admin Dashboard",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Manage access requests and moderation reports for this group.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // SECTION 1: Access Requests
            if (accessRequests.isNotEmpty()) {
                item {
                    AdminSectionHeader(
                        title = "Access Requests",
                        icon = Icons.Rounded.Key,
                        count = accessRequests.size
                    )
                }
                items(accessRequests) { request ->
                    val collectionId = request["collectionId"] as? String ?: ""
                    val requesterId = request["requesterId"] as? String ?: ""
                    val requesterName = request["requesterName"] as? String ?: "Someone"
                    val collectionName = request["collectionName"] as? String ?: "Collection"
                    
                    AccessRequestItem(
                        requesterName = requesterName,
                        collectionName = collectionName,
                        onApprove = { onGrantAccess(collectionId, requesterId) },
                        onDeny = { onDenyAccess(collectionId, requesterId) }
                    )
                }
            }

            // SECTION 2: Message Reports
            if (messageReports.isNotEmpty()) {
                item {
                    AdminSectionHeader(
                        title = "Reported Messages",
                        icon = Icons.Rounded.Report,
                        count = messageReports.size
                    )
                }
                items(messageReports) { report ->
                    val reportId = report["\$id"] as? String ?: ""
                    val reporterId = report["reporterId"] as? String ?: ""
                    val reason = report["reason"] as? String ?: "No reason provided"
                    val reportedAt = (report["reportedAt"] as? Number)?.toLong() ?: 0L
                    
                    // Parse contentJson for message details
                    val contentJson = report["contentJson"] as? String ?: ""
                    var messageId = ""
                    var senderId = ""
                    var payload = ""
                    var timestamp = 0L
                    try {
                        val jsonObj = org.json.JSONObject(contentJson)
                        messageId = jsonObj.optString("messageId", "")
                        senderId = jsonObj.optString("senderId", "Unknown")
                        payload = jsonObj.optString("payload", "")
                        timestamp = jsonObj.optLong("timestamp", 0L)
                    } catch (_: Exception) {}

                    ReportedMessageItem(
                        senderName = senderId,
                        payload = payload,
                        reason = reason,
                        timestamp = timestamp,
                        reportedAt = reportedAt * 1000, // Appwrite uses epoch seconds
                        onDismiss = { onDismissMessageReport(reportId) },
                        onDelete = { onDeleteMessage(messageId, reportId) }
                    )
                }
            }

            // SECTION 3: Group Reports
            if (groupReports.isNotEmpty()) {
                item {
                    AdminSectionHeader(
                        title = "Group Reports",
                        icon = Icons.Rounded.Group,
                        count = groupReports.size
                    )
                }
                items(groupReports) { report ->
                    val reportId = report["\$id"] as? String ?: ""
                    val reporterId = report["reporterId"] as? String ?: ""
                    val reason = report["reason"] as? String ?: "No reason provided"
                    val reportedAt = (report["reportedAt"] as? Number)?.toLong() ?: 0L

                    GroupReportItem(
                        reporterName = reporterId,
                        reason = reason,
                        reportedAt = reportedAt * 1000,
                        onDismiss = { onDismissGroupReport(reportId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminSectionHeader(
    title: String,
    icon: ImageVector,
    count: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        Badge(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(text = count.toString(), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ReportedMessageItem(
    senderName: String,
    payload: String,
    reason: String,
    timestamp: Long,
    reportedAt: Long,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val formattedMsgTime = remember(timestamp) {
        if (timestamp > 0) SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp)) else ""
    }
    val formattedReportTime = remember(reportedAt) {
        if (reportedAt > 0) SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(reportedAt)) else ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Reported message bubble representation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Message,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Message from: $senderName",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    if (formattedMsgTime.isNotEmpty()) {
                        Text(
                            text = formattedMsgTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = payload,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(12.dp))

            // Report details
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Rounded.Gavel,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Report Reason:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (formattedReportTime.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Reported at $formattedReportTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss Report", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Message")
                }
            }
        }
    }
}

@Composable
fun GroupReportItem(
    reporterName: String,
    reason: String,
    reportedAt: Long,
    onDismiss: () -> Unit
) {
    val formattedReportTime = remember(reportedAt) {
        if (reportedAt > 0) SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(reportedAt)) else ""
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Group reported by $reporterName",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reason: $reason",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (formattedReportTime.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Reported at $formattedReportTime",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Dismiss Report")
                }
            }
        }
    }
}
