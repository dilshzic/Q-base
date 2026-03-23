package com.algorithmx.q_base.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val profilePictureUrl: String? = null,
    val friendCode: String = "",
    val intro: String = "",
    val publicKey: String? = null,
    val isBanned: Boolean = false,
    val isPhotoVisible: Boolean = true
)

data class SyncRequest(
    val requestId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val targetCollectionId: String = "",
    val status: String = "PENDING", // PENDING, UPLOADED, DOWNLOADED, FAILED
    val downloadUrl: String? = null,
    val fileId: String? = null, // Appwrite fileId for cleanup
    @ServerTimestamp val timestamp: Date? = null
)
