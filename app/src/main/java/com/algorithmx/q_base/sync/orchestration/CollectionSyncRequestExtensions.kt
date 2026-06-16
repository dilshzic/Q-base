package com.algorithmx.q_base.sync.orchestration

import android.util.Log
import com.algorithmx.q_base.core.data.backend.CoreQuery
import com.algorithmx.q_base.core.data.backend.CoreQueryOperator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.util.UUID

suspend fun CollectionSyncRepository.sendSyncRequest(targetUserId: String, targetCollectionId: String) {
    val senderId = currentUserId ?: return
    val requestRefId = UUID.randomUUID().toString()

    val syncRequest = mapOf(
        "senderId" to senderId,
        "targetUserId" to targetUserId,
        "targetCollectionId" to targetCollectionId,
        "status" to "PENDING"
    )

    try {
        databases.createDocument(
            collectionId = "sync_requests",
            documentId = requestRefId,
            data = syncRequest
        )
    } catch (e: Exception) {
        Log.e("CollectionSyncRepository", "Failed to send sync request in Appwrite", e)
    }
}

fun CollectionSyncRepository.observeIncomingRequests(): Flow<List<SyncRequest>> = callbackFlow {
    val userId = currentUserId ?: return@callbackFlow
    
    repositoryScope.launch {
        try {
            val queries = listOf(
                CoreQuery("targetUserId", CoreQueryOperator.EQUAL, userId),
                CoreQuery("status", CoreQueryOperator.EQUAL, "PENDING")
            )
            val docs = databases.queryDocuments(
                collectionId = "sync_requests",
                queries = queries
            ).getOrThrow()
            val list = docs.map { doc ->
                SyncRequest(
                    requestId = (doc["\$id"] as? String) ?: "",
                    senderId = (doc["senderId"] as? String) ?: "",
                    targetCollectionId = (doc["targetCollectionId"] as? String) ?: "",
                    status = (doc["status"] as? String) ?: "PENDING"
                )
            }
            trySend(list).isSuccess
        } catch (e: Exception) {}
    }

    val realtime = io.appwrite.services.Realtime(appwriteClient)
    val subscription = realtime.subscribe("tablesdb.qbase_db.tables.sync_requests.rows") { event ->
        repositoryScope.launch {
            try {
                val queries = listOf(
                    CoreQuery("targetUserId", CoreQueryOperator.EQUAL, userId),
                    CoreQuery("status", CoreQueryOperator.EQUAL, "PENDING")
                )
                val docs = databases.queryDocuments(
                    collectionId = "sync_requests",
                    queries = queries
                ).getOrThrow()
                val list = docs.map { doc ->
                    SyncRequest(
                        requestId = (doc["\$id"] as? String) ?: "",
                        senderId = (doc["senderId"] as? String) ?: "",
                        targetCollectionId = (doc["targetCollectionId"] as? String) ?: "",
                        status = (doc["status"] as? String) ?: "PENDING"
                    )
                }
                trySend(list).isSuccess
            } catch (e: Exception) {}
        }
    }
    awaitClose { subscription.close() }
}

suspend fun CollectionSyncRepository.requestCollectionAccess(chatId: String, collectionId: String) {
    val userId = currentUserId ?: throw IllegalStateException("User not authenticated")
    val requestId = "${userId}_$collectionId"

    // Resolve requester display name
    val requesterName = userDao.getUserById(userId)?.displayName ?: "Unknown"

    // Resolve collection name from shared_collections
    val collectionName = try {
        val queries = listOf(
            CoreQuery("chatId", CoreQueryOperator.EQUAL, chatId),
            CoreQuery("collectionId", CoreQueryOperator.EQUAL, collectionId)
        )
        val docs = databases.queryDocuments("shared_collections", queries).getOrNull() ?: emptyList()
        docs.firstOrNull()?.get("name") as? String ?: "Collection"
    } catch (_: Exception) { "Collection" }

    val requestData = mapOf(
        "chatId" to chatId,
        "collectionId" to collectionId,
        "requesterId" to userId,
        "requesterName" to requesterName,
        "collectionName" to collectionName,
        "status" to "PENDING",
        "timestamp" to System.currentTimeMillis()
    )

    try {
        databases.createDocument(
            collectionId = "access_requests",
            documentId = requestId,
            data = requestData
        )
    } catch (e: Exception) {
        // Handle duplicate: if the document already exists, check if it was denied and re-submit
        try {
            val existing = databases.getDocument("access_requests", requestId).getOrNull()
            val existingStatus = existing?.get("status") as? String
            if (existingStatus == "DENIED") {
                // Re-submit the denied request as PENDING
                databases.updateDocument(
                    collectionId = "access_requests",
                    documentId = requestId,
                    data = mapOf(
                        "status" to "PENDING",
                        "requesterName" to requesterName,
                        "collectionName" to collectionName,
                        "timestamp" to System.currentTimeMillis()
                    )
                ).getOrThrow()
            } else if (existingStatus == "PENDING") {
                throw IllegalStateException("Access request already pending")
            } else {
                // Already approved or unknown status
                throw IllegalStateException("Access already granted for this collection")
            }
        } catch (innerEx: IllegalStateException) {
            throw innerEx
        } catch (innerEx: Exception) {
            Log.e("CollectionSyncRepository", "Failed to handle duplicate access request", innerEx)
            throw IllegalStateException("Failed to send access request")
        }
    }
}

fun CollectionSyncRepository.observeAccessRequests(chatId: String): Flow<List<Map<String, Any>>> {
    return callbackFlow {
        suspend fun fetchAndEnrich(): List<Map<String, Any>> {
            val queries = listOf(
                CoreQuery("chatId", CoreQueryOperator.EQUAL, chatId),
                CoreQuery("status", CoreQueryOperator.EQUAL, "PENDING")
            )
            val docs = databases.queryDocuments(
                collectionId = "access_requests",
                queries = queries
            ).getOrThrow()
            return docs.map { doc ->
                val data = doc.toMutableMap()
                val rawTimestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                data["timestamp"] = if (rawTimestamp < 1000000000000L) rawTimestamp * 1000 else rawTimestamp

                // Resolve requester name from local DB if not already in the document
                if ((data["requesterName"] as? String).isNullOrBlank()) {
                    val requesterId = data["requesterId"] as? String ?: ""
                    val user = userDao.getUserById(requesterId)
                    data["requesterName"] = user?.displayName ?: "Someone"
                }

                // Resolve collection name if not already in the document
                if ((data["collectionName"] as? String).isNullOrBlank()) {
                    val collId = data["collectionId"] as? String ?: ""
                    val local = collectionDao.getStudyCollectionByIdOnce(collId)
                    data["collectionName"] = local?.name ?: "Collection"
                }

                data
            }
        }

        repositoryScope.launch {
            try {
                trySend(fetchAndEnrich()).isSuccess
            } catch (e: Exception) {
                Log.e("CollectionSyncRepository", "Initial fetch of access requests failed", e)
            }
        }

        val realtime = io.appwrite.services.Realtime(appwriteClient)
        val subscription = realtime.subscribe("tablesdb.qbase_db.tables.access_requests.rows") { event ->
            repositoryScope.launch {
                try {
                    trySend(fetchAndEnrich()).isSuccess
                } catch (e: Exception) {
                    Log.e("CollectionSyncRepository", "Realtime update of access requests failed", e)
                }
            }
        }
        awaitClose { subscription.close() }
    }
}

suspend fun CollectionSyncRepository.grantCollectionAccess(chatId: String, collectionId: String, requesterId: String) {
    try {
        // 1. Fetch requester's public key from the remote profiles
        val reqUserDoc = databases.getDocument("users", requesterId).getOrNull()
            ?: throw IllegalStateException("Requester user document not found")
        val requesterPublicKey = reqUserDoc["publicKey"] as? String
            ?: throw IllegalStateException("Requester's E2EE public key not found")

        // 2. Fetch the existing shared collection record to update its wrappedKeys map
        val queries = listOf(
            CoreQuery("chatId", CoreQueryOperator.EQUAL, chatId),
            CoreQuery("collectionId", CoreQueryOperator.EQUAL, collectionId)
        )
        val sharedCollDocs = databases.queryDocuments("shared_collections", queries).getOrNull() ?: emptyList()
        val sharedCollDoc = sharedCollDocs.firstOrNull() ?: throw IllegalStateException("Shared collection record not found")
        val targetDocId = sharedCollDoc["\$id"] as? String ?: throw IllegalStateException("Shared collection document ID not found")
        val existingWrappedKeysStr = sharedCollDoc["wrappedKeys"] as? String ?: "{}"
        
        // 3. Find the plain symmetricKey from the admin's local collections database
        val existingWrappedKeysObj = org.json.JSONObject(existingWrappedKeysStr)
        val adminWrappedKey = existingWrappedKeysObj.optString(currentUserId ?: "")
        if (adminWrappedKey.isNullOrBlank()) {
            throw IllegalStateException("Owner's own wrapped key is missing; cannot wrap for requester")
        }
        
        val decResult = cryptoManager.decryptMessage(adminWrappedKey)
        val symmetricKey = decResult.getOrNull()
            ?: throw IllegalStateException("Failed to decrypt collection key using owner keyset")

        // 4. Wrap the symmetric key using the requester's public key
        val newWrappedKeyForRequester = cryptoManager.encryptMessage(symmetricKey, requesterPublicKey)
        
        // 5. Append the new wrapped key to the wrappedKeys map and update Appwrite
        existingWrappedKeysObj.put(requesterId, newWrappedKeyForRequester)
        databases.updateDocument(
            collectionId = "shared_collections",
            documentId = targetDocId,
            data = mapOf("wrappedKeys" to existingWrappedKeysObj.toString())
        ).getOrThrow()

        // 6. Finally, approve the access request document
        val requestId = "${requesterId}_$collectionId"
        databases.updateDocument(
            collectionId = "access_requests",
            documentId = requestId,
            data = mapOf("status" to "APPROVED")
        ).getOrThrow()
        
        Log.d("CollectionSyncRepository", "Access granted to $requesterId for $collectionId")
    } catch (e: Exception) {
        Log.e("CollectionSyncRepository", "Failed to grant access", e)
        throw e
    }
}

suspend fun CollectionSyncRepository.denyCollectionAccess(chatId: String, collectionId: String, requesterId: String) {
    try {
        val requestId = "${requesterId}_$collectionId"
        databases.updateDocument(
            collectionId = "access_requests",
            documentId = requestId,
            data = mapOf("status" to "DENIED")
        ).getOrThrow()
        Log.d("CollectionSyncRepository", "Access denied for $requesterId on $collectionId")
    } catch (e: Exception) {
        Log.e("CollectionSyncRepository", "Failed to deny access", e)
        throw e
    }
}