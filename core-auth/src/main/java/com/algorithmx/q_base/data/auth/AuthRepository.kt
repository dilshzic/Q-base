package com.algorithmx.q_base.data.auth

import android.util.Log
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
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
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

    private val _isSessionChecked = MutableStateFlow(false)
    val isSessionChecked: Flow<Boolean> = _isSessionChecked.asStateFlow()

    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    init {
        checkCurrentSession()
    }

    fun checkCurrentSession() {
        repositoryScope.launch {
            try {
                val user = appwriteAccount.get()
                val photoUrl = fetchGooglePhotoUrl()
                _currentUser.value = mapAppwriteUser(user, photoUrl)
            } catch (e: Exception) {
                _currentUser.value = null
            } finally {
                _isSessionChecked.value = true
            }
        }
    }

    private fun mapAppwriteUser(user: User<*>, photoUrl: String? = null): AppwriteUser {
        return AppwriteUser(
            uid = user.id,
            email = user.email,
            displayName = user.name,
            photoUrl = photoUrl?.let { android.net.Uri.parse(it) }
        )
    }

    /**
     * Fetches the Google profile picture URL using the OAuth session's providerAccessToken.
     * Returns null if the session is not a Google OAuth session or if the fetch fails.
     */
    private suspend fun fetchGooglePhotoUrl(): String? {
        var connection: HttpURLConnection? = null
        return try {
            val session = appwriteAccount.getSession("current")
            val accessToken = session.providerAccessToken
            if (accessToken.isNullOrBlank()) {
                Log.d("AuthRepository", "No provider access token available")
                return null
            }

            // Call Google's userinfo endpoint on IO dispatcher
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
                Log.d("AuthRepository", "Fetched Google photo URL: $picture")
                picture
            } else {
                Log.w("AuthRepository", "Google userinfo returned ${connection.responseCode}")
                connection.errorStream?.use { it.readBytes() }
                null
            }
        } catch (e: Exception) {
            Log.d("AuthRepository", "Could not fetch Google photo (non-OAuth session or expired): ${e.message}")
            null
        } finally {
            connection?.disconnect()
        }
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
            val photoUrl = fetchGooglePhotoUrl()
            val appUser = mapAppwriteUser(user, photoUrl)
            _currentUser.value = appUser
            Result.success(appUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        repositoryScope.launch {
            _isSessionChecked.value = false
            try {
                appwriteAccount.deleteSession("current")
            } catch (e: Exception) {}
            _currentUser.value = null
            _isSessionChecked.value = true
        }
    }
}

