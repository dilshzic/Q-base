package com.algorithmx.q_base.data.collections

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "Question_Sets",
    foreignKeys = [
        ForeignKey(
            entity = StudyCollection::class,
            parentColumns = ["collection_id"],
            childColumns = ["parent_collection_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["parent_collection_id"])]
)
data class QuestionSet(
    @PrimaryKey
    @ColumnInfo(name = "set_id")
    val setId: String, // UUID
    @ColumnInfo(name = "title")
    val title: String,
    @ColumnInfo(name = "parent_collection_id")
    val parentCollectionId: String, // FK to Collections
    @ColumnInfo(name = "description")
    val description: String? = null,
    @ColumnInfo(name = "created_timestamp")
    val createdTimestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_user_created")
    val isUserCreated: Boolean = false
)
