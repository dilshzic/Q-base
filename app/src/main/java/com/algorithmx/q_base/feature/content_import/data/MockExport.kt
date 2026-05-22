package com.algorithmx.q_base.feature.content_import.data
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