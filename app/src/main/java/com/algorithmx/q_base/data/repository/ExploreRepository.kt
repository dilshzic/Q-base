package com.algorithmx.q_base.data.repository

import com.algorithmx.q_base.data.dao.CollectionDao
import com.algorithmx.q_base.data.dao.ProblemReportDao
import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.entity.*
import com.algorithmx.q_base.data.entity.Collection as AppCollection
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExploreRepository @Inject constructor(
    private val collectionDao: CollectionDao,
    private val questionDao: QuestionDao,
    private val sessionDao: com.algorithmx.q_base.data.dao.SessionDao,
    private val problemReportDao: ProblemReportDao,
    private val userDao: com.algorithmx.q_base.data.dao.UserDao
) {
    fun getCurrentUser(userId: String): Flow<UserEntity?> = userDao.getCurrentUser(userId)

    fun getCollections(): Flow<List<AppCollection>> = collectionDao.getAllCollections()

    fun getCollectionsWithCount(): Flow<List<com.algorithmx.q_base.data.entity.CollectionWithCount>> =
        collectionDao.getAllCollectionsWithCount()

    fun getCollectionById(collectionId: String): Flow<AppCollection?> = collectionDao.getCollectionById(collectionId)

    fun getSetsByCollectionId(collectionId: String): Flow<List<QuestionSet>> = collectionDao.getSetsByCollectionId(collectionId)

    fun getLastSessionForCollection(collectionId: String): Flow<StudySession?> = sessionDao.getLastSessionForCollection(collectionId)

    fun getQuestionsByCollection(collection: String): Flow<List<Question>> =
        questionDao.getQuestionsByCollection(collection)

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

    fun getQuestionCountByCollection(collectionName: String): Flow<Int> =
        questionDao.getQuestionCountByCollection(collectionName)

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

    suspend fun deleteCollection(collectionId: String) {
        collectionDao.deleteCollectionById(collectionId)
    }
}
