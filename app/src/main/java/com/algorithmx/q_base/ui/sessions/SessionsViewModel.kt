package com.algorithmx.q_base.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.entity.MasterCategory
import com.algorithmx.q_base.data.entity.StudySession
import com.algorithmx.q_base.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val repository: SessionRepository
) : ViewModel() {

    val sessions: StateFlow<List<StudySession>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<MasterCategory>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sessionCreated = MutableSharedFlow<String>()
    val sessionCreated = _sessionCreated.asSharedFlow()

    fun createSession(categoryName: String, questionCount: Int, isTimed: Boolean) {
        viewModelScope.launch {
            val timeLimit = if (isTimed) 3600 else null // Default 1 hour if timed
            val sessionId = repository.createNewSession(categoryName, questionCount, timeLimit)
            _sessionCreated.emit(sessionId)
        }
    }
}
