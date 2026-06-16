package com.algorithmx.q_base.core.data.chat

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

interface ChatLocalDataSource {
    var activeChatId: String?
    fun getTotalUnreadCount(): Flow<Int>
    fun getAllChats(): Flow<List<ChatEntity>>
    fun getChatByIdFlow(chatId: String): Flow<ChatEntity?>
    fun getChatSummaries(): Flow<List<ChatSummary>>
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>
    suspend fun getChatById(chatId: String): ChatEntity?
    suspend fun upsertChat(chat: ChatEntity)
    suspend fun deleteChatById(chatId: String)
    suspend fun incrementUnreadCount(chatId: String)
    suspend fun clearUnreadCount(chatId: String)

    suspend fun getMessageById(messageId: String): MessageEntity?
    suspend fun upsertMessage(message: MessageEntity)
    suspend fun getPendingMessages(): List<MessageEntity>
    suspend fun updateMessageStatus(messageId: String, status: String)
    suspend fun deleteMessageById(messageId: String)
    suspend fun deleteMessagesByChatId(chatId: String)

    suspend fun updateParticipants(chatId: String, participantIds: String)
    suspend fun updateBlockedStatus(chatId: String, isBlocked: Boolean)
    suspend fun updateReportedStatus(chatId: String, isReported: Boolean)
    suspend fun updateMutedStatus(chatId: String, isMuted: Boolean)
    suspend fun updateChatName(chatId: String, newName: String)

    suspend fun clearAllChatsAndMessages()
}

class ChatLocalDataSourceImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) : ChatLocalDataSource {
    override var activeChatId: String? = null

    override fun getTotalUnreadCount(): Flow<Int> =
        chatDao.getTotalUnreadCount().map { it ?: 0 }

    override fun getAllChats(): Flow<List<ChatEntity>> =
        chatDao.getAllChats()

    override fun getChatByIdFlow(chatId: String): Flow<ChatEntity?> =
        chatDao.getChatByIdFlow(chatId)

    override fun getChatSummaries(): Flow<List<ChatSummary>> =
        chatDao.getChatSummaries()

    override fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>> =
        messageDao.getMessagesForChat(chatId)

    override suspend fun updateParticipants(chatId: String, participantIds: String) {
        chatDao.updateParticipants(chatId, participantIds)
    }

    override suspend fun updateBlockedStatus(chatId: String, isBlocked: Boolean) {
        chatDao.updateBlockedStatus(chatId, isBlocked)
    }

    override suspend fun updateReportedStatus(chatId: String, isReported: Boolean) {
        chatDao.updateReportedStatus(chatId, isReported)
    }

    override suspend fun updateMutedStatus(chatId: String, isMuted: Boolean) {
        chatDao.updateMutedStatus(chatId, isMuted)
    }

    override suspend fun updateChatName(chatId: String, newName: String) {
        chatDao.updateChatName(chatId, newName)
    }

    override suspend fun getChatById(chatId: String): ChatEntity? =
        chatDao.getChatById(chatId)

    override suspend fun upsertChat(chat: ChatEntity) {
        chatDao.insertChat(chat)
    }

    override suspend fun deleteChatById(chatId: String) {
        chatDao.deleteChatById(chatId)
    }

    override suspend fun incrementUnreadCount(chatId: String) {
        chatDao.incrementUnreadCount(chatId)
    }

    override suspend fun clearUnreadCount(chatId: String) {
        chatDao.clearUnreadCount(chatId)
    }

    override suspend fun getMessageById(messageId: String): MessageEntity? =
        messageDao.getMessageById(messageId)

    override suspend fun upsertMessage(message: MessageEntity) {
        messageDao.insertMessage(message)
    }

    override suspend fun getPendingMessages(): List<MessageEntity> =
        messageDao.getPendingMessages()

    override suspend fun updateMessageStatus(messageId: String, status: String) {
        messageDao.updateMessageStatus(messageId, status)
    }

    override suspend fun deleteMessagesByChatId(chatId: String) {
        messageDao.deleteMessagesByChatId(chatId)
    }

    override suspend fun deleteMessageById(messageId: String) {
        messageDao.deleteMessageById(messageId)
    }

    override suspend fun clearAllChatsAndMessages() {
        chatDao.deleteAllChats()
        messageDao.deleteAllMessages()
    }
}
