package com.algorithmx.q_base.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Question_Collections")
data class QuestionCollection(
    @PrimaryKey
    @ColumnInfo(name = "category")
    val category: String,
    
    @ColumnInfo(name = "master_category")
    val masterCategory: String,
    
    @ColumnInfo(name = "is_user_created")
    val isUserCreated: Boolean = false
)
