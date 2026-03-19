package com.algorithmx.q_base.data.repository

import com.algorithmx.q_base.data.dao.CategoryDao
import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.dao.SessionDao
import com.algorithmx.q_base.data.entity.*
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

    /**
     * Creates a new session with smart selection.
     * Logic: Prioritizes questions with fewer attempts or recent failures.
     */
    suspend fun createNewSession(
        categoryName: String,
        questionCount: Int,
        timeLimitSeconds: Int?
    ): String {
        val sessionId = UUID.randomUUID().toString()
        
        // Get all questions for the category
        val allQuestions = questionDao.getQuestionsByMasterCategory(categoryName).first()
        
        // Smart Selection Logic: Shuffle and take required count
        // (Future improvement: Join with previous attempts to weight selection)
        val selectedQuestions = allQuestions.shuffled().take(questionCount)

        val session = StudySession(
            sessionId = sessionId,
            title = if (categoryName.isNotEmpty()) "$categoryName Exam" else "Mock Session",
            timeLimitSeconds = timeLimitSeconds,
            createdTimestamp = System.currentTimeMillis(),
            isCompleted = false,
            scoreAchieved = 0f
        )

        val attempts = selectedQuestions.map { question ->
            SessionAttempt(
                sessionId = sessionId,
                questionId = question.questionId,
                attemptStatus = "UNATTEMPTED",
                userSelectedAnswers = "",
                marksObtained = 0f
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

    suspend fun getAnswerForQuestion(questionId: String): Answer? =
        questionDao.getAnswerForQuestion(questionId).first()

    /**
     * Updates an attempt and recalculates the session score.
     */
    suspend fun updateAttemptAndRecalculate(attempt: SessionAttempt) {
        // 1. Calculate marks for this specific attempt
        val answer = questionDao.getAnswerForQuestion(attempt.questionId).first()
        val marks = if (answer != null && attempt.userSelectedAnswers == answer.correctAnswerString) {
            4f // Correct
        } else if (attempt.userSelectedAnswers.isEmpty()) {
            0f // Unattempted
        } else {
            -1f // Incorrect (Medical marking)
        }
        
        val updatedAttempt = attempt.copy(marksObtained = marks)
        sessionDao.updateAttempt(updatedAttempt)

        // 2. Recalculate total session score
        val allAttempts = sessionDao.getAttemptsForSession(attempt.sessionId).first()
        val totalMarks = allAttempts.sumOf { it.marksObtained.toDouble() }.toFloat()
        val maxPossible = allAttempts.size * 4f
        val percentage = if (maxPossible > 0) (totalMarks / maxPossible) * 100f else 0f

        sessionDao.getSessionById(attempt.sessionId)?.let { session ->
            sessionDao.updateSession(session.copy(scoreAchieved = percentage.coerceAtLeast(0f)))
        }
    }

    suspend fun updateAttempt(attempt: SessionAttempt) {
        sessionDao.updateAttempt(attempt)
    }

    suspend fun updateSession(session: StudySession) {
        sessionDao.updateSession(session)
    }
}
