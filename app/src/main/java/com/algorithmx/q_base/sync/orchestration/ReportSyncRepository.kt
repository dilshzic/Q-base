package com.algorithmx.q_base.sync.orchestration

import android.util.Log
import com.algorithmx.q_base.core.data.chat.ChatEntity
import com.algorithmx.q_base.core.data.chat.ChatRemoteRepository
import com.algorithmx.q_base.core.data.chat.MessageEntity
import com.algorithmx.q_base.data.collections.Question
import com.algorithmx.q_base.data.collections.QuestionOption
import com.algorithmx.q_base.data.collections.Answer
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.core.data.UserEntity
import com.algorithmx.q_base.feature.sessions.data.SessionDao
import com.algorithmx.q_base.core.data.auth.AuthRepository
import com.algorithmx.q_base.core.data.backend.CoreDatabase
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.core.data.chat.ChatLocalDataSource
import com.algorithmx.q_base.core.data.UserDao
import com.algorithmx.q_base.core.data.backend.CoreQuery
import com.algorithmx.q_base.core.data.backend.CoreQueryOperator
import io.appwrite.Client
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import dagger.Lazy
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportSyncRepository @Inject constructor(
    private val appwriteClient: Client,
    private val databases: CoreDatabase,
    private val authRepository: AuthRepository,
    private val chatRemoteRepository: ChatRemoteRepository,
    private val sessionDao: SessionDao,
    private val collectionDao: Lazy<CollectionDao>,
    private val questionDao: Lazy<QuestionDao>,
    private val chatLocalDataSource: Lazy<ChatLocalDataSource>,
    private val messageSyncRepository: Lazy<MessageSyncRepository>,
    private val chatManagerRepository: Lazy<ChatManagerRepository>,
    private val userDao: Lazy<UserDao>,
    private val actionQueueDao: com.algorithmx.q_base.data.sync.ActionQueueDao
) {
    private val currentUserId: String?
        get() = authRepository.currentUserId

    private suspend fun sendSystemMessageToUser(targetUserId: String, payloadText: String) {
        val myId = currentUserId ?: return
        if (myId == targetUserId) return
        
        try {
            val chatManager = chatManagerRepository.get()
            val localDataSource = chatLocalDataSource.get()
            
            val allChats = localDataSource.getAllChats().firstOrNull() ?: emptyList()
            var existingChat = allChats.find { chat ->
                !chat.isGroup && chat.participantIds.split(",").contains(myId) && chat.participantIds.split(",").contains(targetUserId)
            }
            
            if (existingChat == null) {
                val newChatId = UUID.randomUUID().toString()
                val targetUser = userDao.get().getUserById(targetUserId)
                val targetUserName = targetUser?.displayName ?: "User"
                val newChat = ChatEntity(
                    chatId = newChatId,
                    chatName = targetUserName,
                    isGroup = false,
                    participantIds = "$myId,$targetUserId",
                    adminIds = listOf(myId)
                )
                localDataSource.upsertChat(newChat)
                try {
                    chatManager.createChatOnRemote(newChat)
                } catch (_: Exception) {}
                existingChat = newChat
            }
            
            val message = MessageEntity(
                messageId = UUID.randomUUID().toString(),
                chatId = existingChat.chatId,
                senderId = myId,
                payload = payloadText,
                type = "TEXT",
                timestamp = System.currentTimeMillis()
            )
            messageSyncRepository.get().sendMessage(message)
        } catch (e: Exception) {
            Log.e("ReportSyncRepository", "Failed to send warning message to user $targetUserId", e)
        }
    }

    private suspend fun sendWarningToGroup(groupId: String, payloadText: String) {
        val myId = currentUserId ?: return
        try {
            val message = MessageEntity(
                messageId = UUID.randomUUID().toString(),
                chatId = groupId,
                senderId = myId,
                payload = payloadText,
                type = "TEXT",
                timestamp = System.currentTimeMillis()
            )
            messageSyncRepository.get().sendMessage(message)
        } catch (e: Exception) {
            Log.e("ReportSyncRepository", "Failed to send warning to group $groupId", e)
        }
    }

    suspend fun reportSession(sessionId: String, reason: String) {
        val payload = org.json.JSONObject().apply {
            put("sessionId", sessionId)
            put("reason", reason)
        }
        
        try {
            submitSessionReportInternal(sessionId, reason)
        } catch (e: Exception) {
            Log.w("ReportSyncRepository", "Offline: Queuing session report for $sessionId")
            actionQueueDao.insertAction(
                OfflineActionEntity(
                    actionType = "REPORT_SESSION",
                    payloadJson = payload.toString()
                )
            )
        }
    }

    internal suspend fun submitSessionReportInternal(sessionId: String, reason: String) {
        val reporterId = currentUserId ?: return
        val session = sessionDao.getSessionById(sessionId) ?: return
        
        val reportRefId = UUID.randomUUID().toString()
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to (System.currentTimeMillis() / 1000).toInt(),
            "sessionId" to session.sessionId,
            "contentJson" to com.google.gson.Gson().toJson(session)
        )

        databases.createDocument(
            collectionId = "reported_sessions",
            documentId = reportRefId,
            data = reportMap
        ).getOrThrow()
    }

    suspend fun reportQuestion(
        question: Question,
        options: List<QuestionOption>,
        answer: Answer?,
        reason: String
    ) {
        val reporterId = currentUserId ?: throw IllegalStateException("User not authenticated")
        
        val contentMap = mapOf(
            "question" to question,
            "options" to options,
            "answer" to answer
        )
        val contentJson = com.google.gson.Gson().toJson(contentMap)

        val payload = org.json.JSONObject().apply {
            put("questionId", question.questionId)
            put("reason", reason)
            put("contentJson", contentJson)
        }

        try {
            submitQuestionReportInternal(question.questionId, reason, contentJson)
        } catch (e: Exception) {
            Log.w("ReportSyncRepository", "Offline: Queuing question report for ${question.questionId}")
            actionQueueDao.insertAction(
                OfflineActionEntity(
                    actionType = "REPORT_QUESTION",
                    payloadJson = payload.toString()
                )
            )
        }
    }

    internal suspend fun submitQuestionReportInternal(questionId: String, reason: String, contentJson: String) {
        val reporterId = currentUserId ?: throw IllegalStateException("User not authenticated")
        val reportRefId = UUID.randomUUID().toString()
        
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to (System.currentTimeMillis() / 1000).toInt(),
            "questionId" to questionId,
            "contentJson" to contentJson
        )

        databases.createDocument(
            collectionId = "reported_questions",
            documentId = reportRefId,
            data = reportMap
        ).getOrThrow()
    }

    suspend fun reportGroup(group: ChatEntity, reason: String) {
        val sampleMessages = chatLocalDataSource.get().getMessagesForChat(group.chatId).firstOrNull()?.takeLast(20) ?: emptyList()
        chatRemoteRepository.reportGroup(group, reason, sampleMessages)
    }

    suspend fun reportUser(user: UserEntity, reason: String) {
        val reporterId = currentUserId ?: return
        val reportRefId = UUID.randomUUID().toString()
        
        val allChats = chatLocalDataSource.get().getAllChats().firstOrNull() ?: emptyList()
        val directChat = allChats.find { chat ->
            !chat.isGroup && chat.participantIds.split(",").contains(reporterId) && chat.participantIds.split(",").contains(user.userId)
        }
        val sampleMessages = if (directChat != null) {
            chatLocalDataSource.get().getMessagesForChat(directChat.chatId).firstOrNull()?.takeLast(20) ?: emptyList()
        } else {
            emptyList()
        }

        val contentMap = mapOf(
            "user" to user,
            "sampleMessages" to sampleMessages
        )
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to (System.currentTimeMillis() / 1000).toInt(),
            "reportedUserId" to user.userId,
            "contentJson" to com.google.gson.Gson().toJson(contentMap)
        )
        try {
            databases.createDocument(
                collectionId = "reported_users",
                documentId = reportRefId,
                data = reportMap
            ).getOrThrow()
        } catch (e: Exception) {
            Log.e("ReportSyncRepository", "Failed to submit reported user ${user.userId}", e)
            throw e
        }

        // Disabled warning messages sent to reported user and group admins
        /*
        sendSystemMessageToUser(
            targetUserId = user.userId,
            payloadText = "⚠️ WARNING: You have been reported for suspicious or abusive behavior. Reason: $reason"
        )

        try {
            val allChats = chatLocalDataSource.get().getAllChats().firstOrNull() ?: emptyList()
            for (chat in allChats) {
                if (chat.isGroup) {
                    val participants = chat.participantIds.split(",")
                    if (participants.contains(reporterId) && participants.contains(user.userId)) {
                        for (adminId in chat.adminIds) {
                            if (adminId != reporterId) {
                                sendSystemMessageToUser(
                                    targetUserId = adminId,
                                    payloadText = "⚠️ USER REPORT: Member '${user.displayName}' (ID: ${user.userId}) was reported by a participant in group '${chat.chatName}'. Reason: $reason"
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ReportSyncRepository", "Failed to notify group admins of user report", e)
        }
        */
    }

    suspend fun reportCollection(collection: StudyCollection, reason: String) {
        val reporterId = currentUserId ?: return
        
        val sets = collectionDao.get().getSetsByStudyCollectionIdOnce(collection.collectionId)
        val questionsList = mutableListOf<Map<String, Any?>>()
        for (set in sets) {
            val questions = collectionDao.get().getQuestionsForSetOnce(set.setId)
            for (question in questions) {
                val options = questionDao.get().getOptionsForQuestionOnce(question.questionId)
                val answer = questionDao.get().getAnswerForQuestionOnce(question.questionId)
                questionsList.add(mapOf(
                    "question" to question,
                    "options" to options,
                    "answer" to answer
                ))
            }
        }

        val contentMap = mapOf(
            "collection" to collection,
            "questions" to questionsList
        )
        val contentJson = com.google.gson.Gson().toJson(contentMap)

        val payload = org.json.JSONObject().apply {
            put("collectionId", collection.collectionId)
            put("reason", reason)
            put("contentJson", contentJson)
        }

        try {
            submitCollectionReportInternal(collection.collectionId, reason, contentJson)
        } catch (e: Exception) {
            Log.w("ReportSyncRepository", "Offline: Queuing collection report for ${collection.collectionId}")
            actionQueueDao.insertAction(
                OfflineActionEntity(
                    actionType = "REPORT_COLLECTION",
                    payloadJson = payload.toString()
                )
            )
        }
    }

    internal suspend fun submitCollectionReportInternal(collectionId: String, reason: String, contentJson: String) {
        val reporterId = currentUserId ?: return
        val reportRefId = UUID.randomUUID().toString()
        
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to (System.currentTimeMillis() / 1000).toInt(),
            "collectionId" to collectionId,
            "contentJson" to contentJson
        )

        databases.createDocument(
            collectionId = "reported_collections",
            documentId = reportRefId,
            data = reportMap
        ).getOrThrow()
    }

    suspend fun reportMessage(message: MessageEntity, reason: String) {
        chatRemoteRepository.reportMessage(message, reason)
    }

    fun observeGroupReports(groupId: String): Flow<List<Map<String, Any>>> {
        return callbackFlow {
            suspend fun fetchReports(): List<Map<String, Any>> {
                val queries = listOf(
                    CoreQuery("groupId", CoreQueryOperator.EQUAL, groupId)
                )
                return try {
                    databases.queryDocuments("reported_groups", queries).getOrNull() ?: emptyList()
                } catch (e: Exception) {
                    Log.e("ReportSyncRepository", "Failed to query reported_groups for $groupId", e)
                    emptyList()
                }
            }

            launch {
                try {
                    trySend(fetchReports())
                } catch (_: Exception) {}
            }

            val realtime = io.appwrite.services.Realtime(appwriteClient)
            val subscription = realtime.subscribe("tablesdb.qbase_db.tables.reported_groups.rows") { event ->
                launch {
                    try {
                        trySend(fetchReports())
                    } catch (_: Exception) {}
                }
            }

            awaitClose { subscription.close() }
        }
    }

    fun observeMessageReports(chatId: String): Flow<List<Map<String, Any>>> {
        return callbackFlow {
            suspend fun fetchAndFilterReports(): List<Map<String, Any>> {
                return try {
                    val allReports = databases.queryDocuments("reported_messages", emptyList()).getOrNull() ?: emptyList()
                    allReports.filter { doc ->
                        val contentJson = doc["contentJson"] as? String ?: ""
                        try {
                            val jsonObj = org.json.JSONObject(contentJson)
                            val msgChatId = jsonObj.optString("chatId")
                            msgChatId == chatId
                        } catch (e: Exception) {
                            false
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ReportSyncRepository", "Failed to query reported_messages for $chatId", e)
                    emptyList()
                }
            }

            launch {
                try {
                    trySend(fetchAndFilterReports())
                } catch (_: Exception) {}
            }

            val realtime = io.appwrite.services.Realtime(appwriteClient)
            val subscription = realtime.subscribe("tablesdb.qbase_db.tables.reported_messages.rows") { event ->
                launch {
                    try {
                        trySend(fetchAndFilterReports())
                    } catch (_: Exception) {}
                }
            }

            awaitClose { subscription.close() }
        }
    }

    suspend fun dismissGroupReport(reportId: String) {
        try {
            databases.deleteDocument("reported_groups", reportId).getOrThrow()
        } catch (e: Exception) {
            Log.e("ReportSyncRepository", "Failed to dismiss group report $reportId", e)
            throw e
        }
    }

    suspend fun dismissMessageReport(reportId: String) {
        try {
            databases.deleteDocument("reported_messages", reportId).getOrThrow()
        } catch (e: Exception) {
            Log.e("ReportSyncRepository", "Failed to dismiss message report $reportId", e)
            throw e
        }
    }

    suspend fun deleteReportedMessage(messageId: String, reportId: String) {
        try {
            // Delete locally
            chatLocalDataSource.get().deleteMessageById(messageId)
            
            // Delete globally from Appwrite messages collection (if it hasn't been deleted yet due to ephemeral delivery)
            try {
                databases.deleteDocument("messages", messageId).getOrThrow()
            } catch (e: Exception) {
                // Ignore, message might already be deleted globally due to ephemeral acknowledgment
            }
            
            // Delete the report record
            databases.deleteDocument("reported_messages", reportId).getOrThrow()
        } catch (e: Exception) {
            Log.e("ReportSyncRepository", "Failed to delete reported message $messageId", e)
            throw e
        }
    }
}