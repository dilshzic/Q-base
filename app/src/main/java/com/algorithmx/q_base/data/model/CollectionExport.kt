package com.algorithmx.q_base.data.model

import com.algorithmx.q_base.data.entity.*
import kotlinx.serialization.Serializable

@Serializable
data class CollectionExport(
    val collection: com.algorithmx.q_base.data.entity.Collection,
    val sets: List<QuestionSet>,
    val questions: List<QuestionExport>,
    val crossRefs: List<SetQuestionCrossRef>
)
