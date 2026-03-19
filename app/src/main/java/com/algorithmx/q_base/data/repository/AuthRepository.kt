package com.algorithmx.q_base.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    fun signInWithEmail(email: String, pass: String, onResult: (Result<FirebaseUser>) -> Unit) {
        firebaseAuth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        onResult(Result.success(user))
                    } else {
                        onResult(Result.failure(Exception("User is null after successful sign in")))
                    }
                } else {
                    onResult(Result.failure(task.exception ?: Exception("Sign in failed")))
                }
            }
    }

    fun signUpWithEmail(email: String, pass: String, onResult: (Result<FirebaseUser>) -> Unit) {
        firebaseAuth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        onResult(Result.success(user))
                    } else {
                        onResult(Result.failure(Exception("User is null after successful sign up")))
                    }
                } else {
                    onResult(Result.failure(task.exception ?: Exception("Sign up failed")))
                }
            }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}
