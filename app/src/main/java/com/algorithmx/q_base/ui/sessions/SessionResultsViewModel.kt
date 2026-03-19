package com.algorithmx.q_base.ui.sessions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.entity.*
import com.algorithmx.q_base.data.repository.SessionRepository
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _attempts = MutableStateFlow<List<SessionAttempt>>(emptyList())
    private val _score = MutableStateFlow(0f)
    private val _isLoading = MutableStateFlow(true)

    private val _reviewQuestion = MutableStateFlow<ReviewState?>(null)
    val reviewQuestion = _reviewQuestion.asStateFlow()

    val uiState: StateFlow<ResultsUiState> = combine(_attempts, _score, _isLoading) { attempts, score, loading ->
        if (loading) ResultsUiState.Loading
        else ResultsUiState.Success(attempts, score)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResultsUiState.Loading)

    init {
        loadResults()
    }

    private fun loadResults() {
        viewModelScope.launch {
            repository.getAttemptsForSession(sessionId).collect { attempts ->
                val totalMarks = attempts.sumOf { it.marksObtained.toDouble() }.toFloat()
                val maxPossibleMarks = attempts.size * 5f 
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
}
