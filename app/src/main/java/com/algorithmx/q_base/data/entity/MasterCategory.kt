package com.algorithmx.q_base.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Master_Categories")
data class MasterCategory(
    @PrimaryKey
    @ColumnInfo(name = "master_category_id")
    val masterCategoryId: String, // UUID

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "is_user_created")
    val isUserCreated: Boolean = false
)

