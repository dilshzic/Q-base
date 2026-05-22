package com.algorithmx.q_base.feature.content_import.data

import androidx.room.Embedded
import androidx.room.Relation

data class StudyCollectionWithSets(
    @Embedded val collection: StudyCollection,
    @Relation(
        parentColumn = "collection_id",
        entityColumn = "parent_collection_id"
    )
    val sets: List<QuestionSet>
)