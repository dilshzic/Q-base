package com.algorithmx.q_base.data.core

import com.algorithmx.q_base.data.chat.ChatDao
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.data.sessions.SessionDao
import com.algorithmx.q_base.data.core.UserDao
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.collections.Question
import com.algorithmx.q_base.data.sessions.StudySession
import com.algorithmx.q_base.data.collections.StudyCollectionWithCount
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
    fun getCurrentUser(userId: String): Flow<com.algorithmx.q_base.data.core.UserEntity?> = 
        userDao.getCurrentUser(userId)

    fun getOngoingSessions(): Flow<List<StudySession>> = sessionDao.getOngoingSessions()
    
    fun getPinnedQuestions(): Flow<List<Question>> = sessionDao.getPinnedQuestions()
    
    fun getRecentSessions(): Flow<List<StudySession>> = sessionDao.getAllSessions()

    fun getAllStudyCollections(): Flow<List<StudyCollection>> = collectionDao.getAllStudyCollections()
    
    fun getAllStudyCollectionsWithCount(): Flow<List<StudyCollectionWithCount>> =
        collectionDao.getAllStudyCollectionsWithCount()

    fun getTotalUnreadCount(): Flow<Int> = chatDao.getTotalUnreadCount().map { it ?: 0 }
}
