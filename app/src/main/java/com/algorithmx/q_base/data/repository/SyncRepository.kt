package com.algorithmx.q_base.data.repository

import com.algorithmx.q_base.BuildConfig
import com.algorithmx.q_base.data.dao.ChatDao
import com.algorithmx.q_base.data.dao.MessageDao
import com.algorithmx.q_base.data.entity.*
import com.algorithmx.q_base.data.model.SyncRequest
import com.algorithmx.q_base.data.util.MockDownloader
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import android.util.Log
import android.util.Base64
import io.appwrite.ID
import io.appwrite.models.InputFile
import io.appwrite.services.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import org.json.JSONObject
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionOption
import com.algorithmx.q_base.data.entity.Answer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val userDao: com.algorithmx.q_base.data.dao.UserDao,
    private val cryptoManager: com.algorithmx.q_base.data.util.CryptoManager,
    private val mockDownloader: MockDownloader,
    private val storage: Storage,
    private val sessionDao: com.algorithmx.q_base.data.dao.SessionDao,
    private val problemReportDao: com.algorithmx.q_base.data.dao.ProblemReportDao,
    private val collectionDao: com.algorithmx.q_base.data.dao.CollectionDao,
    private val questionDao: com.algorithmx.q_base.data.dao.QuestionDao
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bucketId = BuildConfig.APPWRITE_BUCKET_ID
    private val projectId = BuildConfig.APPWRITE_PROJECT_ID

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    fun observeAndSyncMessages(chatId: String): Flow<Unit> {
        return callbackFlow {
            val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SyncRepository", "Observe messages error: ${error.message}")
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val type = doc.getString("type") ?: "TEXT"
                        var payload = doc.getString("payload") ?: ""
                        var decryptionStatus = "NOT_ENCRYPTED"
                        val keyFingerprint = doc.getString("keyFingerprint")
                        
                        val wrappedKeyMap = doc.get("wrappedKeys") as? Map<String, String>
                        val ciphertextPayload = doc.getString("ciphertextPayload")
                        
                        if (wrappedKeyMap != null && ciphertextPayload != null && currentUserId != null) {
                            val myWrappedKey = wrappedKeyMap[currentUserId]
                            if (myWrappedKey != null) {
                                val unwrapResult = cryptoManager.decryptSessionKey(myWrappedKey)
                                if (unwrapResult.isSuccess) {
                                    val sessionKeyHandle = Base64.encodeToString(unwrapResult.getOrThrow(), Base64.NO_WRAP)
                                    val decryptResult = cryptoManager.decryptWithSessionKey(ciphertextPayload, sessionKeyHandle)
                                    if (decryptResult.isSuccess) {
                                        payload = decryptResult.getOrNull() ?: ""
                                        decryptionStatus = "SUCCESS"
                                    } else {
                                        payload = ciphertextPayload
                                        decryptionStatus = "DECRYPTION_ERROR"
                                    }
                                } else {
                                    payload = ciphertextPayload
                                    // If fingerprint matches but can't unwrap -> error
                                    val myCurrentFingerprint = cryptoManager.getPublicKeyFingerprint()
                                    decryptionStatus = if (keyFingerprint != null && keyFingerprint == myCurrentFingerprint) {
                                        "DECRYPTION_ERROR"
                                    } else {
                                        "FAILED" // Session mismatch
                                    }
                                }
                            }
                        }
                        
                        val message = MessageEntity(
                            messageId = doc.id,
                            chatId = chatId,
                            senderId = doc.getString("senderId") ?: "",
                            payload = payload,
                            type = type,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            decryptionStatus = decryptionStatus,
                            keyFingerprint = keyFingerprint
                        )
                        
                        repositoryScope.launch {
                            // SKIP if already in local DB
                            if (messageDao.getMessageById(doc.id) != null) {
                                return@launch
                            }
                            
                            messageDao.insertMessage(message)
                            
                            // 2. Handle COLLECTION_PATCH: Apply minor updates to local DB
                            if (type == "COLLECTION_PATCH" && decryptionStatus == "SUCCESS") {
                                applyCollectionPatch(payload)
                            }
                            
                            // 3. Update chat's last used key fingerprint if it's a success
                            if (decryptionStatus == "SUCCESS" && keyFingerprint != null) {
                                chatDao.getChatById(chatId)?.let { chat ->
                                    if (chat.lastUsedKeyFingerprint != keyFingerprint) {
                                        chatDao.insertChat(chat.copy(lastUsedKeyFingerprint = keyFingerprint))
                                    }
                                }
                            }

                            // EPHEMERAL MAPPING: Delete after delivery
                            val chat = chatDao.getChatById(chatId)
                            if (chat != null) {
                                if (!chat.isGroup) {
                                    // 1-on-1: Delete immediately (sender already has it locally)
                                    if (message.senderId != currentUserId) {
                                        try {
                                            doc.reference.delete().await()
                                        } catch (e: Exception) {
                                            Log.e("SyncRepository", "Failed to delete 1-1 ephemeral message", e)
                                        }
                                    }
                                } else {
                                    // Group: Add ourselves to deliveredTo
                                    try {
                                        doc.reference.update("deliveredTo", FieldValue.arrayUnion(currentUserId)).await()
                                        
                                        // Check if we are the last one. Wait a bit for Firestore sync or fetch fresh doc
                                        val freshDoc = doc.reference.get().await()
                                        val deliveredTo = freshDoc.get("deliveredTo") as? List<String> ?: emptyList()
                                        val participantIds = freshDoc.get("participantIds") as? List<String> ?: emptyList()
                                        
                                        if (participantIds.isNotEmpty() && deliveredTo.containsAll(participantIds)) {
                                            doc.reference.delete().await()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("SyncRepository", "Failed to update delivery status in group", e)
                                    }
                                }
                            }
                        }
                    }
                }
                trySend(Unit)
            }
        
        awaitClose { listener.remove() }
        }
    }

    suspend fun sendMessage(message: MessageEntity) {
        val chat = chatDao.getChatById(message.chatId)
        val participants = chat?.participantIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

        if (participants.isEmpty()) {
            throw IllegalStateException("Cannot send message: No participants in chat.")
        }

        // 1. Generate a single session key and encrypt the payload once
        val (ciphertextPayload, sessionKeyHandle) = cryptoManager.encryptWithSessionKey(message.payload)
        val sessionKeyBytes = Base64.decode(sessionKeyHandle, Base64.NO_WRAP)

        // 2. Wrap the session key for each participant
        val wrappedKeys = mutableMapOf<String, String>()
        val myFingerprint = cryptoManager.getPublicKeyFingerprint()
        
        participants.forEach { targetId ->
            var targetProfile = userDao.getUserById(targetId)
            
            // If missing from local DB, fetch from Firestore
            if (targetProfile?.publicKey == null) {
                try {
                    val doc = firestore.collection("users").document(targetId).get().await()
                    if (doc.exists()) {
                        val p = doc.toObject(com.algorithmx.q_base.data.model.UserProfile::class.java)
                        if (p?.publicKey != null) {
                            targetProfile = UserEntity(
                                userId = targetId,
                                displayName = p.displayName,
                                email = p.email ?: "",
                                profilePictureUrl = p.profilePictureUrl,
                                friendCode = p.friendCode,
                                publicKey = p.publicKey,
                                intro = p.intro
                            )
                            userDao.insertUser(targetProfile!!)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SyncRepository", "Failed to fetch public key for $targetId", e)
                }
            }

            if (targetProfile?.publicKey != null) {
                try {
                    wrappedKeys[targetId] = cryptoManager.encryptSessionKey(sessionKeyBytes, targetProfile!!.publicKey!!)
                } catch (e: Exception) {
                    Log.e("SyncRepository", "Failed to wrap key for $targetId", e)
                }
            }
        }

        // 3. SECURE CHECK: Must have at least ourselves plus someone else if it's a 1-1 chat
        // Actually, just ensure it's not empty, but we SHOULD have everyone.
        if (wrappedKeys.isEmpty()) {
            throw IllegalStateException("Security Abort: Failed to encrypt message for any participant.")
        }

        val messageMap = hashMapOf(
            "senderId" to message.senderId,
            "ciphertextPayload" to ciphertextPayload,
            "wrappedKeys" to wrappedKeys,
            "payload" to "ENCRYPTED_MESSAGE",
            "type" to message.type,
            "timestamp" to message.timestamp,
            "participantIds" to participants,
            "keyFingerprint" to myFingerprint
        )
        
        try {
            firestore.collection("chats")
                .document(message.chatId)
                .collection("messages")
                .document(message.messageId)
                .set(messageMap)
                .await()

            // Update local message with the fingerprint and wrapped key we used
            repositoryScope.launch {
                messageDao.insertMessage(message.copy(
                    keyFingerprint = myFingerprint,
                    wrappedKey = wrappedKeys[message.senderId],
                    decryptionStatus = "SUCCESS"
                ))
            }

            // Notify other participants
            repositoryScope.launch {
                val chat = chatDao.getChatById(message.chatId)
                chat?.let {
                    val otherParticipants = it.participantIds.split(",").filter { id -> id != message.senderId && id.isNotEmpty() }
                    otherParticipants.forEach { targetId ->
                        sendNotification(
                            targetUserId = targetId,
                            title = "New Message",
                            body = "Encrypted Message Content", // Secure against plaintext leaks in Firestore
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
            Log.e("SyncRepository", "Failed to send message to Firestore", e)
        }
    }

    suspend fun addParticipantToFirestore(chatId: String, userId: String) {
        try {
            firestore.collection("chats")
                .document(chatId)
                .update("participantIds", FieldValue.arrayUnion(userId))
                .await()
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to add participant in Firestore", e)
        }
    }

    suspend fun removeParticipantFromFirestore(chatId: String, userId: String) {
        try {
            firestore.collection("chats")
                .document(chatId)
                .update("participantIds", FieldValue.arrayRemove(userId))
                .await()
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to remove participant in Firestore", e)
        }
    }

    suspend fun sendNotification(targetUserId: String, title: String, body: String, data: Map<String, String> = emptyMap()) {
        val notificationMap = hashMapOf(
            "title" to title,
            "body" to body,
            "timestamp" to System.currentTimeMillis(),
            "data" to data,
            "senderId" to (currentUserId ?: "")
        )
        
        try {
            firestore.collection("users")
                .document(targetUserId)
                .collection("notifications")
                .document(java.util.UUID.randomUUID().toString())
                .set(notificationMap)
                .await()
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to send notification in Firestore", e)
        }
    }

    suspend fun createChatOnFirestore(chat: com.algorithmx.q_base.data.entity.ChatEntity) {
        val participants = chat.participantIds.split(",").filter { it.isNotBlank() }.toMutableList()
        // Ensure creator is in the list
        currentUserId?.let { uid ->
            if (!participants.contains(uid)) {
                participants.add(uid)
            }
        }

        val chatMap = hashMapOf(
            "chatName" to chat.chatName,
            "isGroup" to chat.isGroup,
            "participantIds" to participants,
            "adminId" to (chat.adminId ?: currentUserId),
            "createdAt" to System.currentTimeMillis()
        )
        try {
            firestore.collection("chats")
                .document(chat.chatId)
                .set(chatMap)
                .await()
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to create chat in Firestore", e)
        }
    }

    fun observeAllIncomingEvents(notificationHelper: com.algorithmx.q_base.util.NotificationHelper): Flow<Unit> {
        val userId = currentUserId ?: return emptyFlow()
        return callbackFlow {
        
        val listener = firestore.collection("users")
            .document(userId)
            .collection("notifications")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("SyncRepository", "Observe events error: ${error.message}")
                    return@addSnapshotListener
                }
                
                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val title = doc.getString("title") ?: "New Notification"
                        val body = doc.getString("body") ?: ""
                        val data = doc.get("data") as? Map<String, String> ?: emptyMap()
                        
                        val chatId = data["chatId"]
                        val sessionId = data["sessionId"]

                        if (chatId != null) {
                            repositoryScope.launch {
                                // Sync Chat Metadata first
                                val localChat = chatDao.getChatById(chatId)
                                if (localChat == null) {
                                    fetchAndSyncChatMetadata(chatId)
                                }
                                
                                // Increment unread count for local badge
                                chatDao.incrementUnreadCount(chatId)
                                
                                // Trigger immediate background message sync
                                // This solves the "delay" reported by user
                                observeAndSyncMessages(chatId).first()
                                
                                if (localChat?.isBlocked == true) {
                                    Log.d("SyncRepository", "Skipping notification for blocked chat $chatId")
                                    return@launch
                                }
                                
                                notificationHelper.showMessageNotification(chatId, title, body)
                            }
                        } else if (sessionId != null) {
                            notificationHelper.showSessionNotification(sessionId, title, body)
                        }
                        
                        repositoryScope.launch {
                            doc.reference.delete()
                        }
                    }
                }
                trySend(Unit)
            }
        
        awaitClose { listener.remove() }
        }
    }

    private suspend fun fetchAndSyncChatMetadata(chatId: String) {
        try {
            val doc = firestore.collection("chats").document(chatId).get().await()
            if (doc.exists()) {
                val participantsList = doc.get("participantIds") as? List<String> ?: emptyList()
                val chat = com.algorithmx.q_base.data.entity.ChatEntity(
                    chatId = chatId,
                    chatName = doc.getString("chatName"),
                    isGroup = doc.getBoolean("isGroup") ?: false,
                    participantIds = participantsList.joinToString(","),
                    adminId = doc.getString("adminId")
                )
                chatDao.insertChat(chat)
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    suspend fun sendSessionInvite(chatId: String, sessionId: String, sessionTitle: String) {
        val senderId = currentUserId ?: return
        val messageId = java.util.UUID.randomUUID().toString()
        
        val message = MessageEntity(
            messageId = messageId,
            chatId = chatId,
            senderId = senderId,
            payload = "$sessionId|$sessionTitle",
            type = "SESSION_INVITE",
            timestamp = System.currentTimeMillis()
        )
        
        sendMessage(message)

        // Notify other participants (usually it's a 1-to-1 or group chat)
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

    /**
     * Sends a request to another user's inbox asking for a question bank,
     * or notifying them that one is ready.
     */
    suspend fun sendSyncRequest(targetUserId: String, targetCollectionId: String) {
        val senderId = currentUserId ?: return
        val requestRef = firestore.collection("users")
            .document(targetUserId)
            .collection("sync_requests")
            .document() // Auto-generates an ID

        val syncRequest = SyncRequest(
            requestId = requestRef.id,
            senderId = senderId,
            targetCollectionId = targetCollectionId,
            status = "PENDING"
        )

        try {
            // Write to Firestore securely
            requestRef.set(syncRequest).await()
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to send sync request in Firestore", e)
        }
    }

    /**
     * Listens to the current user's inbox in real-time.
     * Your Jetpack Compose UI can collect this Flow to show "Waiting for device..." 
     * or trigger the background `.zip` download.
     */
    fun observeIncomingRequests(): Flow<List<SyncRequest>> = callbackFlow {
        val userId = currentUserId ?: return@callbackFlow
        
        val listenerRegistration = firestore.collection("users")
            .document(userId!!)
            .collection("sync_requests")
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val requests = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(SyncRequest::class.java)
                } ?: emptyList()

                trySend(requests).isSuccess
            }

        awaitClose { listenerRegistration.remove() }
    }

    suspend fun uploadQuestionBankZip(zipFile: File): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            val fileBytes = zipFile.readBytes()
            val (encryptedBytes, symmetricKey) = cryptoManager.encryptFileContent(fileBytes)
            
            // Overwrite zipFile with encrypted contents for upload
            zipFile.writeBytes(encryptedBytes)
            
            // 1. Prepare the file for Appwrite using fromPath which is safer in Android
            val inputFile = InputFile.fromPath(zipFile.absolutePath)

            // 2. Upload to the bucket using a mathematically unique ID
            val uploadedFile = storage.createFile(
                bucketId = bucketId,
                fileId = ID.unique(),
                file = inputFile
            )

            // 3. Construct the Download URL to share via Firestore
            // This URL structure is standard for Appwrite Cloud
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
        try {
            val messagesRef = firestore.collection("chats").document(chatId).collection("messages")
            var snapshot = messagesRef.limit(500).get().await()
            
            while (!snapshot.isEmpty) {
                val batch = firestore.batch()
                snapshot.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }
                batch.commit().await()
                // Get the next batch
                snapshot = messagesRef.limit(500).get().await()
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to clear messages on Firestore", e)
        }
    }

    suspend fun deleteChatOnFirestore(chatId: String) {
        try {
            // 1. Clear messages first
            clearChatMessagesOnFirestore(chatId)
            // 2. Delete the chat document
            firestore.collection("chats").document(chatId).delete().await()
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to delete chat on Firestore", e)
        }
    }


    suspend fun reportSession(sessionId: String, reason: String) {
        val reporterId = currentUserId ?: return
        val session = sessionDao.getSessionById(sessionId) ?: return
        val attempts = sessionDao.getAttemptsForSession(sessionId).first()
        
        val reportRef = firestore.collection("reported_sessions").document()
        
        val problemReports = attempts.map { attempt ->
            val reports = problemReportDao.getReportsForQuestion(attempt.questionId).first()
            reports.map { r: ProblemReport ->
                mapOf(
                    "questionId" to r.questionId,
                    "explanation" to cryptoManager.decryptMessage(r.explanation).getOrElse { r.explanation },
                    "timestamp" to r.timestamp
                )
            }
        }.flatten()

        val reportMap = hashMapOf(
            "sessionId" to session.sessionId,
            "sessionTitle" to session.title,
            "scoreAchieved" to session.scoreAchieved,
            "reporterId" to reporterId,
            "attempts" to attempts.map { a: SessionAttempt ->
                mapOf(
                    "questionId" to a.questionId,
                    "userSelection" to a.userSelectedAnswers,
                    "marks" to a.marksObtained
                )
            },
            "problemReports" to problemReports,
            "reportedAt" to System.currentTimeMillis()
        )

        try {
            reportRef.set(reportMap).await()
            Log.d("SyncRepository", "Session report submitted successfully: ${reportRef.id}")
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
        val reportRef = firestore.collection("reported_questions").document()
        
        val reportMap = hashMapOf(
            "questionId" to question.questionId,
            "stem" to question.stem,
            "collection" to question.collection,
            "category" to question.category,
            "options" to options.map { 
                mapOf("letter" to it.optionLetter, "text" to it.optionText)
            },
            "correctAnswer" to (answer?.correctAnswerString ?: "N/A"),
            "explanation" to if (answer?.generalExplanation != null) {
                cryptoManager.decryptMessage(answer.generalExplanation).getOrElse { answer.generalExplanation }
            } else "N/A",
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis()
        )

        reportRef.set(reportMap).await()
        Log.d("SyncRepository", "Question report submitted successfully: ${reportRef.id}")
    }

    suspend fun reportGroup(group: ChatEntity, reason: String) {
        val reporterId = currentUserId ?: throw IllegalStateException("User not authenticated")
        val reportRef = firestore.collection("reported_groups").document()
        val reportMap = hashMapOf(
            "groupId" to group.chatId,
            "groupName" to group.chatName,
            "participantIds" to group.participantIds,
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis()
        )
        try {
            reportRef.set(reportMap).await()
            Log.d("SyncRepository", "Group report submitted successfully: ${reportRef.id}")
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to submit reported group ${group.chatId}", e)
        }
    }

    suspend fun reportUser(user: UserEntity, reason: String) {
        val reporterId = currentUserId ?: return
        val reportRef = firestore.collection("reported_users").document()
        val reportMap = hashMapOf(
            "userId" to user.userId,
            "displayName" to user.displayName,
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis()
        )
        try {
            reportRef.set(reportMap).await()
            Log.d("SyncRepository", "User report submitted successfully: ${reportRef.id}")
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to submit reported user ${user.userId}", e)
        }
    }

    suspend fun reportCollection(collection: com.algorithmx.q_base.data.entity.Collection, reason: String) {
        val reporterId = currentUserId ?: return
        val reportRef = firestore.collection("reported_collections").document()
        val reportMap = hashMapOf(
            "collectionId" to collection.collectionId,
            "collectionName" to collection.name,
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis()
        )
        try {
            reportRef.set(reportMap).await()
            Log.d("SyncRepository", "Collection report submitted successfully: ${reportRef.id}")
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to submit reported collection", e)
        }
    }

    suspend fun reportMessage(message: MessageEntity, reason: String) {
        val reporterId = currentUserId ?: return
        val reportRef = firestore.collection("reported_messages").document()
        val reportMap = hashMapOf(
            "messageId" to message.messageId,
            "chatId" to message.chatId,
            "senderId" to message.senderId,
            "payload" to message.payload,
            "type" to message.type,
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis()
        )
        try {
            reportRef.set(reportMap).await()
            Log.d("SyncRepository", "Message report submitted successfully: ${reportRef.id}")
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to submit reported message ${message.messageId}", e)
        }
    }

    suspend fun shareCollectionToGroup(chatId: String, collectionMetadata: Map<String, Any>) {
        try {
            val collectionId = collectionMetadata["collectionId"] as String
            
            // 1. Check if a newer version exists in Firestore to clean up old storage
            val existingDoc = firestore.collection("chats")
                .document(chatId)
                .collection("shared_collections")
                .document(collectionId)
                .get()
                .await()

            if (existingDoc.exists()) {
                val oldUrl = existingDoc.getString("downloadUrl")
                if (oldUrl != null) {
                    val oldFileId = extractFileIdFromUrl(oldUrl)
                    if (oldFileId != null) {
                        try {
                            deleteQuestionBankZip(oldFileId)
                            Log.d("SyncRepository", "Deleted old collection version: $oldFileId")
                        } catch (de: Exception) {
                            Log.w("SyncRepository", "Failed to delete old version file", de)
                        }
                    }
                }
            }

            // 2. Encrypt Metadata (Name, Description, and ZIP Key) - Zero Knowledge for Firestore
            val rawName = collectionMetadata["name"] as String
            val rawDesc = collectionMetadata["description"] as? String ?: ""
            val zipKey = collectionMetadata["symmetricKey"] as String
            val metadataPayload = "$rawName|$rawDesc|$zipKey"
            
            val (encryptedPayload, sessionKeyHandle) = cryptoManager.encryptWithSessionKey(metadataPayload)
            val sessionKeyBytes = Base64.decode(sessionKeyHandle, Base64.NO_WRAP)

            // 3. Wrap metadata session key for participants
            val wrappedMetadataKeys = mutableMapOf<String, String>()
            val chat = chatDao.getChatById(chatId)
            val participants = chat?.participantIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

            participants.forEach { targetId ->
                var targetProfile = userDao.getUserById(targetId)
                if (targetProfile?.publicKey == null) {
                    try {
                        val doc = firestore.collection("users").document(targetId).get().await()
                        if (doc.exists()) {
                            val p = doc.toObject(com.algorithmx.q_base.data.model.UserProfile::class.java)
                            if (p?.publicKey != null) {
                                targetProfile = UserEntity(
                                    userId = targetId,
                                    displayName = p.displayName,
                                    email = p.email ?: "",
                                    profilePictureUrl = p.profilePictureUrl,
                                    friendCode = p.friendCode,
                                    publicKey = p.publicKey,
                                    intro = p.intro
                                )
                                userDao.insertUser(targetProfile!!)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SyncRepository", "Failed to fetch public key for metadata encryption: $targetId", e)
                    }
                }

                if (targetProfile?.publicKey != null) {
                    try {
                        wrappedMetadataKeys[targetId] = cryptoManager.encryptSessionKey(sessionKeyBytes, targetProfile!!.publicKey!!)
                    } catch (e: Exception) {
                        Log.e("SyncRepository", "Failed to wrap metadata key for $targetId", e)
                    }
                }
            }

            // 4. Update Firestore with Secure Metadata & TTL
            val secureMetadata = hashMapOf(
                "collectionId" to collectionId,
                "encryptedMetadataPayload" to encryptedPayload,
                "wrappedMetadataKeys" to wrappedMetadataKeys,
                "downloadUrl" to (collectionMetadata["downloadUrl"] as String),
                "updatedAt" to (collectionMetadata["updatedAt"] as Long),
                "sharedBy" to (collectionMetadata["sharedBy"] as String),
                "timestamp" to System.currentTimeMillis(),
                "expiresAt" to (System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)) // 30 Day TTL
            )

            firestore.collection("chats")
                .document(chatId)
                .collection("shared_collections")
                .document(collectionId)
                .set(secureMetadata)
                .await()
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to share collection to group with encryption", e)
            throw e
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
            val listenerRegistration = firestore.collection("chats")
                .document(chatId)
                .collection("shared_collections")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("SyncRepository", "Observe group library error: ${error.message}")
                        return@addSnapshotListener
                    }

                    val items = snapshot?.documents?.map { doc ->
                        val data = doc.data?.toMutableMap() ?: mutableMapOf<String, Any>()
                        
                        // 1. Check TTL Expiry
                        val expiresAt = data["expiresAt"] as? Long ?: 0L
                        if (expiresAt > 0 && System.currentTimeMillis() > expiresAt) {
                            data["isExpired"] = true
                        } else {
                            data["isExpired"] = false
                        }

                        // 2. Decrypt Metadata (Zero Knowledge for Firestore, Plaintext for Local)
                        val encryptedPayload = data["encryptedMetadataPayload"] as? String
                        val wrappedMetadataKeys = data["wrappedMetadataKeys"] as? Map<String, String>
                        
                        if (encryptedPayload != null && wrappedMetadataKeys != null && currentUserId != null) {
                            val myWrappedKey = wrappedMetadataKeys[currentUserId]
                            if (myWrappedKey != null) {
                                val unwrapResult = cryptoManager.decryptSessionKey(myWrappedKey)
                                if (unwrapResult.isSuccess) {
                                    val sessionKeyHandle = Base64.encodeToString(unwrapResult.getOrThrow(), Base64.NO_WRAP)
                                    val decryptResult = cryptoManager.decryptWithSessionKey(encryptedPayload, sessionKeyHandle)
                                    if (decryptResult.isSuccess) {
                                        val decryptedPayload = decryptResult.getOrNull() ?: ""
                                        val parts = decryptedPayload.split("|")
                                        if (parts.size >= 3) {
                                            data["name"] = parts[0]
                                            data["description"] = parts[1]
                                            data["symmetricKey"] = parts[2]
                                        }
                                    }
                                }
                            }
                        }
                        
                        if (data["name"] == null) {
                            data["name"] = "Encrypted Collection"
                            data["description"] = "Metadata decryption failed or restricted."
                        }
                        data
                    } ?: emptyList()
                    
                    trySend(items).isSuccess
                }

            awaitClose { listenerRegistration.remove() }
        }
    }
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

                    // Options
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

                    // Answer
                    val correctAns = data.getString("correctAnswer")
                    questionDao.insertAnswer(Answer(
                        questionId = qId,
                        correctAnswerString = correctAns,
                        generalExplanation = ""
                    ))
                    
                    collectionDao.updateCollectionTimestamp(collectionId, System.currentTimeMillis())
                }
                "DELETE_QUESTION" -> {
                    val qId = data.getString("id")
                    questionDao.deleteQuestionById(qId)
                    collectionDao.updateCollectionTimestamp(collectionId, System.currentTimeMillis())
                }
            }
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to apply collection patch", e)
        }
    }

    suspend fun requestCollectionAccess(chatId: String, collectionId: String) {
        val requestId = "${currentUserId}_$collectionId"
        val requestData = hashMapOf(
            "collectionId" to collectionId,
            "requesterId" to currentUserId,
            "requesterName" to (auth.currentUser?.displayName ?: "Unknown User"),
            "timestamp" to System.currentTimeMillis(),
            "status" to "PENDING"
        )
        
        firestore.collection("chats")
            .document(chatId)
            .collection("access_requests")
            .document(requestId)
            .set(requestData)
            .await()
    }

    fun observeAccessRequests(chatId: String): Flow<List<Map<String, Any>>> {
        return callbackFlow {
            val listenerRegistration = firestore.collection("chats")
                .document(chatId)
                .collection("access_requests")
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("SyncRepository", "Observe access requests error: ${error.message}")
                        return@addSnapshotListener
                    }
                    val items = snapshot?.documents?.map { it.data ?: emptyMap() } ?: emptyList()
                    trySend(items).isSuccess
                }
            awaitClose { listenerRegistration.remove() }
        }
    }

    suspend fun grantCollectionAccess(chatId: String, collectionId: String, requesterId: String) {
        try {
            // 1. Get the shared collection metadata
            val collDoc = firestore.collection("chats")
                .document(chatId)
                .collection("shared_collections")
                .document(collectionId)
                .get()
                .await()

            if (!collDoc.exists()) throw Exception("Collection record missing")

            val wrappedMetadataKeys = (collDoc.get("wrappedMetadataKeys") as? Map<String, String>)?.toMutableMap() ?: mutableMapOf()
            
            // 2. We need the symmetric key for the metadata to re-wrap it for the requester.
            val myWrappedKey = wrappedMetadataKeys[currentUserId] ?: throw Exception("Admin access missing")
            val unwrapResult = cryptoManager.decryptSessionKey(myWrappedKey)
            val sessionKeyBytes = unwrapResult.getOrThrow()

            // 3. Fetch requester public key
            val requesterDoc = firestore.collection("users").document(requesterId).get().await()
            val requesterPublicKey = requesterDoc.getString("publicKey") ?: throw Exception("Requester public key missing")

            // 4. Wrap the key for the requester
            val newWrappedKey = cryptoManager.encryptSessionKey(sessionKeyBytes, requesterPublicKey)
            wrappedMetadataKeys[requesterId] = newWrappedKey

            // 5. Update Firestore record with the new wrapped key
            firestore.collection("chats")
                .document(chatId)
                .collection("shared_collections")
                .document(collectionId)
                .update("wrappedMetadataKeys", wrappedMetadataKeys)
                .await()

            // 6. Mark request as APPROVED
            firestore.collection("chats")
                .document(chatId)
                .collection("access_requests")
                .document("${requesterId}_$collectionId")
                .update("status", "APPROVED")
                .await()
                
            Log.d("SyncRepository", "Access granted to $requesterId for $collectionId")
        } catch (e: Exception) {
            Log.e("SyncRepository", "Failed to grant access", e)
            throw e
        }
    }
}
