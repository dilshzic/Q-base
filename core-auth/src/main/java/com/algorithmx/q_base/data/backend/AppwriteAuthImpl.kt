package com.algorithmx.q_base.data.backend

import androidx.activity.ComponentActivity
import io.appwrite.Client
import io.appwrite.enums.OAuthProvider
import io.appwrite.models.User
import io.appwrite.services.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppwriteAuthImpl @Inject constructor(
    private val appwriteClient: Client,
    private val appwriteAccount: Account
) : CoreAuth {

    private val _currentUser = MutableStateFlow<CoreUser?>(null)
    override val currentUser: Flow<CoreUser?> = _currentUser.asStateFlow()
    
    override val currentUserId: String?
        get() = _currentUser.value?.uid

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        scope.launch {
            checkCurrentSession()
        }
    }

    private fun mapUser(user: User<*>): CoreUser {
        return CoreUser(
            uid = user.id,
            email = user.email.ifBlank { null },
            displayName = user.name.ifBlank { null },
            photoUrl = null
        )
    }

    override suspend fun signInWithEmail(email: String, pass: String): Result<CoreUser> {
        return try {
            try { appwriteAccount.deleteSession("current") } catch (e: Exception) {}
            
            appwriteAccount.createEmailPasswordSession(email, pass)
            val user = appwriteAccount.get()
            val coreUser = mapUser(user)
            _currentUser.value = coreUser
            Result.success(coreUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, pass: String, username: String): Result<CoreUser> {
        return try {
            val userId = io.appwrite.ID.unique()
            appwriteAccount.create(userId, email, pass, username)
            
            appwriteAccount.createEmailPasswordSession(email, pass)
            val user = appwriteAccount.get()
            val coreUser = mapUser(user)
            _currentUser.value = coreUser
            Result.success(coreUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(activity: ComponentActivity): Result<CoreUser> {
        return try {
            appwriteAccount.createOAuth2Session(
                activity = activity,
                provider = OAuthProvider.GOOGLE
            )
            val user = appwriteAccount.get()
            val coreUser = mapUser(user)
            _currentUser.value = coreUser
            Result.success(coreUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            appwriteAccount.deleteSession("current")
            _currentUser.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkCurrentSession(): Result<CoreUser?> {
        return try {
            val user = appwriteAccount.get()
            val coreUser = mapUser(user)
            _currentUser.value = coreUser
            Result.success(coreUser)
        } catch (e: Exception) {
            _currentUser.value = null
            Result.success(null)
        }
    }
}
