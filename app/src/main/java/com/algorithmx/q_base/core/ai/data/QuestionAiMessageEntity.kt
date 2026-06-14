package com.algorithmx.q_base.core.ai.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "question_ai_messages",
    indices = [Index(value = ["questionId"])]
)
data class QuestionAiMessageEntity(
    @PrimaryKey
    val messageId: String,
    val questionId: String,
    val sender: String, // "USER" or "AI"
    val payload: String,
    val timestamp: Long
)
