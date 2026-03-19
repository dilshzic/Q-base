package com.algorithmx.q_base.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Problem_Reports",
    foreignKeys = [
        ForeignKey(
            entity = Question::class,
            parentColumns = ["question_id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProblemReport(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "report_id")
    val reportId: Long = 0,

    @ColumnInfo(name = "question_id")
    val questionId: String,

    @ColumnInfo(name = "explanation")
    val explanation: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis()
)
