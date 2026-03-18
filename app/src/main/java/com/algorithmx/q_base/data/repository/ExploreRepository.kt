package com.algorithmx.q_base.data.repository

import com.algorithmx.q_base.data.dao.CategoryDao
import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.entity.Answer
import com.algorithmx.q_base.data.entity.MasterCategory
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionCollection
import com.algorithmx.q_base.data.entity.QuestionOption
import kotlinx.coroutines.flow.Flow

class ExploreRepository(
    private val categoryDao: CategoryDao,
    private val questionDao: QuestionDao
) {
    fun getAllCategories(): Flow<List<MasterCategory>> = categoryDao.getAllCategories()

    fun getCollectionsByCategory(masterCategory: String): Flow<List<QuestionCollection>> =
        categoryDao.getCollectionsByCategory(masterCategory)

    fun getQuestionsByCategory(category: String): Flow<List<Question>> =
        questionDao.getQuestionsByCategory(category)

    fun getOptionsForQuestion(questionId: String): Flow<List<QuestionOption>> =
        questionDao.getOptionsForQuestion(questionId)

    fun getAnswerForQuestion(questionId: String): Flow<Answer?> =
        questionDao.getAnswerForQuestion(questionId)
}
