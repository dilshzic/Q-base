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
    val senderId: String, // You will use this to look up the name in UserEntity
    val payload: String, // Text, or the Cloudflare .zip URL
    val type: String, // "TEXT", "FILE_TRANSFER", etc.
    val timestamp: Long,
    val decryptionStatus: String = "SUCCESS", // SUCCESS, FAILED, NOT_ENCRYPTED, DECRYPTION_ERROR
    val status: String = "SENT", // SENT, DELIVERED, READ
    val keyFingerprint: String? = null,
    val wrappedKey: String? = null
)
