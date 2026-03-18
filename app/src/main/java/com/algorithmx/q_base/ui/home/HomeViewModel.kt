package com.algorithmx.q_base.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionCollection
import com.algorithmx.q_base.data.entity.StudySession
import com.algorithmx.q_base.data.repository.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: HomeRepository
) : ViewModel() {

    val ongoingSessions: StateFlow<List<StudySession>> = repository.getOngoingSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pinnedQuestions: StateFlow<List<Question>> = repository.getPinnedQuestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentCollections: StateFlow<List<QuestionCollection>> = repository.getRecentCollections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
