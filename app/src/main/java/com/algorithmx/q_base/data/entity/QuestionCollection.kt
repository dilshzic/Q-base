package com.algorithmx.q_base.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "Question_Collections",
    foreignKeys = [
        ForeignKey(
            entity = MasterCategory::class,
            parentColumns = ["master_category_id"],
            childColumns = ["master_category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class QuestionCollection(
    @PrimaryKey
    @ColumnInfo(name = "collection_id")
    val collectionId: String, // UUID

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "master_category_id")
    val masterCategoryId: String, // FK to Master_Categories

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "created_timestamp")
    val createdTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_user_created")
    val isUserCreated: Boolean = false
)
