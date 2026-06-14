package com.algorithmx.q_base.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.core.data.auth.AuthRepository
import com.algorithmx.q_base.core.data.auth.ProfileRepository
import com.algorithmx.q_base.core.data.auth.AppwriteUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val user: AppwriteUser? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isProfileCreated: Boolean = false,
    val requiresBackupRestore: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun signIn(email: String, pass: String) {
        _state.value = AuthState(isLoading = true)
        viewModelScope.launch {
            android.util.Log.d("AuthViewModel", "Starting sign in for $email")
            val result = authRepository.signInWithEmail(email, pass)
            result.onSuccess { user ->
                android.util.Log.d("AuthViewModel", "Sign in successful for ${user.uid}, syncing profile...")
                try {
                    val hasBackup = profileRepository.checkHasSecureBackup(user.uid)
                    profileRepository.syncUserProfile(user.uid)
                    android.util.Log.d("AuthViewModel", "Profile sync complete, setting state. Backup=$hasBackup")
                    _state.value = AuthState(user = user, isSuccess = !hasBackup, isProfileCreated = true, requiresBackupRestore = hasBackup)
                } catch (e: Exception) {
                    android.util.Log.e("AuthViewModel", "Profile sync failed", e)
                    _state.value = AuthState(error = "Sync profile failed: ${e.message}")
                }
            }.onFailure { error ->
                android.util.Log.e("AuthViewModel", "Sign in failed", error)
                _state.value = AuthState(error = error.message)
            }
        }
    }

    fun signUp(email: String, pass: String, username: String, photoUrl: String? = null) {
        _state.value = AuthState(isLoading = true)
        viewModelScope.launch {
            val result = authRepository.signUpWithEmail(email, pass, username, photoUrl)
            result.onSuccess { user ->
                val profileResult = profileRepository.createOrUpdateProfile(
                    userId = user.uid,
                    email = user.email ?: email,
                    displayName = username,
                    profilePictureUrl = photoUrl ?: user.photoUrl?.toString()
                )
                profileResult.onSuccess {
                    _state.value = AuthState(user = user, isSuccess = true, isProfileCreated = true)
                }.onFailure { error ->
                    _state.value = AuthState(error = "User created but profile failed: ${error.message}")
                }
            }.onFailure { error ->
                _state.value = AuthState(error = error.message)
            }
        }
    }

    fun signInWithGoogle(activity: androidx.activity.ComponentActivity) {
        _state.value = AuthState(isLoading = true)
        viewModelScope.launch {
            val result = authRepository.signInWithGoogle(activity)
            result.onSuccess { user ->
                try {
                    val hasBackup = profileRepository.checkHasSecureBackup(user.uid)
                    profileRepository.syncUserProfile(user.uid)
                    _state.value = AuthState(user = user, isSuccess = !hasBackup, isProfileCreated = true, requiresBackupRestore = hasBackup)
                } catch (e: Exception) {
                    _state.value = AuthState(error = "Sync profile failed: ${e.message}")
                }
            }.onFailure { error ->
                _state.value = AuthState(error = error.message)
            }
        }
    }

    fun onGoogleSignInSuccess(user: AppwriteUser) {
        _state.value = AuthState(isLoading = true)
        viewModelScope.launch {
            try {
                val hasBackup = profileRepository.checkHasSecureBackup(user.uid)
                profileRepository.syncUserProfile(user.uid)
                _state.value = AuthState(user = user, isSuccess = !hasBackup, isProfileCreated = true, requiresBackupRestore = hasBackup)
            } catch (e: Exception) {
                _state.value = AuthState(error = "Sync profile failed: ${e.message}")
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}