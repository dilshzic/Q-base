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

    fun getCollectionsByMasterCategoryId(masterCategoryId: String): Flow<List<QuestionCollection>> =
        categoryDao.getCollectionsByMasterCategoryId(masterCategoryId)

    /** All questions in a collection via the crossref join — for subject derivation. */
    fun getQuestionsForCollection(collectionId: String): Flow<List<Question>> =
        categoryDao.getQuestionsForCollection(collectionId)

    /** Returns questions filtered by both collection and subject (for the Questions screen). */
    fun getQuestionsByCategoryAndSubject(category: String, subject: String): Flow<List<Question>> =
        questionDao.getQuestionsByCategoryAndSubject(category, subject)

    fun getOptionsForQuestion(questionId: String): Flow<List<QuestionOption>> =
        questionDao.getOptionsForQuestion(questionId)

    fun getAnswerForQuestion(questionId: String): Flow<Answer?> =
        questionDao.getAnswerForQuestion(questionId)
}

