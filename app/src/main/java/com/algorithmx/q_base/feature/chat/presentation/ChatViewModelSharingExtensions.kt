package com.algorithmx.q_base.feature.chat.presentation
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.core.ai.brain.models.AiCollectionResponse
import com.algorithmx.q_base.core.data.chat.isAdmin
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private val sharedJson = Json { ignoreUnknownKeys = true }

fun ChatViewModel.addSharedCollection(jsonPayload: String) {
    viewModelScope.launch {
        try {
            if (jsonPayload.contains("|E2EE_KEY|")) {
                importSharedCollection(jsonPayload)
                return@launch
            }
            Log.d("ChatViewModel", "Attempting to add AI collection from JSON")
            val response = sharedJson.decodeFromString<AiCollectionResponse>(jsonPayload)

            // Since this is already triggered by a "Save" button in the bubble, it represents explicit consent.
            aiRepository.saveAsCollection(response)
            
            _actionFeedback.emit("New Collection '${response.collectionTitle}' added to your library.")
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to add AI collection", e)
            _actionFeedback.emit("Failed to add collection: ${e.message}")
        }
    }
}

fun ChatViewModel.importSharedCollection(payload: String) {
    viewModelScope.launch {
        try {
            Log.d("ChatViewModel", "Attempting to import shared collection file")
            // Payload format: url|E2EE_KEY|symmetricKey
            val parts = payload.split("|E2EE_KEY|")
            if (parts.size < 2) {
                _actionFeedback.emit("Invalid sharing payload format")
                return@launch
            }
            
            val url = parts[0]
            val remainder = parts[1]
            val key = remainder.substringBefore("|UPDATED_AT|")
            val isAdminOnly = remainder.contains("|ADMIN_ONLY|true")
            val groupId = remainder.substringAfter("|GROUP_ID|", "").substringBefore("|")
            val collectionId = remainder.substringAfter("|COLLECTION_ID|", "").substringBefore("|")
            
            _actionFeedback.emit("Downloading and importing collection...")
            
            val result = mockDownloader.downloadAndImportMock(
                url = url, 
                symmetricKeyBase64 = key,
                sharedWithGroupId = groupId.ifBlank { null },
                isAdminOnly = isAdminOnly
            )
            if (result.isSuccess) {
                _actionFeedback.emit("Collection imported successfully to your library!")
                
                if (collectionId.isNotEmpty()) {
                    try {
                        syncRepository.acknowledgeCollectionDownload(collectionId)
                        Log.d("ChatViewModel", "Acknowledged collection download for $collectionId")
                    } catch (ae: Exception) {
                        Log.e("ChatViewModel", "Failed to acknowledge download receipt", ae)
                    }
                }
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.e("ChatViewModel", "Import failed: $error")
                if (error.contains("404") || error.contains("not found") || error.contains("NotFound")) {
                    _actionFeedback.emit("Collection ZIP has been garbage-collected (zero-retention policy). Please request an admin or owner to Re-upload/Resend this collection.")
                } else {
                    _actionFeedback.emit("Import failed: $error")
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Import exception", e)
            _actionFeedback.emit("Import error: ${e.message}")
        }
    }
}

fun ChatViewModel.shareCollection(chatId: String, collectionId: String) {
    viewModelScope.launch {
        val chat = chatLocalDataSource.getChatById(chatId) ?: return@launch
        _isSharing.value = true
        try {
            val collection = collectionDao.getStudyCollectionByIdOnce(collectionId) ?: throw Exception("Collection not found")
            
            // RESTRICTION: Non-sharable if admin-only and user is not admin
            if (collection.isAdminOnly) {
                if (!chat.isAdmin(currentUserId)) {
                    _actionFeedback.emit("Cannot share: This collection is restricted to group admins only.")
                    _isSharing.value = false
                    return@launch
                }
            }

            val zipFile = mockExporter.exportCollection(collectionId) ?: throw Exception("Failed to export collection")
            
            val (downloadUrl, symmetricKey) = syncRepository.uploadQuestionBankZip(zipFile)
            val updatedAt = collection.updatedAt
            
            if (chat.isGroup) {
                // DIFFERENT MECHANISM FOR GROUPS: Persistent Library
                val metadata = hashMapOf(
                    "collectionId" to collectionId,
                    "name" to collection.name,
                    "description" to (collection.description ?: ""),
                    "downloadUrl" to downloadUrl,
                    "symmetricKey" to symmetricKey,
                    "updatedAt" to updatedAt,
                    "sharedBy" to currentUserId,
                    "timestamp" to System.currentTimeMillis(),
                    "isAdminOnly" to collection.isAdminOnly
                )
                syncRepository.shareCollectionToGroup(chatId, metadata)
                sendMessage(chatId, "shared a collection to the group library: ${collection.name}", type = "DB_CHANGE")
            } else {
                // P2P: Message-based (Ephemeral)
                sendMessage(chatId, "$downloadUrl|E2EE_KEY|$symmetricKey|UPDATED_AT|$updatedAt|COLLECTION_ID|$collectionId", type = "FILE_TRANSFER")
            }
            
            mockExporter.cleanup(zipFile)
            _actionFeedback.emit("Collection shared successfully")
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Sharing failed", e)
            _actionFeedback.emit("Sharing failed: ${e.message}")
        } finally {
            _isSharing.value = false
        }
    }
}

fun ChatViewModel.resendCollection(collectionId: String) {
    val chatId = _currentChatId.value ?: return
    viewModelScope.launch {
        try {
            _actionFeedback.emit("Re-packaging and re-uploading collection...")
            val collection = collectionDao.getStudyCollectionByIdOnce(collectionId)
                ?: throw Exception("Collection not found in local library")
            // RESTRICTION: Non-resendable if admin-only and user is not admin
            val chat = chatLocalDataSource.getChatById(chatId)
            if (collection.isAdminOnly) {
                if (chat == null || !chat.isAdmin(currentUserId)) {
                    _actionFeedback.emit("Cannot resend: This collection is restricted to group admins only.")
                    return@launch
                }
            }
            
            val zipFile = mockExporter.exportCollection(collectionId)
                ?: throw Exception("Failed to export collection")
            
            val (downloadUrl, symmetricKey) = syncRepository.uploadQuestionBankZip(zipFile)
            val updatedAt = collection.updatedAt
            
            val metadata = hashMapOf(
                "collectionId" to collectionId,
                "name" to collection.name,
                "description" to (collection.description ?: ""),
                "downloadUrl" to downloadUrl,
                "symmetricKey" to symmetricKey,
                "updatedAt" to updatedAt,
                "sharedBy" to currentUserId,
                "timestamp" to System.currentTimeMillis(),
                "isAdminOnly" to collection.isAdminOnly
            )
            syncRepository.shareCollectionToGroup(chatId, metadata)
            mockExporter.cleanup(zipFile)
            _actionFeedback.emit("Collection resent and re-uploaded successfully!")
            sendMessage(chatId, "re-uploaded and updated collection in group library: ${collection.name}", type = "DB_CHANGE")
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Resend failed", e)
            _actionFeedback.emit("Resend failed: ${e.message}")
        }
    }
}

fun ChatViewModel.shareSession(chatId: String, sessionId: String) {
    viewModelScope.launch {
        _isAiLoading.value = true // Show loading
        try {
            val session = sessionDao.getSessionById(sessionId) ?: throw Exception("Session not found")
            val sessionTitle = session.title
            
            val chat = chatLocalDataSource.getChatById(chatId) ?: throw Exception("Chat not found")
            
            // RESTRICTION: Non-sharable if admin-only and user is not admin
            if (session.isAdminOnly) {
                if (!chat.isAdmin(currentUserId)) {
                    _actionFeedback.emit("Cannot share: This session is restricted to group admins only.")
                    return@launch
                }
            }
            
            _actionFeedback.emit("Exporting and zipping study session...")
            val zipFile = mockExporter.exportSession(sessionId) ?: throw Exception("Failed to export session")
            
            _actionFeedback.emit("Uploading encrypted session...")
            val (downloadUrl, symmetricKey) = syncRepository.uploadQuestionBankZip(zipFile)
            
            if (chat.isGroup) {
                syncRepository.addSharedSessionToGroup(chatId, sessionId, sessionTitle, downloadUrl, symmetricKey)
                sendMessage(chatId, "shared a study session: $sessionTitle", type = "DB_CHANGE")
            } else {
                syncRepository.sendSessionInvite(chatId, sessionId, sessionTitle, downloadUrl, symmetricKey)
            }
            
            mockExporter.cleanup(zipFile)
            _actionFeedback.emit("Session shared successfully")
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to share session", e)
            _actionFeedback.emit("Failed to share session: ${e.message}")
        } finally {
            _isAiLoading.value = false
        }
    }
}

fun ChatViewModel.joinSession(sessionIdOrPayload: String, onSessionImported: (String) -> Unit) {
    viewModelScope.launch {
        _isAiLoading.value = true
        try {
            var downloadUrl: String? = null
            var symmetricKey: String? = null
            var sessionId: String = sessionIdOrPayload
            
            if (sessionIdOrPayload.contains("|E2EE_KEY|")) {
                val parts = sessionIdOrPayload.split("|E2EE_KEY|")
                downloadUrl = parts.getOrNull(0)
                val remainder = parts.getOrNull(1) ?: ""
                symmetricKey = remainder.substringBefore("|SESSION_ID|")
                sessionId = remainder.substringAfter("|SESSION_ID|", "").substringBefore("|TITLE|")
            } else {
                // Try finding in sharedSessions list
                val sharedSessionsList = _sharedSessions.value
                val matchingSession = sharedSessionsList.find { (it["sessionId"] as? String) == sessionIdOrPayload }
                if (matchingSession != null) {
                    downloadUrl = matchingSession["downloadUrl"] as? String
                    symmetricKey = matchingSession["symmetricKey"] as? String
                    sessionId = sessionIdOrPayload
                }
            }
            
            if (!downloadUrl.isNullOrBlank()) {
                _actionFeedback.emit("Downloading and joining study session...")
                val result = mockDownloader.downloadAndImportSession(downloadUrl, symmetricKey)
                if (result.isSuccess) {
                    val importedId = result.getOrNull() ?: sessionId
                    _actionFeedback.emit("Session joined successfully!")
                    onSessionImported(importedId)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _actionFeedback.emit("Failed to join session: $error")
                }
            } else {
                val existing = sessionDao.getSessionById(sessionId)
                if (existing != null) {
                    onSessionImported(sessionId)
                } else {
                    _actionFeedback.emit("Session data not found locally or on the cloud.")
                }
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Error joining session", e)
            _actionFeedback.emit("Failed to join session: ${e.message}")
        } finally {
            _isAiLoading.value = false
        }
    }
}