package com.algorithmx.q_base.ui.sessions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.algorithmx.q_base.data.repository.SessionRepository
import com.algorithmx.q_base.data.repository.AiRepository

class ActiveSessionViewModelFactory(
    private val repository: SessionRepository,
    private val aiRepository: AiRepository,
    private val authRepository: com.algorithmx.q_base.data.repository.AuthRepository,
    private val syncRepository: com.algorithmx.q_base.data.repository.SyncRepository,
    private val sessionId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActiveSessionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ActiveSessionViewModel(
                repository,
                aiRepository,
                authRepository,
                syncRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
