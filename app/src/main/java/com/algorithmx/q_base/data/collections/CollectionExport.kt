package com.algorithmx.q_base.data.collections
import kotlinx.serialization.Serializable

@Serializable
data class CollectionExport(
    val collection: com.algorithmx.q_base.data.collections.StudyCollection,
    val sets: List<QuestionSet>,
    val questions: List<QuestionExport>,
    val crossRefs: List<SetQuestionCrossRef>
)
