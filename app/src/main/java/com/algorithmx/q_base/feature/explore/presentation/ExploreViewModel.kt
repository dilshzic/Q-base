package com.algorithmx.q_base.feature.explore.presentation
 
import kotlinx.coroutines.ExperimentalCoroutinesApi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.feature.content_import.data.*
import com.algorithmx.q_base.core.data.UserEntity
import com.algorithmx.q_base.feature.sessions.data.StudySession
import com.algorithmx.q_base.core.ai.data.AiRepository
import com.algorithmx.q_base.core.data.auth.AuthRepository
import com.algorithmx.q_base.core.data.chat.isAdmin
import com.algorithmx.q_base.sync.orchestration.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

data class ExploreQuestionState(
    val question: Question,
    val options: List<QuestionOption> = emptyList(),
    val answer: com.algorithmx.q_base.data.collections.Answer? = null,
    val selectedOption: String? = null,
    val isAnswerRevealed: Boolean = false,
    val aiResponse: String? = null,
    val isAiLoading: Boolean = false,
    val isEditable: Boolean = true
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    internal val repository: ExploreRepository,
    internal val aiRepository: AiRepository,
    internal val questionDao: QuestionDao,
    internal val syncRepository: SyncRepository,
    internal val authRepository: AuthRepository,
    internal val configRepository: com.algorithmx.q_base.data.core.ConfigRepository
) : ViewModel() {

    internal val _actionFeedback = MutableSharedFlow<String>()
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

    internal val _collections = MutableStateFlow<List<StudyCollectionWithCount>>(emptyList())
    val collections: StateFlow<List<StudyCollectionWithCount>> = _collections.asStateFlow()

    val personalCollections: StateFlow<List<StudyCollectionWithCount>> = collections.map { list ->
        list.filter { !it.collection.isShared }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sharedCollections: StateFlow<List<StudyCollectionWithCount>> = collections.map { list ->
        list.filter { it.collection.isShared }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    internal val _questionStates = MutableStateFlow<List<ExploreQuestionState>>(emptyList())
    val questionStates: StateFlow<List<ExploreQuestionState>> = _questionStates.asStateFlow()

    internal val _sets = MutableStateFlow<List<QuestionSet>>(emptyList())
    val sets: StateFlow<List<QuestionSet>> = _sets.asStateFlow()

    internal val _selectedCollection = MutableStateFlow<StudyCollection?>(null)
    val selectedCollection: StateFlow<StudyCollection?> = _selectedCollection.asStateFlow()

    internal val _collectionSets = MutableStateFlow<List<QuestionSet>>(emptyList())
    val collectionSets: StateFlow<List<QuestionSet>> = _collectionSets.asStateFlow()

    internal val _lastSession = MutableStateFlow<StudySession?>(null)
    val lastSession: StateFlow<StudySession?> = _lastSession.asStateFlow()

    internal val _sessionsList = MutableStateFlow<List<StudySession>>(emptyList())
    val sessions: StateFlow<List<StudySession>> = _sessionsList.asStateFlow()

    internal val _questionCount = MutableStateFlow(0)
    val questionCount: StateFlow<Int> = _questionCount.asStateFlow()

    internal val _collectionAiResponse = MutableStateFlow<String?>(null)
    val collectionAiResponse = _collectionAiResponse.asStateFlow()

    internal val _isCollectionAiLoading = MutableStateFlow(false)
    val isCollectionAiLoading = _isCollectionAiLoading.asStateFlow()

    // Selection Mode for Library/Sets
    internal val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    internal val _selectedSetIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSetIds = _selectedSetIds.asStateFlow()

    init {
        loadCollections()
        loadSetsAndSessions()
        viewModelScope.launch {
            configRepository.fetchRemoteConfig()
        }
    }

    /**
     * Clears per-screen question state to prevent stale data from flashing
     * when navigating between different Explore-family screens that share
     * this Activity-scoped ViewModel.
     */
    fun resetQuestionStates() {
        _questionStates.value = emptyList()
        _selectedCollection.value = null
        _collectionSets.value = emptyList()
        _lastSession.value = null
        _questionCount.value = 0
        _collectionAiResponse.value = null
        _isCollectionAiLoading.value = false
        _sourceGroupName.value = null
        _isUserGroupAdmin.value = false
        _isSelectionMode.value = false
        _selectedSetIds.value = emptySet()
    }

    internal fun loadSetsAndSessions() {
        viewModelScope.launch {
            repository.getAllSets().collect { _sets.value = it }
        }
        viewModelScope.launch {
            repository.getAllSessions().collect { _sessionsList.value = it }
        }
    }

    internal fun loadCollections() {
        viewModelScope.launch {
            repository.getStudyCollectionsWithCount().collect { _collections.value = it }
        }
    }

    fun loadQuestionsByStudyCollection(collectionId: String) {
        viewModelScope.launch {
            repository.getStudyCollectionById(collectionId).collect { collection ->
                collection?.let {
                    val isEditable = checkIsEditable(it)
                    repository.getQuestionsByStudyCollection(it.name).collect { questions ->
                        val states = questions.map { ExploreQuestionState(it, isEditable = isEditable) }
                        _questionStates.value = states
                        if (states.isNotEmpty()) loadQuestionDetails(0)
                    }
                }
            }
        }
    }

    fun loadQuestionsBySet(setId: String) {
        viewModelScope.launch {
            repository.getQuestionsBySet(setId).collect { questions ->
                val states = questions.map { ExploreQuestionState(it) }
                _questionStates.value = states
                if (states.isNotEmpty()) loadQuestionDetails(0)
            }
        }
    }

    private suspend fun checkIsEditable(collection: StudyCollection?): Boolean {
        if (collection == null) return true
        if (!collection.isAdminOnly) return true
        val groupId = collection.sharedWithGroupId ?: return true
        val chat = syncRepository.getChatById(groupId)
        val currentUid = authRepository.currentUser.first()?.uid
        return chat?.isAdmin(currentUid ?: "") == true
    }

    fun loadPinnedQuestions() {
        viewModelScope.launch {
            repository.getPinnedQuestions().collect { questions ->
                val states = questions.map { ExploreQuestionState(it) }
                _questionStates.value = states
                if (states.isNotEmpty()) loadQuestionDetails(0)
            }
        }
    }

    internal val _isUserGroupAdmin = MutableStateFlow(false)
    val isUserGroupAdmin: StateFlow<Boolean> = _isUserGroupAdmin.asStateFlow()

    internal val _sourceGroupName = MutableStateFlow<String?>(null)
    val sourceGroupName = _sourceGroupName.asStateFlow()

    fun updateCollectionAdminOnly(collectionId: String, isAdminOnly: Boolean) {
        viewModelScope.launch {
            repository.getStudyCollectionByIdOnce(collectionId)?.let { col ->
                // If this collection is shared with a group, ensure caller is an admin
                val groupId = col.sharedWithGroupId
                if (groupId != null) {
                    val chat = syncRepository.getChatById(groupId)
                    val currentUid = authRepository.currentUser.firstOrNull()?.uid
                    if (chat != null && (currentUid == null || !chat.isAdmin(currentUid))) {
                        _actionFeedback.emit("Only a group admin can change collection access settings")
                        return@launch
                    }
                }

                val updated = col.copy(isAdminOnly = isAdminOnly)
                repository.updateStudyCollection(updated)
                _selectedCollection.value = updated
                _actionFeedback.emit("Collection access updated: ${if (isAdminOnly) "Admin-Only" else "Editable & Sharable by Members"}")
            }
        }
    }

    fun loadCollectionOverview(collectionId: String) {
        viewModelScope.launch {
            repository.getStudyCollectionById(collectionId).collect { collection ->
                _selectedCollection.value = collection
                
                // Fetch group name if shared
                collection?.sharedWithGroupId?.let { groupId ->
                    val chat = syncRepository.getChatById(groupId)
                    _sourceGroupName.value = chat?.chatName
                    
                    val currentUid = authRepository.currentUser.firstOrNull()?.uid
                    _isUserGroupAdmin.value = chat == null || chat.isAdmin(currentUid ?: "")
                } ?: run {
                    _sourceGroupName.value = null
                    _isUserGroupAdmin.value = true // Owners have full control over personal sets
                }
            }
        }
        viewModelScope.launch {
            repository.getSetsByStudyCollectionId(collectionId).collect { _collectionSets.value = it }
        }
        viewModelScope.launch {
            repository.getLastSessionForStudyCollection(collectionId).collect { _lastSession.value = it }
        }
        viewModelScope.launch {
            repository.getStudyCollectionById(collectionId).collect { col ->
                col?.name?.let { name ->
                    repository.getQuestionCountByStudyCollection(name).collect { _questionCount.value = it }
                }
            }
        }
    }

    fun updateSessionProgress(sessionId: String, index: Int) {
        viewModelScope.launch {
            repository.getSessionById(sessionId)?.let { session ->
                repository.updateSession(session.copy(lastQuestionIndex = index))
            }
        }
    }

    fun loadQuestionDetails(index: Int) {
        val states = _questionStates.value
        if (index !in states.indices) return
        val state = states[index]
        if (state.options.isNotEmpty()) return

        viewModelScope.launch {
            val options = repository.getOptionsForQuestion(state.question.questionId).first()
            val answer = repository.getAnswerForQuestion(state.question.questionId).first()
            
            _questionStates.update { current ->
                current.mapIndexed { i, s ->
                    if (i == index) s.copy(options = options, answer = answer) else s
                }
            }
        }
    }

    fun selectOption(index: Int, optionLetter: String) {
        _questionStates.update { current ->
            current.mapIndexed { i, s ->
                if (i == index && !s.isAnswerRevealed) {
                    val type = s.question.questionType?.trim()?.uppercase()
                    val newSelection = when (type) {
                        "SBA" -> optionLetter
                        "MTF", "MCQ", "T/F", "MCQ1" -> {
                            val currentSelected = s.selectedOption?.split(",")?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
                            val (letter, _) = if (optionLetter.contains("_")) {
                                optionLetter.split("_")
                            } else {
                                listOf(optionLetter, "")
                            }
                            
                            currentSelected.removeAll { it.startsWith("${letter}_") }
                            if (s.selectedOption?.contains(optionLetter) != true) {
                                currentSelected.add(optionLetter)
                            }
                            currentSelected.sorted().joinToString(",")
                        }
                        else -> { // Other multi-select
                            val currentSelected = s.selectedOption?.split(",")?.filter { it.isNotEmpty() }?.toMutableList() ?: mutableListOf()
                            if (currentSelected.contains(optionLetter)) {
                                currentSelected.remove(optionLetter)
                            } else {
                                currentSelected.add(optionLetter)
                            }
                            currentSelected.sorted().joinToString(",")
                        }
                    }
                    s.copy(selectedOption = newSelection)
                } else s
            }
        }
    }

    fun revealAnswer(index: Int) {
        _questionStates.update { current ->
            current.mapIndexed { i, s ->
                if (i == index) s.copy(isAnswerRevealed = true) else s
            }
        }
    }

    fun togglePin(index: Int) {
        val state = _questionStates.value.getOrNull(index) ?: return
        val updatedQuestion = state.question.copy(isPinned = !state.question.isPinned)
        
        viewModelScope.launch {
            // Verify admin-only restrictions for this question's collection
            val collectionName = state.question.collection
            val collection = if (!collectionName.isNullOrBlank()) repository.getStudyCollectionByNameOnce(collectionName) else null
            if (collection != null && collection.isAdminOnly) {
                val groupId = collection.sharedWithGroupId
                if (groupId != null) {
                    val chat = syncRepository.getChatById(groupId)
                    val currentUid = authRepository.currentUser.firstOrNull()?.uid
                    if (chat == null || currentUid == null || !chat.isAdmin(currentUid)) {
                        _actionFeedback.emit("Only a group admin can modify this collection")
                        return@launch
                    }
                }
            }

            repository.updateQuestion(updatedQuestion)
            _questionStates.update { current ->
                current.mapIndexed { i, s ->
                    if (i == index) s.copy(question = updatedQuestion) else s
                }
            }
        }
    }
}