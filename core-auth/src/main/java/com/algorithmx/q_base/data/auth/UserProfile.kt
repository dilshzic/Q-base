package com.algorithmx.q_base.core.data.auth

data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val profilePictureUrl: String? = null,
    val friendCode: String = "",
    val intro: String = "",
    val publicKey: String? = null,
    var isBanned: Boolean = false,
    var isPhotoVisible: Boolean = true
)
