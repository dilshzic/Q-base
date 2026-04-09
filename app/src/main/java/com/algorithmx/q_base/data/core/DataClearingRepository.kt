package com.algorithmx.q_base.data.core

import com.algorithmx.q_base.data.chat.ChatDao
import com.algorithmx.q_base.data.chat.MessageDao
import com.algorithmx.q_base.data.sessions.SessionDao
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.data.core.UserDao
import com.algorithmx.q_base.data.ai.AiResponseDao
import com.algorithmx.q_base.data.ai.BrainUsageDao
import com.algorithmx.q_base.data.collections.CollectionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataClearingRepository @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val sessionDao: SessionDao,
    private val questionDao: QuestionDao,
    private val userDao: UserDao,
    private val aiResponseDao: AiResponseDao,
    private val brainUsageDao: BrainUsageDao,
    private val collectionDao: CollectionDao,
    private val cryptoManager: com.algorithmx.q_base.data.util.CryptoManager
) {
    suspend fun clearAllData(clearCollections: Boolean) = withContext(Dispatchers.IO) {
        // Always clear chats and sessions
        chatDao.deleteAllChats()
        messageDao.deleteAllMessages()
        sessionDao.deleteAllSessions()
        sessionDao.deleteAllAttempts()
        sessionDao.deleteAllSets()
        userDao.deleteAllUsers()
        aiResponseDao.deleteAllAiResponses()
        brainUsageDao.deleteAllBrainUsage()
        
        // Clear encryption keys
        cryptoManager.clearKeys()

        // Optionally clear collections and questions
        if (clearCollections) {
            collectionDao.deleteAllStudyCollections()
            questionDao.deleteAllQuestions()
            questionDao.deleteAllOptions()
            questionDao.deleteAllAnswers()
            questionDao.deleteAllCrossRefs()
        }
    }
}
