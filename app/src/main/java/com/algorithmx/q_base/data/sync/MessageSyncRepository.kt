package com.algorithmx.q_base.data.sync

import android.util.Base64
import android.util.Log
import com.algorithmx.q_base.data.chat.ChatDao
import com.algorithmx.q_base.data.chat.ChatEntity
import com.algorithmx.q_base.data.chat.ChatRemoteRepository
import com.algorithmx.q_base.data.chat.MessageDao
import com.algorithmx.q_base.data.chat.MessageEntity
import com.algorithmx.q_base.data.core.UserDao
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.data.auth.AuthRepository
import com.algorithmx.q_base.core_crypto.CryptoManager
import com.algorithmx.q_base.util.NotificationHelper
import io.appwrite.Client
import io.appwrite.Query
import io.appwrite.services.Databases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy

@Singleton
class MessageSyncRepository @Inject constructor(
    private val appwriteClient: Client,
    private val databases: Databases,
    private val authRepository: AuthRepository,
    private val chatRemoteRepository: ChatRemoteRepository,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val cryptoManager: CryptoManager,
    private val collectionSyncRepository: Lazy<CollectionSyncRepository>,
    private val sessionSyncRepository: Lazy<SessionSyncRepository>,
    private val chatManagerRepository: Lazy<ChatManagerRepository>
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val currentUserId: String?
        get() = authRepository.currentUserId

    fun serializeWrappedKeys(map: Map<String, String>): String {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    fun deserializeWrappedKeys(jsonStr: String?): Map<String, String> {
        if (jsonStr.isNullOrBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.getString(key)
            }
        } catch (e: Exception) {
            Log.e("MessageSyncRepository", "Failed to deserialize wrapped keys", e)
        }
        return map
    }

    suspend fun acknowledgeMessageDelivery(
        messageId: String,
        chatId: String,
        senderId: String,
        wrappedKeyStr: String
    ) {
        if (senderId == currentUserId) return // Senders never acknowledge their own sent messages

        val chat = chatDao.getChatById(chatId) ?: return
        if (!chat.isGroup) {
            // For P2P: Delete immediately from Appwrite relay
            try {
                databases.deleteDocument("qbase_db", "messages", messageId)
                Log.d("MessageSyncRepository", "P2P Ephemeral: Cleared message $messageId")
            } catch (e: Exception) {
                Log.e("MessageSyncRepository", "Failed to clear P2P ephemeral message", e)
            }
            return
        }

        // For Group Chats: Receipt-based self-healing cleanup
        try {
            val wrappedKeysMap = deserializeWrappedKeys(wrappedKeyStr).toMutableMap()
            
            // SECURITY/RACE CONDITION AUDIT FIX:
            // Check if we are actually in the pending recipients list.
            if (!wrappedKeysMap.containsKey(currentUserId)) {
                return
            }
            
            wrappedKeysMap.remove(currentUserId) // Remove self
            val pendingReceivers = wrappedKeysMap.keys.filter { it != senderId }

            if (pendingReceivers.isEmpty()) {
                databases.deleteDocument("qbase_db", "messages", messageId)
                Log.d("MessageSyncRepository", "Group Ephemeral: All delivered! Deleted message $messageId")
            } else {
                val updatedWrappedKeyStr = serializeWrappedKeys(wrappedKeysMap)
                databases.updateDocument(
                    databaseId = "qbase_db",
                    collectionId = "messages",
                    documentId = messageId,
                    data = mapOf("wrappedKey" to updatedWrappedKeyStr)
                )
                Log.d("MessageSyncRepository", "Group Ephemeral: Acknowledged delivery for $messageId. Pending: $pendingReceivers")
            }
        } catch (e: Exception) {
            Log.e("MessageSyncRepository", "Error running group ephemeral acknowledgment for $messageId", e)
        }
    }

    fun observeAndSyncMessages(chatId: String): Flow<MessageEntity?> {
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
                        org.json.JSONObject(event.payload as Map<*, *>)
                    } else {
                        org.json.JSONObject(event.payload.toString())
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

    suspend fun sendMessage(message: MessageEntity) {
        val chat = chatDao.getChatById(message.chatId)
        val participants = chat?.participantIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        val senderUid = currentUserId ?: throw IllegalStateException("User not authenticated")

        if (participants.isEmpty()) {
            throw IllegalStateException("Cannot send message: No participants in chat.")
        }

        val (ciphertextPayload, sessionKeyHandle) = cryptoManager.encryptWithSessionKey(message.payload)
        val sessionKeyBytes = Base64.decode(sessionKeyHandle, Base64.NO_WRAP)

        val wrappedKeys = mutableMapOf<String, String>()
        val myFingerprint = cryptoManager.getPublicKeyFingerprint()
        
        val missingKeyUserIds = mutableListOf<String>()
        val wrapFailures = mutableListOf<String>()

        val deferredKeys = participants.map { targetId ->
            repositoryScope.async {
                val publicKeyBase64: String? = if (targetId == senderUid) {
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
                        Log.w("MessageSyncRepository", "Failed to refresh public key for $targetId", e)
                    }
                    candidateKey
                }
                targetId to publicKeyBase64
            }
        }

        val resolvedKeys = deferredKeys.awaitAll()
        
        resolvedKeys.forEach { (targetId, publicKeyBase64) ->
            if (publicKeyBase64.isNullOrBlank()) {
                missingKeyUserIds.add(targetId)
            } else {
                try {
                    wrappedKeys[targetId] = cryptoManager.encryptSessionKey(sessionKeyBytes, publicKeyBase64)
                } catch (e: Exception) {
                    Log.e("MessageSyncRepository", "Key wrap failed for $targetId", e)
                    wrapFailures.add(targetId)
                }
            }
        }

        if (missingKeyUserIds.isNotEmpty()) {
            throw IllegalStateException(
                "Cannot send encrypted message: missing encryption keys for ${missingKeyUserIds.joinToString(", ")}. " +
                    "Ask them to sign in at least once on the latest app version."
            )
        }

        if (wrapFailures.isNotEmpty()) {
            throw IllegalStateException(
                "Cannot send encrypted message: failed to encrypt for ${wrapFailures.joinToString(", ")}."
            )
        }

        val missingWrappedRecipients = participants.filter { wrappedKeys[it].isNullOrBlank() }
        if (missingWrappedRecipients.isNotEmpty()) {
            throw IllegalStateException(
                "Security abort: missing wrapped session keys for ${missingWrappedRecipients.joinToString(", ")}."
            )
        }

        val messageMap = mapOf(
            "chatId" to message.chatId,
            "senderId" to message.senderId,
            "type" to message.type,
            "payload" to ciphertextPayload,
            "wrappedKey" to serializeWrappedKeys(wrappedKeys),
            "keyFingerprint" to myFingerprint.orEmpty(),
            "timestamp" to message.timestamp / 1000
        )
        
        repositoryScope.launch {
            try {
                chatDao.getChatById(message.chatId)?.let { chat ->
                    chatManagerRepository.get().createChatOnRemote(chat)
                }
            } catch (e: Exception) {
                Log.w("MessageSyncRepository", "Auto-ensure chat document exists failed", e)
            }
        }

        try {
            databases.createDocument(
                databaseId = "qbase_db",
                collectionId = "messages",
                documentId = message.messageId,
                data = messageMap,
                permissions = listOf(
                    io.appwrite.Permission.read(io.appwrite.Role.users()),
                    io.appwrite.Permission.write(io.appwrite.Role.users()),
                    io.appwrite.Permission.update(io.appwrite.Role.users()),
                    io.appwrite.Permission.delete(io.appwrite.Role.users())
                )
            )

            repositoryScope.launch {
                messageDao.insertMessage(message.copy(
                    keyFingerprint = myFingerprint,
                    wrappedKey = wrappedKeys[senderUid],
                    decryptionStatus = "SUCCESS"
                ))
            }
        } catch (e: Exception) {
            Log.e("MessageSyncRepository", "Failed to send message to Appwrite", e)
            throw e
        }
    }

    suspend fun flushQueue() {
        try {
            val pendingMessages = messageDao.getPendingMessages()
            Log.d("MessageSyncRepository", "Flushing ${pendingMessages.size} pending messages...")
            for (message in pendingMessages) {
                try {
                    sendMessage(message)
                    messageDao.updateMessageStatus(message.messageId, "SENT")
                    Log.d("MessageSyncRepository", "Successfully flushed message ${message.messageId}")
                } catch (e: Exception) {
                    Log.e("MessageSyncRepository", "Failed to flush pending message ${message.messageId}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("MessageSyncRepository", "Failed to get pending messages from local DB", e)
        }
    }

    fun observeAllIncomingMessages(notificationHelper: NotificationHelper): Flow<Unit> {
        val userId = currentUserId ?: return emptyFlow()
        return callbackFlow {
            val realtime = io.appwrite.services.Realtime(appwriteClient)
            val subscription = realtime.subscribe("databases.qbase_db.collections.messages.documents") { event ->
                try {
                    val payloadObj = if (event.payload is Map<*, *>) {
                        org.json.JSONObject(event.payload as Map<*, *>)
                    } else {
                        org.json.JSONObject(event.payload.toString())
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
                        org.json.JSONObject(event.payload as Map<*, *>)
                    } else {
                        org.json.JSONObject(event.payload.toString())
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
                            @Suppress("UNCHECKED_CAST")
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

    suspend fun fetchAndSyncMessages(chatId: String) {
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

    suspend fun clearChatMessagesOnRemote(chatId: String) {
        chatRemoteRepository.clearChatMessagesOnRemote(chatId)
    }
}
