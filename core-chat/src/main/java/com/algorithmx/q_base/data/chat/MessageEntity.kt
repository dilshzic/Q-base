package com.algorithmx.q_base.data.chat

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["chatId"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chatId")]
)
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val chatId: String,
    val senderId: String,
    val payload: String,
    val type: String,
    val timestamp: Long,
    val decryptionStatus: String = "SUCCESS",
    val status: String = "SENT",
    val keyFingerprint: String? = null,
    val wrappedKey: String? = null
)