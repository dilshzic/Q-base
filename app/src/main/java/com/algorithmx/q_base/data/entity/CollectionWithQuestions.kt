package com.algorithmx.q_base.data.entity

import androidx.room.Embedded
import androidx.room.Junction
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

data class CollectionWithQuestions(
    @Embedded val collection: QuestionCollection,
    @Relation(
        parentColumn = "collection_id",
        entityColumn = "question_id",
        associateBy = Junction(CollectionQuestionCrossRef::class)
    )
    val questions: List<QuestionWithContent>
)
