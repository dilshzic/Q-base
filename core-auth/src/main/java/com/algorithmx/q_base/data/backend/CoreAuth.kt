package com.algorithmx.q_base.data.backend

import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.Flow

data class CoreUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String? = null
)

interface CoreAuth {
    val currentUser: Flow<CoreUser?>
    val currentUserId: String?

    suspend fun signInWithEmail(email: String, pass: String): Result<CoreUser>
    suspend fun signUpWithEmail(email: String, pass: String, username: String): Result<CoreUser>
    suspend fun signInWithGoogle(activity: ComponentActivity): Result<CoreUser>
    suspend fun signOut(): Result<Unit>
    suspend fun checkCurrentSession(): Result<CoreUser?>
}
