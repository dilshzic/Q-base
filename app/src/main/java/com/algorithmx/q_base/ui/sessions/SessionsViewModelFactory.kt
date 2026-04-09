package com.algorithmx.q_base.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.algorithmx.q_base.data.sessions.SessionRepository
import com.algorithmx.q_base.data.auth.AuthRepository
import com.algorithmx.q_base.data.sync.SyncRepository
import com.algorithmx.q_base.data.core.UserDao

class SessionsViewModelFactory(
    private val repository: SessionRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val userDao: UserDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SessionsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SessionsViewModel(repository, authRepository, userDao, syncRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
