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
import com.algorithmx.q_base.core.data.chat.ChatLocalDataSource
import com.algorithmx.q_base.core.data.UserDao
import kotlinx.coroutines.flow.firstOrNull
import dagger.Lazy
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportSyncRepository @Inject constructor(
    private val databases: CoreDatabase,
    private val authRepository: AuthRepository,
    private val chatRemoteRepository: ChatRemoteRepository,
    private val sessionDao: SessionDao,
    private val collectionDao: Lazy<CollectionDao>,
    private val chatLocalDataSource: Lazy<ChatLocalDataSource>,
    private val messageSyncRepository: Lazy<MessageSyncRepository>,
    private val chatManagerRepository: Lazy<ChatManagerRepository>,
    private val userDao: Lazy<UserDao>
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

        try {
            databases.createDocument(
                collectionId = "reported_sessions",
                documentId = reportRefId,
                data = reportMap
            ).getOrThrow()
        } catch (e: Exception) {
            Log.e("ReportSyncRepository", "Failed to submit reported session for $sessionId", e)
        }

        val colId = session.collectionId
        if (!colId.isNullOrBlank()) {
            try {
                val collection = collectionDao.get().getStudyCollectionByIdOnce(colId)
                val groupId = collection?.sharedWithGroupId
                if (!groupId.isNullOrBlank()) {
                    sendWarningToGroup(
                        groupId = groupId,
                        payloadText = "⚠️ PROBLEM REPORT: Issue with shared session '${session.title}'. Reason: $reason"
                    )
                }
            } catch (e: Exception) {
                Log.e("ReportSyncRepository", "Failed to send session warning to group", e)
            }
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
        
        val contentMap = mapOf(
            "question" to question,
            "options" to options,
            "answer" to answer
        )

        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to (System.currentTimeMillis() / 1000).toInt(),
            "questionId" to question.questionId,
            "contentJson" to com.google.gson.Gson().toJson(contentMap)
        )

        try {
            databases.createDocument(
                collectionId = "reported_questions",
                documentId = reportRefId,
                data = reportMap
            ).getOrThrow()
        } catch (e: Exception) {
            Log.e("ReportSyncRepository", "Failed to submit reported question", e)
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
            "reportedAt" to (System.currentTimeMillis() / 1000).toInt(),
            "reportedUserId" to user.userId,
            "contentJson" to com.google.gson.Gson().toJson(user)
        )
        try {
            databases.createDocument(
                collectionId = "reported_users",
                documentId = reportRefId,
                data = reportMap
            ).getOrThrow()
        } catch (e: Exception) {
            Log.e("ReportSyncRepository", "Failed to submit reported user ${user.userId}", e)
        }

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
    }

    suspend fun reportCollection(collection: StudyCollection, reason: String) {
        val reporterId = currentUserId ?: return
        val reportRefId = UUID.randomUUID().toString()
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to (System.currentTimeMillis() / 1000).toInt(),
            "collectionId" to collection.collectionId,
            "contentJson" to com.google.gson.Gson().toJson(collection)
        )
        try {
            databases.createDocument(
                collectionId = "reported_collections",
                documentId = reportRefId,
                data = reportMap
            ).getOrThrow()
        } catch (e: Exception) {
            Log.e("ReportSyncRepository", "Failed to submit reported collection", e)
        }

        val groupId = collection.sharedWithGroupId
        if (!groupId.isNullOrBlank()) {
            sendWarningToGroup(
                groupId = groupId,
                payloadText = "⚠️ PROBLEM REPORT: Issue with shared collection '${collection.name}'. Reason: $reason"
            )
        }
    }

    suspend fun reportMessage(message: MessageEntity, reason: String) {
        chatRemoteRepository.reportMessage(message, reason)
    }
}