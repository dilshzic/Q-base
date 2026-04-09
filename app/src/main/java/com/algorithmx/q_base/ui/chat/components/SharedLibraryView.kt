package com.algorithmx.q_base.ui.chat.components

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
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.collections.StudyCollection
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SharedLibraryView(
    collections: List<Map<String, Any>>,
    sessions: List<Map<String, Any>>,
    onImport: (String) -> Unit,
    onJoinSession: (String) -> Unit,
    localCollections: List<StudyCollection>,
    isAdmin: Boolean,
    accessRequests: List<Map<String, Any>>,
    onRequestAccess: (String) -> Unit,
    onGrantAccess: (String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Collections", "Sessions")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { 
                        Text(
                            text = title, 
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        ) 
                    }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                0 -> CollectionsTabContent(
                    collections = collections,
                    onImport = onImport,
                    localCollections = localCollections,
                    isAdmin = isAdmin,
                    accessRequests = accessRequests,
                    onRequestAccess = onRequestAccess,
                    onGrantAccess = onGrantAccess
                )
                1 -> SessionsTabContent(
                    sessions = sessions,
                    onJoinSession = onJoinSession
                )
            }
        }
    }
}

@Composable
fun CollectionsTabContent(
    collections: List<Map<String, Any>>,
    onImport: (String) -> Unit,
    localCollections: List<StudyCollection>,
    isAdmin: Boolean,
    accessRequests: List<Map<String, Any>>,
    onRequestAccess: (String) -> Unit,
    onGrantAccess: (String, String) -> Unit
) {
    if (collections.isEmpty()) {
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
                    PendingRequestsSection(accessRequests, onGrantAccess)
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

                val payload = "$downloadUrl|E2EE_KEY|$symmetricKey|UPDATED_AT|$updatedAt|COLLECTION_ID|$collectionId"
                
                SharedCollectionCard(
                    collectionId = collectionId,
                    name = name,
                    description = description,
                    isExpired = isExpired,
                    isRestricted = isRestricted || symmetricKey.isBlank(),
                    onImport = { onImport(payload) },
                    onRequestAccess = { onRequestAccess(collectionId) },
                    localCollections = localCollections
                )
            }
        }
    }
}

@Composable
fun SessionsTabContent(
    sessions: List<Map<String, Any>>,
    onJoinSession: (String) -> Unit
) {
    if (sessions.isEmpty()) {
        LibraryEmptyState("No active sessions in this group.")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                LibraryHeader("Group Sessions", "Persistent record of study sessions shared in this group.")
            }
            
            items(sessions) { data ->
                val sessionId = data["sessionId"] as? String ?: ""
                val title = data["title"] as? String ?: "Untitled Session"
                val timestamp = data["timestamp"] as? Long ?: 0L
                
                SharedSessionCard(
                    title = title,
                    timestamp = timestamp,
                    onJoin = { onJoinSession(sessionId) }
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
fun PendingRequestsSection(accessRequests: List<Map<String, Any>>, onGrantAccess: (String, String) -> Unit) {
    Text("Pending Access Requests", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    Spacer(modifier = Modifier.height(8.dp))
    accessRequests.forEach { request ->
        val collectionId = request["collectionId"] as? String ?: ""
        val requesterId = request["requesterId"] as? String ?: ""
        val requesterName = request["requesterName"] as? String ?: "Someone"
        AccessRequestItem(requesterName = requesterName, onApprove = { onGrantAccess(collectionId, requesterId) })
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
    onImport: () -> Unit,
    onRequestAccess: () -> Unit,
    localCollections: List<StudyCollection>
) {
    val localCopy = localCollections.find { it.collectionId == collectionId }
    val isImported = localCopy != null

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
            }
            
            if (isExpired) {
                Text(
                    "Link Expired (30-day limit reached). Request access from members to refresh.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (isRestricted) {
                Text(
                    "Encrypted metadata not available yet. Request access from group admins.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = if (isExpired || isRestricted) onRequestAccess else onImport,
                enabled = !isImported,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = when {
                    isImported -> ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    isExpired || isRestricted -> ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    else -> ButtonDefaults.buttonColors()
                }
            ) {
                Icon(
                    when {
                        isImported -> Icons.Rounded.CheckCircle
                        isExpired || isRestricted -> Icons.Rounded.LockOpen
                        else -> Icons.Rounded.CloudDownload
                    }, 
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    when {
                        isImported -> "Imported to Library"
                        isExpired || isRestricted -> "Request Access"
                        else -> "Import to Library"
                    }
                )
            }
        }
    }
}

@Composable
fun AccessRequestItem(
    requesterName: String,
    onApprove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.PersonPin, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$requesterName requested access",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            TextButton(onClick = onApprove) {
                Text("Approve")
            }
        }
    }
}
