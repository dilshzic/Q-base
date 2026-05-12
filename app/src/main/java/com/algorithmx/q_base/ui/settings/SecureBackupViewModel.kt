package com.algorithmx.q_base.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.auth.ProfileRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SecureBackupState(
    val hasBackup: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class SecureBackupViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SecureBackupState(isLoading = true))
    val state: StateFlow<SecureBackupState> = _state.asStateFlow()

    init {
        checkBackupStatus()
    }

    fun checkBackupStatus() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val hasBackup = profileRepository.checkHasSecureBackup(userId)
            _state.value = _state.value.copy(isLoading = false, hasBackup = hasBackup)
        }
    }

    fun setupBackup(passphrase: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (passphrase.length < 6) {
            _state.value = _state.value.copy(error = "Passphrase must be at least 6 characters")
            return
        }
        
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = profileRepository.setupSecureBackup(userId, passphrase)
            if (result.isSuccess) {
                _state.value = _state.value.copy(isLoading = false, isSuccess = true, hasBackup = true)
            } else {
                _state.value = _state.value.copy(
                    isLoading = false, 
                    error = "Failed to setup backup: ${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun restoreBackup(passphrase: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _state.value = _state.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            val result = profileRepository.restoreSecureBackup(userId, passphrase)
            if (result.isSuccess) {
                _state.value = _state.value.copy(isLoading = false, isSuccess = true)
            } else {
                _state.value = _state.value.copy(
                    isLoading = false, 
                    error = "Invalid passphrase or decryption failed."
                )
            }
        }
    }

    fun clearState() {
        _state.value = _state.value.copy(error = null, isSuccess = false)
    }
}
