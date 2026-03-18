package com.algorithmx.q_base.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionOption
import com.algorithmx.q_base.data.entity.SessionAttempt
import com.algorithmx.q_base.data.repository.SessionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ActiveSessionViewModel(
    private val repository: SessionRepository,
    private val sessionId: String
) : ViewModel() {

    private val _attempts = MutableStateFlow<List<SessionAttempt>>(emptyList())
    val attempts: StateFlow<List<SessionAttempt>> = _attempts.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _currentQuestion = MutableStateFlow<Question?>(null)
    val currentQuestion: StateFlow<Question?> = _currentQuestion.asStateFlow()

    private val _currentOptions = MutableStateFlow<List<QuestionOption>>(emptyList())
    val currentOptions: StateFlow<List<QuestionOption>> = _currentOptions.asStateFlow()

    init {
        loadAttempts()
    }

    private fun loadAttempts() {
        viewModelScope.launch {
            repository.getAttemptsForSession(sessionId).collect { attemptsList ->
                _attempts.value = attemptsList
                if (_currentQuestion.value == null && attemptsList.isNotEmpty()) {
                    loadQuestion(attemptsList[0].questionId)
                }
            }
        }
    }

    fun navigateToQuestion(index: Int) {
        if (index in _attempts.value.indices) {
            _currentQuestionIndex.value = index
            loadQuestion(_attempts.value[index].questionId)
        }
    }

    private fun loadQuestion(questionId: String) {
        viewModelScope.launch {
            val question = repository.getQuestionById(questionId)
            _currentQuestion.value = question
            repository.getOptionsForQuestion(questionId).collect { options ->
                _currentOptions.value = options
            }
        }
    }

    fun onAnswerSelected(optionLetter: String) {
        val currentAttempt = _attempts.value.getOrNull(_currentQuestionIndex.value) ?: return
        val question = _currentQuestion.value ?: return
        
        val newUserAnswers = if (question.questionType == "SBA") {
            optionLetter
        } else {
            val currentSelected = currentAttempt.userSelectedAnswers.split(",").filter { it.isNotEmpty() }.toMutableList()
            if (currentSelected.contains(optionLetter)) {
                currentSelected.remove(optionLetter)
            } else {
                currentSelected.add(optionLetter)
            }
            currentSelected.sorted().joinToString(",")
        }

        updateAttempt(currentAttempt.copy(
            userSelectedAnswers = newUserAnswers,
            attemptStatus = "ATTEMPTED"
        ))
    }

    fun toggleFlag() {
        val currentAttempt = _attempts.value.getOrNull(_currentQuestionIndex.value) ?: return
        val newStatus = if (currentAttempt.attemptStatus == "FLAGGED") "ATTEMPTED" else "FLAGGED"
        updateAttempt(currentAttempt.copy(attemptStatus = newStatus))
    }

    private fun updateAttempt(attempt: SessionAttempt) {
        viewModelScope.launch {
            repository.updateAttempt(attempt)
        }
    }
}
