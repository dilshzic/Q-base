package com.algorithmx.q_base.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "Collections")
data class Collection(
    @PrimaryKey
    @ColumnInfo(name = "collection_id")
    val collectionId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String? = null,
    
    @ColumnInfo(name = "is_user_created")
    val isUserCreated: Boolean = false,

    @ColumnInfo(name = "is_shared")
    val isShared: Boolean = false,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = 0
)

data class CollectionWithCount(
    @androidx.room.Embedded val collection: Collection,
    val questionCount: Int
)
