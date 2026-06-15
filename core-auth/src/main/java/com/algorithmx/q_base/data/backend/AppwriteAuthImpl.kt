package com.algorithmx.q_base.core.data.backend

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
import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
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

    private fun mapUser(user: User<*>, photoUrl: String? = null): CoreUser {
        return CoreUser(
            uid = user.id,
            email = user.email.ifBlank { null },
            displayName = user.name.ifBlank { null },
            photoUrl = photoUrl
        )
    }

    private suspend fun fetchGooglePhotoUrl(): String? {
        var connection: HttpURLConnection? = null
        return try {
            val session = appwriteAccount.getSession("current")
            val accessToken = session.providerAccessToken
            if (accessToken.isNullOrBlank()) {
                Log.d("AppwriteAuthImpl", "No provider access token available")
                return null
            }

            val url = URL("https://www.googleapis.com/oauth2/v3/userinfo")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)
                val picture = if (json.has("picture")) json.getString("picture") else null
                Log.d("AppwriteAuthImpl", "Fetched Google photo URL: $picture")
                picture
            } else {
                Log.w("AppwriteAuthImpl", "Google userinfo returned ${connection.responseCode}")
                connection.errorStream?.use { it.readBytes() }
                null
            }
        } catch (e: Exception) {
            Log.d("AppwriteAuthImpl", "Could not fetch Google photo (non-OAuth session or expired): ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
    }

    override suspend fun signInWithEmail(email: String, pass: String): Result<CoreUser> {
        return try {
            try { appwriteAccount.deleteSession("current") } catch (e: Exception) {}

            try {
                appwriteAccount.createEmailPasswordSession(email, pass)
            } catch (e: NullPointerException) {
                // Bypass Appwrite SDK 5.0.0 bug: Session parsing may throw NPE,
                // but the session cookie is already stored by the HTTP client.
            } catch (e: Exception) {
                // Re-throw legitimate errors (e.g., wrong password)
                throw e
            }

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
            
            try {
                appwriteAccount.createEmailPasswordSession(email, pass)
            } catch (e: NullPointerException) {
                // Bypass Appwrite SDK 5.0.0 NPE bug
            } catch (e: Exception) {
                throw e
            }

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
                provider = OAuthProvider.GOOGLE,
                scopes = listOf("profile", "email", "openid")
            )
            val user = appwriteAccount.get()
            val photoUrl = fetchGooglePhotoUrl()
            val coreUser = mapUser(user, photoUrl)
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
            val photoUrl = fetchGooglePhotoUrl()
            val coreUser = mapUser(user, photoUrl)
            _currentUser.value = coreUser
            Result.success(coreUser)
        } catch (e: Exception) {
            _currentUser.value = null
            Result.success(null)
        }
    }
}
