package com.algorithmx.q_base.data.model

import com.algorithmx.q_base.data.entity.*
import kotlinx.serialization.Serializable

@Serializable
data class QuestionExport(
    val question: Question,
    val options: List<QuestionOption>,
    val answer: Answer?
)

@Serializable
data class MockExport(
    val collection: QuestionCollection,
    val questions: List<QuestionExport>
)
