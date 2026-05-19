package com.algorithmx.q_base.data.sync

import android.util.Log
import com.algorithmx.q_base.BuildConfig
import com.algorithmx.q_base.data.chat.ChatDao
import com.algorithmx.q_base.data.chat.MessageEntity
import com.algorithmx.q_base.data.collections.*
import com.algorithmx.q_base.data.core.UserDao
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.data.auth.AuthRepository
import com.algorithmx.q_base.core_crypto.CryptoManager
import com.algorithmx.q_base.data.util.MockDownloader
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy

@Singleton
class CollectionSyncRepository @Inject constructor(
    private val appwriteClient: Client,
    private val databases: Databases,
    private val authRepository: AuthRepository,
    private val storage: Storage,
    private val chatDao: ChatDao,
    private val userDao: UserDao,
    private val cryptoManager: CryptoManager,
    private val mockDownloader: MockDownloader,
    private val collectionDao: CollectionDao,
    private val questionDao: QuestionDao,
    private val collectionVersionLedgerDao: CollectionVersionLedgerDao,
    private val chatSyncRepository: Lazy<ChatSyncRepository>
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val bucketId = BuildConfig.APPWRITE_BUCKET_ID
    private val projectId = BuildConfig.APPWRITE_PROJECT_ID

    private val currentUserId: String?
        get() = authRepository.currentUserId

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
            Log.e("CollectionSyncRepository", "Failed to send sync request in Appwrite", e)
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

    private fun extractFileIdFromUrl(url: String): String? {
        return try {
            val pattern = "files/([^/]+)/download".toRegex()
            val matchResult = pattern.find(url)
            matchResult?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun acknowledgeCollectionDownload(collectionId: String) {
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

    suspend fun applyCollectionMicroUpdate(payload: String) {
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject(payload)
                val collectionId = json.getString("collectionId")
                val revisionId = json.getInt("revisionId")
                val diff = json.getJSONObject("diff")
                
                val ledger = collectionVersionLedgerDao.getLedgerForCollection(collectionId)
                val localRevision = ledger?.currentRevisionId ?: 0
                
                if (revisionId <= localRevision) {
                    Log.d("CollectionSyncRepository", "Micro-update redundant: received $revisionId, local is $localRevision. Skipping.")
                    return@withContext
                }
                
                if (revisionId > localRevision + 1) {
                    Log.w("CollectionSyncRepository", "Sequence gap detected in collection micro-update: received $revisionId, expected ${localRevision + 1}. Buffering.")
                    return@withContext
                }
                
                val action = diff.optString("action", "UPSERT_QUESTION")
                if (action == "UPSERT_QUESTION") {
                    val questionObj = diff.getJSONObject("question")
                    val qId = questionObj.getString("questionId")
                    val stem = questionObj.getString("stem")
                    
                    val question = Question(
                        questionId = qId,
                        collection = null,
                        category = "General",
                        tags = "Group Synchronized",
                        questionType = "SBA",
                        stem = stem,
                        isPinned = false
                    )
                    questionDao.insertQuestion(question)
                    questionDao.deleteOptionsForQuestion(qId)
                    
                    val optionsArr = questionObj.getJSONArray("options")
                    val options = mutableListOf<QuestionOption>()
                    for (i in 0 until optionsArr.length()) {
                        val optObj = optionsArr.getJSONObject(i)
                        options.add(
                            QuestionOption(
                                questionId = qId,
                                optionLetter = optObj.getString("optionLetter"),
                                optionText = optObj.getString("optionText"),
                                optionExplanation = null
                            )
                        )
                    }
                    questionDao.insertOptions(options)
                    
                    if (questionObj.has("answer")) {
                        val ansObj = questionObj.getJSONObject("answer")
                        val answer = Answer(
                            questionId = qId,
                            correctAnswerString = ansObj.getString("correctAnswerString"),
                            generalExplanation = ansObj.optString("generalExplanation", ""),
                            references = ansObj.optString("references", "")
                        )
                        questionDao.insertAnswer(answer)
                    }
                }
                
                collectionVersionLedgerDao.insertLedger(
                    CollectionVersionLedgerEntity(
                        collectionId = collectionId,
                        currentRevisionId = revisionId,
                        lastAppliedTimestamp = System.currentTimeMillis()
                    )
                )
                Log.d("CollectionSyncRepository", "Successfully applied collection micro-update revision $revisionId for collection $collectionId")
            } catch (e: Exception) {
                Log.e("CollectionSyncRepository", "Failed to apply collection micro-update", e)
            }
        }
    }

    suspend fun broadcastCollectionMicroUpdate(chatId: String, collectionId: String, diff: JSONObject) {
        withContext(Dispatchers.IO) {
            try {
                val ledger = collectionVersionLedgerDao.getLedgerForCollection(collectionId)
                val newRevision = (ledger?.currentRevisionId ?: 0) + 1
                
                val payloadObj = JSONObject()
                payloadObj.put("collectionId", collectionId)
                payloadObj.put("revisionId", newRevision)
                payloadObj.put("diff", diff)
                
                val payloadStr = payloadObj.toString()
                
                val message = MessageEntity(
                    messageId = UUID.randomUUID().toString(),
                    chatId = chatId,
                    senderId = currentUserId ?: "",
                    payload = payloadStr,
                    type = "COLLECTION_MICRO_UPDATE",
                    timestamp = System.currentTimeMillis(),
                    decryptionStatus = "SUCCESS",
                    keyFingerprint = "",
                    wrappedKey = ""
                )
                chatSyncRepository.get().sendMessage(message)
                
                collectionVersionLedgerDao.insertLedger(
                    CollectionVersionLedgerEntity(
                        collectionId = collectionId,
                        currentRevisionId = newRevision,
                        lastAppliedTimestamp = System.currentTimeMillis()
                    )
                )
                Log.d("CollectionSyncRepository", "Broadcasted collection micro-update revision $newRevision for collection $collectionId")
            } catch (e: Exception) {
                Log.e("CollectionSyncRepository", "Failed to broadcast collection micro-update", e)
            }
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
        chatSyncRepository.get().sendMessage(message)
    }

    suspend fun applyCollectionPatch(jsonString: String) {
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
            Log.e("CollectionSyncRepository", "Failed to apply collection patch", e)
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
            Log.e("CollectionSyncRepository", "Failed to request collection access", e)
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
            
            Log.d("CollectionSyncRepository", "Access granted to $requesterId for $collectionId")
        } catch (e: Exception) {
            Log.e("CollectionSyncRepository", "Failed to grant access", e)
            throw e
        }
    }
}
