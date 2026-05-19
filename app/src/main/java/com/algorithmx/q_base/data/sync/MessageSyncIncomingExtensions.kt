package com.algorithmx.q_base.data.sync

import android.util.Base64
import android.util.Log
import com.algorithmx.q_base.data.chat.MessageEntity
import io.appwrite.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

fun MessageSyncRepository.observeAndSyncMessages(chatId: String): Flow<MessageEntity?> {
    val uid = currentUserId
    if (uid != null) {
        repositoryScope.launch {
            try {
                val localPk = cryptoManager.initializeAndGetPublicKey()
                val doc = databases.getDocument(
                    databaseId = "qbase_db",
                    collectionId = "users",
                    documentId = uid
                )
                val remotePk = doc.data["publicKey"] as? String
                if (remotePk != localPk) {
                    Log.d("MessageSyncRepository", "Aligning E2EE public key on Appwrite for current user $uid.")
                    val updatedData = doc.data.toMutableMap()
                    updatedData["publicKey"] = localPk
                    val cleanData = updatedData.filterKeys { !it.startsWith("$") }
                    databases.updateDocument(
                        databaseId = "qbase_db",
                        collectionId = "users",
                        documentId = uid,
                        data = cleanData
                    )
                }
            } catch (e: Exception) {
                Log.e("MessageSyncRepository", "E2EE public key alignment failed", e)
            }
        }
    }

    return callbackFlow {
        repositoryScope.launch {
            try {
                val response = databases.listDocuments(
                    databaseId = "qbase_db",
                    collectionId = "messages",
                    queries = listOf(Query.equal("chatId", chatId))
                )
                response.documents.forEach { doc ->
                    val payloadVal = doc.data["payload"] as? String ?: ""
                    val senderId = doc.data["senderId"] as? String ?: ""
                    val type = doc.data["type"] as? String ?: "TEXT"
                    val rawTimestamp = (doc.data["timestamp"] as? Number)?.toLong() ?: (System.currentTimeMillis() / 1000)
                    val timestamp = if (rawTimestamp < 1000000000000L) rawTimestamp * 1000 else rawTimestamp
                    val wrappedKeyStr = doc.data["wrappedKey"] as? String
                    val keyFingerprint = doc.data["keyFingerprint"] as? String

                    val wrappedKeyMap = deserializeWrappedKeys(wrappedKeyStr)
                    val isEncrypted = wrappedKeyMap.isNotEmpty() && payloadVal.isNotEmpty()

                    var wrappedKey: String? = null
                    var payload = payloadVal
                    var decryptionStatus = if (isEncrypted) "DECRYPTION_ERROR" else "NOT_ENCRYPTED"

                    if (isEncrypted) {
                        val uid = currentUserId
                        if (uid == null) {
                            decryptionStatus = "FAILED"
                        } else {
                            wrappedKey = wrappedKeyMap[uid]
                            if (wrappedKey == null) {
                                decryptionStatus = "FAILED"
                            } else {
                                val unwrapResult = cryptoManager.decryptSessionKey(wrappedKey)
                                if (unwrapResult.isSuccess) {
                                    val sessionKeyHandle = Base64.encodeToString(unwrapResult.getOrThrow(), Base64.NO_WRAP)
                                    val decryptResult = cryptoManager.decryptWithSessionKey(payloadVal, sessionKeyHandle)
                                    if (decryptResult.isSuccess) {
                                        payload = decryptResult.getOrNull() ?: ""
                                        decryptionStatus = "SUCCESS"
                                    } else {
                                        decryptionStatus = "DECRYPTION_ERROR"
                                        payload = "[Message locked: Decryption key lost. Set up Secure Backup to prevent this.]"
                                    }
                                } else {
                                    decryptionStatus = "DECRYPTION_ERROR"
                                    payload = "[Message locked: Decryption key lost. Set up Secure Backup to prevent this.]"
                                }
                            }
                        }
                    }

                    val message = MessageEntity(
                        messageId = doc.id,
                        chatId = chatId,
                        senderId = senderId,
                        payload = payload,
                        type = type,
                        timestamp = timestamp,
                        decryptionStatus = decryptionStatus,
                        keyFingerprint = keyFingerprint ?: "",
                        wrappedKey = wrappedKey ?: ""
                    )

                    if (messageDao.getMessageById(doc.id) == null) {
                        messageDao.insertMessage(message)
                        
                        if (type == "COLLECTION_PATCH" && decryptionStatus == "SUCCESS") {
                            collectionSyncRepository.get().applyCollectionPatch(payload)
                        }

                        if (type == "SESSION_PATCH" && decryptionStatus == "SUCCESS") {
                            sessionSyncRepository.get().applySessionPatch(payload)
                        }

                        if (decryptionStatus == "SUCCESS" && keyFingerprint != null) {
                            chatDao.getChatById(chatId)?.let { chat ->
                                if (chat.lastUsedKeyFingerprint != keyFingerprint) {
                                    chatDao.insertChat(chat.copy(lastUsedKeyFingerprint = keyFingerprint))
                                }
                            }
                        }

                        if (message.senderId != currentUserId) {
                            trySend(message).isSuccess
                        }
                    }
                    repositoryScope.launch {
                        acknowledgeMessageDelivery(doc.id, chatId, senderId, wrappedKeyStr ?: "")
                    }
                }
                trySend(null).isSuccess
            } catch (e: Exception) {
                Log.e("MessageSyncRepository", "Initial fetch messages error", e)
            }
        }

        val realtime = io.appwrite.services.Realtime(appwriteClient)
        val subscription = realtime.subscribe("databases.qbase_db.collections.messages.documents") { event ->
            try {
                val payloadObj = if (event.payload is Map<*, *>) {
                    JSONObject(event.payload as Map<*, *>)
                } else {
                    JSONObject(event.payload.toString())
                }
                val docChatId = payloadObj.optString("chatId")
                if (docChatId == chatId) {
                    if (event.events.any { it.contains(".delete") }) {
                        return@subscribe
                    }

                    repositoryScope.launch {
                        val senderId = payloadObj.optString("senderId", "")
                        chatManagerRepository.get().ensureChatExistsLocally(chatId, senderId)

                        val docId = payloadObj.optString("\$id")
                        val type = payloadObj.optString("type", "TEXT")
                        val payloadVal = payloadObj.optString("payload", "")
                        val rawTimestamp = payloadObj.optLong("timestamp", System.currentTimeMillis() / 1000)
                        val timestamp = if (rawTimestamp < 1000000000000L) rawTimestamp * 1000 else rawTimestamp
                        val wrappedKeyStr = payloadObj.optString("wrappedKey", "")
                        val keyFingerprint = payloadObj.optString("keyFingerprint", null)

                        val wrappedKeyMap = deserializeWrappedKeys(wrappedKeyStr)
                        val isEncrypted = wrappedKeyMap.isNotEmpty() && payloadVal.isNotEmpty()

                        var wrappedKey: String? = null
                        var payload = payloadVal
                        var decryptionStatus = if (isEncrypted) "DECRYPTION_ERROR" else "NOT_ENCRYPTED"

                        if (isEncrypted) {
                            val uid = currentUserId
                            if (uid == null) {
                                decryptionStatus = "FAILED"
                            } else {
                                wrappedKey = wrappedKeyMap[uid]
                                if (wrappedKey == null) {
                                    decryptionStatus = "FAILED"
                                } else {
                                    val unwrapResult = cryptoManager.decryptSessionKey(wrappedKey)
                                    if (unwrapResult.isSuccess) {
                                        val sessionKeyHandle = Base64.encodeToString(unwrapResult.getOrThrow(), Base64.NO_WRAP)
                                        val decryptResult = cryptoManager.decryptWithSessionKey(payloadVal, sessionKeyHandle)
                                        if (decryptResult.isSuccess) {
                                            payload = decryptResult.getOrNull() ?: ""
                                            decryptionStatus = "SUCCESS"
                                        } else {
                                            decryptionStatus = "DECRYPTION_ERROR"
                                            payload = "[Message locked: Decryption key lost. Set up Secure Backup to prevent this.]"
                                        }
                                    } else {
                                        decryptionStatus = "DECRYPTION_ERROR"
                                        payload = "[Message locked: Decryption key lost. Set up Secure Backup to prevent this.]"
                                    }
                                }
                            }
                        }

                        val message = MessageEntity(
                            messageId = docId,
                            chatId = chatId,
                            senderId = senderId,
                            payload = payload,
                            type = type,
                            timestamp = timestamp,
                            decryptionStatus = decryptionStatus,
                            keyFingerprint = keyFingerprint ?: "",
                            wrappedKey = wrappedKey ?: ""
                        )

                        if (messageDao.getMessageById(docId) == null) {
                            messageDao.insertMessage(message)

                            if (type == "COLLECTION_PATCH" && decryptionStatus == "SUCCESS") {
                                collectionSyncRepository.get().applyCollectionPatch(payload)
                            }

                            if (type == "SESSION_PATCH" && decryptionStatus == "SUCCESS") {
                                sessionSyncRepository.get().applySessionPatch(payload)
                            }

                            if (decryptionStatus == "SUCCESS" && keyFingerprint != null) {
                                chatDao.getChatById(chatId)?.let { chat ->
                                    if (chat.lastUsedKeyFingerprint != keyFingerprint) {
                                        chatDao.insertChat(chat.copy(lastUsedKeyFingerprint = keyFingerprint))
                                    }
                                }
                            }

                            if (message.senderId != currentUserId) {
                                trySend(message).isSuccess
                            }
                        }

                        repositoryScope.launch {
                            acknowledgeMessageDelivery(docId, chatId, senderId, wrappedKeyStr)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MessageSyncRepository", "Observe messages parse error", e)
            }
        }
        awaitClose {
            subscription.close()
        }
    }
}
