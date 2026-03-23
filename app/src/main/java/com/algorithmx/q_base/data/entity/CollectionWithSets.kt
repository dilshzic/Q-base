package com.algorithmx.q_base.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class CollectionWithSets(
    @Embedded val collection: Collection,
    @Relation(
        parentColumn = "collection_id",
        entityColumn = "parent_collection_id"
    )
    val sets: List<QuestionSet>
)
