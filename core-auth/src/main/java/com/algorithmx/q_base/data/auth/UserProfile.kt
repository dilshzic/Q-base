package com.algorithmx.q_base.data.auth

import com.google.firebase.firestore.PropertyName
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
    @get:PropertyName("banned")
    @set:PropertyName("banned")
    var isBanned: Boolean = false,
    @get:PropertyName("photoVisible")
    @set:PropertyName("photoVisible")
    var isPhotoVisible: Boolean = true
)
