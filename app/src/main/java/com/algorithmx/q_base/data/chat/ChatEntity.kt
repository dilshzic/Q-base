package com.algorithmx.q_base.data.chat

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val chatName: String?, // Null for 1-on-1 chats
    val isGroup: Boolean,
    val participantIds: String, // e.g., "uid1,uid2,uid3"
    val adminId: String? = null,
    val isBlocked: Boolean = false,
    val isReported: Boolean = false,
    val isMuted: Boolean = false,
    val unreadCount: Int = 0,
    val lastUsedKeyFingerprint: String? = null
)
