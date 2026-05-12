package com.algorithmx.q_base.data.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val profileCache: ProfileCache,
    private val cryptoManager: com.algorithmx.q_base.core_crypto.CryptoManager
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

            val finalProfile = if (profile.profilePictureUrl == null) {
                val firebasePhotoUrl = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
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

            val publicProfile = profile.copy(email = "")
            docRef.set(publicProfile).await()

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null && currentUser.uid == profile.userId && currentUser.displayName != profile.displayName) {
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = profile.displayName
                }
                currentUser.updateProfile(profileUpdates).await()
            }

            profileCache.upsert(profile)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to update profile", e)
            Result.failure(e)
        }
    }

    private suspend fun saveProfile(profile: UserProfile, email: String) {
        val docRef = firestore.collection("users").document(profile.userId)
        try {
            val publicProfile = profile.copy(email = "")
            docRef.set(publicProfile).await()

            docRef.collection("private_settings")
                .document("settings")
                .set(mapOf("email" to email))
                .await()

            profileCache.upsert(profile)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to save profile remotely, caching locally", e)
            profileCache.upsert(profile)
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
                profileCache.upsert(profile)
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncUserProfile(userId: String) {
        Log.d("ProfileRepository", "Starting syncUserProfile for $userId")
        try {
            Log.d("ProfileRepository", "Fetching user doc from Firestore...")
            val doc = firestore.collection("users").document(userId).get().await()
            Log.d("ProfileRepository", "User doc fetched, exists: ${doc.exists()}")
            val profile = if (doc.exists()) {
                val p = doc.toObject(UserProfile::class.java)!!
                Log.d("ProfileRepository", "Fetching private settings...")
                val privateDoc = firestore.collection("users")
                    .document(userId)
                    .collection("private_settings")
                    .document("settings")
                    .get()
                    .await()
                Log.d("ProfileRepository", "Private settings fetched")

                val privateEmail = privateDoc.getString("email") ?: ""
                p.copy(email = privateEmail)
            } else {
                Log.d("ProfileRepository", "User doc does not exist, creating new profile from FirebaseAuth...")
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null && currentUser.uid == userId) {
                    val email = currentUser.email ?: ""
                    UserProfile(
                        userId = userId,
                        email = email,
                        displayName = currentUser.displayName ?: "Learner",
                        profilePictureUrl = currentUser.photoUrl?.toString(),
                        friendCode = generateUniqueFriendCode(),
                        publicKey = cryptoManager.initializeAndGetPublicKey(),
                        isPhotoVisible = true
                    ).also { newProfile ->
                        Log.d("ProfileRepository", "Saving new profile for $userId")
                        saveProfile(newProfile, email)
                    }
                } else {
                    Log.w("ProfileRepository", "CurrentUser is null or UID mismatch during profile creation")
                    null
                }
            }

            profile?.let { initialProfile ->
                var p = initialProfile
                Log.d("ProfileRepository", "Syncing profile for ${p.userId}: ${p.displayName}, code: ${p.friendCode}")

                if (p.friendCode.isBlank()) {
                    Log.d("ProfileRepository", "Generating new friend code for ${p.userId}")
                    p = p.copy(friendCode = generateUniqueFriendCode()).also { updated ->
                        firestore.collection("users").document(userId).set(updated).await()
                    }
                }

                val myUid = FirebaseAuth.getInstance().currentUser?.uid
                if (p.userId == myUid) {
                    val localPk = cryptoManager.initializeAndGetPublicKey()
                    if (p.publicKey != localPk) {
                        Log.d("ProfileRepository", "Device public key mismatch! Updating Firestore with local key.")
                        p = p.copy(publicKey = localPk)
                        firestore.collection("users").document(p.userId).set(p).await()
                    }
                }

                profileCache.upsert(p)
                Log.d("ProfileRepository", "Successfully cached profile locally")
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
        return "$prefix-$part1-$part2"
    }

    suspend fun setupSecureBackup(userId: String, passphrase: String): Result<Unit> {
        return try {
            val encryptedBackup = cryptoManager.exportEncryptedKeyset(passphrase)
            val docRef = firestore.collection("users").document(userId)
                .collection("private_settings").document("e2ee_backup")
            docRef.set(mapOf("backup" to encryptedBackup)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to setup secure backup", e)
            Result.failure(e)
        }
    }

    suspend fun checkHasSecureBackup(userId: String): Boolean {
        return try {
            val doc = firestore.collection("users").document(userId)
                .collection("private_settings").document("e2ee_backup")
                .get().await()
            doc.exists() && doc.getString("backup") != null
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to check secure backup", e)
            false
        }
    }

    suspend fun restoreSecureBackup(userId: String, passphrase: String): Result<Unit> {
        return try {
            val doc = firestore.collection("users").document(userId)
                .collection("private_settings").document("e2ee_backup")
                .get().await()
            
            val encryptedBackup = doc.getString("backup") ?: throw IllegalStateException("Backup not found")
            
            val importResult = cryptoManager.importEncryptedKeyset(encryptedBackup, passphrase)
            if (importResult.isSuccess) {
                // Re-sync the profile to ensure the restored public key is published
                syncUserProfile(userId)
            }
            importResult
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to restore secure backup", e)
            Result.failure(e)
        }
    }
}
