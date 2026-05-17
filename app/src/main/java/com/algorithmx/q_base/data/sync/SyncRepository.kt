package com.algorithmx.q_base.data.sync

import android.util.Base64
import android.util.Log
import com.algorithmx.q_base.BuildConfig
import com.algorithmx.q_base.data.chat.ChatDao
import com.algorithmx.q_base.data.chat.ChatEntity
import com.algorithmx.q_base.data.chat.ChatFirestoreRepository
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
    private val chatFirestoreRepository: ChatFirestoreRepository,
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

    fun observeAndSyncMessages(chatId: String): Flow<MessageEntity?> {
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
                        val timestamp = (doc.data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
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
                            keyFingerprint = keyFingerprint,
                            wrappedKey = wrappedKey
                        )

                        if (messageDao.getMessageById(doc.id) == null) {
                            messageDao.insertMessage(message)
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
                        repositoryScope.launch {
                            val docId = payloadObj.optString("\$id")
                            val type = payloadObj.optString("type", "TEXT")
                            val payloadVal = payloadObj.optString("payload", "")
                            val senderId = payloadObj.optString("senderId", "")
                            val timestamp = payloadObj.optLong("timestamp", System.currentTimeMillis())
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
                                keyFingerprint = keyFingerprint,
                                wrappedKey = wrappedKey
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

                                val chat = chatDao.getChatById(chatId)
                                if (chat != null) {
                                    if (!chat.isGroup) {
                                        if (message.senderId != currentUserId) {
                                            try {
                                                databases.deleteDocument("qbase_db", "messages", docId)
                                            } catch (e: Exception) {
                                                Log.e("SyncRepository", "Failed to delete 1-1 ephemeral message", e)
                                            }
                                        }
                                    }
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
            "timestamp" to message.timestamp
        )
        
        try {
            databases.createDocument(
                databaseId = "qbase_db",
                collectionId = "messages",
                documentId = message.messageId,
                data = messageMap
            )

            repositoryScope.launch {
                messageDao.insertMessage(message.copy(
                    keyFingerprint = myFingerprint,
                    wrappedKey = wrappedKeys[senderUid],
                    decryptionStatus = "SUCCESS"
                ))
            }

            repositoryScope.launch {
                val chatMeta = chatDao.getChatById(message.chatId)
                chatMeta?.let {
                    val otherParticipants = it.participantIds.split(",").filter { id -> id != message.senderId && id.isNotEmpty() }
                    otherParticipants.forEach { targetId ->
                        sendNotification(
                            targetUserId = targetId,
                            title = "New Message",
                            body = message.payload,
                            data = mapOf(
                                "chatId" to message.chatId,
                                "type" to message.type,
                                "senderId" to message.senderId
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to send message to Appwrite", e)
            throw e
        }
    }

    suspend fun addParticipantToFirestore(chatId: String, userId: String) {
        chatFirestoreRepository.addParticipantToFirestore(chatId, userId)
    }

    suspend fun removeParticipantFromFirestore(chatId: String, userId: String) {
        chatFirestoreRepository.removeParticipantFromFirestore(chatId, userId)
    }

    suspend fun sendNotification(targetUserId: String, title: String, body: String, data: Map<String, String> = emptyMap()) {
        val notificationMap = mapOf(
            "targetUserId" to targetUserId,
            "senderId" to (currentUserId ?: ""),
            "title" to title,
            "body" to body,
            "data" to JSONObject(data).toString(),
            "timestamp" to System.currentTimeMillis()
        )
        
        try {
            databases.createDocument(
                databaseId = "qbase_db",
                collectionId = "notifications",
                documentId = UUID.randomUUID().toString(),
                data = notificationMap
            )
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to send notification in Appwrite", e)
        }
    }

    suspend fun createChatOnFirestore(chat: ChatEntity) {
        chatFirestoreRepository.createChatOnFirestore(chat)
    }

    fun observeAllIncomingEvents(notificationHelper: NotificationHelper): Flow<Unit> {
        val userId = currentUserId ?: return emptyFlow()
        return callbackFlow {
            val realtime = io.appwrite.services.Realtime(appwriteClient)
            val subscription = realtime.subscribe("databases.qbase_db.collections.notifications.documents") { event ->
                try {
                    val payloadObj = if (event.payload is Map<*, *>) {
                        org.json.JSONObject(event.payload as Map<*, *>)
                    } else {
                        org.json.JSONObject(event.payload.toString())
                    }
                    val targetUserId = payloadObj.optString("targetUserId")
                    if (targetUserId == userId) {
                        val docId = payloadObj.optString("\$id")
                        val title = payloadObj.optString("title", "New Notification")
                        val body = payloadObj.optString("body", "")
                        
                        val dataStr = payloadObj.optString("data", "{}")
                        val dataMap = mutableMapOf<String, String>()
                        val dataJson = JSONObject(dataStr)
                        val keys = dataJson.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            dataMap[k] = dataJson.getString(k)
                        }
                        
                        val chatId = dataMap["chatId"]
                        val sessionId = dataMap["sessionId"]

                        if (chatId != null) {
                            repositoryScope.launch {
                                val localChat = chatDao.getChatById(chatId)
                                if (localChat == null) {
                                    fetchAndSyncChatMetadata(chatId)
                                }
                                chatDao.incrementUnreadCount(chatId)
                                
                                if (localChat?.isBlocked == true) {
                                    return@launch
                                }
                                
                                notificationHelper.showMessageNotification(
                                    chatId = chatId, 
                                    senderName = title, 
                                    message = body
                                )
                            }
                        } else if (sessionId != null) {
                            notificationHelper.showSessionNotification(sessionId, title, body)
                        }
                        
                        repositoryScope.launch {
                            try {
                                databases.deleteDocument("qbase_db", "notifications", docId)
                            } catch (e: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SyncRepository", "Observe events error", e)
                }
            }
        
            awaitClose { subscription.close() }
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
            val chat = ChatEntity(
                chatId = chatId,
                chatName = doc.data["chatName"] as? String,
                isGroup = doc.data["isGroup"] as? Boolean ?: false,
                participantIds = participantsList.joinToString(","),
                adminId = doc.data["adminId"] as? String
            )
            chatDao.insertChat(chat)
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to fetch chat metadata", e)
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

        repositoryScope.launch {
            val chat = chatDao.getChatById(chatId)
            chat?.let {
                val otherParticipants = it.participantIds.split(",").filter { id -> id != senderId && id.isNotEmpty() }
                otherParticipants.forEach { targetId ->
                    sendNotification(
                        targetUserId = targetId,
                        title = "Session Invite",
                        body = "You've been invited to $sessionTitle",
                        data = mapOf("sessionId" to sessionId)
                    )
                }
            }
        }
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
                data = syncRequest
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

    suspend fun clearChatMessagesOnFirestore(chatId: String) {
        chatFirestoreRepository.clearChatMessagesOnFirestore(chatId)
    }

    suspend fun deleteChatOnFirestore(chatId: String) {
        chatFirestoreRepository.deleteChatOnFirestore(chatId)
    }

    suspend fun reportSession(sessionId: String, reason: String) {
        val reporterId = currentUserId ?: return
        val session = sessionDao.getSessionById(sessionId) ?: return
        
        val reportRefId = UUID.randomUUID().toString()
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis(),
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
            "reportedAt" to System.currentTimeMillis(),
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
        chatFirestoreRepository.reportGroup(group, reason)
    }

    suspend fun reportUser(user: UserEntity, reason: String) {
        val reporterId = currentUserId ?: return
        val reportRefId = UUID.randomUUID().toString()
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis(),
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
            "reportedAt" to System.currentTimeMillis(),
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
        chatFirestoreRepository.reportMessage(message, reason)
    }

    suspend fun shareCollectionToGroup(chatId: String, collectionMetadata: Map<String, Any>) {
        try {
            val collectionId = collectionMetadata["collectionId"] as String
            
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

            val secureMetadata = mapOf(
                "collectionId" to collectionId,
                "chatId" to chatId,
                "sharedBy" to (collectionMetadata["sharedBy"] as String),
                "adminIds" to participants,
                "updatedAt" to (collectionMetadata["updatedAt"] as Long)
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
                "timestamp" to System.currentTimeMillis()
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
                    trySend(response.documents.map { it.data }).isSuccess
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
                        trySend(response.documents.map { it.data }).isSuccess
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
            data["name"] = local?.name ?: "Group Collection"
            data["description"] = local?.description ?: "Shared by ${doc["sharedBy"]}"
            data["isExpired"] = false
            data["isRestricted"] = false
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
                    trySend(response.documents.map { it.data }).isSuccess
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
                        trySend(response.documents.map { it.data }).isSuccess
                    } catch (e: Exception) {}
                }
            }
            awaitClose { subscription.close() }
        }
    }

    suspend fun grantCollectionAccess(chatId: String, collectionId: String, requesterId: String) {
        try {
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
