package com.algorithmx.q_base.core.data.chat

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters

@Entity(tableName = "chats")
@TypeConverters(ChatTypeConverters::class)
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val chatName: String?,
    val isGroup: Boolean,
    val participantIds: String,
    val adminIds: List<String> = emptyList(),
    val isBlocked: Boolean = false,
    val isReported: Boolean = false,
    val isMuted: Boolean = false,
    val unreadCount: Int = 0,
    val lastUsedKeyFingerprint: String? = null
)

fun ChatEntity.isAdmin(userId: String): Boolean {
    return if (!isGroup) false
    else adminIds.contains(userId)
}