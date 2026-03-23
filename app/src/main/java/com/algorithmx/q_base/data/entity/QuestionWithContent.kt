package com.algorithmx.q_base.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class QuestionWithContent(
    @Embedded val question: Question,
    @Relation(
        parentColumn = "question_id",
        entityColumn = "question_id"
    )
    val options: List<QuestionOption>,
    @Relation(
        parentColumn = "question_id",
        entityColumn = "question_id"
    )
    val answer: Answer?
)
