package com.algorithmx.q_base.data.collections
import kotlinx.serialization.Serializable

@Serializable
data class QuestionExport(
    val question: Question,
    val options: List<QuestionOption>,
    val answer: Answer?
)

@Serializable
data class MockExport(
    val collection: QuestionSet,
    val questions: List<QuestionExport>
)
