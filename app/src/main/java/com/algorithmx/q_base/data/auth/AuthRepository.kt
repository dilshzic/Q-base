package com.algorithmx.q_base.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    val currentUser: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val task = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            val user = task.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("User is null after successful sign in"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, username: String, photoUrl: String? = null): Result<FirebaseUser> {
        return try {
            val task = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val user = task.user
            if (user != null) {
                // 1. Update Profile DisplayName and PhotoUrl
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = username
                    photoUrl?.let { this.photoUri = android.net.Uri.parse(it) }
                }
                user.updateProfile(profileUpdates).await()
                Result.success(user)
            } else {
                Result.failure(Exception("User is null after successful sign up"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
