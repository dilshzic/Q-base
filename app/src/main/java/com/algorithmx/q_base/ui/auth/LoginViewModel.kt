package com.algorithmx.q_base.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.repository.AuthRepository
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
    val isSuccess: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun signIn(email: String, pass: String) {
        _state.value = AuthState(isLoading = true)
        repository.signInWithEmail(email, pass) { result ->
            result.onSuccess { user ->
                _state.value = AuthState(user = user, isSuccess = true)
            }.onFailure { error ->
                _state.value = AuthState(error = error.message)
            }
        }
    }

    fun signUp(email: String, pass: String) {
        _state.value = AuthState(isLoading = true)
        repository.signUpWithEmail(email, pass) { result ->
            result.onSuccess { user ->
                _state.value = AuthState(user = user, isSuccess = true)
            }.onFailure { error ->
                _state.value = AuthState(error = error.message)
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
