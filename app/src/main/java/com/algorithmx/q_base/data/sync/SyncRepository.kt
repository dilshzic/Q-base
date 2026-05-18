package com.algorithmx.q_base.data.sync

import android.util.Base64
import android.util.Log
import com.algorithmx.q_base.BuildConfig
import com.algorithmx.q_base.data.chat.ChatDao
import com.algorithmx.q_base.data.chat.ChatEntity
import com.algorithmx.q_base.data.chat.isAdmin
import com.algorithmx.q_base.data.chat.ChatRemoteRepository
import com.algorithmx.q_base.data.chat.MessageDao
import com.algorithmx.q_base.data.chat.MessageEntity
import com.algorithmx.q_base.data.collections.*
import com.algorithmx.q_base.data.core.UserDao
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.data.auth.UserProfile
import com.algorithmx.q_base.data.sessions.SessionDao
import com.algorithmx.q_base.data.sessions.SessionAttempt
import com.algorithmx.q_base.data.sessions.StudySession
import com.algorithmx.q_base.core_crypto.CryptoManager
import com.algorithmx.q_base.data.util.MockDownloader
import com.algorithmx.q_base.util.NotificationHelper
import com.algorithmx.q_base.data.auth.AuthRepository
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.models.InputFile
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val appwriteClient: Client,
    private val databases: Databases,
    private val authRepository: AuthRepository,
    private val chatRemoteRepository: ChatRemoteRepository,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val cryptoManager: CryptoManager,
    private val mockDownloader: MockDownloader,
    private val storage: Storage,
    private val sessionDao: SessionDao,
    private val problemReportDao: ProblemReportDao,
    private val collectionDao: CollectionDao,
    private val questionDao: QuestionDao
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bucketId = BuildConfig.APPWRITE_BUCKET_ID
    private val projectId = BuildConfig.APPWRITE_PROJECT_ID

    private val currentUserId: String?
        get() = authRepository.currentUserId

    private fun serializeWrappedKeys(map: Map<String, String>): String {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    private fun deserializeWrappedKeys(jsonStr: String?): Map<String, String> {
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
            Log.e("SyncRepository", "Failed to deserialize wrapped keys", e)
        }
        return map
    }

    private suspend fun acknowledgeMessageDelivery(
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
                Log.d("SyncRepository", "P2P Ephemeral: Cleared message $messageId")
            } catch (e: Exception) {
                Log.e("SyncRepository", "Failed to clear P2P ephemeral message", e)
            }
            return
        }

        // For Group Chats: Receipt-based self-healing cleanup
        try {
            val wrappedKeysMap = deserializeWrappedKeys(wrappedKeyStr).toMutableMap()
            wrappedKeysMap.remove(currentUserId) // Remove self

            // Filter out the sender to find pending receivers
            val pendingReceivers = wrappedKeysMap.keys.filter { it != senderId }

            if (pendingReceivers.isEmpty()) {
                // We are the last recipient to fetch the message: delete completely
                databases.deleteDocument("qbase_db", "messages", messageId)
                Log.d("SyncRepository", "Group Ephemeral: All delivered! Deleted message $messageId")
            } else {
                // Other recipients are pending: Update document with our receipt checked off
                val updatedWrappedKeyStr = serializeWrappedKeys(wrappedKeysMap)
                databases.updateDocument(
                    databaseId = "qbase_db",
                    collectionId = "messages",
                    documentId = messageId,
                    data = mapOf("wrappedKey" to updatedWrappedKeyStr)
                )
                Log.d("SyncRepository", "Group Ephemeral: Acknowledged delivery for $messageId. Pending: $pendingReceivers")
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error running group ephemeral acknowledgment for $messageId", e)
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
                        Log.d("SyncRepository", "Aligning E2EE public key on Appwrite for current user $uid. Remote: $remotePk, Local: $localPk")
                        val updatedData = doc.data.toMutableMap()
                        updatedData["publicKey"] = localPk
                        val cleanData = updatedData.filterKeys { !it.startsWith("$") }
                        databases.updateDocument(
                            databaseId = "qbase_db",
                            collectionId = "users",
                            documentId = uid,
                            data = cleanData
                        )
                        Log.d("SyncRepository", "E2EE public key alignment successful on Appwrite!")
                    }
                } catch (e: Exception) {
                    Log.e("SyncRepository", "E2EE public key alignment failed", e)
                }
            }
        }

        return callbackFlow {
            // Initial load of past messages from database
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
                            repositoryScope.launch {
                                acknowledgeMessageDelivery(doc.id, chatId, senderId, wrappedKeyStr ?: "")
                            }
                        }
                    }
                    trySend(null).isSuccess
                } catch (e: Exception) {
                    Log.e("SyncRepository", "Initial fetch messages error", e)
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
                        val docId = payloadObj.optString("\$id").ifEmpty { payloadObj.optString("id") }
                        if (event.events.any { it.contains(".delete") }) {
                            // Cloud cleanup events are ignored to preserve permanent local Room DB storage
                            return@subscribe
                        }

                        repositoryScope.launch {
                            val senderId = payloadObj.optString("senderId", "")
                            ensureChatExistsLocally(chatId, senderId)

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
                                    applyCollectionPatch(payload)
                                }

                                if (type == "SESSION_PATCH" && decryptionStatus == "SUCCESS") {
                                    applySessionPatch(payload)
                                }

                                if (decryptionStatus == "SUCCESS" && keyFingerprint != null) {
                                    chatDao.getChatById(chatId)?.let { chat ->
                                        if (chat.lastUsedKeyFingerprint != keyFingerprint) {
                                            chatDao.insertChat(chat.copy(lastUsedKeyFingerprint = keyFingerprint))
                                        }
                                    }
                                }

                                repositoryScope.launch {
                                    acknowledgeMessageDelivery(docId, chatId, senderId, wrappedKeyStr)
                                }

                                if (message.senderId != currentUserId) {
                                    trySend(message).isSuccess
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SyncRepository", "Observe messages parse error", e)
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
                        Log.w("SyncRepository", "Failed to refresh public key for $targetId; using cached key if present", e)
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
                    Log.e("SyncRepository", "Key wrap failed for $targetId", e)
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
        
        // Self-healing: Ensure chat document exists on remote Appwrite
        repositoryScope.launch {
            try {
                chatDao.getChatById(message.chatId)?.let { chat ->
                    createChatOnRemote(chat)
                }
            } catch (e: Exception) {
                Log.w("SyncRepository", "Auto-ensure chat document exists failed", e)
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
            // Chat message notifications are now handled by the receiver's
            // observeAllIncomingMessages() — decrypted locally, never sent as plaintext
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to send message to Appwrite", e)
            throw e
        }
    }

    suspend fun addParticipantToRemote(chatId: String, userId: String) {
        chatRemoteRepository.addParticipantToRemote(chatId, userId)
    }

    suspend fun removeParticipantFromRemote(chatId: String, userId: String) {
        chatRemoteRepository.removeParticipantFromRemote(chatId, userId)
    }

    suspend fun promoteParticipantToAdminOnRemote(chatId: String, userId: String) {
        chatRemoteRepository.promoteParticipantToAdminOnRemote(chatId, userId)
    }

    suspend fun demoteAdminOnRemote(chatId: String, userId: String) {
        chatRemoteRepository.demoteAdminOnRemote(chatId, userId)
    }

    suspend fun createChatOnRemote(chat: ChatEntity) {
        chatRemoteRepository.createChatOnRemote(chat)
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
                        // Cloud cleanup events are ignored to preserve permanent local Room DB storage
                        return@subscribe
                    }

                    val docChatId = payloadObj.optString("chatId")
                    val senderId = payloadObj.optString("senderId")
                    if (senderId == userId) {
                        return@subscribe
                    }

                    repositoryScope.launch {
                        ensureChatExistsLocally(docChatId, senderId)

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

                            repositoryScope.launch {
                                acknowledgeMessageDelivery(docId, docChatId, senderId, wrappedKeyStr)
                            }

                            // Show local notification using the locally-decrypted content
                            // No plaintext ever leaves the device — true E2EE
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

                            // Show session invite notification for SESSION_PATCH messages
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
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SyncRepository", "Observe global messages error", e)
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
                        return@subscribe
                    }

                    if (event.events.any { it.contains(".update") }) {
                        val participantsList = mutableListOf<String>()
                        val participantsJson = payloadObj.optJSONArray("participantIds")
                        if (participantsJson != null) {
                            for (i in 0 until participantsJson.length()) {
                                participantsList.add(participantsJson.getString(i))
                            }
                        }

                        repositoryScope.launch {
                            val currentUserId = currentUserId ?: return@launch
                            if (!isGroup) {
                                // For 1-1 / P2P chats: if either participant has deleted/left
                                // (i.e. participantsList does not contain both of them anymore),
                                // it must be deleted for the other user too!
                                if (participantsList.size < 2 || !participantsList.contains(currentUserId)) {
                                    chatDao.deleteChatById(docId)
                                    messageDao.deleteMessagesByChatId(docId)

                                    // Clean up from Appwrite completely if we are the remaining participant
                                    try {
                                        chatRemoteRepository.clearChatMessagesOnRemote(docId)
                                        databases.deleteDocument("qbase_db", "chats", docId)
                                    } catch (_: Exception) {}
                                }
                            } else {
                                // For group chats: if current user was removed, clean up local DB
                                if (!participantsList.contains(currentUserId)) {
                                    chatDao.deleteChatById(docId)
                                    messageDao.deleteMessagesByChatId(docId)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SyncRepository", "Observe global chats delete/update error", e)
                }
            }
        
            awaitClose { 
                subscription.close() 
                chatsSubscription.close()
            }
        }
    }

    suspend fun syncUserChatsFromRemote() {
        val uid = currentUserId ?: return
        var success = false
        var attempts = 0
        while (!success && attempts < 3) {
            try {
                Log.d("SyncRepository", "Syncing user chats from remote (attempt ${attempts + 1}) for uid: $uid")
                val response = databases.listDocuments(
                    databaseId = "qbase_db",
                    collectionId = "chats",
                    queries = listOf(
                        Query.contains("participantIds", uid)
                    )
                )
                for (doc in response.documents) {
                    @Suppress("UNCHECKED_CAST")
                    val participantsList = doc.data["participantIds"] as? List<String> ?: emptyList()
                    val remoteAdminIds = doc.data["adminIds"] as? List<String>
                    val remoteAdminId = doc.data["adminId"] as? String
                    val isGroupVal = doc.data["isGroup"] as? Boolean ?: false
                    
                    val chat = ChatEntity(
                        chatId = doc.id,
                        chatName = doc.data["chatName"] as? String,
                        isGroup = isGroupVal,
                        participantIds = participantsList.joinToString(","),
                        adminIds = if (!remoteAdminIds.isNullOrEmpty()) remoteAdminIds.joinToString(",") else (remoteAdminId ?: "")
                    )
                    chatDao.insertChat(chat)
                    Log.d("SyncRepository", "Synced chat from remote: ${chat.chatId} (${chat.chatName})")
                    
                    // Fetch and sync messages for this chat
                    fetchAndSyncMessages(chat.chatId)
                }
                success = true
            } catch (e: Exception) {
                attempts++
                Log.e("SyncRepository", "Failed to sync user chats from remote (attempt $attempts)", e)
                if (attempts < 3) {
                    kotlinx.coroutines.delay(2000)
                } else {
                    throw e
                }
            }
        }
    }

    suspend fun findExistingP2PChat(uid: String, userId: String): ChatEntity? {
        try {
            val localChats = chatDao.getAllChats().first()
            val existing = localChats.find { chat ->
                if (chat.isGroup) return@find false
                val list = chat.participantIds.split(",").map { it.trim() }
                list.contains(uid) && list.contains(userId)
            }
            if (existing != null) return existing
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error fetching local chats", e)
        }

        try {
            val response = databases.listDocuments(
                databaseId = "qbase_db",
                collectionId = "chats",
                queries = listOf(
                    Query.equal("isGroup", false)
                )
            )
            for (doc in response.documents) {
                @Suppress("UNCHECKED_CAST")
                val participantsList = doc.data["participantIds"] as? List<String> ?: emptyList()
                val trimmedParticipants = participantsList.map { it.trim() }
                if (trimmedParticipants.contains(uid) && trimmedParticipants.contains(userId)) {
                    val remoteAdminIds = doc.data["adminIds"] as? List<String>
                    val remoteAdminId = doc.data["adminId"] as? String
                    val chat = ChatEntity(
                        chatId = doc.id,
                        chatName = doc.data["chatName"] as? String,
                        isGroup = false,
                        participantIds = participantsList.joinToString(","),
                        adminIds = if (!remoteAdminIds.isNullOrEmpty()) remoteAdminIds.joinToString(",") else (remoteAdminId ?: "")
                    )
                    chatDao.insertChat(chat)
                    return chat
                }
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Error querying Appwrite for existing P2P chat", e)
        }
        return null
    }

    private suspend fun ensureChatExistsLocally(chatId: String, senderId: String? = null) {
        val localChat = chatDao.getChatById(chatId)
        if (localChat == null) {
            fetchAndSyncChatMetadata(chatId)
            val updatedChat = chatDao.getChatById(chatId)
            if (updatedChat == null) {
                val userId = currentUserId ?: ""
                val peerId = senderId ?: ""
                val dummyChat = ChatEntity(
                    chatId = chatId,
                    chatName = "Secure Chat",
                    isGroup = false,
                    participantIds = if (peerId.isNotEmpty()) "$userId,$peerId" else userId,
                    adminIds = ""
                )
                chatDao.insertChat(dummyChat)
            }
        }
    }

    private suspend fun fetchAndSyncChatMetadata(chatId: String) {
        try {
            val doc = databases.getDocument(
                databaseId = "qbase_db",
                collectionId = "chats",
                documentId = chatId
            )
            @Suppress("UNCHECKED_CAST")
            val participantsList = doc.data["participantIds"] as? List<String> ?: emptyList()
            val remoteAdminIds = doc.data["adminIds"] as? List<String>
            val remoteAdminId = doc.data["adminId"] as? String
            val chat = ChatEntity(
                chatId = chatId,
                chatName = doc.data["chatName"] as? String,
                isGroup = doc.data["isGroup"] as? Boolean ?: false,
                participantIds = participantsList.joinToString(","),
                adminIds = if (!remoteAdminIds.isNullOrEmpty()) remoteAdminIds.joinToString(",") else (remoteAdminId ?: "")
            )
            chatDao.insertChat(chat)
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to fetch chat metadata", e)
        }
    }

    suspend fun fetchAndSyncMessages(chatId: String) {
        try {
            val response = databases.listDocuments(
                databaseId = "qbase_db",
                collectionId = "messages",
                queries = listOf(
                    Query.equal("chatId", chatId),
                    Query.limit(100)
                )
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
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "fetchAndSyncMessages error", e)
        }
    }

    suspend fun sendSessionInvite(
        chatId: String, 
        sessionId: String, 
        sessionTitle: String,
        downloadUrl: String,
        symmetricKey: String
    ) {
        val senderId = currentUserId ?: return
        val messageId = UUID.randomUUID().toString()
        
        val message = MessageEntity(
            messageId = messageId,
            chatId = chatId,
            senderId = senderId,
            payload = "$downloadUrl|E2EE_KEY|$symmetricKey|SESSION_ID|$sessionId|TITLE|$sessionTitle",
            type = "SESSION_INVITE",
            timestamp = System.currentTimeMillis()
        )
        
        sendMessage(message)
        // Session invite notifications are now handled by the receiver's
        // observeAllIncomingMessages() via the SESSION_INVITE message type
    }

    suspend fun sendSyncRequest(targetUserId: String, targetCollectionId: String) {
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
                databaseId = "qbase_db",
                collectionId = "sync_requests",
                documentId = requestRefId,
                data = syncRequest,
                permissions = listOf(
                    io.appwrite.Permission.read(io.appwrite.Role.users()),
                    io.appwrite.Permission.delete(io.appwrite.Role.users()),
                    io.appwrite.Permission.read(io.appwrite.Role.user(senderId)),
                    io.appwrite.Permission.write(io.appwrite.Role.user(senderId)),
                    io.appwrite.Permission.update(io.appwrite.Role.user(senderId)),
                    io.appwrite.Permission.delete(io.appwrite.Role.user(senderId))
                )
            )
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to send sync request in Appwrite", e)
        }
    }

    fun observeIncomingRequests(): Flow<List<SyncRequest>> = callbackFlow {
        val userId = currentUserId ?: return@callbackFlow
        
        repositoryScope.launch {
            try {
                val response = databases.listDocuments(
                    databaseId = "qbase_db",
                    collectionId = "sync_requests",
                    queries = listOf(
                        Query.equal("targetUserId", userId),
                        Query.equal("status", "PENDING")
                    )
                )
                val list = response.documents.map { doc ->
                    SyncRequest(
                        requestId = doc.id,
                        senderId = (doc.data["senderId"] as? String) ?: "",
                        targetCollectionId = (doc.data["targetCollectionId"] as? String) ?: "",
                        status = (doc.data["status"] as? String) ?: "PENDING"
                    )
                }
                trySend(list).isSuccess
            } catch (e: Exception) {}
        }

        val realtime = io.appwrite.services.Realtime(appwriteClient)
        val subscription = realtime.subscribe("databases.qbase_db.collections.sync_requests.documents") { event ->
            repositoryScope.launch {
                try {
                    val response = databases.listDocuments(
                        databaseId = "qbase_db",
                        collectionId = "sync_requests",
                        queries = listOf(
                            Query.equal("targetUserId", userId),
                            Query.equal("status", "PENDING")
                        )
                    )
                    val list = response.documents.map { doc ->
                        SyncRequest(
                            requestId = doc.id,
                            senderId = (doc.data["senderId"] as? String) ?: "",
                            targetCollectionId = (doc.data["targetCollectionId"] as? String) ?: "",
                            status = (doc.data["status"] as? String) ?: "PENDING"
                        )
                    }
                    trySend(list).isSuccess
                } catch (e: Exception) {}
            }
        }
        awaitClose { subscription.close() }
    }

    suspend fun uploadQuestionBankZip(zipFile: File): Pair<String, String> {
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
    
    suspend fun deleteQuestionBankZip(fileId: String) {
        withContext(Dispatchers.IO) {
            storage.deleteFile(
                bucketId = bucketId,
                fileId = fileId
            )
        }
    }

    suspend fun clearChatMessagesOnRemote(chatId: String) {
        chatRemoteRepository.clearChatMessagesOnRemote(chatId)
    }

    suspend fun deleteChatOnRemote(chatId: String) {
        chatRemoteRepository.deleteChatOnRemote(chatId)
    }

    fun deleteChatAndMessagesGlobally(chatId: String) {
        repositoryScope.launch {
            try {
                Log.d("SyncRepository", "deleteChatAndMessagesGlobally: Starting for chatId=$chatId")
                val chat = chatDao.getChatById(chatId)
                Log.d("SyncRepository", "deleteChatAndMessagesGlobally: chat=$chat, isGroup=${chat?.isGroup}, adminIds=${chat?.adminIds}, currentUserId=$currentUserId")
                
                if (chat != null && !chat.isAdmin(currentUserId ?: "") && chat.isGroup) {
                    // Non-admin leaving a group: remove from participants first, then local
                    Log.d("SyncRepository", "deleteChatAndMessagesGlobally: Removing participant from group")
                    removeParticipantFromRemote(chatId, currentUserId ?: "")
                    chatDao.deleteChatById(chatId)
                    messageDao.deleteMessagesByChatId(chatId)
                    Log.d("SyncRepository", "deleteChatAndMessagesGlobally: Group leave complete")
                } else {
                    // Delete from Appwrite FIRST (before local), so the network call completes
                    Log.d("SyncRepository", "deleteChatAndMessagesGlobally: Deleting from Appwrite FIRST...")
                    try {
                        chatRemoteRepository.clearChatMessagesOnRemote(chatId)
                        Log.d("SyncRepository", "deleteChatAndMessagesGlobally: Messages cleared from Appwrite")
                    } catch (e: Exception) {
                        Log.e("SyncRepository", "deleteChatAndMessagesGlobally: Failed to clear messages from Appwrite", e)
                    }
                    try {
                        chatRemoteRepository.deleteChatOnRemote(chatId)
                        Log.d("SyncRepository", "deleteChatAndMessagesGlobally: Chat deleted from Appwrite")
                    } catch (e: Exception) {
                        Log.e("SyncRepository", "deleteChatAndMessagesGlobally: Failed to delete chat from Appwrite", e)
                    }
                    // Now delete locally
                    chatDao.deleteChatById(chatId)
                    messageDao.deleteMessagesByChatId(chatId)
                    Log.d("SyncRepository", "deleteChatAndMessagesGlobally: Local deletion complete")
                }
            } catch (e: Exception) {
                Log.e("SyncRepository", "deleteChatAndMessagesGlobally: FATAL ERROR", e)
            }
        }
    }

    suspend fun reportSession(sessionId: String, reason: String) {
        val reporterId = currentUserId ?: return
        val session = sessionDao.getSessionById(sessionId) ?: return
        
        val reportRefId = UUID.randomUUID().toString()
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis() / 1000,
            "sessionId" to session.sessionId
        )

        try {
            databases.createDocument(
                databaseId = "qbase_db",
                collectionId = "reported_sessions",
                documentId = reportRefId,
                data = reportMap
            )
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to submit reported session for $sessionId", e)
        }
    }

    suspend fun reportQuestion(
        question: Question,
        options: List<QuestionOption>,
        answer: Answer?,
        reason: String
    ) {
        val reporterId = currentUserId ?: throw IllegalStateException("User not authenticated")
        val reportRefId = UUID.randomUUID().toString()
        
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis() / 1000,
            "questionId" to question.questionId
        )

        try {
            databases.createDocument(
                databaseId = "qbase_db",
                collectionId = "reported_questions",
                documentId = reportRefId,
                data = reportMap
            )
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to submit reported question", e)
        }
    }

    suspend fun reportGroup(group: ChatEntity, reason: String) {
        chatRemoteRepository.reportGroup(group, reason)
    }

    suspend fun reportUser(user: UserEntity, reason: String) {
        val reporterId = currentUserId ?: return
        val reportRefId = UUID.randomUUID().toString()
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis() / 1000,
            "reportedUserId" to user.userId
        )
        try {
            databases.createDocument(
                databaseId = "qbase_db",
                collectionId = "reported_users",
                documentId = reportRefId,
                data = reportMap
            )
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to submit reported user ${user.userId}", e)
        }
    }

    suspend fun reportCollection(collection: StudyCollection, reason: String) {
        val reporterId = currentUserId ?: return
        val reportRefId = UUID.randomUUID().toString()
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis() / 1000,
            "collectionId" to collection.collectionId
        )
        try {
            databases.createDocument(
                databaseId = "qbase_db",
                collectionId = "reported_collections",
                documentId = reportRefId,
                data = reportMap
            )
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to submit reported collection", e)
        }
    }

    suspend fun reportMessage(message: MessageEntity, reason: String) {
        chatRemoteRepository.reportMessage(message, reason)
    }

    suspend fun shareCollectionToGroup(chatId: String, collectionMetadata: Map<String, Any>) {
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
                                Log.w("SyncRepository", "Failed to refresh public key for $targetId during share", e)
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
                            Log.e("SyncRepository", "Key wrap failed for $targetId in group sharing", e)
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
            Log.e("SyncRepository", "Failed to share collection in Appwrite", e)
            throw e
        }
    }

    suspend fun addSharedSessionToGroup(
        chatId: String, 
        sessionId: String, 
        sessionTitle: String,
        downloadUrl: String,
        symmetricKey: String
    ) {
        try {
            val sessionMetadata = mapOf(
                "sessionId" to sessionId,
                "chatId" to chatId,
                "title" to sessionTitle,
                "downloadUrl" to downloadUrl,
                "symmetricKey" to symmetricKey,
                "adminIds" to listOf(currentUserId ?: "Unknown"),
                "isAdminOnly" to false,
                "timestamp" to System.currentTimeMillis() / 1000
            )

            try {
                databases.createDocument(
                    databaseId = "qbase_db",
                    collectionId = "shared_sessions",
                    documentId = sessionId,
                    data = sessionMetadata
                )
            } catch (e: io.appwrite.exceptions.AppwriteException) {
                if (e.code == 409) {
                    databases.updateDocument(
                        databaseId = "qbase_db",
                        collectionId = "shared_sessions",
                        documentId = sessionId,
                        data = sessionMetadata
                    )
                } else {
                    throw e
                }
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to share session to group", e)
            throw e
        }
    }

    fun observeSharedSessions(chatId: String): Flow<List<Map<String, Any>>> {
        return callbackFlow {
            repositoryScope.launch {
                try {
                    val response = databases.listDocuments(
                        databaseId = "qbase_db",
                        collectionId = "shared_sessions",
                        queries = listOf(Query.equal("chatId", chatId))
                    )
                    val mapped = response.documents.map { doc ->
                        val data = doc.data.toMutableMap()
                        val rawTimestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                        data["timestamp"] = if (rawTimestamp < 1000000000000L) rawTimestamp * 1000 else rawTimestamp
                        data
                    }
                    trySend(mapped).isSuccess
                } catch (e: Exception) {}
            }

            val realtime = io.appwrite.services.Realtime(appwriteClient)
            val subscription = realtime.subscribe("databases.qbase_db.collections.shared_sessions.documents") { event ->
                repositoryScope.launch {
                    try {
                        val response = databases.listDocuments(
                            databaseId = "qbase_db",
                            collectionId = "shared_sessions",
                            queries = listOf(Query.equal("chatId", chatId))
                        )
                        val mapped = response.documents.map { doc ->
                            val data = doc.data.toMutableMap()
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

    private fun extractFileIdFromUrl(url: String): String? {
        return try {
            val pattern = "files/([^/]+)/download".toRegex()
            val matchResult = pattern.find(url)
            matchResult?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    fun observeGroupLibrary(chatId: String): Flow<List<Map<String, Any>>> {
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

    private suspend fun mapGroupLibrary(list: List<Map<String, Any>>): List<Map<String, Any>> {
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
                    Log.e("SyncRepository", "Failed to decrypt wrapped key in mapGroupLibrary", e)
                }
            }
            
            data["symmetricKey"] = decKey
            data["isRestricted"] = decKey.isBlank()
            
            val rawUpdatedAt = (doc["updatedAt"] as? Number)?.toLong() ?: 0L
            data["updatedAt"] = if (rawUpdatedAt < 1000000000000L) rawUpdatedAt * 1000 else rawUpdatedAt
            data
        }
    }

    suspend fun getChatById(chatId: String): ChatEntity? = chatDao.getChatById(chatId)

    suspend fun sendCollectionPatch(chatId: String, collectionId: String, op: String, data: JSONObject) {
        val patchObj = JSONObject()
        patchObj.put("collectionId", collectionId)
        patchObj.put("op", op)
        patchObj.put("data", data)
        
        val message = MessageEntity(
            messageId = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = currentUserId ?: "",
            payload = patchObj.toString(),
            type = "COLLECTION_PATCH",
            timestamp = System.currentTimeMillis()
        )
        sendMessage(message)
    }

    private suspend fun applyCollectionPatch(jsonString: String) {
        try {
            val patch = JSONObject(jsonString)
            val op = patch.getString("op")
            val collectionId = patch.getString("collectionId")
            val data = patch.getJSONObject("data")

            when (op) {
                "UPSERT_QUESTION" -> {
                    val qId = data.getString("id")
                    val collectionName = data.getString("collectionName")
                    val qText = data.getString("text")
                    val category = data.optString("category", "General")
                    val tags = data.optString("tags", "")

                    val question = Question(
                        questionId = qId,
                        collection = collectionName,
                        category = category,
                        tags = tags,
                        questionType = "Multiple Choice",
                        stem = qText,
                        isPinned = false
                    )
                    questionDao.insertQuestion(question)

                    questionDao.deleteOptionsForQuestion(qId)
                    val optionsArray = data.getJSONArray("options")
                    val letters = listOf("A", "B", "C", "D", "E", "F")
                    for (i in 0 until optionsArray.length()) {
                        val optText = optionsArray.getString(i)
                        questionDao.insertOption(QuestionOption(
                            questionId = qId,
                            optionLetter = letters.getOrNull(i) ?: "?",
                            optionText = optText,
                            optionExplanation = null
                        ))
                    }

                    val correctAns = data.getString("correctAnswer")
                    questionDao.insertAnswer(Answer(
                        questionId = qId,
                        correctAnswerString = correctAns,
                        generalExplanation = ""
                    ))
                    
                    collectionDao.updateStudyCollectionTimestamp(collectionId, System.currentTimeMillis())
                }
                "DELETE_QUESTION" -> {
                    val qId = data.getString("id")
                    questionDao.deleteQuestionById(qId)
                    collectionDao.updateStudyCollectionTimestamp(collectionId, System.currentTimeMillis())
                }
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to apply collection patch", e)
        }
    }

    suspend fun sendSessionPatch(chatId: String, sessionId: String, op: String, data: JSONObject) {
        val patchObj = JSONObject()
        patchObj.put("sessionId", sessionId)
        patchObj.put("op", op)
        patchObj.put("data", data)
        
        val message = MessageEntity(
            messageId = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = currentUserId ?: "",
            payload = patchObj.toString(),
            type = "SESSION_PATCH",
            timestamp = System.currentTimeMillis()
        )
        sendMessage(message)
    }

    private suspend fun applySessionPatch(jsonString: String) {
        try {
            val patch = JSONObject(jsonString)
            val op = patch.getString("op")
            val sessionId = patch.getString("sessionId")
            val data = patch.getJSONObject("data")

            when (op) {
                "UPSERT_ATTEMPT" -> {
                    val qId = data.getString("questionId")
                    val attemptStatus = data.getString("attemptStatus")
                    val userSelectedAnswers = data.getString("userSelectedAnswers")
                    val marksObtained = data.optDouble("marksObtained", 0.0).toFloat()

                    val attempt = SessionAttempt(
                        sessionId = sessionId,
                        questionId = qId,
                        attemptStatus = attemptStatus,
                        userSelectedAnswers = userSelectedAnswers,
                        marksObtained = marksObtained
                    )
                    sessionDao.insertAttempts(listOf(attempt))
                }
                "UPDATE_SESSION" -> {
                    val title = data.getString("title")
                    val isCompleted = data.getBoolean("isCompleted")
                    val scoreAchieved = data.optDouble("scoreAchieved", 0.0).toFloat()
                    
                    val existing = sessionDao.getSessionById(sessionId)
                    if (existing != null) {
                        sessionDao.updateSession(existing.copy(
                            title = title,
                            isCompleted = isCompleted,
                            scoreAchieved = scoreAchieved
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to apply session patch", e)
        }
    }

    suspend fun requestCollectionAccess(chatId: String, collectionId: String) {
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
                databaseId = "qbase_db",
                collectionId = "access_requests",
                documentId = requestId,
                data = requestData
            )
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to request collection access", e)
        }
    }

    fun observeAccessRequests(chatId: String): Flow<List<Map<String, Any>>> {
        return callbackFlow {
            repositoryScope.launch {
                try {
                    val response = databases.listDocuments(
                        databaseId = "qbase_db",
                        collectionId = "access_requests",
                        queries = listOf(
                            Query.equal("chatId", chatId),
                            Query.equal("status", "PENDING")
                        )
                    )
                    val mapped = response.documents.map { doc ->
                        val data = doc.data.toMutableMap()
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
                        val response = databases.listDocuments(
                            databaseId = "qbase_db",
                            collectionId = "access_requests",
                            queries = listOf(
                                Query.equal("chatId", chatId),
                                Query.equal("status", "PENDING")
                            )
                        )
                        val mapped = response.documents.map { doc ->
                            val data = doc.data.toMutableMap()
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

    suspend fun grantCollectionAccess(chatId: String, collectionId: String, requesterId: String) {
        try {
            // 1. Fetch requester's public key from the remote profiles
            val reqUserDoc = databases.getDocument("qbase_db", "users", requesterId)
            val requesterPublicKey = reqUserDoc.data["publicKey"] as? String
                ?: throw IllegalStateException("Requester's E2EE public key not found")

            // 2. Fetch the existing shared collection record to update its wrappedKeys map
            val sharedCollDoc = databases.getDocument("qbase_db", "shared_collections", collectionId)
            val existingWrappedKeysStr = sharedCollDoc.data["wrappedKeys"] as? String ?: "{}"
            
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
                databaseId = "qbase_db",
                collectionId = "shared_collections",
                documentId = collectionId,
                data = mapOf("wrappedKeys" to existingWrappedKeysObj.toString())
            )

            // 6. Finally, approve the access request document
            val requestId = "${requesterId}_$collectionId"
            databases.updateDocument(
                databaseId = "qbase_db",
                collectionId = "access_requests",
                documentId = requestId,
                data = mapOf("status" to "APPROVED")
            )
            
            Log.d("SyncRepository", "Access granted to $requesterId for $collectionId")
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to grant access", e)
            throw e
        }
    }
}
