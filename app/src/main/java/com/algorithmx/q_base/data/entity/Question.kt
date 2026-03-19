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
        Index(value = ["master_category"]),
        Index(value = ["category"]),
        Index(value = ["subject"])
    ]
)
data class Question(
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    val questionId: String,
    
    @ColumnInfo(name = "master_category")
    val masterCategory: String?,
    
    @ColumnInfo(name = "category")
    val category: String?,
    
    @ColumnInfo(name = "question_type")
    val questionType: String?,
    
    @ColumnInfo(name = "stem")
    val stem: String?,
    
    @ColumnInfo(name = "subject")
    val subject: String?,
    
    @ColumnInfo(name = "batch")
    val batch: String?,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false
)
