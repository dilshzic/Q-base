package com.algorithmx.q_base.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "Answers")
data class Answer(
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "correct_answer_string")
    val correctAnswerString: String?,
    @ColumnInfo(name = "general_explanation")
    val generalExplanation: String?,
    @ColumnInfo(name = "references")
    val references: String? = null
)
