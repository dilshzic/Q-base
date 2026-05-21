package com.algorithmx.q_base.data.sync

import android.util.Log
import com.algorithmx.q_base.data.backend.CoreQuery
import com.algorithmx.q_base.data.backend.CoreQueryOperator
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
    val subscription = realtime.subscribe("databases.qbase_db.collections.sync_requests.documents") { event ->
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
    val requestId = "${currentUserId}_$collectionId"
    val requestData = mapOf(
        "chatId" to chatId,
        "collectionId" to collectionId,
        "requesterId" to (currentUserId ?: ""),
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
        Log.e("CollectionSyncRepository", "Failed to request collection access", e)
    }
}

fun CollectionSyncRepository.observeAccessRequests(chatId: String): Flow<List<Map<String, Any>>> {
    return callbackFlow {
        repositoryScope.launch {
            try {
                val queries = listOf(
                    CoreQuery("chatId", CoreQueryOperator.EQUAL, chatId),
                    CoreQuery("status", CoreQueryOperator.EQUAL, "PENDING")
                )
                val docs = databases.queryDocuments(
                    collectionId = "access_requests",
                    queries = queries
                ).getOrThrow()
                val mapped = docs.map { doc ->
                    val data = doc.toMutableMap()
                    val rawTimestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                    data["timestamp"] = if (rawTimestamp < 1000000000000L) rawTimestamp * 1000 else rawTimestamp
                    data
                }
                trySend(mapped).isSuccess
            } catch (e: Exception) {}
        }

        val realtime = io.appwrite.services.Realtime(appwriteClient)
        val subscription = realtime.subscribe("databases.qbase_db.collections.access_requests.documents") { event ->
            repositoryScope.launch {
                try {
                    val queries = listOf(
                        CoreQuery("chatId", CoreQueryOperator.EQUAL, chatId),
                        CoreQuery("status", CoreQueryOperator.EQUAL, "PENDING")
                    )
                    val docs = databases.queryDocuments(
                        collectionId = "access_requests",
                        queries = queries
                    ).getOrThrow()
                    val mapped = docs.map { doc ->
                        val data = doc.toMutableMap()
                        val rawTimestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                        data["timestamp"] = if (rawTimestamp < 1000000000000L) rawTimestamp * 1000 else rawTimestamp
                        data
                    }
                    trySend(mapped).isSuccess
                } catch (e: Exception) {}
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
        val sharedCollDoc = databases.getDocument("shared_collections", collectionId).getOrNull()
            ?: throw IllegalStateException("Shared collection record not found")
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
            documentId = collectionId,
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