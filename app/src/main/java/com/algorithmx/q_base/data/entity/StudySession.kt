package com.algorithmx.q_base.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Study_Sessions")
data class StudySession(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "title")
    val title: String = "",
    @ColumnInfo(name = "time_limit_seconds")
    val timeLimitSeconds: Int?,
    @ColumnInfo(name = "score_achieved")
    val scoreAchieved: Float = 0f,
    @ColumnInfo(name = "created_timestamp")
    val createdTimestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "timing_type")
    val timingType: String = "NONE", // "TOTAL", "PER_QUESTION", "NONE"
    @ColumnInfo(name = "is_random")
    val isRandom: Boolean = false,
    @ColumnInfo(name = "collection_id")
    val collectionId: String? = null,
    @ColumnInfo(name = "last_question_index")
    val lastQuestionIndex: Int = 0
)
