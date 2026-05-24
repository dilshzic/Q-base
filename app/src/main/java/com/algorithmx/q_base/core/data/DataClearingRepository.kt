package com.algorithmx.q_base.core.data

import com.algorithmx.q_base.core.data.chat.ChatLocalDataSource
import com.algorithmx.q_base.feature.sessions.data.SessionDao
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.core.data.UserDao
import com.algorithmx.q_base.core.ai.data.AiResponseDao
import com.algorithmx.q_base.core.ai.data.BrainUsageDao
import com.algorithmx.q_base.data.collections.CollectionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataClearingRepository @Inject constructor(
    private val chatLocalDataSource: ChatLocalDataSource,
    private val sessionDao: SessionDao,
    private val questionDao: QuestionDao,
    private val userDao: UserDao,
    private val aiResponseDao: AiResponseDao,
    private val brainUsageDao: BrainUsageDao,
    private val collectionDao: CollectionDao,
    private val cryptoManager: com.algorithmx.q_base.core_crypto.CryptoManager
) {
    suspend fun clearAllData(clearCollections: Boolean) = withContext(Dispatchers.IO) {
        // Always clear chats and sessions
        chatLocalDataSource.clearAllChatsAndMessages()
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