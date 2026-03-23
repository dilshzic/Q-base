package com.algorithmx.q_base.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "Questions",
    indices = [
        Index(value = ["collection"]),
        Index(value = ["category"]),
        Index(value = ["tags"])
    ]
)
data class Question(
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    val questionId: String,
    
    @ColumnInfo(name = "collection")
    val collection: String?,
    
    @ColumnInfo(name = "category")
    val category: String?,
    
    @ColumnInfo(name = "tags")
    val tags: String?,
    
    @ColumnInfo(name = "question_type")
    val questionType: String?,
    
    @ColumnInfo(name = "stem")
    val stem: String?,
    
    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false
)
