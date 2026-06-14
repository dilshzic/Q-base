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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportSyncRepository @Inject constructor(
    private val databases: CoreDatabase,
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
            "reportedAt" to System.currentTimeMillis() / 1000,
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
            "reportedAt" to System.currentTimeMillis() / 1000,
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
    }

    suspend fun reportCollection(collection: StudyCollection, reason: String) {
        val reporterId = currentUserId ?: return
        val reportRefId = UUID.randomUUID().toString()
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis() / 1000,
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
    }

    suspend fun reportMessage(message: MessageEntity, reason: String) {
        chatRemoteRepository.reportMessage(message, reason)
    }
}