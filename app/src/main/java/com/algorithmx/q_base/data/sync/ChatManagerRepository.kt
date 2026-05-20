package com.algorithmx.q_base.data.sync

import android.util.Log
import com.algorithmx.q_base.data.chat.ChatDao
import com.algorithmx.q_base.data.chat.ChatEntity
import com.algorithmx.q_base.data.chat.isAdmin
import com.algorithmx.q_base.data.chat.ChatRemoteRepository
import com.algorithmx.q_base.data.chat.MessageDao
import com.algorithmx.q_base.data.auth.AuthRepository
import com.algorithmx.q_base.data.backend.CoreDatabase
import com.algorithmx.q_base.data.backend.CoreQuery
import com.algorithmx.q_base.data.backend.CoreQueryOperator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy

@Singleton
class ChatManagerRepository @Inject constructor(
    private val databases: CoreDatabase,
    private val authRepository: AuthRepository,
    private val chatRemoteRepository: ChatRemoteRepository,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val messageSyncRepository: Lazy<MessageSyncRepository>
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val currentUserId: String?
        get() = authRepository.currentUserId

    suspend fun addParticipantToRemote(chatId: String, userId: String) {
        chatRemoteRepository.addParticipantToRemote(chatId, userId)
    }

    suspend fun removeParticipantFromRemote(chatId: String, userId: String) {
        chatRemoteRepository.removeParticipantFromRemote(chatId, userId)
    }

    suspend fun promoteParticipantToAdminOnRemote(chatId: String, userId: String) {
        chatRemoteRepository.promoteParticipantToAdminOnRemote(chatId, userId)
    }

    suspend fun demoteAdminOnRemote(chatId: String, userId: String) {
        chatRemoteRepository.demoteAdminOnRemote(chatId, userId)
    }

    suspend fun createChatOnRemote(chat: ChatEntity) {
        chatRemoteRepository.createChatOnRemote(chat)
    }

    suspend fun getChatById(chatId: String): ChatEntity? {
        return chatDao.getChatById(chatId)
    }

    suspend fun syncUserChatsFromRemote() {
        val uid = currentUserId ?: return
        var success = false
        var attempts = 0
        while (!success && attempts < 3) {
            try {
                Log.d("ChatManagerRepository", "Syncing user chats from remote (attempt ${attempts + 1}) for uid: $uid")
                val queries = listOf(
                    CoreQuery("participantIds", CoreQueryOperator.ARRAY_CONTAINS, uid)
                )
                val docs = databases.queryDocuments(
                    collectionId = "chats",
                    queries = queries
                ).getOrThrow()

                for (doc in docs) {
                    @Suppress("UNCHECKED_CAST")
                    val participantsList = doc["participantIds"] as? List<String> ?: emptyList()
                    val remoteAdminIds = doc["adminIds"] as? List<String>
                    val remoteAdminId = doc["adminId"] as? String
                    val isGroupVal = doc["isGroup"] as? Boolean ?: false
                    
                    val chat = ChatEntity(
                        chatId = doc["\$id"] as String,
                        chatName = doc["chatName"] as? String,
                        isGroup = isGroupVal,
                        participantIds = participantsList.joinToString(","),
                        adminIds = if (!remoteAdminIds.isNullOrEmpty()) remoteAdminIds else (remoteAdminId?.let { listOf(it) } ?: emptyList())
                    )
                    chatDao.insertChat(chat)
                    Log.d("ChatManagerRepository", "Synced chat from remote: ${chat.chatId} (${chat.chatName})")
                    
                    // Fetch and sync messages for this chat
                    messageSyncRepository.get().fetchAndSyncMessages(chat.chatId)
                }
                success = true
            } catch (e: Exception) {
                attempts++
                Log.e("ChatManagerRepository", "Failed to sync user chats from remote (attempt $attempts)", e)
                if (attempts < 3) {
                    kotlinx.coroutines.delay(2000)
                } else {
                    throw e
                }
            }
        }
    }

    suspend fun findExistingP2PChat(uid: String, userId: String): ChatEntity? {
        try {
            val localChats = chatDao.getAllChats().first()
            val existing = localChats.find { chat ->
                if (chat.isGroup) return@find false
                val list = chat.participantIds.split(",").map { it.trim() }
                list.contains(uid) && list.contains(userId)
            }
            if (existing != null) return existing
        } catch (e: Exception) {
            Log.e("ChatManagerRepository", "Error fetching local chats", e)
        }

        try {
            val queries = listOf(
                CoreQuery("isGroup", CoreQueryOperator.EQUAL, false)
            )
            val docs = databases.queryDocuments(
                collectionId = "chats",
                queries = queries
            ).getOrThrow()

            for (doc in docs) {
                @Suppress("UNCHECKED_CAST")
                val participantsList = doc["participantIds"] as? List<String> ?: emptyList()
                val trimmedParticipants = participantsList.map { it.trim() }
                if (trimmedParticipants.contains(uid) && trimmedParticipants.contains(userId)) {
                    val remoteAdminIds = doc["adminIds"] as? List<String>
                    val remoteAdminId = doc["adminId"] as? String
                    val chat = ChatEntity(
                        chatId = doc["\$id"] as String,
                        chatName = doc["chatName"] as? String,
                        isGroup = false,
                        participantIds = participantsList.joinToString(","),
                        adminIds = if (!remoteAdminIds.isNullOrEmpty()) remoteAdminIds else (remoteAdminId?.let { listOf(it) } ?: emptyList())
                    )
                    chatDao.insertChat(chat)
                    return chat
                }
            }
        } catch (e: Exception) {
            Log.e("ChatManagerRepository", "Error querying Appwrite for existing P2P chat", e)
        }
        return null
    }

    suspend fun ensureChatExistsLocally(chatId: String, senderId: String? = null) {
        val localChat = chatDao.getChatById(chatId)
        if (localChat == null) {
            fetchAndSyncChatMetadata(chatId)
            val updatedChat = chatDao.getChatById(chatId)
            if (updatedChat == null) {
                val userId = currentUserId ?: ""
                val peerId = senderId ?: ""
                val dummyChat = ChatEntity(
                    chatId = chatId,
                    chatName = "Secure Chat",
                    isGroup = false,
                    participantIds = if (peerId.isNotEmpty()) "$userId,$peerId" else userId,
                    adminIds = emptyList()
                )
                chatDao.insertChat(dummyChat)
            }
        }
    }

    suspend fun fetchAndSyncChatMetadata(chatId: String) {
        try {
            val doc = databases.getDocument(
                collectionId = "chats",
                documentId = chatId
            ).getOrThrow() ?: throw IllegalStateException("Chat not found")
            @Suppress("UNCHECKED_CAST")
            val participantsList = doc["participantIds"] as? List<String> ?: emptyList()
            val remoteAdminIds = doc["adminIds"] as? List<String>
            val remoteAdminId = doc["adminId"] as? String
            val chat = ChatEntity(
                chatId = chatId,
                chatName = doc["chatName"] as? String,
                isGroup = doc["isGroup"] as? Boolean ?: false,
                participantIds = participantsList.joinToString(","),
                adminIds = if (!remoteAdminIds.isNullOrEmpty()) remoteAdminIds else (remoteAdminId?.let { listOf(it) } ?: emptyList())
            )
            chatDao.insertChat(chat)
        } catch (e: Exception) {
            Log.e("ChatManagerRepository", "Failed to fetch chat metadata", e)
        }
    }

    suspend fun deleteChatOnRemote(chatId: String) {
        chatRemoteRepository.deleteChatOnRemote(chatId)
    }

    fun deleteChatAndMessagesGlobally(chatId: String) {
        repositoryScope.launch {
            try {
                Log.d("ChatManagerRepository", "deleteChatAndMessagesGlobally: Starting for chatId=$chatId")
                val chat = chatDao.getChatById(chatId)
                Log.d("ChatManagerRepository", "deleteChatAndMessagesGlobally: chat=$chat, isGroup=${chat?.isGroup}, adminIds=${chat?.adminIds}, currentUserId=$currentUserId")
                
                if (chat != null && !chat.isAdmin(currentUserId ?: "") && chat.isGroup) {
                    // Non-admin leaving a group: remove from participants first, then local
                    Log.d("ChatManagerRepository", "deleteChatAndMessagesGlobally: Removing participant from group")
                    removeParticipantFromRemote(chatId, currentUserId ?: "")
                    chatDao.deleteChatById(chatId)
                    messageDao.deleteMessagesByChatId(chatId)
                    Log.d("ChatManagerRepository", "deleteChatAndMessagesGlobally: Group leave complete")
                } else {
                    // Delete from Appwrite FIRST (before local), so the network call completes
                    Log.d("ChatManagerRepository", "deleteChatAndMessagesGlobally: Deleting from Appwrite FIRST...")
                    try {
                        chatRemoteRepository.clearChatMessagesOnRemote(chatId)
                        Log.d("ChatManagerRepository", "deleteChatAndMessagesGlobally: Messages cleared from Appwrite")
                    } catch (e: Exception) {
                        Log.e("ChatManagerRepository", "deleteChatAndMessagesGlobally: Failed to clear messages from Appwrite", e)
                    }
                    try {
                        chatRemoteRepository.deleteChatOnRemote(chatId)
                        Log.d("ChatManagerRepository", "deleteChatAndMessagesGlobally: Chat deleted from Appwrite")
                    } catch (e: Exception) {
                        Log.e("ChatManagerRepository", "deleteChatAndMessagesGlobally: Failed to delete chat from Appwrite", e)
                    }
                    // Now delete locally
                    chatDao.deleteChatById(chatId)
                    messageDao.deleteMessagesByChatId(chatId)
                    Log.d("ChatManagerRepository", "deleteChatAndMessagesGlobally: Local deletion complete")
                }
            } catch (e: Exception) {
                Log.e("ChatManagerRepository", "deleteChatAndMessagesGlobally: FATAL ERROR", e)
            }
        }
    }
}
