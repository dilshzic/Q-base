package com.algorithmx.q_base.core.data

import com.algorithmx.q_base.core.data.chat.ChatLocalDataSource
import com.algorithmx.q_base.core.data.chat.isAdmin
import com.algorithmx.q_base.core.data.backend.CoreAuth
import com.algorithmx.q_base.core.data.backend.CoreDatabase
import com.algorithmx.q_base.core.data.chat.ChatRemoteRepository
import com.algorithmx.q_base.feature.sessions.data.SessionDao
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.core.ai.data.AiResponseDao
import com.algorithmx.q_base.core.ai.data.BrainUsageDao
import com.algorithmx.q_base.data.collections.CollectionDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
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
    private val cryptoManager: com.algorithmx.q_base.core_crypto.CryptoManager,
    private val coreAuth: CoreAuth,
    private val coreDatabase: CoreDatabase,
    private val chatRemoteRepository: ChatRemoteRepository
) {
    suspend fun clearRemoteData() = withContext(Dispatchers.IO) {
        val userId = coreAuth.currentUserId ?: return@withContext
        
        // 1. Leave or delete chats on remote
        try {
            val chats = chatLocalDataSource.getAllChats().first()
            for (chat in chats) {
                try {
                    val isUserAdmin = chat.isAdmin(userId)
                    if (isUserAdmin) {
                        chatRemoteRepository.deleteChatOnRemote(chat.chatId)
                    } else {
                        chatRemoteRepository.removeParticipantFromRemote(chat.chatId, userId)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("DataClearingRepository", "Failed to clear remote chat ${chat.chatId}", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DataClearingRepository", "Failed to retrieve local chats for remote clearing", e)
        }

        // 2. Delete user private settings / backup document
        try {
            coreDatabase.deleteDocument("user_private_settings", userId).getOrThrow()
        } catch (e: Exception) {
            android.util.Log.e("DataClearingRepository", "Failed to delete user_private_settings on remote", e)
        }

        // 3. Delete user profile document
        try {
            coreDatabase.deleteDocument("users", userId).getOrThrow()
        } catch (e: Exception) {
            android.util.Log.e("DataClearingRepository", "Failed to delete user profile on remote", e)
        }
    }

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