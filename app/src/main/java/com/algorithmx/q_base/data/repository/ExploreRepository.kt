package com.algorithmx.q_base.data.repository

import com.algorithmx.q_base.data.dao.CategoryDao
import com.algorithmx.q_base.data.dao.ProblemReportDao
import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExploreRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val questionDao: QuestionDao,
    private val problemReportDao: ProblemReportDao
) {
    fun getAllCategories(): Flow<List<MasterCategory>> = categoryDao.getAllCategories()

    fun getQuestionsByMasterCategory(masterCategory: String): Flow<List<Question>> =
        questionDao.getQuestionsByMasterCategory(masterCategory)

    suspend fun getQuestionById(questionId: String): Question? =
        questionDao.getQuestionById(questionId)

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
}
