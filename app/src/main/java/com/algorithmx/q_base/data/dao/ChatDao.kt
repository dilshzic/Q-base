package com.algorithmx.q_base.data.dao

import androidx.room.*
import com.algorithmx.q_base.data.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("SELECT * FROM chats WHERE chatId = :chatId")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Query("SELECT * FROM chats WHERE chatId = :chatId")
    fun getChatByIdFlow(chatId: String): Flow<ChatEntity?>

    @Query("SELECT * FROM chats WHERE isGroup = 0 AND ((participantIds = :ids1) OR (participantIds = :ids2))")
    suspend fun getP2PChat(ids1: String, ids2: String): ChatEntity?

    @Query("SELECT * FROM chats")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("UPDATE chats SET isBlocked = :isBlocked WHERE chatId = :chatId")
    suspend fun updateBlockedStatus(chatId: String, isBlocked: Boolean)

    @Query("UPDATE chats SET isReported = :isReported WHERE chatId = :chatId")
    suspend fun updateReportedStatus(chatId: String, isReported: Boolean)

    @Query("UPDATE chats SET isMuted = :isMuted WHERE chatId = :chatId")
    suspend fun updateMutedStatus(chatId: String, isMuted: Boolean)

    @Query("UPDATE chats SET participantIds = :participantIds WHERE chatId = :chatId")
    suspend fun updateParticipants(chatId: String, participantIds: String)

    @Delete
    suspend fun deleteChat(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE chatId = :chatId")
    suspend fun deleteChatById(chatId: String)

    @Query("UPDATE chats SET unreadCount = unreadCount + 1 WHERE chatId = :chatId")
    suspend fun incrementUnreadCount(chatId: String)

    @Query("UPDATE chats SET unreadCount = 0 WHERE chatId = :chatId")
    suspend fun clearUnreadCount(chatId: String)

    @Query("SELECT SUM(unreadCount) FROM chats")
    fun getTotalUnreadCount(): Flow<Int?>

    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()
}
