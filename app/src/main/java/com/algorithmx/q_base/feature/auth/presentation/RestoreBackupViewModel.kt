package com.algorithmx.q_base.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.core.data.auth.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RestoreBackupState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRestored: Boolean = false,
    val isFreshStart: Boolean = false
)

@HiltViewModel
class RestoreBackupViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RestoreBackupState())
    val state: StateFlow<RestoreBackupState> = _state.asStateFlow()

    fun restoreBackup(userId: String, passphrase: String) {
        _state.value = RestoreBackupState(isLoading = true)
        viewModelScope.launch {
            val result = profileRepository.restoreSecureBackup(userId, passphrase)
            if (result.isSuccess) {
                _state.value = RestoreBackupState(isRestored = true)
            } else {
                _state.value = RestoreBackupState(error = result.exceptionOrNull()?.message ?: "Failed to restore backup")
            }
        }
    }

    fun startFresh(userId: String) {
        _state.value = RestoreBackupState(isLoading = true)
        viewModelScope.launch {
            try {
                profileRepository.deleteSecureBackup(userId)
                _state.value = RestoreBackupState(isFreshStart = true)
            } catch (e: Exception) {
                _state.value = RestoreBackupState(error = "Failed to start fresh: ${e.message}")
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
