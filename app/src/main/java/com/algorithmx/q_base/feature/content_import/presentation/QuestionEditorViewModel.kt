package com.algorithmx.q_base.feature.content_import.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.data.collections.*
import com.algorithmx.q_base.core.ai.data.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import com.algorithmx.q_base.sync.orchestration.SyncRepository
import org.json.JSONObject

data class OptionState(
    val letter: String,
    val text: String,
    val explanation: String = ""
)

data class QuestionEditorState(
    val stem: String = "",
    val options: List<OptionState> = listOf(
        OptionState("A", ""), OptionState("B", ""), OptionState("C", ""), OptionState("D", ""), OptionState("E", "")
    ),
    val correctAnswer: String = "A",
    val explanation: String = "",
    val references: String = "",
    val aiSuggestions: String? = null,
    // TODO: REMOVE_IN_PHASE6 - temporary AI preview field. Replace with
    // a stable field name or remove after AI feature finalization and UI
    // flow consolidation.
    val temporaryAiExplanation: String? = null,
    val isAiLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class QuestionEditorViewModel @Inject constructor(
    private val questionDao: QuestionDao,
    private val collectionDao: CollectionDao,
    private val aiRepository: AiRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _state = MutableStateFlow(QuestionEditorState())
    val state = _state.asStateFlow()

    private var currentQuestionId: String? = null
    private var targetSetId: String? = null
    private var isInitialized = false

    fun init(questionId: String?, setId: String) {
        if (isInitialized && currentQuestionId == questionId && targetSetId == setId) return
        
        isInitialized = true
        currentQuestionId = questionId
        targetSetId = setId
        
        // Reset state
        _state.value = QuestionEditorState()
        
        if (questionId != null) {
            viewModelScope.launch {
                val q = questionDao.getQuestionById(questionId)
                if (q != null) {
                    val opts = questionDao.getOptionsForQuestionOnce(questionId)
                    val ans = questionDao.getAnswerForQuestionOnce(questionId)
                    
                    _state.value = _state.value.copy(
                        stem = q.stem,
                        options = opts.map { OptionState(it.optionLetter, it.optionText ?: "", it.optionExplanation ?: "") },
                        correctAnswer = ans?.correctAnswerString ?: "A",
                        explanation = ans?.generalExplanation ?: "",
                        references = ans?.references ?: ""
                    )
                }
            }
        }
    }

    fun updateStem(stem: String) {
        _state.value = _state.value.copy(stem = stem)
    }

    fun updateOption(letter: String, text: String) {
        val newOptions = _state.value.options.map { 
            if (it.letter == letter) it.copy(text = text) else it 
        }
        _state.value = _state.value.copy(options = newOptions)
    }

    fun updateOptionExplanation(letter: String, explanation: String) {
        val newOptions = _state.value.options.map { 
            if (it.letter == letter) it.copy(explanation = explanation) else it 
        }
        _state.value = _state.value.copy(options = newOptions)
    }

    fun updateCorrectAnswer(answer: String) {
        _state.value = _state.value.copy(correctAnswer = answer)
    }

    fun updateExplanation(explanation: String) {
        _state.value = _state.value.copy(explanation = explanation)
    }

    fun updateReferences(refs: String) {
        _state.value = _state.value.copy(references = refs)
    }

    fun getAiAssist() {
        val currentState = _state.value
        _state.value = currentState.copy(isAiLoading = true)
        viewModelScope.launch {
            val optionsList = currentState.options.joinToString { "${it.letter}: ${it.text}" }
            val result = aiRepository.assistQuestionEditing(currentState.stem, optionsList)
            if (result.isSuccess) {
                _state.value = _state.value.copy(aiSuggestions = result.getOrNull(), isAiLoading = false)
            } else {
                _state.value = _state.value.copy(isAiLoading = false)
            }
        }
    }

    fun generateAiExplanation() {
        val currentState = _state.value
        _state.value = currentState.copy(isAiLoading = true)
        viewModelScope.launch {
            val optionsList = currentState.options.joinToString { "${it.letter}: ${it.text}" }
            val prompt = """
                Explain the following question and why the correct answer is ${currentState.correctAnswer}.
                Stem: ${currentState.stem}
                Options: $optionsList
                
                Provide a high-quality detailed explanation suitable for a educational textbook.
            """.trimIndent()
            
            val result = aiRepository.getAiAssistance(prompt)
            if (result.isSuccess) {
                _state.value = _state.value.copy(temporaryAiExplanation = result.getOrNull(), isAiLoading = false)
            } else {
                _state.value = _state.value.copy(isAiLoading = false)
            }
        }
    }

    fun applyAiExplanation() {
        val aiExp = _state.value.temporaryAiExplanation ?: return
        _state.value = _state.value.copy(
            explanation = aiExp,
            temporaryAiExplanation = null
        )
    }

    fun discardAiExplanation() {
        _state.value = _state.value.copy(temporaryAiExplanation = null)
    }

    fun clearAiSuggestions() {
        _state.value = _state.value.copy(aiSuggestions = null)
    }

    fun saveQuestion() {
        val currentState = _state.value
        _state.value = currentState.copy(isSaving = true)
        
        viewModelScope.launch {
            val qId = currentQuestionId ?: UUID.randomUUID().toString()
            val sId = targetSetId ?: return@launch
            
            val existingQuestion = if (currentQuestionId != null) {
                questionDao.getQuestionById(currentQuestionId!!)
            } else null
            
            val set = questionDao.getSetWithContent(sId)?.set
            var colName: String? = null
            set?.parentCollectionId?.let { colId ->
                colName = collectionDao.getStudyCollectionByIdOnce(colId)?.name
            }

            val question = existingQuestion?.copy(
                stem = currentState.stem,
                collection = existingQuestion.collection ?: colName
            ) ?: Question(
                questionId = qId,
                collection = colName,
                category = "General",
                tags = "User Created, Manual",
                questionType = "SBA",
                stem = currentState.stem,
                isPinned = false
            )

            if (currentQuestionId != null) {
                questionDao.updateQuestion(question)
            } else {
                questionDao.insertQuestion(question)
                questionDao.insertSetQuestionCrossRef(SetQuestionCrossRef(setId = sId, questionId = qId))
            }

            questionDao.deleteOptionsForQuestion(qId)
            
            val options = currentState.options.map { 
                QuestionOption(
                    questionId = qId,
                    optionLetter = it.letter,
                    optionText = it.text,
                    optionExplanation = it.explanation.takeIf { exp -> exp.isNotBlank() }
                )
            }
            questionDao.insertOptions(options)
            
            val answer = Answer(
                questionId = qId,
                correctAnswerString = currentState.correctAnswer,
                generalExplanation = currentState.explanation,
                references = currentState.references
            )
            questionDao.insertAnswer(answer)
            
            // Refresh collection timestamp for smart update logic
            set?.parentCollectionId?.let { colId ->
                val col = collectionDao.getStudyCollectionByIdOnce(colId)
                if (col != null) {
                    collectionDao.updateStudyCollectionTimestamp(colId, System.currentTimeMillis())
                    android.util.Log.d("QuestionEditorViewModel", "Updated collection $colId timestamp")
                    
                    if (col.sharedWithGroupId != null) {
                        try {
                            val data = JSONObject().apply {
                                put("id", qId)
                                put("setId", sId)
                                put("collectionName", col.name)
                                put("text", currentState.stem)
                                put("category", "General")
                                put("tags", "User Created, Manual")
                                
                                val optsArray = org.json.JSONArray()
                                currentState.options.forEach { optsArray.put(it.text) }
                                put("options", optsArray)
                                
                                put("correctAnswer", currentState.correctAnswer)
                                put("explanation", currentState.explanation)
                                put("references", currentState.references)
                            }
                            syncRepository.sendCollectionPatch(col.sharedWithGroupId, colId, "UPSERT_QUESTION", data)
                        } catch (e: Exception) {
                            android.util.Log.e("QuestionEditorViewModel", "Failed to send UPSERT_QUESTION patch", e)
                        }
                    }
                }
            }
            
            _state.value = _state.value.copy(isSaving = false, isSaved = true)
        }
    }
}