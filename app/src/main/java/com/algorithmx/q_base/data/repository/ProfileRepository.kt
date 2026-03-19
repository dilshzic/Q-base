package com.algorithmx.q_base.data.repository

import com.algorithmx.q_base.data.dao.UserDao
import com.algorithmx.q_base.data.entity.UserEntity
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
    suspend fun createProfile(userId: String, displayName: String): Result<UserEntity> {
        return try {
            val friendCode = generateFriendCode()
            val user = UserEntity(userId, displayName, friendCode)
            
            // Save to Firestore
            firestore.collection("users").document(userId).set(user).await()
            
            // Cache locally
            userDao.insertUser(user)
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findUserByFriendCode(friendCode: String): Result<UserEntity?> {
        return try {
            val query = firestore.collection("users")
                .whereEqualTo("friendCode", friendCode.uppercase())
                .limit(1)
                .get()
                .await()
            
            val doc = query.documents.firstOrNull()
            if (doc != null) {
                val user = UserEntity(
                    userId = doc.id,
                    displayName = doc.getString("displayName") ?: "Unknown",
                    friendCode = doc.getString("friendCode") ?: ""
                )
                // Cache the found user
                userDao.insertUser(user)
                Result.success(user)
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
                val user = UserEntity(
                    userId = doc.id,
                    displayName = doc.getString("displayName") ?: "Unknown",
                    friendCode = doc.getString("friendCode") ?: ""
                )
                userDao.insertUser(user)
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
