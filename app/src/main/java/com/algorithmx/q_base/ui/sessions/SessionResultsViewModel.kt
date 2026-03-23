package com.algorithmx.q_base.ui.sessions
 
import kotlinx.coroutines.ExperimentalCoroutinesApi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.entity.*
import com.algorithmx.q_base.data.repository.SessionRepository
import com.algorithmx.q_base.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewState(
    val attempt: SessionAttempt,
    val question: Question,
    val options: List<QuestionOption>,
    val answer: Answer?
)

sealed class ResultsUiState {
    object Loading : ResultsUiState()
    data class Success(val attempts: List<SessionAttempt>, val score: Float) : ResultsUiState()
}

@HiltViewModel
class SessionResultsViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: com.algorithmx.q_base.data.repository.AuthRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<UserEntity?> = authRepository.currentUser
        .flatMapLatest { firebaseUser ->
            if (firebaseUser != null) {
                repository.getCurrentUser(firebaseUser.uid)
            } else {
                flowOf<UserEntity?>(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private var _sessionId: String = ""

    private val _attempts = MutableStateFlow<List<SessionAttempt>>(emptyList())
    private val _score = MutableStateFlow(0f)
    private val _isLoading = MutableStateFlow(true)

    private val _reviewQuestion = MutableStateFlow<ReviewState?>(null)
    val reviewQuestion = _reviewQuestion.asStateFlow()

    val uiState: StateFlow<ResultsUiState> = combine(_attempts, _score, _isLoading) { attempts, score, loading ->
        if (loading) ResultsUiState.Loading
        else ResultsUiState.Success(attempts, score)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResultsUiState.Loading)

    fun initSession(id: String) {
        if (_sessionId == id) return
        _sessionId = id
        loadResults()
    }

    private fun loadResults() {
        viewModelScope.launch {
            repository.getAttemptsForSession(_sessionId).collect { attempts ->
                val totalMarks = attempts.sumOf { it.marksObtained.toDouble() }.toFloat()
                val maxPossibleMarks = attempts.size * 4f 
                val scorePercentage = if (maxPossibleMarks > 0) (totalMarks / maxPossibleMarks) * 100 else 0f
                
                _attempts.value = attempts
                _score.value = scorePercentage
                _isLoading.value = false
            }
        }
    }

    fun selectQuestionForReview(attempt: SessionAttempt) {
        viewModelScope.launch {
            val question = repository.getQuestionById(attempt.questionId) ?: return@launch
            val options = repository.getOptionsForQuestion(attempt.questionId).first()
            val answer = repository.getAnswerForQuestion(attempt.questionId)
            
            _reviewQuestion.value = ReviewState(
                attempt = attempt,
                question = question,
                options = options,
                answer = answer
            )
        }
    }

    fun clearReview() {
        _reviewQuestion.value = null
    }

    fun reportCurrentSession(reason: String) {
        viewModelScope.launch {
            if (_sessionId.isNotEmpty()) {
                try {
                    syncRepository.reportSession(_sessionId, reason)
                } catch (e: Exception) {
                    // Log or show error
                }
            }
        }
    }
}
