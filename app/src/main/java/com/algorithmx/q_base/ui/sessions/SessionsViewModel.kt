package com.algorithmx.q_base.ui.sessions
 
import kotlinx.coroutines.ExperimentalCoroutinesApi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.entity.Collection as AppCollection
import com.algorithmx.q_base.data.entity.StudySession
import com.algorithmx.q_base.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.algorithmx.q_base.data.repository.AuthRepository
import com.algorithmx.q_base.data.dao.UserDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.algorithmx.q_base.data.entity.UserEntity

@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val repository: SessionRepository,
    private val authRepository: AuthRepository,
    private val userDao: UserDao,
    private val syncRepository: com.algorithmx.q_base.data.repository.SyncRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val currentUser: StateFlow<UserEntity?> = authRepository.currentUser
        .flatMapLatest { firebaseUser ->
            if (firebaseUser != null) {
                userDao.getCurrentUser(firebaseUser.uid)
            } else {
                flowOf<UserEntity?>(null)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    private val _selectedSessionIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSessionIds = _selectedSessionIds.asStateFlow()

    val sessions: StateFlow<List<StudySession>> = combine(
        repository.getAllSessions(),
        _searchQuery
    ) { sessionsList: List<StudySession>, query: String ->
        if (query.isBlank()) {
            sessionsList
        } else {
            sessionsList.filter { it.title.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val collections: StateFlow<List<AppCollection>> = combine(
        repository.getAllCollections(),
        _searchQuery
    ) { collectionsList: List<AppCollection>, query: String ->
        if (query.isBlank()) {
            collectionsList
        } else {
            collectionsList.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sessionCreated = MutableSharedFlow<String>()
    val sessionCreated = _sessionCreated.asSharedFlow()

    // Wizard State
    private val _wizardStep = MutableStateFlow(1) // 1: Collection, 2: Questions, 3: Config
    val wizardStep = _wizardStep.asStateFlow()

    private val _selectedCollection = MutableStateFlow<String?>(null)
    val selectedCollection = _selectedCollection.asStateFlow()

    private val _availableQuestions = MutableStateFlow<List<com.algorithmx.q_base.data.entity.Question>>(emptyList())
    val availableQuestions = _availableQuestions.asStateFlow()

    private val _selectedQuestionIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedQuestionIds = _selectedQuestionIds.asStateFlow()

    private val _sessionOrder = MutableStateFlow("SEQUENTIAL") // "RANDOM", "SEQUENTIAL"
    val sessionOrder = _sessionOrder.asStateFlow()

    private val _timingType = MutableStateFlow("NONE") // "TOTAL", "PER_QUESTION", "NONE"
    val timingType = _timingType.asStateFlow()

    private val _timeLimitSeconds = MutableStateFlow(300) // 5 mins default
    val timeLimitSeconds = _timeLimitSeconds.asStateFlow()

    private val _lastRandomCount = MutableStateFlow<Int?>(null)
    val lastRandomCount = _lastRandomCount.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setWizardStep(step: Int) {
        _wizardStep.value = step
    }

    fun selectCollection(name: String) {
        _selectedCollection.value = name
        viewModelScope.launch {
            repository.getQuestionsByCollection(name).collect { questions ->
                _availableQuestions.value = questions
            }
        }
        _wizardStep.value = 2
    }

    fun toggleQuestionSelection(id: String) {
        val current = _selectedQuestionIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedQuestionIds.value = current
        _lastRandomCount.value = null
    }

    fun selectAllQuestions() {
        _selectedQuestionIds.value = _availableQuestions.value.map { it.questionId }.toSet()
    }

    fun deselectAllQuestions() {
        _selectedQuestionIds.value = emptySet()
    }

    fun selectRandomQuestions(count: Int) {
        val questions = _availableQuestions.value
        if (questions.isEmpty()) return
        val actualCount = count.coerceIn(1, questions.size)
        _selectedQuestionIds.value = questions.shuffled().take(actualCount).map { it.questionId }.toSet()
        _lastRandomCount.value = count
    }

    fun setOrder(order: String) { _sessionOrder.value = order }
    fun setTimingType(type: String) { _timingType.value = type }
    fun setTimeLimit(seconds: Int) { _timeLimitSeconds.value = seconds }

    fun launchSession(title: String) {
        viewModelScope.launch {
            val sessionId = repository.createNewSession(
                title = title,
                questionIds = _selectedQuestionIds.value.toList(),
                timeLimitSeconds = if (_timingType.value != "NONE") _timeLimitSeconds.value else null,
                timingType = _timingType.value,
                isRandom = _sessionOrder.value == "RANDOM"
            )
            _sessionCreated.emit(sessionId)
            resetWizard()
        }
    }

    fun resetWizard() {
        _wizardStep.value = 1
        _selectedCollection.value = null
        _selectedQuestionIds.value = emptySet()
        _availableQuestions.value = emptyList()
        _sessionOrder.value = "SEQUENTIAL"
        _timingType.value = "NONE"
        _lastRandomCount.value = null
    }

    fun toggleSessionSelection(sessionId: String) {
        val current = _selectedSessionIds.value.toMutableSet()
        if (current.contains(sessionId)) {
            current.remove(sessionId)
        } else {
            current.add(sessionId)
        }
        _selectedSessionIds.value = current
        _isSelectionMode.value = current.isNotEmpty()
    }

    fun clearSessionSelection() {
        _selectedSessionIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun deleteSelectedSessions() {
        val idsToDelete = _selectedSessionIds.value.toList()
        if (idsToDelete.isEmpty()) return
        
        viewModelScope.launch {
            repository.deleteSessions(idsToDelete)
            clearSessionSelection()
        }
    }

    fun reportSession(sessionId: String, reason: String) {
        viewModelScope.launch {
            syncRepository.reportSession(sessionId, reason)
        }
    }

    // Deprecated but kept for compatibility during migration if needed
    fun createSession(categoryName: String, questionCount: Int, isTimed: Boolean) {
        viewModelScope.launch {
            val timeLimit = if (isTimed) 3600 else null
            val sessionId = repository.createNewSessionSmart(categoryName, questionCount, timeLimit)
            _sessionCreated.emit(sessionId)
        }
    }
}
