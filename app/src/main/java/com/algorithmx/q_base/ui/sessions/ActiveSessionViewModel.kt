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
import com.algorithmx.q_base.data.chat.isAdmin
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
    private var _chatId: String? = null

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

    fun setSessionId(id: String, chatId: String? = null) {
        _chatId = chatId
        if (_sessionId == id) return
        _sessionId = id
        loadSessionData()
        loadAttempts()
        startTimer()
    }

    private val _isReadOnly = MutableStateFlow(false)
    val isReadOnly: StateFlow<Boolean> = _isReadOnly.asStateFlow()

    fun getSessionId(): String = _sessionId

    private fun loadSessionData() {
        viewModelScope.launch {
            repository.getSessionById(_sessionId)?.let { session ->
                _session.value = session
                _timeLimitSeconds.value = session.timeLimitSeconds 

                // Evaluate if session editing is restricted to admins only
                if (session.isAdminOnly) {
                    val colId = session.collectionId
                    if (colId != null) {
                        val collection = repository.getStudyCollectionByIdOnce(colId)
                        val groupId = collection?.sharedWithGroupId
                        if (groupId != null) {
                            val chat = syncRepository.getChatById(groupId)
                            val currentUid = currentUser.value?.userId ?: authRepository.currentUser.firstOrNull()?.uid
                            if (chat != null && !chat.isAdmin(currentUid ?: "")) {
                                _isReadOnly.value = true
                                Log.d("ActiveSessionViewModel", "Session is marked Admin-Only. Non-admin user is restricted to Read-Only access.")
                            }
                        }
                    }
                }
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
        if (_isReadOnly.value) return // Block if session is read-only
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
        if (_isReadOnly.value) return // Block if session is read-only
        val currentAttempt = _attempts.value.getOrNull(_currentQuestionIndex.value) ?: return
        val newStatus = if (currentAttempt.attemptStatus == "FLAGGED") "ATTEMPTED" else "FLAGGED"
        updateAttempt(currentAttempt.copy(attemptStatus = newStatus))
    }

    private fun updateAttempt(attempt: SessionAttempt) {
        viewModelScope.launch {
            repository.updateAttemptAndRecalculate(attempt)
            
            // Micro-update real-time synchronization pipeline
            try {
                val activeChatId = _chatId ?: _session.value?.collectionId?.let { colId ->
                    repository.getStudyCollectionByIdOnce(colId)?.sharedWithGroupId
                }
                
                if (activeChatId != null) {
                    val data = org.json.JSONObject()
                    data.put("questionId", attempt.questionId)
                    data.put("attemptStatus", attempt.attemptStatus)
                    data.put("userSelectedAnswers", attempt.userSelectedAnswers)
                    data.put("marksObtained", attempt.marksObtained.toDouble())
                    
                    syncRepository.sendSessionPatch(activeChatId, _sessionId, "UPSERT_ATTEMPT", data)
                    Log.d("ActiveSessionViewModel", "Dispatched attempt sync patch for question ${attempt.questionId} to chat $activeChatId")
                }
            } catch (e: Exception) {
                Log.e("ActiveSessionViewModel", "Failed to send micro-update attempt patch", e)
            }
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
        if (_isReadOnly.value) return // Block if session is read-only
        viewModelScope.launch {
            val currentSession = _session.value ?: return@launch
            val updatedSession = currentSession.copy(isCompleted = true)
            repository.updateSession(updatedSession)
            _session.value = updatedSession

            val finalAttempts = _attempts.value.map { it.copy(attemptStatus = "FINALIZED") }
            finalAttempts.forEach { repository.updateAttempt(it) }
            
            // Micro-update real-time synchronization pipeline on final submit
            try {
                val activeChatId = _chatId ?: _session.value?.collectionId?.let { colId ->
                    repository.getStudyCollectionByIdOnce(colId)?.sharedWithGroupId
                }
                
                if (activeChatId != null) {
                    val data = org.json.JSONObject()
                    data.put("title", updatedSession.title)
                    data.put("isCompleted", true)
                    data.put("scoreAchieved", updatedSession.scoreAchieved.toDouble())
                    
                    syncRepository.sendSessionPatch(activeChatId, _sessionId, "UPDATE_SESSION", data)
                    Log.d("ActiveSessionViewModel", "Dispatched session completion sync patch to chat $activeChatId")
                }
            } catch (e: Exception) {
                Log.e("ActiveSessionViewModel", "Failed to send micro-update session patch", e)
            }

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