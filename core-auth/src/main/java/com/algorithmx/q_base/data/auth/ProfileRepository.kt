package com.algorithmx.q_base.core.data.auth

import android.util.Log
import com.algorithmx.q_base.core.data.backend.CoreAuth
import com.algorithmx.q_base.core.data.backend.CoreDatabase
import com.algorithmx.q_base.core.data.backend.CoreQuery
import com.algorithmx.q_base.core.data.backend.CoreQueryOperator
import io.appwrite.exceptions.AppwriteException
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val coreDatabase: CoreDatabase,
    private val coreAuth: CoreAuth,
    private val profileCache: ProfileCache,
    private val cryptoManager: com.algorithmx.q_base.core_crypto.CryptoManager
) {
    private data class RemoteDoc(
        val rowId: String,
        val data: Map<String, Any>
    )

    private fun logDocumentFailure(
        operation: String,
        collectionId: String,
        documentId: String,
        error: Throwable
    ) {
        val appwriteCode = (error as? AppwriteException)?.code
        Log.e(
            "ProfileRepository",
            "$operation failed: collection=$collectionId documentId=$documentId appwriteCode=${appwriteCode ?: "n/a"} message=${error.message}",
            error
        )
    }

    private fun safeDisplayName(existing: String, userId: String): String {
        return existing.takeIf { it.isNotBlank() } ?: "User ${userId.take(6)}"
    }

    private suspend fun getUserDocWithFallback(userId: String): RemoteDoc? {
        val directResult = try {
            coreDatabase.getDocument("users", userId)
        } catch (e: Exception) {
            logDocumentFailure("getDocument", "users", userId, e)
            Result.failure(e)
        }
        val directDoc = directResult.getOrNull()
        if (directDoc != null && directDoc.isNotEmpty()) {
            return RemoteDoc(rowId = userId, data = directDoc)
        }

        val fallbackResult = try {
            coreDatabase.queryDocuments(
                "users",
                listOf(CoreQuery("userId", CoreQueryOperator.EQUAL, userId))
            )
        } catch (e: Exception) {
            logDocumentFailure("queryDocuments", "users", "userId=$userId", e)
            Result.failure(e)
        }
        val fallbackDoc = fallbackResult.getOrNull()?.firstOrNull { it.isNotEmpty() } ?: return null
        val fallbackRowId = fallbackDoc["\$id"] as? String ?: userId
        return RemoteDoc(rowId = fallbackRowId, data = fallbackDoc)
    }

    private suspend fun getPrivateSettingsDoc(userId: String): Map<String, Any>? {
        val directResult = try {
            coreDatabase.getDocument("user_private_settings", userId)
        } catch (e: Exception) {
            logDocumentFailure("getDocument", "user_private_settings", userId, e)
            Result.failure(e)
        }
        val directDoc = directResult.getOrNull()
        if (directDoc != null && directDoc.isNotEmpty()) {
            return directDoc
        }

        val fallbackResult = try {
            coreDatabase.queryDocuments(
                "user_private_settings",
                listOf(CoreQuery("userId", CoreQueryOperator.EQUAL, userId))
            )
        } catch (e: Exception) {
            logDocumentFailure("queryDocuments", "user_private_settings", "userId=$userId", e)
            Result.failure(e)
        }
        return fallbackResult.getOrNull()?.firstOrNull { it.isNotEmpty() }
    }

    private suspend fun normalizeUsersRow(
        userId: String,
        rowId: String,
        profile: UserProfile
    ) {
        if (rowId != userId) {
            Log.w(
                "ProfileRepository",
                "Legacy users row id mismatch detected: rowId=$rowId userId=$userId. Normalizing to canonical UID row."
            )
        }

        try {
            coreDatabase.createDocument("users", userId, userProfileToMap(profile)).getOrThrow()
        } catch (e: Exception) {
            logDocumentFailure("createDocument", "users", userId, e)
        }

        if (rowId != userId) {
            try {
                coreDatabase.deleteDocument("users", rowId).getOrThrow()
            } catch (e: Exception) {
                logDocumentFailure("deleteDocument", "users", rowId, e)
            }
        }
    }

    private fun mapToUserProfile(map: Map<String, Any>): UserProfile {
        val userId = map["userId"] as? String ?: map["\$id"] as? String ?: ""
        val publicKey = map["publicKey"] as? String
        
        if (publicKey.isNullOrBlank()) {
            Log.w("ProfileRepository", "PublicKey is missing for user: $userId. E2EE sync will be limited for this user.")
        }

        return UserProfile(
            userId = userId,
            email = map["email"] as? String ?: "",
            displayName = map["displayName"] as? String ?: map["name"] as? String ?: "",
            profilePictureUrl = map["profilePictureUrl"] as? String ?: map["avatar"] as? String,
            friendCode = map["friendCode"] as? String ?: "",
            intro = map["intro"] as? String ?: "",
            publicKey = publicKey,
            isBanned = map["isBanned"] as? Boolean ?: map["banned"] as? Boolean ?: false,
            isPhotoVisible = map["isPhotoVisible"] as? Boolean ?: map["photoVisible"] as? Boolean ?: true
        )
    }

    private fun userProfileToMap(profile: UserProfile): Map<String, Any> {
        return mapOf(
            "userId" to profile.userId,
            "displayName" to profile.displayName,
            "profilePictureUrl" to (profile.profilePictureUrl ?: ""),
            "friendCode" to profile.friendCode,
            "intro" to profile.intro,
            "publicKey" to (profile.publicKey ?: ""),
            "isBanned" to profile.isBanned,
            "isPhotoVisible" to profile.isPhotoVisible
        )
    }

    suspend fun createOrUpdateProfile(
        userId: String,
        email: String,
        displayName: String,
        profilePictureUrl: String? = null,
        isPhotoVisible: Boolean = true
    ): Result<UserProfile> {
        return try {
            val docResult = coreDatabase.getDocument("users", userId)
            val existingDoc = docResult.getOrNull()

            val profile = if (existingDoc != null && existingDoc.isNotEmpty()) {
                val currentProfile = mapToUserProfile(existingDoc)
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
                val authPhotoUrl = coreAuth.currentUser.firstOrNull()?.photoUrl
                profile.copy(profilePictureUrl = authPhotoUrl)
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
            val myUid = coreAuth.currentUserId
            if (profile.userId == myUid) {
                val publicProfileMap = userProfileToMap(profile)
                coreDatabase.createDocument("users", profile.userId, publicProfileMap)
            }

            profileCache.upsert(profile)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to update profile", e)
            // Cache locally so UI reflects the change immediately and will be synced later
            try {
                profileCache.upsert(profile)
            } catch (cacheEx: Exception) {
                Log.e("ProfileRepository", "Failed to cache profile locally after update failure", cacheEx)
            }
            Result.failure(e)
        }
    }

    private suspend fun saveProfile(profile: UserProfile, email: String) {
        try {
            val myUid = coreAuth.currentUserId
            if (profile.userId == myUid) {
                val publicProfileMap = userProfileToMap(profile)
                coreDatabase.createDocument("users", profile.userId, publicProfileMap)

                // Save private settings
                coreDatabase.createDocument(
                    "user_private_settings",
                    profile.userId,
                    mapOf(
                        "userId" to profile.userId,
                        "email" to email
                    )
                )
            }

            profileCache.upsert(profile)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to save profile remotely, caching locally", e)
            profileCache.upsert(profile)
        }
    }

    suspend fun findUserByFriendCode(friendCode: String): Result<UserProfile?> {
        return try {
            val queries = listOf(
                CoreQuery("friendCode", CoreQueryOperator.EQUAL, friendCode.uppercase())
            )
            val result = coreDatabase.queryDocuments("users", queries)
            val doc = result.getOrNull()?.firstOrNull()
            if (doc != null) {
                val profile = mapToUserProfile(doc)
                profileCache.upsert(profile)
                Result.success(profile)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncUserProfile(userId: String): UserProfile? {
        Log.d("ProfileRepository", "Starting syncUserProfile for $userId")
        return try {
            val myUid = coreAuth.currentUserId
            val remoteDoc = getUserDocWithFallback(userId)
            val doc = remoteDoc?.data
            Log.d("ProfileRepository", "User doc fetched, exists: ${doc != null}, rowId=${remoteDoc?.rowId}")
            
            val profile = if (doc != null && doc.isNotEmpty()) {
                val p = mapToUserProfile(doc).copy(userId = userId)
                val privateDoc = getPrivateSettingsDoc(userId)
                Log.d("ProfileRepository", "Private settings fetched")

                val privateEmail = privateDoc?.get("email") as? String ?: ""
                var finalProfile = p.copy(email = privateEmail)
                
                val sessionUser = coreAuth.currentUser.firstOrNull() ?: coreAuth.checkCurrentSession().getOrNull()
                if (sessionUser != null && sessionUser.uid == userId) {
                    val sessionName = sessionUser.displayName
                    val currentName = finalProfile.displayName
                    var needsUpdate = false
                    
                    if (!sessionName.isNullOrBlank() && (currentName.isBlank() || currentName == "Learner" || currentName == "Knowledge Seeker" || currentName != sessionName)) {
                        Log.d("ProfileRepository", "Updating remote profile name from Auth session: $sessionName")
                        finalProfile = finalProfile.copy(displayName = sessionName)
                        needsUpdate = true
                    }
                    
                    val sessionEmail = sessionUser.email
                    if (!sessionEmail.isNullOrBlank() && (finalProfile.email.isBlank() || finalProfile.email != sessionEmail)) {
                        Log.d("ProfileRepository", "Updating private email from Auth session: $sessionEmail")
                        finalProfile = finalProfile.copy(email = sessionEmail)
                        needsUpdate = true
                    }
                    
                    if (needsUpdate && userId == myUid) {
                        try {
                            val updatedMap = userProfileToMap(finalProfile)
                            coreDatabase.updateDocument("users", userId, updatedMap).getOrThrow()
                            coreDatabase.updateDocument("user_private_settings", userId, mapOf("email" to finalProfile.email)).getOrThrow()
                        } catch (e: Exception) {
                            logDocumentFailure("updateDocument", "users", userId, e)
                        }
                    }
                }
                finalProfile
            } else {
                Log.d("ProfileRepository", "User doc does not exist, checking current user session...")
                val sessionUser = coreAuth.currentUser.firstOrNull() ?: coreAuth.checkCurrentSession().getOrNull()
                if (sessionUser != null && sessionUser.uid == userId) {
                    val email = sessionUser.email ?: ""
                    UserProfile(
                        userId = userId,
                        email = email,
                        displayName = sessionUser.displayName ?: "Learner",
                        profilePictureUrl = sessionUser.photoUrl,
                        friendCode = generateUniqueFriendCode(),
                        publicKey = cryptoManager.initializeAndGetPublicKey(),
                        isPhotoVisible = true
                    ).also { newProfile ->
                        Log.d("ProfileRepository", "Saving new profile for $userId")
                        saveProfile(newProfile, email)
                    }
                } else {
                    Log.w("ProfileRepository", "CurrentUser session is null or UID mismatch during profile creation")
                    null
                }
            }

            profile?.let { initialProfile ->
                var p = initialProfile
                Log.d("ProfileRepository", "Syncing profile for ${p.userId}: ${p.displayName}, code: ${p.friendCode}")

                if (p.displayName.isBlank()) {
                    p = p.copy(displayName = safeDisplayName(p.displayName, p.userId))
                }

                if (p.friendCode.isBlank() && p.userId == myUid) {
                    Log.d("ProfileRepository", "Generating new friend code for ${p.userId}")
                    p = p.copy(friendCode = generateUniqueFriendCode()).also { updated ->
                        try {
                            val updatedMap = userProfileToMap(updated)
                            coreDatabase.updateDocument("users", userId, updatedMap).getOrThrow()
                        } catch (e: Exception) {
                            logDocumentFailure("updateDocument", "users", userId, e)
                        }
                    }
                }

                if (p.userId == myUid) {
                    val localPk = cryptoManager.initializeAndGetPublicKey()
                    if (p.publicKey.isNullOrBlank() || p.publicKey != localPk) {
                        Log.d("ProfileRepository", "Device public key mismatch! Updating CoreDatabase with local key.")
                        p = p.copy(publicKey = localPk)
                        try {
                            val updatedMap = userProfileToMap(p)
                            coreDatabase.updateDocument("users", p.userId, updatedMap).getOrThrow()
                        } catch (e: Exception) {
                            logDocumentFailure("updateDocument", "users", p.userId, e)
                        }
                    }
                }

                val remoteRowId = remoteDoc?.rowId ?: p.userId
                if (
                    (remoteRowId != p.userId ||
                    p.displayName.isBlank() ||
                    p.friendCode.isBlank() ||
                    (p.userId == myUid && p.publicKey.isNullOrBlank())) &&
                    p.userId == myUid
                ) {
                    normalizeUsersRow(p.userId, remoteRowId, p)
                } else if (p.userId == myUid) {
                    // Force an update to re-apply correct document permissions (Self-healing logic)
                    try {
                        coreDatabase.updateDocument("users", p.userId, userProfileToMap(p)).getOrThrow()
                        coreDatabase.updateDocument("user_private_settings", p.userId, mapOf("userId" to p.userId)).getOrThrow()
                        Log.d("ProfileRepository", "Repaired permissions for users and user_private_settings for ${p.userId}")
                    } catch (e: Exception) {
                        logDocumentFailure("repairPermissions", "users/settings", p.userId, e)
                    }
                }

                profileCache.upsert(p)
                Log.d(
                    "ProfileRepository",
                    "Cached profile for ${p.userId}: displayName='${p.displayName}', email='${p.email}', friendCode='${p.friendCode}', publicKey=${p.publicKey != null}"
                )
                Log.d("ProfileRepository", "Successfully cached profile locally")
                p
            } ?: run {
                Log.e("ProfileRepository", "Failed to get profile for $userId")
                null
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error syncing user profile", e)
            null
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
            val queries = listOf(
                CoreQuery("friendCode", CoreQueryOperator.EQUAL, friendCode.uppercase())
            )
            val result = coreDatabase.queryDocuments("users", queries)
            result.getOrNull()?.isEmpty() ?: true
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

    suspend fun fetchAndCacheContactProfile(userId: String): UserProfile? {
        Log.d("ProfileRepository", "Starting fetchAndCacheContactProfile for $userId")
        return try {
            val remoteDoc = getUserDocWithFallback(userId)
            val doc = remoteDoc?.data
            
            if (doc != null && doc.isNotEmpty()) {
                val profile = mapToUserProfile(doc).copy(userId = userId)
                profileCache.upsert(profile)
                Log.d("ProfileRepository", "Successfully fetched and cached contact profile for $userId")
                profile
            } else {
                Log.w("ProfileRepository", "Contact profile doc not found for $userId")
                null
            }
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Error fetching contact profile for $userId", e)
            null
        }
    }

    suspend fun setupSecureBackup(userId: String, passphrase: String): Result<Unit> {
        return try {
            val encryptedBackup = cryptoManager.exportEncryptedKeyset(passphrase)
            coreDatabase.createDocument(
                "user_private_settings",
                userId,
                mapOf(
                    "userId" to userId,
                    "e2eeBackup" to encryptedBackup
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to setup secure backup", e)
            Result.failure(e)
        }
    }

    suspend fun checkHasSecureBackup(userId: String): Boolean {
        return try {
            val doc = coreDatabase.getDocument("user_private_settings", userId).getOrNull()
            doc != null && doc["e2eeBackup"] != null
        } catch (e: Exception) {
            Log.e("ProfileRepository", "Failed to check secure backup", e)
            false
        }
    }

    suspend fun restoreSecureBackup(userId: String, passphrase: String): Result<Unit> {
        return try {
            val doc = coreDatabase.getDocument("user_private_settings", userId).getOrNull()
                ?: throw IllegalStateException("Backup not found")
            
            val encryptedBackup = doc["e2eeBackup"] as? String ?: throw IllegalStateException("Backup not found")
            
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
