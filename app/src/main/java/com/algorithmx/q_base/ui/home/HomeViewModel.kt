package com.algorithmx.q_base.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.entity.Collection
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.StudySession
import com.algorithmx.q_base.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository,
    private val authRepository: com.algorithmx.q_base.data.repository.AuthRepository
) : ViewModel() {

    val currentUser: StateFlow<com.algorithmx.q_base.data.entity.UserEntity?> = authRepository.currentUser
        .flatMapLatest { firebaseUser ->
            if (firebaseUser != null) {
                repository.getCurrentUser(firebaseUser.uid)
            } else {
                kotlinx.coroutines.flow.flowOf(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val ongoingSessions: StateFlow<List<StudySession>> = repository.getOngoingSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedQuestions: StateFlow<List<Question>> = repository.getPinnedQuestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSessions: StateFlow<List<StudySession>> = repository.getRecentSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collections: StateFlow<List<com.algorithmx.q_base.data.entity.CollectionWithCount>> = repository.getAllCollectionsWithCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalUnreadCount: StateFlow<Int> = repository.getTotalUnreadCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
