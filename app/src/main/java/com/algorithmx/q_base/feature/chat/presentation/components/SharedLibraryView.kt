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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.collections.StudyCollection
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.algorithmx.q_base.core.designsystem.theme.QbaseTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SharedLibraryView(
    chatId: String,
    collections: List<Map<String, Any>>,
    onImport: (String) -> Unit,
    onResend: (String) -> Unit,
    localCollections: List<StudyCollection>,
    isAdmin: Boolean,
    accessRequests: List<Map<String, Any>>,
    onRequestAccess: (String) -> Unit,
    onGrantAccess: (String, String) -> Unit,
    onDenyAccess: (String, String) -> Unit,
    onNavigateToCollection: (String) -> Unit,
    onDelete: (String) -> Unit,
    currentUserId: String = "",
    isLoading: Boolean = false
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            CollectionsTabContent(
                chatId = chatId,
                collections = collections,
                onImport = onImport,
                onResend = onResend,
                localCollections = localCollections,
                isAdmin = isAdmin,
                accessRequests = accessRequests,
                onRequestAccess = onRequestAccess,
                onGrantAccess = onGrantAccess,
                onDenyAccess = onDenyAccess,
                onNavigateToCollection = onNavigateToCollection,
                onDelete = onDelete,
                currentUserId = currentUserId,
                isLoading = isLoading
            )
        }
    }
}

@Composable
fun CollectionsTabContent(
    chatId: String,
    collections: List<Map<String, Any>>,
    onImport: (String) -> Unit,
    onResend: (String) -> Unit,
    localCollections: List<StudyCollection>,
    isAdmin: Boolean,
    accessRequests: List<Map<String, Any>>,
    onRequestAccess: (String) -> Unit,
    onGrantAccess: (String, String) -> Unit,
    onDenyAccess: (String, String) -> Unit,
    onNavigateToCollection: (String) -> Unit,
    onDelete: (String) -> Unit,
    currentUserId: String,
    isLoading: Boolean
) {
    if (isLoading && collections.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (collections.isEmpty()) {
        LibraryEmptyState("No collections shared yet.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LibraryHeader("Group Collections", "This is a persistent drive for all collections shared within the group.")
                
                if (isAdmin && accessRequests.isNotEmpty()) {
                    PendingRequestsSection(accessRequests, onGrantAccess, onDenyAccess)
                }
            }
            
            items(collections) { data ->
                val name = data["name"] as? String ?: "Untitled Collection"
                val description = data["description"] as? String ?: ""
                val downloadUrl = data["downloadUrl"] as? String ?: ""
                val symmetricKey = data["symmetricKey"] as? String ?: ""
                val updatedAt = data["updatedAt"] as? Long ?: 0L
                val collectionId = data["collectionId"] as? String ?: ""
                val isExpired = data["isExpired"] as? Boolean ?: false
                val isRestricted = data["isRestricted"] as? Boolean ?: false
                val isAdminOnly = data["isAdminOnly"] as? Boolean ?: false

                // Check if the current user has a pending access request for this collection
                val hasPendingRequest = accessRequests.any {
                    it["collectionId"] == collectionId && it["requesterId"] == currentUserId
                }
 
                val payload = "$downloadUrl|E2EE_KEY|$symmetricKey|UPDATED_AT|$updatedAt|COLLECTION_ID|$collectionId|ADMIN_ONLY|$isAdminOnly|GROUP_ID|$chatId"
                
                SharedCollectionCard(
                    collectionId = collectionId,
                    name = name,
                    description = description,
                    isExpired = isExpired,
                    isRestricted = isRestricted || symmetricKey.isBlank(),
                    hasPendingRequest = hasPendingRequest,
                    onImport = { onImport(payload) },
                    onRequestAccess = { onRequestAccess(collectionId) },
                    onResend = { onResend(collectionId) },
                    onNavigate = { onNavigateToCollection(collectionId) },
                    onDelete = if (isAdmin) { { onDelete(collectionId) } } else null,
                    localCollections = localCollections
                )
            }
        }
    }
}



@Composable
fun LibraryEmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.FolderOff, 
                contentDescription = null, 
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LibraryHeader(title: String, description: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SharedSessionCard(title: String, timestamp: Long, onJoin: () -> Unit) {
    val dateString = remember(timestamp) {
        SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Shared on $dateString", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Button(onClick = onJoin) {
                Text("Join")
            }
        }
    }
}

@Composable
fun PendingRequestsSection(
    accessRequests: List<Map<String, Any>>,
    onGrantAccess: (String, String) -> Unit,
    onDenyAccess: (String, String) -> Unit
) {
    Text("Pending Access Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(8.dp))
    accessRequests.forEach { request ->
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
        Spacer(modifier = Modifier.height(8.dp))
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun SharedCollectionCard(
    collectionId: String,
    name: String,
    description: String,
    isExpired: Boolean,
    isRestricted: Boolean,
    hasPendingRequest: Boolean = false,
    onImport: () -> Unit,
    onRequestAccess: () -> Unit,
    onResend: (() -> Unit)? = null,
    onNavigate: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    localCollections: List<StudyCollection>
) {
    val localCopy = localCollections.find { it.collectionId == collectionId }
    val isImported = localCopy != null
    val needsAccess = (isExpired || isRestricted) && !isImported

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.FolderZip, 
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    if (description.isNotEmpty()) {
                        Text(
                            description, 
                            style = MaterialTheme.typography.bodySmall, 
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            if (isExpired) {
                Text(
                    "Link Expired (30-day limit reached). Request access from members to refresh.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (isRestricted && !isImported) {
                Text(
                    if (hasPendingRequest) "Access request sent. Waiting for admin approval."
                    else "Encrypted metadata not available yet. Request access from group admins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasPendingRequest) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = when {
                    isImported -> { onNavigate ?: {} }
                    needsAccess && hasPendingRequest -> { {} } // Disabled — already pending
                    needsAccess -> onRequestAccess
                    else -> onImport
                },
                enabled = !(needsAccess && hasPendingRequest),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = when {
                    isImported -> ButtonDefaults.buttonColors()
                    needsAccess && hasPendingRequest -> ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    needsAccess -> ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    else -> ButtonDefaults.buttonColors()
                }
            ) {
                Icon(
                    when {
                        isImported -> Icons.Rounded.OpenInNew
                        needsAccess && hasPendingRequest -> Icons.Rounded.HourglassTop
                        needsAccess -> Icons.Rounded.LockOpen
                        else -> Icons.Rounded.CloudDownload
                    }, 
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    when {
                        isImported -> "Open Collection"
                        needsAccess && hasPendingRequest -> "Request Pending"
                        needsAccess -> "Request Access"
                        else -> "Import to Library"
                    }
                )
            }
            
            if (isImported && onResend != null) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onResend,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Rounded.CloudUpload, 
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Re-upload / Resend ZIP")
                }
            }
        }
    }
}

@Composable
fun AccessRequestItem(
    requesterName: String,
    collectionName: String = "Collection",
    onApprove: () -> Unit,
    onDeny: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.PersonPin, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = requesterName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Requested access to \"$collectionName\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDeny) {
                    Text("Deny", color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onApprove,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Approve")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SharedLibraryViewPreview() {
    val mockCollections = listOf(
        mapOf(
            "name" to "Internal Physics Collection",
            "description" to "Key principles of classical mechanics.",
            "collectionId" to "col1",
            "isRestricted" to false
        ),
        mapOf(
            "name" to "Space Science Advanced",
            "description" to "Deep dive into orbital mechanics.",
            "collectionId" to "col2",
            "isRestricted" to true
        )
    )
    val mockSessions = listOf(
        mapOf(
            "title" to "Midterm Prep Session",
            "sessionId" to "sess1",
            "timestamp" to System.currentTimeMillis()
        )
    )
    val mockRequests = listOf(
        mapOf(
            "requesterName" to "John Doe",
            "collectionId" to "col2",
            "requesterId" to "user1"
        )
    )

    QbaseTheme {
        SharedLibraryView(
            chatId = "chat1",
            collections = mockCollections,
            onImport = {},
            onResend = {},
            localCollections = emptyList(),
            isAdmin = true,
            accessRequests = mockRequests,
            onRequestAccess = {},
            onGrantAccess = { _, _ -> },
            onDenyAccess = { _, _ -> },
            onNavigateToCollection = {},
            onDelete = {},
            currentUserId = "me",
            isLoading = false
        )
    }
}