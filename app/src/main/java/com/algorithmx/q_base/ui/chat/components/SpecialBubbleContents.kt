package com.algorithmx.q_base.ui.chat.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algorithmx.q_base.data.collections.StudyCollection

@Composable
fun CollectionBubbleContent(
    payload: String,
    isSaved: Boolean,
    onSave: (String) -> Unit,
    isMine: Boolean
) {
    val json = remember { kotlinx.serialization.json.Json { ignoreUnknownKeys = true } }
    val collection = remember(payload) {
        try {
            json.decodeFromString<com.algorithmx.q_base.core_ai.brain.models.AiCollectionResponse>(payload)
        } catch (e: Exception) { null }
    }

    if (collection != null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.CollectionsBookmark,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(collection.collectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(collection.collectionDescription, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            Button(
                onClick = { if (!isSaved) onSave(payload) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaved,
                colors = if (isSaved) ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors()
            ) {
                Icon(if (isSaved) Icons.Rounded.CheckCircle else Icons.Default.Add, null, Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isSaved) "Saved" else "Save")
            }
        }
    }
}

@Composable
fun SessionBubbleContent(payload: String, onJoin: (String) -> Unit, isMine: Boolean) {
    val parts = payload.split("|")
    val sessionId = parts.getOrNull(0) ?: ""
    val sessionTitle = parts.getOrNull(1) ?: "Active Session"

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.RocketLaunch,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(sessionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text("Join this collaborative study session to track progress together.", style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = { onJoin(sessionId) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Join Session")
        }
    }
}

@Composable
fun FileTransferBubbleContent(
    payload: String,
    localCollections: List<StudyCollection>,
    onImport: (String) -> Unit,
    isMine: Boolean
) {
    val parts = payload.split("|")
    val updatedAtRemote = parts.getOrNull(4)?.toLongOrNull() ?: 0L
    val collectionIdMetadata = parts.getOrNull(6)
    val url = parts.getOrNull(0) ?: ""
    val collectionId = collectionIdMetadata ?: url.substringAfter("/files/").substringBefore("/download")
    val localCopy = localCollections.find { it.collectionId == collectionId }
    val buttonState = when {
        localCopy == null -> "IMPORT"
        updatedAtRemote > localCopy.updatedAt -> "UPDATE"
        else -> "UP_TO_DATE"
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.FolderZip,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(localCopy?.name ?: "Collection Shared", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        Text(
            text = when(buttonState) {
                "IMPORT" -> "A question collection has been shared with you. Tap below to import it."
                "UPDATE" -> "A newer version of this collection is available. Tap below to update."
                else -> "You have the latest version of this collection in your library."
            },
            style = MaterialTheme.typography.bodySmall
        )
        Button(
            onClick = { if (buttonState != "UP_TO_DATE") onImport(payload) },
            modifier = Modifier.fillMaxWidth(),
            enabled = buttonState != "UP_TO_DATE"
        ) {
            Icon(when(buttonState) { "IMPORT" -> Icons.Rounded.CloudDownload; "UPDATE" -> Icons.Rounded.Update; else -> Icons.Rounded.CheckCircle }, null, Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(when(buttonState) { "IMPORT" -> "Import"; "UPDATE" -> "Update"; else -> "Up to date" })
        }
    }
}

@Composable
fun DecryptionErrorContent(status: String, isMine: Boolean, onDeleteChat: () -> Unit) {
    Column(modifier = Modifier.padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(2.dp)) {
            Icon(
                imageVector = if (status == "FAILED") Icons.Default.Lock else Icons.Default.Sync,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = when (status) {
                    "FAILED" -> "Encrypted for a different session/key. If you just joined, you cannot see previous history."
                    "DECRYPTION_ERROR" -> "Decryption failed. Secure session keys missing or invalid."
                    else -> "Encountered decryption error"
                },
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) 
                            else MaterialTheme.colorScheme.error
                )
            )
        }
        if (!isMine) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onDeleteChat,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete Chat & Restart")
            }
        }
    }
}

@Composable
fun MessageTimestampAndStatus(timeString: String, isMine: Boolean, status: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically, 
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Text(
            text = timeString,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f) 
                    else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f)
        )
        if (isMine) {
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                imageVector = if (status == "READ" || status == "DELIVERED") Icons.Rounded.DoneAll else Icons.Rounded.Done,
                contentDescription = status,
                modifier = Modifier.size(11.dp),
                tint = if (status == "READ") Color(0xFF00E676) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
            )
        }
    }
}

