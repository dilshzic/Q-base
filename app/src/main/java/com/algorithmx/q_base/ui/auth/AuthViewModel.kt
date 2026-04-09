package com.algorithmx.q_base.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.auth.AuthRepository
import com.algorithmx.q_base.data.auth.ProfileRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthState(
    val user: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isProfileCreated: Boolean = false
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
            val result = authRepository.signInWithEmail(email, pass)
            result.onSuccess { user ->
                try {
                    profileRepository.syncUserProfile(user.uid)
                    _state.value = AuthState(user = user, isSuccess = true, isProfileCreated = true)
                } catch (e: Exception) {
                    _state.value = AuthState(error = "Sync profile failed: ${e.message}")
                }
            }.onFailure { error ->
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

    fun onGoogleSignInSuccess(user: FirebaseUser) {
        _state.value = AuthState(isLoading = true)
        viewModelScope.launch {
            try {
                profileRepository.syncUserProfile(user.uid)
                _state.value = AuthState(user = user, isSuccess = true, isProfileCreated = true)
            } catch (e: Exception) {
                _state.value = AuthState(error = "Sync profile failed: ${e.message}")
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
