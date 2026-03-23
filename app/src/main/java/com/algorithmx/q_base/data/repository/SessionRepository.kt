package com.algorithmx.q_base.data.repository

import com.algorithmx.q_base.data.dao.CollectionDao
import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.dao.SessionDao
import com.algorithmx.q_base.data.entity.*
import com.algorithmx.q_base.data.entity.Collection as AppCollection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val collectionDao: CollectionDao,
    private val questionDao: QuestionDao,
    private val userDao: com.algorithmx.q_base.data.dao.UserDao
) {
    fun getCurrentUser(userId: String): Flow<com.algorithmx.q_base.data.entity.UserEntity?> = 
        userDao.getCurrentUser(userId)

    fun getAllSessions(): Flow<List<StudySession>> = sessionDao.getAllSessions()

    fun getAllCollections(): Flow<List<AppCollection>> = collectionDao.getAllCollections()

    fun getAllSets(): Flow<List<QuestionSet>> = sessionDao.getAllSets()

    suspend fun getSessionById(sessionId: String): StudySession? = sessionDao.getSessionById(sessionId)

    /**
     * Creates a new session with smart selection.
     */
    suspend fun createNewSession(
        title: String,
        questionIds: List<String>,
        timeLimitSeconds: Int?,
        timingType: String = "NONE",
        isRandom: Boolean = false
    ): String {
        val sessionId = UUID.randomUUID().toString()
        
        val finalQuestionIds = if (isRandom) questionIds.shuffled() else questionIds

        val session = StudySession(
            sessionId = sessionId,
            title = title.ifEmpty { "Practice Session" },
            timeLimitSeconds = timeLimitSeconds,
            createdTimestamp = System.currentTimeMillis(),
            isCompleted = false,
            scoreAchieved = 0f,
            timingType = timingType,
            isRandom = isRandom
        )

        val attempts = finalQuestionIds.map { qId ->
            SessionAttempt(
                sessionId = sessionId,
                questionId = qId,
                attemptStatus = "UNATTEMPTED",
                userSelectedAnswers = "",
                marksObtained = 0f
            )
        }

        sessionDao.insertSession(session)
        sessionDao.insertAttempts(attempts)

        return sessionId
    }

    suspend fun createNewSessionSmart(
        collectionName: String,
        questionCount: Int,
        timeLimitSeconds: Int?
    ): String {
        val allQuestions = questionDao.getQuestionsByCollection(collectionName).first()
        val selectedIds = allQuestions.shuffled().take(questionCount).map { it.questionId }
        
        return createNewSession(
            title = if (collectionName.isNotEmpty()) "$collectionName Exam" else "Mock Session",
            questionIds = selectedIds,
            timeLimitSeconds = timeLimitSeconds,
            timingType = if (timeLimitSeconds != null) "TOTAL" else "NONE",
            isRandom = true
        )
    }

    fun getQuestionsByCollection(collection: String): Flow<List<Question>> = 
        questionDao.getQuestionsByCollection(collection)

    fun getAttemptsForSession(sessionId: String): Flow<List<SessionAttempt>> =
        sessionDao.getAttemptsForSession(sessionId)

    suspend fun getQuestionById(questionId: String): Question? =
        questionDao.getQuestionById(questionId)

    fun getOptionsForQuestion(questionId: String): Flow<List<QuestionOption>> =
        questionDao.getOptionsForQuestion(questionId)

    suspend fun getAnswerForQuestion(questionId: String): Answer? =
        questionDao.getAnswerForQuestion(questionId).first()

    suspend fun updateAttemptAndRecalculate(attempt: SessionAttempt) {
        val question = questionDao.getQuestionById(attempt.questionId) ?: return
        val answer = questionDao.getAnswerForQuestion(attempt.questionId).first()
        val options = questionDao.getOptionsForQuestionOnce(attempt.questionId)
        
        val type = question.questionType?.trim()?.uppercase()
        val marks = when (type) {
            "MTF", "MCQ", "T/F", "MCQ1" -> {
                val correctLetters = answer?.correctAnswerString?.split(",")?.map { it.trim() }
                if (correctLetters == null || attempt.userSelectedAnswers.isEmpty()) 0f
                else {
                    var subTotal = 0f
                    val userSelections = attempt.userSelectedAnswers.split(",")
                    
                    options.forEach { option ->
                        val letter = option.optionLetter ?: ""
                        val userValue = when {
                            userSelections.contains("${letter}_T") -> true
                            userSelections.contains("${letter}_F") -> false
                            userSelections.contains(letter) -> true
                            else -> if (type == "MCQ" || type == "MCQ1") false else null
                        }
                        val isActuallyTrue = correctLetters.contains(letter)
                        
                        if (userValue != null) {
                            if (userValue == isActuallyTrue) subTotal += 1 else subTotal -= 1
                        }
                    }
                    subTotal.coerceIn(0f, options.size.toFloat())
                }
            }
            else -> { // SBA
                if (answer != null && attempt.userSelectedAnswers == answer.correctAnswerString) {
                    4f 
                } else if (attempt.userSelectedAnswers.isEmpty()) {
                    0f 
                } else {
                    -1f 
                }
            }
        }
        
        val updatedAttempt = attempt.copy(marksObtained = marks)
        sessionDao.updateAttempt(updatedAttempt)

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

    suspend fun saveAnswer(answer: Answer) {
        questionDao.insertAnswer(answer)
    }

    suspend fun deleteSessions(sessionIds: List<String>) {
        sessionDao.deleteAttemptsForSessions(sessionIds)
        sessionDao.deleteSessionsByIds(sessionIds)
    }
}
