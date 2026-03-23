package com.algorithmx.q_base.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String, // Matches Firebase Auth UID
    val displayName: String,
    val email: String? = null,
    val intro: String? = null,
    val profilePictureUrl: String?,
    val friendCode: String, // e.g., "UOK-9A2B"
    val publicKey: String? = null,
    val isBanned: Boolean = false,
    val isPhotoVisible: Boolean = true
)
