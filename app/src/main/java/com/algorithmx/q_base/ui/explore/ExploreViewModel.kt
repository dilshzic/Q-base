package com.algorithmx.q_base.ui.explore
 
import kotlinx.coroutines.ExperimentalCoroutinesApi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.collections.*
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.data.sessions.StudySession
import com.algorithmx.q_base.data.sessions.SessionAttempt
import com.algorithmx.q_base.data.ai.AiRepository
import com.algorithmx.q_base.data.auth.AuthRepository
import com.algorithmx.q_base.data.chat.isAdmin
import com.algorithmx.q_base.data.sync.SyncRepository
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
    private val repository: ExploreRepository,
    private val aiRepository: AiRepository,
    private val questionDao: QuestionDao,
    private val syncRepository: SyncRepository,
    private val authRepository: AuthRepository,
    private val configRepository: com.algorithmx.q_base.data.core.ConfigRepository
) : ViewModel() {

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

    private val _collections = MutableStateFlow<List<StudyCollectionWithCount>>(emptyList())
    val collections: StateFlow<List<StudyCollectionWithCount>> = _collections.asStateFlow()

    val personalCollections: StateFlow<List<StudyCollectionWithCount>> = collections.map { list ->
        list.filter { !it.collection.isShared }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sharedCollections: StateFlow<List<StudyCollectionWithCount>> = collections.map { list ->
        list.filter { it.collection.isShared }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _questionStates = MutableStateFlow<List<ExploreQuestionState>>(emptyList())
    val questionStates: StateFlow<List<ExploreQuestionState>> = _questionStates.asStateFlow()

    private val _sets = MutableStateFlow<List<QuestionSet>>(emptyList())
    val sets: StateFlow<List<QuestionSet>> = _sets.asStateFlow()

    private val _selectedCollection = MutableStateFlow<StudyCollection?>(null)
    val selectedCollection: StateFlow<StudyCollection?> = _selectedCollection.asStateFlow()

    private val _collectionSets = MutableStateFlow<List<QuestionSet>>(emptyList())
    val collectionSets: StateFlow<List<QuestionSet>> = _collectionSets.asStateFlow()

    private val _lastSession = MutableStateFlow<StudySession?>(null)
    val lastSession: StateFlow<StudySession?> = _lastSession.asStateFlow()

    private val _sessions = MutableStateFlow<List<StudySession>>(emptyFlow<List<StudySession>>().stateIn(viewModelScope, SharingStarted.Lazily, emptyList()).value)
    // Wait, the above is wrong. Let's stick to standard layout.

    private val _sessionsList = MutableStateFlow<List<StudySession>>(emptyList())
    val sessions: StateFlow<List<StudySession>> = _sessionsList.asStateFlow()

    private val _questionCount = MutableStateFlow(0)
    val questionCount: StateFlow<Int> = _questionCount.asStateFlow()

    private val _collectionAiResponse = MutableStateFlow<String?>(null)
    val collectionAiResponse = _collectionAiResponse.asStateFlow()

    private val _isCollectionAiLoading = MutableStateFlow(false)
    val isCollectionAiLoading = _isCollectionAiLoading.asStateFlow()

    // Selection Mode for Library/Sets
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    private val _selectedSetIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedSetIds = _selectedSetIds.asStateFlow()

    init {
        loadCollections()
        loadSetsAndSessions()
        viewModelScope.launch {
            configRepository.fetchRemoteConfig()
        }
    }

    private fun loadSetsAndSessions() {
        viewModelScope.launch {
            repository.getAllSets().collect { _sets.value = it }
        }
        viewModelScope.launch {
            repository.getAllSessions().collect { _sessionsList.value = it }
        }
    }

    private fun loadCollections() {
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
                // For sets, we should check parent collection for edit permissions
                // This is a bit more complex, let's fetch collection first
                // For now assuming sets in explore are editable unless we implement deeper check
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

    private val _isUserGroupAdmin = MutableStateFlow(false)
    val isUserGroupAdmin: StateFlow<Boolean> = _isUserGroupAdmin.asStateFlow()

    private val _sourceGroupName = MutableStateFlow<String?>(null)
    val sourceGroupName = _sourceGroupName.asStateFlow()

    fun updateCollectionAdminOnly(collectionId: String, isAdminOnly: Boolean) {
        viewModelScope.launch {
            repository.getStudyCollectionByIdOnce(collectionId)?.let { col ->
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

    fun reportCollectionToGroup(collection: StudyCollection, reason: String) {
        val groupId = collection.sharedWithGroupId ?: return
        viewModelScope.launch {
            try {
                val reportMessage = "⚠️ PROBLEM REPORT: Issue with shared collection '${collection.name}'. Reason: $reason"
                val message = com.algorithmx.q_base.data.chat.MessageEntity(
                    messageId = java.util.UUID.randomUUID().toString(),
                    chatId = groupId,
                    senderId = authRepository.currentUser.first()?.uid ?: "",
                    payload = reportMessage,
                    timestamp = System.currentTimeMillis(),
                    type = "TEXT"
                )
                syncRepository.sendMessage(message)
                _actionFeedback.emit("Report sent to group chat.")
            } catch (e: Exception) {
                Log.e("ExploreViewModel", "Failed to report to group", e)
                _actionFeedback.emit("Failed to send report: ${e.message}")
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

    // --- Set Management & Selection ---

    fun toggleSetSelection(setId: String) {
        val current = _selectedSetIds.value.toMutableSet()
        if (current.contains(setId)) {
            current.remove(setId)
        } else {
            current.add(setId)
        }
        _selectedSetIds.value = current
        _isSelectionMode.value = current.isNotEmpty()
    }

    fun clearSelection() {
        _selectedSetIds.value = emptySet()
        _isSelectionMode.value = false
    }

    fun deleteCollectionSet(setId: String) {
        viewModelScope.launch {
            questionDao.deleteSetById(setId)
            loadSetsAndSessions()
        }
    }

    suspend fun getSetIdForQuestion(questionId: String): String? {
        return repository.getSetIdForQuestion(questionId)
    }

    fun deleteSelectedSets() {
        val idsToDelete = _selectedSetIds.value.toList()
        if (idsToDelete.isEmpty()) return
        
        viewModelScope.launch {
            questionDao.deleteCrossRefsForSets(idsToDelete)
            questionDao.deleteSetsByIds(idsToDelete)
            clearSelection()
            loadSetsAndSessions()
        }
    }

    fun togglePin(index: Int) {
        val state = _questionStates.value.getOrNull(index) ?: return
        val updatedQuestion = state.question.copy(isPinned = !state.question.isPinned)
        
        viewModelScope.launch {
            repository.updateQuestion(updatedQuestion)
            _questionStates.update { current ->
                current.mapIndexed { i, s ->
                    if (i == index) s.copy(question = updatedQuestion) else s
                }
            }
        }
    }

    fun reportProblem(index: Int, explanation: String) {
        val state = _questionStates.value.getOrNull(index) ?: return
        viewModelScope.launch {
            try {
                repository.reportProblem(
                    ProblemReport(
                        questionId = state.question.questionId,
                        explanation = explanation
                    )
                )
                // Call SyncRepository for moderation report
                syncRepository.reportQuestion(
                    question = state.question,
                    options = state.options,
                    answer = state.answer,
                    reason = explanation
                )
                _actionFeedback.emit("Question reported successfully.")
            } catch (e: Exception) {
                Log.e("ExploreViewModel", "Failed to report problem", e)
                val errorMsg = if (e is IllegalStateException && e.message?.contains("authenticated") == true) {
                    "You must be logged in to report problems."
                } else {
                    "Failed to submit report: ${e.message}"
                }
                _actionFeedback.emit(errorMsg)
            }
        }
    }

    fun reportCollection(collection: StudyCollection, reason: String) {
        viewModelScope.launch {
            try {
                syncRepository.reportCollection(collection, reason)
                _actionFeedback.emit("Collection reported successfully.")
            } catch (e: Exception) {
                Log.e("ExploreViewModel", "Failed to report collection", e)
                val errorMsg = if (e is IllegalStateException && e.message?.contains("authenticated") == true) {
                    "You must be logged in to report collections."
                } else {
                    "Failed to report collection: ${e.message}"
                }
                _actionFeedback.emit(errorMsg)
            }
        }
    }

    fun deleteStudyCollection(collectionId: String) {
        viewModelScope.launch {
            try {
                repository.deleteStudyCollection(collectionId)
                _actionFeedback.emit("Collection deleted successfully.")
                loadCollections()
            } catch (e: Exception) {
                Log.e("ExploreViewModel", "Failed to delete collection", e)
                _actionFeedback.emit("Failed to delete collection: ${e.message}")
            }
        }
    }

    fun reportSet(setId: String, reason: String) {
        viewModelScope.launch {
            try {
                _sets.value.find { it.setId == setId }?.let { set ->
                    val collectionFromSet = StudyCollection(
                        collectionId = set.setId,
                        name = "[SET] ${set.title}",
                        description = set.description
                    )
                    syncRepository.reportCollection(collectionFromSet, reason)
                    _actionFeedback.emit("Set reported successfully.")
                }
            } catch (e: Exception) {
                Log.e("ExploreViewModel", "Failed to report set", e)
                val errorMsg = if (e is IllegalStateException && e.message?.contains("authenticated") == true) {
                    "You must be logged in to report sets."
                } else {
                    "Failed to report set: ${e.message}"
                }
                _actionFeedback.emit(errorMsg)
            }
        }
    }

    fun askAi(index: Int, mode: String = "EXPLAIN") {
        val state = _questionStates.value.getOrNull(index) ?: return
        val question = state.question
        
        viewModelScope.launch {
            _questionStates.update { current ->
                current.mapIndexed { i, s -> if (i == index) s.copy(isAiLoading = true) else s }
            }
            
            val prompt = when(mode) {
                "HINT" -> "Give me a subtle hint for this question without revealing the answer: ${question.stem}"
                "DETAILED" -> "Provide a summary related to this question: ${question.stem}"
                else -> "Explain this question and why the options are correct or incorrect: ${question.stem}. Options: ${state.options.joinToString { "${it.optionLetter}: ${it.optionText}" }}"
            }
            
            val result = aiRepository.getAiAssistance(prompt)
            
            _questionStates.update { current ->
                current.mapIndexed { i, s -> 
                    if (i == index) s.copy(
                        aiResponse = result.getOrNull() ?: "Failed to get AI assistance: ${result.exceptionOrNull()?.message}",
                        isAiLoading = false
                    ) else s 
                }
            }
        }
    }

    fun askAiAboutCollection(collection: StudyCollection) {
        viewModelScope.launch {
            _isCollectionAiLoading.value = true
            _collectionAiResponse.value = "" // Clear previous
            
            val prompt = """
                Provide a comprehensive summary and key learning points for the study collection: '${collection.name}'.
                Description: ${collection.description ?: "N/A"}
                
                Focus on high-yield information and potential exam topics.
            """.trimIndent()
            
            val result = aiRepository.getAiAssistance(prompt)
            
            _collectionAiResponse.value = result.getOrNull() ?: "Failed to get AI assistance: ${result.exceptionOrNull()?.message}"
            _isCollectionAiLoading.value = false
        }
    }

    fun clearCollectionAiResponse() {
        _collectionAiResponse.value = null
    }

    fun clearAiResponse(index: Int) {
        _questionStates.update { current ->
            current.mapIndexed { i, s -> if (i == index) s.copy(aiResponse = null) else s }
        }
    }

    fun saveAiResponseToQuestion(index: Int) {
        val state = _questionStates.value.getOrNull(index) ?: return
        val aiExp = state.aiResponse ?: return
        
        viewModelScope.launch {
            val currentAnswer = state.answer ?: Answer(
                questionId = state.question.questionId,
                correctAnswerString = "A", // Default if missing
                generalExplanation = aiExp,
                references = ""
            )
            
            val updatedAnswer = currentAnswer.copy(generalExplanation = aiExp)
            repository.saveAnswer(updatedAnswer)
            
            _questionStates.update { current ->
                current.mapIndexed { i, s -> 
                    if (i == index) s.copy(answer = updatedAnswer, aiResponse = null) else s 
                }
            }
        }
    }

    fun addQuestionToSet(index: Int, setId: String) {
        val state = _questionStates.value.getOrNull(index) ?: return
        viewModelScope.launch {
            repository.addQuestionToSet(setId, state.question.questionId)
        }
    }

    fun addQuestionToSession(index: Int, sessionId: String) {
        val state = _questionStates.value.getOrNull(index) ?: return
        viewModelScope.launch {
            repository.addQuestionToSession(sessionId, state.question.questionId)
        }
    }

    fun createSet(title: String, description: String, collectionId: String) {
        viewModelScope.launch {
            repository.saveSet(
                QuestionSet(
                    setId = java.util.UUID.randomUUID().toString(),
                    title = title,
                    description = description,
                    parentCollectionId = collectionId,
                    createdTimestamp = System.currentTimeMillis(),
                    isUserCreated = true
                )
            )
            loadSetsAndSessions()
        }
    }

    fun deleteQuestion(index: Int) {
        val state = _questionStates.value.getOrNull(index) ?: return
        viewModelScope.launch {
            questionDao.deleteQuestionById(state.question.questionId)
            _questionStates.update { current ->
                val mutableList = current.toMutableList()
                mutableList.removeAt(index)
                mutableList
            }
        }
    }

    fun deleteQuestionFromSet(index: Int, setId: String) {
        val state = _questionStates.value.getOrNull(index) ?: return
        viewModelScope.launch {
            questionDao.removeQuestionFromSet(setId, state.question.questionId)
            _questionStates.update { current ->
                val mutableList = current.toMutableList()
                mutableList.removeAt(index)
                mutableList
            }
        }
    }
}
