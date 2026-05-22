package com.algorithmx.q_base.feature.content_import.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "Question_Options",
    primaryKeys = ["question_id", "option_letter"],
    foreignKeys = [
        ForeignKey(
            entity = Question::class,
            parentColumns = ["question_id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["question_id"])]
)
data class QuestionOption(
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "option_letter")
    val optionLetter: String,
    @ColumnInfo(name = "option_text")
    val optionText: String?,
    @ColumnInfo(name = "option_explanation")
    val optionExplanation: String? = null
)