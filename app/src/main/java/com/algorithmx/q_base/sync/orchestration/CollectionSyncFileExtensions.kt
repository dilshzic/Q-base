package com.algorithmx.q_base.sync.orchestration

import android.util.Log
import com.algorithmx.q_base.core.data.UserEntity
import com.algorithmx.q_base.core.data.backend.CoreQuery
import com.algorithmx.q_base.core.data.backend.CoreQueryOperator
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

suspend fun CollectionSyncRepository.uploadQuestionBankZip(zipFile: File): Pair<String, String> {
    return withContext(Dispatchers.IO) {
        val fileBytes = zipFile.readBytes()
        val (encryptedBytes, symmetricKey) = cryptoManager.encryptFileContent(fileBytes)
        
        zipFile.writeBytes(encryptedBytes)
        val inputFile = InputFile.fromPath(zipFile.absolutePath)

        val uploadedFile = storage.createFile(
            bucketId = bucketId,
            fileId = ID.unique(),
            file = inputFile
        )

        val downloadUrl = "https://syd.cloud.appwrite.io/v1/storage/buckets/$bucketId/files/${uploadedFile.id}/download?project=$projectId"
        
        return@withContext Pair(downloadUrl, symmetricKey)
    }
}

suspend fun CollectionSyncRepository.deleteQuestionBankZip(fileId: String) {
    withContext(Dispatchers.IO) {
        storage.deleteFile(
            bucketId = bucketId,
            fileId = fileId
        )
    }
}

suspend fun CollectionSyncRepository.shareCollectionToGroup(chatId: String, collectionMetadata: Map<String, Any>) {
    try {
        val collectionId = collectionMetadata["collectionId"] as String
        val symmetricKey = collectionMetadata["symmetricKey"] as? String ?: ""
        
        var targetDocId: String? = null
        try {
            val queries = listOf(
                CoreQuery("collectionId", CoreQueryOperator.EQUAL, collectionId)
            )
            val existingDocs = databases.queryDocuments("shared_collections", queries).getOrThrow()
            val existingDoc = existingDocs.firstOrNull { it["chatId"] == chatId }
            
            if (existingDoc != null) {
                targetDocId = existingDoc["\$id"] as? String
                val oldUrl = existingDoc["downloadUrl"] as? String
                if (oldUrl != null) {
                    val oldFileId = extractFileIdFromUrl(oldUrl)
                    if (oldFileId != null) {
                        try {
                            deleteQuestionBankZip(oldFileId)
                        } catch (de: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {}

        val chat = chatDao.getChatById(chatId)
        val participants = chat?.participantIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

        // Asymmetric E2EE Key Wrapping for all current group participants
        val wrappedKeysMap = mutableMapOf<String, String>()
        if (symmetricKey.isNotBlank()) {
            val deferredKeys = participants.map { targetId ->
                repositoryScope.async {
                    val publicKeyBase64: String? = if (targetId == currentUserId) {
                        cryptoManager.initializeAndGetPublicKey()
                    } else {
                        val local = userDao.getUserById(targetId)
                        var candidateKey: String? = local?.publicKey
                        try {
                            val docData = databases.getDocument(
                                collectionId = "users",
                                documentId = targetId
                            ).getOrNull()
                            val remoteKey = docData?.get("publicKey") as? String
                            if (!remoteKey.isNullOrBlank() && docData != null) {
                                candidateKey = remoteKey
                                val remoteDisplay = (docData["displayName"] as? String)?.takeIf { it.isNotBlank() }
                                val localDisplay = local?.displayName?.takeIf { it.isNotBlank() }
                                val cached = UserEntity(
                                    userId = targetId,
                                    displayName = remoteDisplay ?: localDisplay ?: "Unknown",
                                    email = local?.email,
                                    intro = (docData["intro"] as? String) ?: local?.intro,
                                    profilePictureUrl = (docData["profilePictureUrl"] as? String) ?: local?.profilePictureUrl,
                                    friendCode = (docData["friendCode"] as? String) ?: (local?.friendCode ?: ""),
                                    publicKey = remoteKey,
                                    isBanned = (docData["isBanned"] as? Boolean) ?: (local?.isBanned ?: false),
                                    isPhotoVisible = (docData["isPhotoVisible"] as? Boolean) ?: (local?.isPhotoVisible ?: true)
                                )
                                userDao.insertUser(cached)
                            }
                        } catch (e: Exception) {
                            Log.w("CollectionSyncRepository", "Failed to refresh public key for $targetId during share", e)
                        }
                        candidateKey
                    }
                    targetId to publicKeyBase64
                }
            }
            
            val resolvedKeys = deferredKeys.awaitAll()
            resolvedKeys.forEach { (targetId, publicKeyBase64) ->
                if (!publicKeyBase64.isNullOrBlank()) {
                    try {
                        wrappedKeysMap[targetId] = cryptoManager.encryptMessage(symmetricKey, publicKeyBase64)
                    } catch (e: Exception) {
                        Log.e("CollectionSyncRepository", "Key wrap failed for $targetId in group sharing", e)
                    }
                }
            }
        }

        val wrappedKeysJsonStr = org.json.JSONObject(wrappedKeysMap as Map<*, *>).toString()

        val secureMetadata = mapOf(
            "collectionId" to collectionId,
            "chatId" to chatId,
            "name" to (collectionMetadata["name"] as? String ?: "Group Collection"),
            "description" to (collectionMetadata["description"] as? String ?: ""),
            "downloadUrl" to (collectionMetadata["downloadUrl"] as? String ?: ""),
            "symmetricKey" to "", // Leave public column blank for maximum E2EE security
            "wrappedKeys" to wrappedKeysJsonStr,
            "sharedBy" to (collectionMetadata["sharedBy"] as String),
            "adminIds" to participants,
            "pendingDownloads" to participants.filter { it != currentUserId },
            "isAdminOnly" to (collectionMetadata["isAdminOnly"] as? Boolean ?: false),
            "updatedAt" to (collectionMetadata["updatedAt"] as Long) / 1000
        )

        try {
            if (targetDocId != null) {
                databases.updateDocument("shared_collections", targetDocId, secureMetadata).getOrThrow()
            } else {
                databases.createDocument("shared_collections", ID.unique(), secureMetadata).getOrThrow()
            }
        } catch (e: Exception) {
            Log.e("CollectionSyncRepository", "Failed to create/update shared collection", e)
            throw e
        }
    } catch (e: Exception) {
        Log.e("CollectionSyncRepository", "Failed to share collection in Appwrite", e)
        throw e
    }
}

internal fun CollectionSyncRepository.extractFileIdFromUrl(url: String): String? {
    return try {
        val pattern = "files/([^/]+)/download".toRegex()
        val matchResult = pattern.find(url)
        matchResult?.groupValues?.get(1)
    } catch (e: Exception) {
        null
    }
}

suspend fun CollectionSyncRepository.acknowledgeCollectionDownload(chatId: String?, collectionId: String) {
    withContext(Dispatchers.IO) {
        try {
            val doc = if (chatId != null) {
                val queries = listOf(
                    CoreQuery("collectionId", CoreQueryOperator.EQUAL, collectionId)
                )
                databases.queryDocuments("shared_collections", queries).getOrNull()?.firstOrNull { it["chatId"] == chatId }
            } else {
                val queries = listOf(CoreQuery("collectionId", CoreQueryOperator.EQUAL, collectionId))
                databases.queryDocuments("shared_collections", queries).getOrNull()?.firstOrNull()
            }
            if (doc != null) {
                val targetDocId = doc["\$id"] as? String ?: return@withContext
                val url = doc["downloadUrl"] as? String ?: ""
                val pendingDownloadsList = (doc["pendingDownloads"] as? List<*>)?.mapNotNull { it?.toString() }?.toMutableList() ?: mutableListOf()
                
                if (currentUserId != null && pendingDownloadsList.contains(currentUserId)) {
                    pendingDownloadsList.remove(currentUserId)
                    
                    databases.updateDocument(
                        collectionId = "shared_collections",
                        documentId = targetDocId,
                        data = mapOf(
                            "pendingDownloads" to pendingDownloadsList
                        )
                    ).getOrThrow()
                    
                    if (pendingDownloadsList.isEmpty() && url.isNotBlank()) {
                        val fileId = extractFileIdFromUrl(url)
                        if (fileId != null) {
                            try {
                                deleteQuestionBankZip(fileId)
                                Log.d("CollectionSyncRepository", "Zero-retention triggered: deleted ZIP file $fileId from Appwrite Storage")
                            } catch (de: Exception) {
                                Log.e("CollectionSyncRepository", "Failed to delete storage ZIP file", de)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CollectionSyncRepository", "Failed to acknowledge collection download", e)
        }
    }
}

fun CollectionSyncRepository.observeGroupLibrary(chatId: String): Flow<List<Map<String, Any>>> {
    return callbackFlow {
        repositoryScope.launch {
            try {
                val docs = databases.queryDocuments(
                    collectionId = "shared_collections",
                    queries = emptyList()
                ).getOrThrow()
                Log.e("CollectionSyncRepository", "observeGroupLibrary: fetched ${docs.size} docs from Appwrite. Filtering for chatId=$chatId")
                docs.forEach { Log.e("CollectionSyncRepository", "Doc found: id=${it["\$id"]} chatId=${it["chatId"]} collectionId=${it["collectionId"]}") }
                val filtered = docs.filter { it["chatId"] == chatId }
                Log.e("CollectionSyncRepository", "observeGroupLibrary: filtered down to ${filtered.size} docs")
                val mapped = mapGroupLibrary(filtered)
                trySend(mapped).isSuccess
            } catch (e: Exception) {
                Log.e("CollectionSyncRepository", "Initial fetch in observeGroupLibrary failed", e)
            }
        }

        val realtime = io.appwrite.services.Realtime(appwriteClient)
        val subscription = realtime.subscribe("tablesdb.qbase_db.tables.shared_collections.rows") { event ->
            repositoryScope.launch {
                try {
                    val docs = databases.queryDocuments(
                        collectionId = "shared_collections",
                        queries = emptyList()
                    ).getOrThrow()
                    Log.e("CollectionSyncRepository", "observeGroupLibrary Realtime: fetched ${docs.size} docs. Filtering for chatId=$chatId")
                    val filtered = docs.filter { it["chatId"] == chatId }
                    val mapped = mapGroupLibrary(filtered)
                    trySend(mapped).isSuccess
                } catch (e: Exception) {
                    Log.e("CollectionSyncRepository", "Realtime update in observeGroupLibrary failed", e)
                }
            }
        }
        awaitClose { subscription.close() }
    }
}

private suspend fun CollectionSyncRepository.mapGroupLibrary(list: List<Map<String, Any>>): List<Map<String, Any>> {
    return list.map { doc ->
        val data = doc.toMutableMap()
        val collectionId = doc["collectionId"] as? String ?: ""
        val local = collectionDao.getStudyCollectionByIdOnce(collectionId)
        
        data["name"] = doc["name"] as? String ?: (local?.name ?: "Group Collection")
        data["description"] = doc["description"] as? String ?: (local?.description ?: "Shared by ${doc["sharedBy"]}")
        data["isExpired"] = false
        
        // Resolve E2EE symmetric key locally
        var decKey = ""
        val wrappedKeysStr = doc["wrappedKeys"] as? String ?: ""
        if (!wrappedKeysStr.isBlank()) {
            try {
                val jsonObj = org.json.JSONObject(wrappedKeysStr)
                val encKey = jsonObj.optString(currentUserId ?: "")
                if (!encKey.isNullOrBlank()) {
                    val decResult = cryptoManager.decryptMessage(encKey)
                    if (decResult.isSuccess) {
                        decKey = decResult.getOrNull() ?: ""
                    }
                }
            } catch (e: Exception) {
                Log.e("CollectionSyncRepository", "Failed to decrypt wrapped key in mapGroupLibrary", e)
            }
        }
        
        data["symmetricKey"] = decKey
        data["isRestricted"] = decKey.isBlank()
        
        val rawUpdatedAt = (doc["updatedAt"] as? Number)?.toLong() ?: 0L
        data["updatedAt"] = if (rawUpdatedAt < 1000000000000L) rawUpdatedAt * 1000 else rawUpdatedAt
        data
    }
}