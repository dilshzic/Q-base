package com.algorithmx.q_base.feature.sessions.presentation
 
import kotlinx.coroutines.ExperimentalCoroutinesApi

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.collections.*
import com.algorithmx.q_base.feature.sessions.data.*
import com.algorithmx.q_base.core.data.UserEntity
import com.algorithmx.q_base.feature.sessions.data.SessionRepository
import com.algorithmx.q_base.sync.orchestration.SyncRepository
import com.algorithmx.q_base.core.data.chat.isAdmin
import com.algorithmx.q_base.core.data.auth.AuthRepository
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
    data class Success(val session: StudySession?, val attempts: List<SessionAttempt>, val score: Float) : ResultsUiState()
}

@HiltViewModel
class SessionResultsViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<UserEntity?> = authRepository.currentUser
        .flatMapLatest { authUser ->
            if (authUser != null) {
                repository.getCurrentUser(authUser.uid)
            } else {
                flowOf<UserEntity?>(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    private var _sessionId: String = ""

    private val _attempts = MutableStateFlow<List<SessionAttempt>>(emptyList())
    private val _score = MutableStateFlow(0f)
    private val _session = MutableStateFlow<StudySession?>(null)
    private val _isLoading = MutableStateFlow(true)

    private val _reviewQuestion = MutableStateFlow<ReviewState?>(null)
    val reviewQuestion = _reviewQuestion.asStateFlow()

    private val _isUserGroupAdmin = MutableStateFlow(false)
    val isUserGroupAdmin: StateFlow<Boolean> = _isUserGroupAdmin.asStateFlow()

    val uiState: StateFlow<ResultsUiState> = combine(_attempts, _score, _session, _isLoading) { attempts, score, session, loading ->
        if (loading) ResultsUiState.Loading
        else ResultsUiState.Success(session, attempts, score)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ResultsUiState.Loading)

    fun initSession(id: String) {
        if (_sessionId == id) return
        _sessionId = id
        loadResults()
    }

    private fun loadResults() {
        viewModelScope.launch {
            val sessionVal = repository.getSessionById(_sessionId)
            _session.value = sessionVal
            if (sessionVal != null) {
                val colId = sessionVal.collectionId
                if (colId != null) {
                    val collection = repository.getStudyCollectionByIdOnce(colId)
                    val groupId = collection?.sharedWithGroupId
                    if (groupId != null) {
                        val chat = syncRepository.getChatById(groupId)
                        val currentUid = currentUser.value?.userId ?: authRepository.currentUser.firstOrNull()?.uid
                        _isUserGroupAdmin.value = (chat != null && chat.isAdmin(currentUid ?: ""))
                    } else {
                        _isUserGroupAdmin.value = true
                    }
                } else {
                    _isUserGroupAdmin.value = true
                }
            }
            repository.getAttemptsForSession(_sessionId).collect { attempts ->
                var totalMarks = 0f
                var maxPossibleMarks = 0f
                
                attempts.forEach { att ->
                    totalMarks += att.marksObtained
                    val q = repository.getQuestionById(att.questionId)
                    val qType = q?.questionType?.trim()?.uppercase() ?: "SBA"
                    if (qType == "SBA") {
                        maxPossibleMarks += 4f
                    } else {
                        val optCount = repository.getOptionsForQuestion(att.questionId).size
                        maxPossibleMarks += optCount.toFloat().coerceAtLeast(1f)
                    }
                }

                val scorePercentage = if (maxPossibleMarks > 0) (totalMarks / maxPossibleMarks) * 100 else 0f
                
                _attempts.value = attempts
                _score.value = scorePercentage
                _isLoading.value = false
            }
        }
    }

    fun updateSessionAdminOnly(sessionId: String, isAdminOnly: Boolean) {
        viewModelScope.launch {
            val sessionVal = _session.value ?: return@launch

            // If the session is associated with a collection shared to a group, ensure caller is an admin
            val colId = sessionVal.collectionId
            if (colId != null) {
                val collection = repository.getStudyCollectionByIdOnce(colId)
                val groupId = collection?.sharedWithGroupId
                if (groupId != null) {
                    val chat = syncRepository.getChatById(groupId)
                    val currentUid = currentUser.value?.userId ?: authRepository.currentUser.firstOrNull()?.uid
                    if (chat != null && (currentUid == null || !chat.isAdmin(currentUid))) {
                        return@launch
                    }
                }
            }

            val updated = sessionVal.copy(isAdminOnly = isAdminOnly)
            repository.updateSession(updated)
            _session.value = updated
        }
    }

    fun selectQuestionForReview(attempt: SessionAttempt) {
        viewModelScope.launch {
            val question = repository.getQuestionById(attempt.questionId) ?: return@launch
            val options = repository.getOptionsForQuestion(attempt.questionId)
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