package com.algorithmx.q_base.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Many-to-many junction table linking questions to collections.
 * One question can belong to multiple collections (e.g., seeded collection + user custom list).
 * One collection holds many questions.
 */
@Entity(
    tableName = "Collection_Questions_CrossRef",
    foreignKeys = [
        ForeignKey(
            entity = QuestionCollection::class,
            parentColumns = ["collection_id"],
            childColumns = ["collection_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Question::class,
            parentColumns = ["question_id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class CollectionQuestionCrossRef(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "mapping_id")
    val mappingId: Int = 0,

    @ColumnInfo(name = "collection_id", index = true)
    val collectionId: String,

    @ColumnInfo(name = "question_id", index = true)
    val questionId: String
)
