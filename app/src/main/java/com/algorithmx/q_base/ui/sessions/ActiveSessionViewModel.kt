package com.algorithmx.q_base.ui.sessions
 
import kotlinx.coroutines.ExperimentalCoroutinesApi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.collections.*
import com.algorithmx.q_base.data.sessions.*
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.data.sessions.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import com.algorithmx.q_base.data.ai.AiRepository
import com.algorithmx.q_base.data.auth.AuthRepository
import com.algorithmx.q_base.data.sync.SyncRepository
import android.util.Log
import javax.inject.Inject

sealed class SessionNavEvent {
    data class NavigateToResults(val sessionId: String) : SessionNavEvent()
}

data class NavigatorDot(
    val index: Int,
    val status: String,
    val isSelected: Boolean
)

@HiltViewModel
class ActiveSessionViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val aiRepository: AiRepository,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _navigationEvents = MutableSharedFlow<SessionNavEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    private val _actionFeedback = MutableSharedFlow<String>()
    val actionFeedback = _actionFeedback.asSharedFlow()

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
    val attempts: StateFlow<List<SessionAttempt>> = _attempts.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _currentQuestion = MutableStateFlow<Question?>(null)
    val currentQuestion: StateFlow<Question?> = _currentQuestion.asStateFlow()

    private val _currentOptions = MutableStateFlow<List<QuestionOption>>(emptyList())
    val currentOptions: StateFlow<List<QuestionOption>> = _currentOptions.asStateFlow()

    private val _currentAnswer = MutableStateFlow<Answer?>(null)
    val currentAnswer: StateFlow<Answer?> = _currentAnswer.asStateFlow()

    private val _session = MutableStateFlow<StudySession?>(null)
    val session: StateFlow<StudySession?> = _session.asStateFlow()

    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

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

    fun setSessionId(id: String) {
        if (_sessionId == id) return
        _sessionId = id
        loadSessionData()
        loadAttempts()
        startTimer()
    }

    fun getSessionId(): String = _sessionId

    private fun loadSessionData() {
        viewModelScope.launch {
            repository.getSessionById(_sessionId)?.let { 
                _session.value = it
                _timeLimitSeconds.value = it.timeLimitSeconds 
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _elapsedSeconds.value++
                val limit = _timeLimitSeconds.value
                val session = _session.value
                
                if (limit != null && _elapsedSeconds.value >= limit) {
                    if (session?.timingType == "PER_QUESTION") {
                        // Auto-advance or submit
                        if (_currentQuestionIndex.value < _attempts.value.size - 1) {
                            navigateToQuestion(_currentQuestionIndex.value + 1)
                        } else {
                            submitSession()
                            break
                        }
                    } else {
                        // TOTAL time reached
                        submitSession()
                        break
                    }
                }
            }
        }
    }

    private fun loadAttempts() {
        viewModelScope.launch {
            repository.getAttemptsForSession(_sessionId).collect { attemptsList ->
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
            if (_session.value?.timingType == "PER_QUESTION") {
                _elapsedSeconds.value = 0
            }
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
            _currentAnswer.value = repository.getAnswerForQuestion(questionId)
        }
    }

    fun onAnswerSelected(optionLetter: String) {
        val currentAttempt = _attempts.value.getOrNull(_currentQuestionIndex.value) ?: return
        if (_session.value?.isCompleted == true) return // Disable answering in completed sessions

        val question = _currentQuestion.value ?: return
        
        val newUserAnswers = when (question.questionType?.trim()?.uppercase()) {
            "SBA" -> optionLetter
            "MTF", "MCQ", "T/F", "MCQ1" -> {
                val currentSelected = currentAttempt.userSelectedAnswers.split(",").filter { it.isNotEmpty() }.toMutableList()
                val (letter, _) = if (optionLetter.contains("_")) {
                    optionLetter.split("_")
                } else {
                    listOf(optionLetter, "")
                }
                
                currentSelected.removeAll { it.startsWith("${letter}_") }
                
                if (!currentAttempt.userSelectedAnswers.contains(optionLetter)) {
                    currentSelected.add(optionLetter)
                }
                currentSelected.sorted().joinToString(",")
            }
            else -> {
                val currentSelected = currentAttempt.userSelectedAnswers.split(",").filter { it.isNotEmpty() }.toMutableList()
                if (currentSelected.contains(optionLetter)) {
                    currentSelected.remove(optionLetter)
                } else {
                    currentSelected.add(optionLetter)
                }
                currentSelected.sorted().joinToString(",")
            }
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
            repository.updateAttemptAndRecalculate(attempt)
        }
    }

    fun askAi(mode: String = "EXPLAIN") {
        val question = _currentQuestion.value ?: return
        val options = _currentOptions.value
        
        viewModelScope.launch {
            _isAiLoading.value = true
            
            val prompt = when(mode) {
                "HINT" -> "Give me a subtle hint for this question without revealing the answer: ${question.stem}"
                "SUMMARY" -> "Provide a detailed summary related to this question: ${question.stem}"
                else -> "Explain this question and why the options are correct or incorrect: ${question.stem}. Options: ${options.joinToString { "${it.optionLetter}: ${it.optionText}" }}"
            }
            
            val result = aiRepository.getAiAssistance(prompt)
            _aiResponse.value = result.getOrNull() ?: "Failed to get AI assistance: ${result.exceptionOrNull()?.message}"
            _isAiLoading.value = false
        }
    }

    fun clearAiResponse() {
        _aiResponse.value = null
    }

    fun saveAiResponseToQuestion() {
        val question = _currentQuestion.value ?: return
        val aiExp = _aiResponse.value ?: return
        val currentAnswer = _currentAnswer.value
        
        viewModelScope.launch {
            val answerToSave = currentAnswer?.copy(generalExplanation = aiExp) ?: Answer(
                questionId = question.questionId,
                correctAnswerString = "A",
                generalExplanation = aiExp,
                references = ""
            )
            repository.saveAnswer(answerToSave)
            _currentAnswer.value = answerToSave
            _aiResponse.value = null
        }
    }

    fun submitSession() {
        viewModelScope.launch {
            val currentSession = _session.value ?: return@launch
            val updatedSession = currentSession.copy(isCompleted = true)
            repository.updateSession(updatedSession)
            _session.value = updatedSession

            val finalAttempts = _attempts.value.map { it.copy(attemptStatus = "FINALIZED") }
            finalAttempts.forEach { repository.updateAttempt(it) }
            timerJob?.cancel()
            _navigationEvents.emit(SessionNavEvent.NavigateToResults(_sessionId))
        }
    }

    fun reportSession(reason: String) {
        viewModelScope.launch {
            try {
                if (_sessionId.isNotEmpty()) {
                    syncRepository.reportSession(_sessionId, reason)
                    _actionFeedback.emit("Session reported successfully.")
                }
            } catch (e: Exception) {
                Log.e("ActiveSessionViewModel", "Failed to report session", e)
                _actionFeedback.emit("Failed to report session: ${e.message}")
            }
        }
    }

    fun reportQuestion(reason: String) {
        val question = _currentQuestion.value ?: return
        val options = _currentOptions.value
        val answer = _currentAnswer.value
        
        viewModelScope.launch {
            try {
                syncRepository.reportQuestion(question, options, answer, reason)
                _actionFeedback.emit("Question reported successfully.")
            } catch (e: Exception) {
                Log.e("ActiveSessionViewModel", "Failed to report question", e)
                _actionFeedback.emit("Failed to report question: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
