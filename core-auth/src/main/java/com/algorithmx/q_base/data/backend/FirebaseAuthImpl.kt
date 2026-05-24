package com.algorithmx.q_base.core.data.backend

import android.content.Context
import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAuthImpl @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val context: Context
) : CoreAuth {

    private val _currentUser = MutableStateFlow<CoreUser?>(null)
    override val currentUser: Flow<CoreUser?> = _currentUser.asStateFlow()
    
    override val currentUserId: String?
        get() = firebaseAuth.currentUser?.uid

    init {
        // Track Firebase auth state changes in real-time
        firebaseAuth.addAuthStateListener { auth ->
            val user = auth.currentUser
            _currentUser.value = user?.let {
                CoreUser(
                    uid = it.uid,
                    email = it.email,
                    displayName = it.displayName,
                    photoUrl = it.photoUrl?.toString()
                )
            }
        }
    }

    override suspend fun signInWithEmail(email: String, pass: String): Result<CoreUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user ?: throw Exception("Firebase user is null")
            val coreUser = CoreUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = firebaseUser.displayName,
                photoUrl = firebaseUser.photoUrl?.toString()
            )
            _currentUser.value = coreUser
            Result.success(coreUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmail(email: String, pass: String, username: String): Result<CoreUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, pass).await()
            val firebaseUser = authResult.user ?: throw Exception("Firebase user is null")
            
            // Set displayName
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(username)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()
            
            val coreUser = CoreUser(
                uid = firebaseUser.uid,
                email = firebaseUser.email,
                displayName = username,
                photoUrl = null
            )
            _currentUser.value = coreUser
            Result.success(coreUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(activity: ComponentActivity): Result<CoreUser> {
        return try {
            // Standard Firebase Google Sign-In logic
            // Uses GoogleSignIn API to retrieve the ID token and signs in with credentials
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("your_web_client_id_here") // Should ideally be configured or fetched dynamically
                .requestEmail()
                .build()
            
            val googleSignInClient = GoogleSignIn.getClient(activity, gso)
            // Note: Since calling startActivityForResult inside a suspend function requires registration,
            // we assume the UI handles launching the intent, or the implementation can provide a direct 
            // sign-in with a pre-fetched ID token.
            // Let's implement a standard sign-in if the user is already authenticated on Google:
            val account = GoogleSignIn.getLastSignedInAccount(activity)
            if (account != null && account.idToken != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                val authResult = firebaseAuth.signInWithCredential(credential).await()
                val firebaseUser = authResult.user ?: throw Exception("Firebase user is null")
                val coreUser = CoreUser(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email,
                    displayName = firebaseUser.displayName,
                    photoUrl = firebaseUser.photoUrl?.toString()
                )
                Result.success(coreUser)
            } else {
                throw Exception("Google account ID token not found. Launch sign-in launcher.")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut(): Result<Unit> {
        return try {
            firebaseAuth.signOut()
            _currentUser.value = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkCurrentSession(): Result<CoreUser?> {
        val user = firebaseAuth.currentUser
        val coreUser = user?.let {
            CoreUser(
                uid = it.uid,
                email = it.email,
                displayName = it.displayName,
                photoUrl = it.photoUrl?.toString()
            )
        }
        _currentUser.value = coreUser
        return Result.success(coreUser)
    }
}
