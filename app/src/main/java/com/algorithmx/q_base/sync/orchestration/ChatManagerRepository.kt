package com.algorithmx.q_base.sync.orchestration

import android.util.Log
import com.algorithmx.q_base.core.data.chat.ChatDao
import com.algorithmx.q_base.core.data.chat.ChatEntity
import com.algorithmx.q_base.core.data.chat.isAdmin
import com.algorithmx.q_base.core.data.chat.ChatRemoteRepository
import com.algorithmx.q_base.core.data.chat.MessageDao
import com.algorithmx.q_base.core.data.auth.AuthRepository
import com.algorithmx.q_base.core.data.backend.CoreDatabase
import com.algorithmx.q_base.core.data.backend.CoreQuery
import com.algorithmx.q_base.core.data.backend.CoreQueryOperator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy

@Singleton
class ChatManagerRepository @Inject constructor(
    private val databases: CoreDatabase,
    private val authRepository: AuthRepository,
    private val profileRepository: com.algorithmx.q_base.core.data.auth.ProfileRepository,
    private val chatRemoteRepository: ChatRemoteRepository,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val userDao: com.algorithmx.q_base.core.data.UserDao,
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
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext
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

                        Log.d("ChatManagerRepository", "Synced chat doc keys: ${doc.keys}, values: ${doc.values}")
                        val chat = ChatEntity(
                            chatId = doc["\$id"] as String,
                            chatName = doc["chatName"] as? String,
                            isGroup = isGroupVal,
                            participantIds = participantsList.joinToString(","),
                            adminIds = if (!remoteAdminIds.isNullOrEmpty()) remoteAdminIds else (remoteAdminId?.let { listOf(it) } ?: emptyList())
                        )
                        chatDao.insertChat(chat)
                        Log.d("ChatManagerRepository", "Synced chat from remote: ${chat.chatId} (${chat.chatName}), participants: ${chat.participantIds}")

                        // Fetch and sync profiles for all participants
                        participantsList.forEach { participantId ->
                            Log.d("ChatManagerRepository", "Checking participant: '$participantId' against uid: '$uid'")
                            if (participantId.isNotBlank() && participantId != uid) {
                                Log.d("ChatManagerRepository", "Syncing profile for other participant: $participantId")
                                // Run this without throwing exceptions to ensure it doesn't block message sync
                                repositoryScope.launch {
                                    try {
                                        profileRepository.syncUserProfile(participantId)
                                    } catch (e: Exception) {
                                        Log.e("ChatManagerRepository", "Failed to sync profile for $participantId", e)
                                    }
                                }
                            }
                        }

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
            // Try to fetch the real metadata first
            fetchAndSyncChatMetadata(chatId)
            val updatedChat = chatDao.getChatById(chatId)
            if (updatedChat == null) {
                // Insert a minimal placeholder with null chatName so a later real sync
                // can overwrite it cleanly without a hardcoded name poisoning the cache
                val userId = currentUserId ?: ""
                val peerId = senderId ?: ""
                val dummyChat = ChatEntity(
                    chatId = chatId,
                    chatName = null,  // never hardcode a name here
                    isGroup = false,
                    participantIds = if (peerId.isNotEmpty()) "$userId,$peerId" else userId,
                    adminIds = emptyList()
                )
                chatDao.insertChat(dummyChat)
            }
        } else if (localChat.chatName.isNullOrBlank()) {
            // Local chat exists but has no name yet — refresh metadata from remote
            fetchAndSyncChatMetadata(chatId)
        }

        // Fetch/sync the profile for the other participant if missing
        val finalChat = chatDao.getChatById(chatId)
        val uid = currentUserId
        val otherParticipantId = finalChat?.participantIds?.split(",")
            ?.firstOrNull { it != uid && it.isNotBlank() }
        if (otherParticipantId != null) {
            val user = userDao.getUserById(otherParticipantId)
            if (user == null) {
                repositoryScope.launch {
                    try {
                        profileRepository.syncUserProfile(otherParticipantId)
                    } catch (e: Exception) {
                        Log.e("ChatManagerRepository", "Failed to sync profile for missing participant $otherParticipantId", e)
                    }
                }
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
            val remoteName = doc["chatName"] as? String

            val localChat = chatDao.getChatById(chatId)
            val chat = if (localChat != null) {
                // Always apply remote name if present, preserving all other local fields
                localChat.copy(
                    chatName = remoteName?.takeIf { it.isNotBlank() } ?: localChat.chatName,
                    isGroup = doc["isGroup"] as? Boolean ?: localChat.isGroup,
                    participantIds = participantsList.joinToString(",").ifBlank { localChat.participantIds },
                    adminIds = if (!remoteAdminIds.isNullOrEmpty()) remoteAdminIds
                               else (remoteAdminId?.let { listOf(it) } ?: localChat.adminIds)
                )
            } else {
                ChatEntity(
                    chatId = chatId,
                    chatName = remoteName,
                    isGroup = doc["isGroup"] as? Boolean ?: false,
                    participantIds = participantsList.joinToString(","),
                    adminIds = if (!remoteAdminIds.isNullOrEmpty()) remoteAdminIds
                               else (remoteAdminId?.let { listOf(it) } ?: emptyList())
                )
            }
            chatDao.insertChat(chat)
            Log.d("ChatManagerRepository", "fetchAndSyncChatMetadata: chatId=$chatId name='${chat.chatName}'")

            // Sync user profiles for all other participants
            val uid = currentUserId
            participantsList.forEach { participantId ->
                if (participantId.isNotBlank() && participantId != uid) {
                    repositoryScope.launch {
                        try {
                            profileRepository.syncUserProfile(participantId)
                        } catch (e: Exception) {
                            Log.e("ChatManagerRepository", "Failed to sync profile for participant $participantId during metadata update", e)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatManagerRepository", "Failed to fetch chat metadata for $chatId", e)
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