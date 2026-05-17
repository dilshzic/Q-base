package com.algorithmx.q_base.data.auth

import androidx.activity.ComponentActivity
import io.appwrite.enums.OAuthProvider
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class AppwriteUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: android.net.Uri? = null
)

@Singleton
class AuthRepository @Inject constructor(
    private val appwriteClient: Client,
    private val appwriteAccount: Account
) {
    private val _currentUser = MutableStateFlow<AppwriteUser?>(null)
    val currentUser: Flow<AppwriteUser?> = _currentUser.asStateFlow()
    val currentUserId: String?
        get() = _currentUser.value?.uid

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        checkCurrentSession()
    }

    fun checkCurrentSession() {
        repositoryScope.launch {
            try {
                val user = appwriteAccount.get()
                _currentUser.value = mapAppwriteUser(user)
            } catch (e: Exception) {
                _currentUser.value = null
            }
        }
    }

    private fun mapAppwriteUser(user: User<*>): AppwriteUser {
        return AppwriteUser(
            uid = user.id,
            email = user.email,
            displayName = user.name,
            photoUrl = null
        )
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<AppwriteUser> {
        return try {
            try { appwriteAccount.deleteSession("current") } catch (e: Exception) {}
            
            appwriteAccount.createEmailPasswordSession(email, pass)
            val user = appwriteAccount.get()
            val appUser = mapAppwriteUser(user)
            _currentUser.value = appUser
            Result.success(appUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(email: String, pass: String, username: String, photoUrl: String? = null): Result<AppwriteUser> {
        return try {
            val userId = io.appwrite.ID.unique()
            appwriteAccount.create(userId, email, pass, username)
            
            appwriteAccount.createEmailPasswordSession(email, pass)
            val user = appwriteAccount.get()
            val appUser = mapAppwriteUser(user)
            _currentUser.value = appUser
            Result.success(appUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(activity: ComponentActivity): Result<AppwriteUser> {
        return try {
            appwriteAccount.createOAuth2Session(
                activity = activity,
                provider = OAuthProvider.GOOGLE
            )
            val user = appwriteAccount.get()
            val appUser = mapAppwriteUser(user)
            _currentUser.value = appUser
            Result.success(appUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        repositoryScope.launch {
            try {
                appwriteAccount.deleteSession("current")
            } catch (e: Exception) {}
            _currentUser.value = null
        }
    }
}
