package com.algorithmx.q_base.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Question_Options",
    foreignKeys = [
        ForeignKey(
            entity = Question::class,
            parentColumns = ["question_id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QuestionOption(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "option_id")
    val optionId: Int = 0,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "option_letter")
    val optionLetter: String?,
    @ColumnInfo(name = "option_text")
    val optionText: String?
)
