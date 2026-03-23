package com.algorithmx.q_base.data.repository

import com.algorithmx.q_base.data.dao.ChatDao
import com.algorithmx.q_base.data.dao.CollectionDao
import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.dao.SessionDao
import com.algorithmx.q_base.data.dao.UserDao
import com.algorithmx.q_base.data.entity.Collection
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.StudySession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val questionDao: QuestionDao,
    private val collectionDao: CollectionDao,
    private val userDao: UserDao,
    private val chatDao: ChatDao
) {
    fun getCurrentUser(userId: String): Flow<com.algorithmx.q_base.data.entity.UserEntity?> = 
        userDao.getCurrentUser(userId)

    fun getOngoingSessions(): Flow<List<StudySession>> = sessionDao.getOngoingSessions()
    
    fun getPinnedQuestions(): Flow<List<Question>> = sessionDao.getPinnedQuestions()
    
    fun getRecentSessions(): Flow<List<StudySession>> = sessionDao.getAllSessions()

    fun getAllCollections(): Flow<List<Collection>> = collectionDao.getAllCollections()

    fun getAllCollectionsWithCount(): Flow<List<com.algorithmx.q_base.data.entity.CollectionWithCount>> =
        collectionDao.getAllCollectionsWithCount()

    fun getTotalUnreadCount(): Flow<Int> = chatDao.getTotalUnreadCount().map { it ?: 0 }
}
