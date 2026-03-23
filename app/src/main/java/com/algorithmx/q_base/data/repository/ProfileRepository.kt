package com.algorithmx.q_base.data.repository

import com.algorithmx.q_base.data.dao.UserDao
import com.algorithmx.q_base.data.entity.UserEntity
import com.algorithmx.q_base.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log
import kotlinx.coroutines.tasks.await
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao,
    private val cryptoManager: com.algorithmx.q_base.data.util.CryptoManager
) {
    suspend fun createOrUpdateProfile(
        userId: String, 
        email: String, 
        displayName: String,
        profilePictureUrl: String? = null,
        isPhotoVisible: Boolean = true
    ): Result<UserProfile> {
        return try {
            val docRef = firestore.collection("users").document(userId)
            val existingDoc = docRef.get().await()
            
            val profile = if (existingDoc.exists()) {
                val currentProfile = existingDoc.toObject(UserProfile::class.java)!!
                currentProfile.copy(
                    email = email,
                    displayName = displayName,
                    profilePictureUrl = profilePictureUrl ?: currentProfile.profilePictureUrl,
                    isPhotoVisible = isPhotoVisible,
                    friendCode = if (currentProfile.friendCode.isNotBlank()) currentProfile.friendCode else generateUniqueFriendCode()
                )
            } else {
                UserProfile(
                    userId = userId,
                    email = email,
                    displayName = displayName,
                    profilePictureUrl = profilePictureUrl,
                    friendCode = generateUniqueFriendCode(),
                    publicKey = cryptoManager.initializeAndGetPublicKey(),
                    isPhotoVisible = isPhotoVisible
                )
            }
            
            // If Firebase has a photoUrl and we don't have one yet, use it
            val finalProfile = if (profile.profilePictureUrl == null) {
                val firebasePhotoUrl = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
                profile.copy(profilePictureUrl = firebasePhotoUrl)
            } else {
                profile
            }

            saveProfile(finalProfile, email)
            Result.success(finalProfile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(profile: UserProfile): Result<Unit> {
        return try {
            val docRef = firestore.collection("users").document(profile.userId)
            
            // 1. Update Firestore Profile
            val publicProfile = profile.copy(email = "") // Shields email
            docRef.set(publicProfile).await()

            // 2. Also update Display Name in Firebase Auth if it changed
            val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
            if (currentUser != null && currentUser.uid == profile.userId && currentUser.displayName != profile.displayName) {
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = profile.displayName
                }
                currentUser.updateProfile(profileUpdates).await()
            }

            // 3. Cache locally in Room
            userDao.insertUser(UserEntity(
                userId = profile.userId,
                displayName = profile.displayName,
                email = profile.email,
                intro = profile.intro,
                profilePictureUrl = profile.profilePictureUrl,
                friendCode = profile.friendCode,
                publicKey = profile.publicKey,
                isBanned = profile.isBanned,
                isPhotoVisible = profile.isPhotoVisible
            ))

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to update profile", e)
            Result.failure(e)
        }
    }

    private suspend fun saveProfile(profile: UserProfile, email: String) {
        val docRef = firestore.collection("users").document(profile.userId)
        try {
            // Save public profile to Firestore
            val publicProfile = profile.copy(email = "") 
            docRef.set(publicProfile).await()

            // Save private email to a subcollection
            docRef.collection("private_settings")
                .document("settings")
                .set(mapOf("email" to email))
                .await()

            // Cache locally in Room
            userDao.insertUser(UserEntity(
                userId = profile.userId,
                displayName = profile.displayName,
                email = profile.email,
                intro = profile.intro,
                profilePictureUrl = profile.profilePictureUrl,
                friendCode = profile.friendCode,
                publicKey = profile.publicKey,
                isBanned = profile.isBanned,
                isPhotoVisible = profile.isPhotoVisible
            ))
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to save profile remotely, caching locally", e)
            userDao.insertUser(UserEntity(
                userId = profile.userId,
                displayName = profile.displayName,
                email = profile.email,
                intro = profile.intro,
                profilePictureUrl = profile.profilePictureUrl,
                friendCode = profile.friendCode,
                publicKey = profile.publicKey,
                isBanned = profile.isBanned,
                isPhotoVisible = profile.isPhotoVisible
            ))
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
                    email = profile.email,
                    intro = profile.intro,
                    profilePictureUrl = profile.profilePictureUrl,
                    friendCode = profile.friendCode,
                    publicKey = profile.publicKey,
                    isBanned = profile.isBanned,
                    isPhotoVisible = profile.isPhotoVisible
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
            val profile = if (doc.exists()) {
                val p = doc.toObject(UserProfile::class.java)!!
                // Fetch private email
                val privateDoc = firestore.collection("users")
                    .document(userId)
                    .collection("private_settings")
                    .document("settings")
                    .get()
                    .await()
                
                val privateEmail = privateDoc.getString("email") ?: ""
                p.copy(email = privateEmail)
            } else {
                // If not in Firestore, create from Firebase Auth if possible
                val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (currentUser != null && currentUser.uid == userId) {
                    val email = currentUser.email ?: ""
                    UserProfile(
                        userId = userId,
                        email = email,
                        displayName = currentUser.displayName ?: "Medical Student",
                        profilePictureUrl = currentUser.photoUrl?.toString(),
                        friendCode = generateUniqueFriendCode(),
                        publicKey = cryptoManager.initializeAndGetPublicKey(),
                        isPhotoVisible = true
                    ).also { newProfile ->
                        saveProfile(newProfile, email)
                    }
                } else {
                    null
                }
            }

            profile?.let { initialProfile ->
                var p = initialProfile
                Log.d("ProfileRepository", "Syncing profile for ${p.userId}: ${p.displayName}, code: ${p.friendCode}")
                
                // If synced profile has no friend code, generate and save it
                if (p.friendCode.isBlank()) {
                    Log.d("ProfileRepository", "Generating new friend code for ${p.userId}")
                    p = p.copy(friendCode = generateUniqueFriendCode()).also { updated ->
                        firestore.collection("users").document(userId).set(updated).await()
                    }
                }

                // MULTI-DEVICE SUPPORT: If this is our own profile, ensure Firestore has OUR device's public key
                val myUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (p.userId == myUid) {
                    val localPk = cryptoManager.initializeAndGetPublicKey()
                    if (p.publicKey != localPk) {
                        Log.d("ProfileRepository", "Device public key mismatch! Updating Firestore with local key.")
                        p = p.copy(publicKey = localPk)
                        // Update Firestore immediately so other devices see the new key
                        firestore.collection("users").document(p.userId).set(p).await()
                    }
                }

                userDao.insertUser(UserEntity(
                    userId = p.userId,
                    displayName = p.displayName,
                    email = p.email,
                    intro = p.intro,
                    profilePictureUrl = p.profilePictureUrl,
                    friendCode = p.friendCode,
                    publicKey = p.publicKey,
                    isBanned = p.isBanned,
                    isPhotoVisible = p.isPhotoVisible
                ))
                Log.d("ProfileRepository", "Successfully cached profile in local DB")
            } ?: Log.e("ProfileRepository", "Failed to get profile for $userId")
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error syncing user profile", e)
        }
    }

    private suspend fun generateUniqueFriendCode(): String {
        var code: String
        var isUnique = false
        var attempts = 0
        val maxAttempts = 5
        
        do {
            code = generateFriendCode().uppercase()
            isUnique = isFriendCodeUnique(code)
            attempts++
            if (!isUnique && attempts >= maxAttempts) {
                Log.w("ProfileRepository", "Failed to find unique friend code after $maxAttempts attempts, using timestamp suffix")
                code += "-${System.currentTimeMillis().toString().takeLast(4)}"
                isUnique = true
            }
        } while (!isUnique)
        return code
    }

    private suspend fun isFriendCodeUnique(friendCode: String): Boolean {
        return try {
            val query = firestore.collection("users")
                .whereEqualTo("friendCode", friendCode.uppercase())
                .get()
                .await()
            query.isEmpty
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error checking friend code uniqueness for $friendCode", e)
            false 
        }
    }

    private fun generateFriendCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val prefix = "QBS"
        val part1 = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        val part2 = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "$prefix-$part1-$part2".uppercase()
    }
}
