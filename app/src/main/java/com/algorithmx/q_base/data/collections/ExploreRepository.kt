package com.algorithmx.q_base.data.collections

import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.data.core.UserDao
import com.algorithmx.q_base.data.sessions.SessionDao
import com.algorithmx.q_base.data.sessions.StudySession
import com.algorithmx.q_base.data.sessions.SessionAttempt
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.collections.StudyCollectionWithCount
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExploreRepository @Inject constructor(
    private val collectionDao: CollectionDao,
    private val questionDao: QuestionDao,
    private val sessionDao: SessionDao,
    private val problemReportDao: ProblemReportDao,
    private val userDao: UserDao
) {
    fun getCurrentUser(userId: String): Flow<UserEntity?> = userDao.getCurrentUser(userId)

    fun getStudyCollections(): Flow<List<StudyCollection>> = collectionDao.getAllStudyCollections()

    fun getStudyCollectionsWithCount(): Flow<List<StudyCollectionWithCount>> =
        collectionDao.getAllStudyCollectionsWithCount()

    fun getStudyCollectionById(collectionId: String): Flow<StudyCollection?> = collectionDao.getStudyCollectionById(collectionId)

    fun getSetsByStudyCollectionId(collectionId: String): Flow<List<QuestionSet>> = collectionDao.getSetsByStudyCollectionId(collectionId)

    fun getLastSessionForStudyCollection(collectionId: String): Flow<StudySession?> = sessionDao.getLastSessionForStudyCollection(collectionId)

    fun getQuestionsByStudyCollection(collection: String): Flow<List<Question>> =
        questionDao.getQuestionsByStudyCollection(collection)

    fun getQuestionsBySet(setId: String): Flow<List<Question>> = collectionDao.getQuestionsForSet(setId)

    suspend fun getQuestionById(questionId: String): Question? =
        questionDao.getQuestionById(questionId)

    suspend fun getSessionById(sessionId: String): StudySession? = sessionDao.getSessionById(sessionId)

    suspend fun updateSession(session: StudySession) = sessionDao.updateSession(session)

    fun getOptionsForQuestion(questionId: String): Flow<List<QuestionOption>> =
        questionDao.getOptionsForQuestion(questionId)

    fun getAnswerForQuestion(questionId: String): Flow<Answer?> =
        questionDao.getAnswerForQuestion(questionId)

    suspend fun updateQuestion(question: Question) {
        questionDao.updateQuestion(question)
    }

    suspend fun reportProblem(report: ProblemReport) {
        problemReportDao.insertReport(report)
    }

    fun getAllSets(): Flow<List<QuestionSet>> = sessionDao.getAllSets()

    fun getAllSessions(): Flow<List<StudySession>> = sessionDao.getAllSessions()

    fun getQuestionCountByStudyCollection(collectionName: String): Flow<Int> =
        questionDao.getQuestionCountByStudyCollection(collectionName)

    suspend fun addQuestionToSet(setId: String, questionId: String) {
        questionDao.insertSetQuestionCrossRef(
            SetQuestionCrossRef(setId = setId, questionId = questionId)
        )
    }

    suspend fun addQuestionToSession(sessionId: String, questionId: String) {
        sessionDao.insertAttempts(listOf(
            SessionAttempt(
                sessionId = sessionId,
                questionId = questionId,
                attemptStatus = "UNATTEMPTED",
                userSelectedAnswers = "",
                marksObtained = 0f
            )
        ))
    }

    suspend fun saveSet(set: QuestionSet) {
        sessionDao.insertSet(set)
    }

    suspend fun saveAnswer(answer: Answer) {
        questionDao.insertAnswer(answer)
    }

    suspend fun deleteStudyCollection(collectionId: String) {
        collectionDao.deleteStudyCollectionById(collectionId)
    }
}
