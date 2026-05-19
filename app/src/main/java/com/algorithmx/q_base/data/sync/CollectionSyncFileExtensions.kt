package com.algorithmx.q_base.data.sync

import android.util.Log
import com.algorithmx.q_base.data.core.UserEntity
import io.appwrite.ID
import io.appwrite.Query
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
        
        try {
            val existingDoc = databases.getDocument("qbase_db", "shared_collections", collectionId)
            val oldUrl = existingDoc.data["downloadUrl"] as? String
            if (oldUrl != null) {
                val oldFileId = extractFileIdFromUrl(oldUrl)
                if (oldFileId != null) {
                    try {
                        deleteQuestionBankZip(oldFileId)
                    } catch (de: Exception) {}
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
                            val doc = databases.getDocument(
                                databaseId = "qbase_db",
                                collectionId = "users",
                                documentId = targetId
                            )
                            val remoteKey = doc.data["publicKey"] as? String
                            if (!remoteKey.isNullOrBlank()) {
                                candidateKey = remoteKey
                                val cached = UserEntity(
                                    userId = targetId,
                                    displayName = (doc.data["displayName"] as? String) ?: (local?.displayName ?: "Unknown"),
                                    email = local?.email,
                                    intro = (doc.data["intro"] as? String) ?: local?.intro,
                                    profilePictureUrl = (doc.data["profilePictureUrl"] as? String) ?: local?.profilePictureUrl,
                                    friendCode = (doc.data["friendCode"] as? String) ?: (local?.friendCode ?: ""),
                                    publicKey = remoteKey,
                                    isBanned = (doc.data["isBanned"] as? Boolean) ?: (local?.isBanned ?: false),
                                    isPhotoVisible = (doc.data["isPhotoVisible"] as? Boolean) ?: (local?.isPhotoVisible ?: true)
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
            databases.createDocument("qbase_db", "shared_collections", collectionId, secureMetadata)
        } catch (e: io.appwrite.exceptions.AppwriteException) {
            if (e.code == 409) {
                databases.updateDocument("qbase_db", "shared_collections", collectionId, secureMetadata)
            } else {
                throw e
            }
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

suspend fun CollectionSyncRepository.acknowledgeCollectionDownload(collectionId: String) {
    withContext(Dispatchers.IO) {
        try {
            val doc = databases.getDocument("qbase_db", "shared_collections", collectionId)
            val url = doc.data["downloadUrl"] as? String ?: ""
            val pendingDownloadsList = (doc.data["pendingDownloads"] as? List<*>)?.mapNotNull { it?.toString() }?.toMutableList() ?: mutableListOf()
            
            if (currentUserId != null && pendingDownloadsList.contains(currentUserId)) {
                pendingDownloadsList.remove(currentUserId)
                
                databases.updateDocument(
                    databaseId = "qbase_db",
                    collectionId = "shared_collections",
                    documentId = collectionId,
                    data = mapOf(
                        "pendingDownloads" to pendingDownloadsList
                    )
                )
                
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
        } catch (e: Exception) {
            Log.e("CollectionSyncRepository", "Failed to acknowledge collection download", e)
        }
    }
}

fun CollectionSyncRepository.observeGroupLibrary(chatId: String): Flow<List<Map<String, Any>>> {
    return callbackFlow {
        repositoryScope.launch {
            try {
                val response = databases.listDocuments(
                    databaseId = "qbase_db",
                    collectionId = "shared_collections",
                    queries = listOf(Query.equal("chatId", chatId))
                )
                val mapped = mapGroupLibrary(response.documents.map { it.data })
                trySend(mapped).isSuccess
            } catch (e: Exception) {}
        }

        val realtime = io.appwrite.services.Realtime(appwriteClient)
        val subscription = realtime.subscribe("databases.qbase_db.collections.shared_collections.documents") { event ->
            repositoryScope.launch {
                try {
                    val response = databases.listDocuments(
                        databaseId = "qbase_db",
                        collectionId = "shared_collections",
                        queries = listOf(Query.equal("chatId", chatId))
                    )
                    val mapped = mapGroupLibrary(response.documents.map { it.data })
                    trySend(mapped).isSuccess
                } catch (e: Exception) {}
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
