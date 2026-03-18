package com.algorithmx.q_base.data.repository

import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.dao.SessionDao
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionCollection
import com.algorithmx.q_base.data.entity.StudySession
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val questionDao: QuestionDao
) {
    fun getOngoingSessions(): Flow<List<StudySession>> = sessionDao.getOngoingSessions()
    
    fun getPinnedQuestions(): Flow<List<Question>> = sessionDao.getPinnedQuestions()
    
    fun getRecentCollections(): Flow<List<QuestionCollection>> = sessionDao.getRecentCollections()
}
