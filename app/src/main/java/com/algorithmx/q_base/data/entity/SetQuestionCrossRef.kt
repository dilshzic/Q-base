package com.algorithmx.q_base.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "Set_Questions_CrossRef",
    foreignKeys = [
        ForeignKey(
            entity = QuestionSet::class,
            parentColumns = ["set_id"],
            childColumns = ["set_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Question::class,
            parentColumns = ["question_id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["set_id"]),
        Index(value = ["question_id"])
    ]
)
data class SetQuestionCrossRef(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "mapping_id")
    val mappingId: Long = 0,

    @ColumnInfo(name = "set_id")
    val setId: String,

    @ColumnInfo(name = "question_id")
    val questionId: String
)
