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

data class CheckingBackupState(
    val isLoading: Boolean = true,
    val hasBackup: Boolean? = null,
    val error: String? = null,
    val isProceeding: Boolean = false
)

@HiltViewModel
class CheckingBackupViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CheckingBackupState())
    val state: StateFlow<CheckingBackupState> = _state.asStateFlow()

    fun checkBackup(userId: String) {
        _state.value = CheckingBackupState(isLoading = true)
        viewModelScope.launch {
            try {
                android.util.Log.d("CheckingBackupViewModel", "Checking secure backup for $userId")
                val hasBackup = profileRepository.checkHasSecureBackup(userId)
                android.util.Log.d("CheckingBackupViewModel", "Backup check result: $hasBackup")
                if (hasBackup) {
                    _state.value = CheckingBackupState(isLoading = false, hasBackup = true)
                } else {
                    android.util.Log.d("CheckingBackupViewModel", "No backup found. Syncing profile...")
                    profileRepository.syncUserProfile(userId)
                    android.util.Log.d("CheckingBackupViewModel", "Profile sync complete. Proceeding...")
                    _state.value = CheckingBackupState(isLoading = false, hasBackup = false, isProceeding = true)
                }
            } catch (e: Exception) {
                android.util.Log.e("CheckingBackupViewModel", "Error checking backup or syncing profile", e)
                _state.value = CheckingBackupState(isLoading = false, error = e.message ?: "An unexpected error occurred")
            }
        }
    }
}
