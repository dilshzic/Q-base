package com.algorithmx.q_base.data.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val chatName: String?,
    val isGroup: Boolean,
    val participantIds: String,
    val adminIds: String = "",
    val isBlocked: Boolean = false,
    val isReported: Boolean = false,
    val isMuted: Boolean = false,
    val unreadCount: Int = 0,
    val lastUsedKeyFingerprint: String? = null
)

fun ChatEntity.isAdmin(userId: String): Boolean {
    return if (!isGroup) false
    else adminIds.split(",").map { it.trim() }.contains(userId)
}