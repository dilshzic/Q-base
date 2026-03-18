package com.algorithmx.q_base.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Session_Attempts",
    foreignKeys = [
        ForeignKey(
            entity = StudySession::class,
            parentColumns = ["session_id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Question::class,
            parentColumns = ["question_id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SessionAttempt(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "attempt_id")
    val attemptId: Int = 0,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "attempt_status")
    val attemptStatus: String, // "UNATTEMPTED", "ATTEMPTED", "FLAGGED", "FINALIZED"
    @ColumnInfo(name = "user_selected_answers")
    val userSelectedAnswers: String,
    @ColumnInfo(name = "time_spent_seconds")
    val timeSpentSeconds: Int = 0
)

