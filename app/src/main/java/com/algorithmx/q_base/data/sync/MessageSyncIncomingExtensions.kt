package com.algorithmx.q_base.data.sync

import android.util.Base64
import android.util.Log
import com.algorithmx.q_base.data.chat.ChatEntity
import com.algorithmx.q_base.data.chat.MessageEntity
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.util.NotificationHelper
import io.appwrite.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
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

fun MessageSyncRepository.observeAllIncomingMessages(notificationHelper: NotificationHelper): Flow<Unit> {
    val userId = currentUserId ?: return emptyFlow()
    return callbackFlow {
        val realtime = io.appwrite.services.Realtime(appwriteClient)
        val subscription = realtime.subscribe("databases.qbase_db.collections.messages.documents") { event ->
            try {
                val payloadObj = if (event.payload is Map<*, *>) {
                    JSONObject(event.payload as Map<*, *>)
                } else {
                    JSONObject(event.payload.toString())
                }
                val docId = payloadObj.optString("\$id").ifEmpty { payloadObj.optString("id") }
                if (event.events.any { it.contains(".delete") }) {
                    return@subscribe
                }

                val docChatId = payloadObj.optString("chatId")
                val senderId = payloadObj.optString("senderId")
                if (senderId == userId) {
                    return@subscribe
                }

                repositoryScope.launch {
                    chatManagerRepository.get().ensureChatExistsLocally(docChatId, senderId)

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
                        if (uid != null) {
                            wrappedKey = wrappedKeyMap[uid]
                            if (wrappedKey != null) {
                                val unwrapResult = cryptoManager.decryptSessionKey(wrappedKey)
                                if (unwrapResult.isSuccess) {
                                    val sessionKeyHandle = Base64.encodeToString(unwrapResult.getOrThrow(), Base64.NO_WRAP)
                                    val decryptResult = cryptoManager.decryptWithSessionKey(payloadVal, sessionKeyHandle)
                                    if (decryptResult.isSuccess) {
                                        payload = decryptResult.getOrNull() ?: ""
                                        decryptionStatus = "SUCCESS"
                                    }
                                }
                            }
                        }
                    }

                    val message = MessageEntity(
                        messageId = docId,
                        chatId = docChatId,
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
                        chatDao.incrementUnreadCount(docChatId)

                        val localChat = chatDao.getChatById(docChatId)
                        if (localChat?.isMuted != true && localChat?.isBlocked != true) {
                            val senderUser = userDao.getUserById(senderId)
                            val senderName = senderUser?.displayName 
                                ?: localChat?.chatName 
                                ?: "New Message"
                            val notificationBody = when {
                                type != "TEXT" -> "Sent an attachment"
                                decryptionStatus == "SUCCESS" -> payload
                                else -> "New encrypted message"
                            }
                            notificationHelper.showMessageNotification(
                                chatId = docChatId,
                                senderName = senderName,
                                message = notificationBody
                            )
                        }

                        if (type == "SESSION_PATCH" && decryptionStatus == "SUCCESS") {
                            try {
                                val patchJson = JSONObject(payload)
                                val sessionTitle = patchJson.optString("title", "Study Session")
                                val sessionId = patchJson.optString("sessionId", "")
                                if (sessionId.isNotEmpty()) {
                                    notificationHelper.showSessionNotification(
                                        sessionId = sessionId,
                                        title = "Session Invite",
                                        description = "You've been invited to $sessionTitle"
                                    )
                                }
                            } catch (e: Exception) {}
                        }

                        if (type == "COLLECTION_MICRO_UPDATE" && decryptionStatus == "SUCCESS") {
                            collectionSyncRepository.get().applyCollectionMicroUpdate(payload)
                        }
                    }

                    repositoryScope.launch {
                        acknowledgeMessageDelivery(docId, docChatId, senderId, wrappedKeyStr)
                    }
                }
            } catch (e: Exception) {
                Log.e("MessageSyncRepository", "Observe global messages error", e)
            }
        }

        val chatsSubscription = realtime.subscribe("databases.qbase_db.collections.chats.documents") { event ->
            try {
                val payloadObj = if (event.payload is Map<*, *>) {
                    JSONObject(event.payload as Map<*, *>)
                } else {
                    JSONObject(event.payload.toString())
                }
                val docId = payloadObj.optString("\$id").ifEmpty { payloadObj.optString("id") }
                val isGroup = payloadObj.optBoolean("isGroup", false)

                if (event.events.any { it.contains(".delete") }) {
                    repositoryScope.launch {
                        chatDao.deleteChatById(docId)
                        messageDao.deleteMessagesByChatId(docId)
                    }
                } else if (event.events.any { it.contains(".create") || it.contains(".update") }) {
                    repositoryScope.launch {
                        val participantsList = payloadObj.optJSONArray("participantIds")?.let { arr ->
                            List(arr.length()) { arr.getString(it) }
                        } ?: emptyList()

                        if (isGroup) {
                            if (participantsList.contains(userId)) {
                                val remoteAdminIds = payloadObj.optJSONArray("adminIds")?.let { arr ->
                                    List(arr.length()) { arr.getString(it) }
                                } ?: emptyList()
                                val remoteAdminId = payloadObj.optString("adminId")
                                val chat = ChatEntity(
                                    chatId = docId,
                                    chatName = payloadObj.optString("chatName", "Group Chat"),
                                    isGroup = true,
                                    participantIds = participantsList.joinToString(","),
                                    adminIds = if (remoteAdminIds.isNotEmpty()) remoteAdminIds.joinToString(",") else remoteAdminId
                                )
                                chatDao.insertChat(chat)
                            } else {
                                chatDao.deleteChatById(docId)
                                messageDao.deleteMessagesByChatId(docId)
                            }
                        }
                        
                        val localChat = chatDao.getChatById(docId)
                        if (localChat != null) {
                            val remoteAdminIds = payloadObj.optJSONArray("adminIds")?.let { arr ->
                                List(arr.length()) { arr.getString(it) }
                            } ?: emptyList()
                            val remoteAdminId = payloadObj.optString("adminId")
                            val updatedChat = localChat.copy(
                                chatName = payloadObj.optString("chatName", localChat.chatName),
                                participantIds = participantsList.joinToString(","),
                                adminIds = if (remoteAdminIds.isNotEmpty()) remoteAdminIds.joinToString(",") else remoteAdminId
                            )
                            chatDao.insertChat(updatedChat)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MessageSyncRepository", "Observe global chats delete/update error", e)
            }
        }
    
        awaitClose { 
            subscription.close() 
            chatsSubscription.close()
        }
    }
}

suspend fun MessageSyncRepository.fetchAndSyncMessages(chatId: String) {
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
                if (uid != null) {
                    wrappedKey = wrappedKeyMap[uid]
                    if (wrappedKey != null) {
                        val unwrapResult = cryptoManager.decryptSessionKey(wrappedKey)
                        if (unwrapResult.isSuccess) {
                            val sessionKeyHandle = Base64.encodeToString(unwrapResult.getOrThrow(), Base64.NO_WRAP)
                            val decryptResult = cryptoManager.decryptWithSessionKey(payloadVal, sessionKeyHandle)
                            if (decryptResult.isSuccess) {
                                payload = decryptResult.getOrNull() ?: ""
                                decryptionStatus = "SUCCESS"
                            }
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
            }
            repositoryScope.launch {
                acknowledgeMessageDelivery(doc.id, chatId, senderId, wrappedKeyStr ?: "")
            }
        }
    } catch (e: Exception) {
        Log.e("MessageSyncRepository", "fetchAndSyncMessages error", e)
    }
}
