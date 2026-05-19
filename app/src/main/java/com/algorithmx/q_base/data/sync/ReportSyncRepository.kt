package com.algorithmx.q_base.data.sync

import android.util.Log
import com.algorithmx.q_base.data.chat.ChatEntity
import com.algorithmx.q_base.data.chat.ChatRemoteRepository
import com.algorithmx.q_base.data.chat.MessageEntity
import com.algorithmx.q_base.data.collections.Question
import com.algorithmx.q_base.data.collections.QuestionOption
import com.algorithmx.q_base.data.collections.Answer
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.data.sessions.SessionDao
import com.algorithmx.q_base.data.auth.AuthRepository
import io.appwrite.services.Databases
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportSyncRepository @Inject constructor(
    private val databases: Databases,
    private val authRepository: AuthRepository,
    private val chatRemoteRepository: ChatRemoteRepository,
    private val sessionDao: SessionDao
) {
    private val currentUserId: String?
        get() = authRepository.currentUserId

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
            Log.e("ReportSyncRepository", "Failed to submit reported session for $sessionId", e)
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
            Log.e("ReportSyncRepository", "Failed to submit reported user ${user.userId}", e)
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
            Log.e("ReportSyncRepository", "Failed to submit reported collection", e)
        }
    }

    suspend fun reportMessage(message: MessageEntity, reason: String) {
        chatRemoteRepository.reportMessage(message, reason)
    }
}
