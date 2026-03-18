package com.algorithmx.q_base.data.repository

import com.algorithmx.q_base.data.dao.CategoryDao
import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.dao.SessionDao
import com.algorithmx.q_base.data.entity.MasterCategory
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionCollection
import com.algorithmx.q_base.data.entity.QuestionOption
import com.algorithmx.q_base.data.entity.SessionAttempt
import com.algorithmx.q_base.data.entity.StudySession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val categoryDao: CategoryDao,
    private val questionDao: QuestionDao
) {
    fun getAllSessions(): Flow<List<StudySession>> = sessionDao.getAllSessions()

    fun getAllCategories(): Flow<List<MasterCategory>> = categoryDao.getAllCategories()

    fun getAllCollections(): Flow<List<QuestionCollection>> = sessionDao.getAllCollections()

    suspend fun getSessionById(sessionId: String): StudySession? = sessionDao.getSessionById(sessionId)

    suspend fun createNewSession(
        categoryName: String,
        questionCount: Int,
        timeLimitSeconds: Int?
    ): String {
        val sessionId = UUID.randomUUID().toString()
        
        // Use getQuestionsByMasterCategory instead of getQuestionsByCategory 
        // because categoryName passed from UI is the MasterCategory name.
        val allQuestions = questionDao.getQuestionsByMasterCategory(categoryName).first()
        val selectedQuestions = allQuestions.shuffled().take(questionCount)

        val session = StudySession(
            sessionId = sessionId,
            title = "$categoryName – $questionCount Qs",
            timeLimitSeconds = timeLimitSeconds,
            createdTimestamp = System.currentTimeMillis()
        )

        val attempts = selectedQuestions.map { question ->
            SessionAttempt(
                sessionId = sessionId,
                questionId = question.questionId,
                attemptStatus = "UNATTEMPTED",
                userSelectedAnswers = ""
            )
        }

        sessionDao.insertSession(session)
        sessionDao.insertAttempts(attempts)

        return sessionId
    }

    fun getAttemptsForSession(sessionId: String): Flow<List<SessionAttempt>> =
        sessionDao.getAttemptsForSession(sessionId)

    suspend fun getQuestionById(questionId: String): Question? =
        questionDao.getQuestionById(questionId)

    fun getOptionsForQuestion(questionId: String): Flow<List<QuestionOption>> =
        questionDao.getOptionsForQuestion(questionId)

    suspend fun updateAttempt(attempt: SessionAttempt) {
        sessionDao.updateAttempt(attempt)
    }
}
