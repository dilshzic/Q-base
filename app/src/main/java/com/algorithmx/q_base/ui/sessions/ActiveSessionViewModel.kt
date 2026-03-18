package com.algorithmx.q_base.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionOption
import com.algorithmx.q_base.data.entity.SessionAttempt
import com.algorithmx.q_base.data.repository.SessionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale

data class NavigatorDot(
    val index: Int,
    val status: String,
    val isSelected: Boolean
)

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

    private val _timeLimitSeconds = MutableStateFlow<Int?>(null)
    private val _elapsedSeconds = MutableStateFlow(0L)

    val timerDisplay: StateFlow<String> = _elapsedSeconds.map { elapsed ->
        val limit = _timeLimitSeconds.value
        val displaySeconds = if (limit != null) {
            (limit - elapsed).coerceAtLeast(0)
        } else {
            elapsed
        }
        val mins = displaySeconds / 60
        val secs = displaySeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "00:00")

    val navigatorDots: StateFlow<List<NavigatorDot>> = combine(
        _attempts,
        _currentQuestionIndex
    ) { attempts, currentIndex ->
        attempts.mapIndexed { index, attempt ->
            NavigatorDot(
                index = index,
                status = attempt.attemptStatus,
                isSelected = index == currentIndex
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var timerJob: Job? = null

    init {
        loadSessionData()
        loadAttempts()
        startTimer()
    }

    private fun loadSessionData() {
        viewModelScope.launch {
            // We'll need to add getSessionById to repository if not there, 
            // but for now we'll assume we can get it or just use a default.
            // repository.getSessionById(sessionId)?.let { _timeLimitSeconds.value = it.timeLimitSeconds }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value++
                val limit = _timeLimitSeconds.value
                if (limit != null && _elapsedSeconds.value >= limit) {
                    submitSession()
                    break
                }
            }
        }
    }

    private fun loadAttempts() {
        viewModelScope.launch {
            repository.getAttemptsForSession(sessionId).collect { attemptsList ->
                _attempts.value = attemptsList
                if (_currentQuestion.value == null && attemptsList.isNotEmpty()) {
                    loadQuestion(attemptsList[_currentQuestionIndex.value].questionId)
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

    fun submitSession() {
        viewModelScope.launch {
            val finalAttempts = _attempts.value.map { it.copy(attemptStatus = "FINALIZED") }
            finalAttempts.forEach { repository.updateAttempt(it) }
            timerJob?.cancel()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
