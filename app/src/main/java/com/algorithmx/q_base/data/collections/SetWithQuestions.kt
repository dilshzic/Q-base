package com.algorithmx.q_base.data.collections

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class SetWithQuestions(
    @Embedded val set: QuestionSet,
    @Relation(
        parentColumn = "set_id",
        entityColumn = "question_id",
        associateBy = Junction(SetQuestionCrossRef::class)
    )
    val questions: List<Question>
)
