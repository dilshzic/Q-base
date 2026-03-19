package com.algorithmx.q_base.data.repository

import com.algorithmx.q_base.data.dao.UserDao
import com.algorithmx.q_base.data.entity.UserEntity
import com.algorithmx.q_base.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class ProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao
) {
    /**
     * Creates or updates a user profile in Firestore and caches it locally.
     * Used during first login or sign up.
     */
    suspend fun createOrUpdateProfile(userId: String, email: String, displayName: String): Result<UserProfile> {
        return try {
            val docRef = firestore.collection("users").document(userId)
            val existingDoc = docRef.get().await()
            
            val profile = if (existingDoc.exists()) {
                existingDoc.toObject(UserProfile::class.java)!!.copy(
                    email = email,
                    displayName = displayName
                )
            } else {
                UserProfile(
                    userId = userId,
                    email = email,
                    displayName = displayName,
                    friendCode = generateFriendCode()
                )
            }
            
            // Save to Firestore
            docRef.set(profile).await()
            
            // Cache locally in Room
            userDao.insertUser(UserEntity(
                userId = profile.userId,
                displayName = profile.displayName,
                friendCode = profile.friendCode
            ))
            
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findUserByFriendCode(friendCode: String): Result<UserProfile?> {
        return try {
            val query = firestore.collection("users")
                .whereEqualTo("friendCode", friendCode.uppercase())
                .limit(1)
                .get()
                .await()
            
            val doc = query.documents.firstOrNull()
            if (doc != null) {
                val profile = doc.toObject(UserProfile::class.java)!!
                // Cache the found user locally
                userDao.insertUser(UserEntity(
                    userId = profile.userId,
                    displayName = profile.displayName,
                    friendCode = profile.friendCode
                ))
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncUserProfile(userId: String) {
        try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val profile = doc.toObject(UserProfile::class.java)!!
                userDao.insertUser(UserEntity(
                    userId = profile.userId,
                    displayName = profile.displayName,
                    friendCode = profile.friendCode
                ))
            }
        } catch (e: Exception) {
            // Log error
        }
    }

    private fun generateFriendCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val prefix = "QBS"
        val code = (1..5).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "$prefix-$code"
    }
}
