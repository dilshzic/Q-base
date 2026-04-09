package com.algorithmx.q_base.ui.content_import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.data.collections.*
import com.algorithmx.q_base.data.ai.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class QuestionEditorState(
    val stem: String = "",
    val options: List<Pair<String, String>> = listOf("A" to "", "B" to "", "C" to "", "D" to "", "E" to ""),
    val correctAnswer: String = "A",
    val explanation: String = "",
    val references: String = "",
    val aiSuggestions: String? = null,
    val temporaryAiExplanation: String? = null,
    val isAiLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class QuestionEditorViewModel @Inject constructor(
    private val questionDao: QuestionDao,
    private val collectionDao: CollectionDao,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(QuestionEditorState())
    val state = _state.asStateFlow()

    private var currentQuestionId: String? = null
    private var targetSetId: String? = null

    fun init(questionId: String?, setId: String) {
        currentQuestionId = questionId
        targetSetId = setId
        
        // Reset state
        _state.value = QuestionEditorState()
        
        if (questionId != null) {
            viewModelScope.launch {
                val q = questionDao.getQuestionById(questionId)
                if (q != null) {
                    // Combine flows to update state once
                    val optionsFlow = questionDao.getOptionsForQuestion(questionId)
                    val answerFlow = questionDao.getAnswerForQuestion(questionId)
                    
                    optionsFlow.combine(answerFlow) { opts, ans ->
                        _state.value = _state.value.copy(
                            stem = q.stem ?: "",
                            options = opts.map { (it.optionLetter ?: "") to (it.optionText ?: "") },
                            correctAnswer = ans?.correctAnswerString ?: "A",
                            explanation = ans?.generalExplanation ?: "",
                            references = ans?.references ?: ""
                        )
                    }.collect()
                }
            }
        }
    }

    fun updateStem(stem: String) {
        _state.value = _state.value.copy(stem = stem)
    }

    fun updateOption(letter: String, text: String) {
        val newOptions = _state.value.options.map { 
            if (it.first == letter) it.first to text else it 
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
            val optionsList = currentState.options.joinToString { "${it.first}: ${it.second}" }
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
            val optionsList = currentState.options.joinToString { "${it.first}: ${it.second}" }
            val prompt = """
                Explain the following medical question and why the correct answer is ${currentState.correctAnswer}.
                Stem: ${currentState.stem}
                Options: $optionsList
                
                Provide a high-quality clinical explanation suitable for a medical textbook.
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
            
            val question = Question(
                questionId = qId,
                collection = null, // Will be linked via Set
                category = "General",
                tags = "User Created, Manual",
                questionType = "SBA",
                stem = currentState.stem,
                isPinned = false
            )

            questionDao.insertQuestion(question)
            questionDao.deleteOptionsForQuestion(qId)
            
            val options = currentState.options.map { 
                QuestionOption(
                    optionId = 0,
                    questionId = qId,
                    optionLetter = it.first,
                    optionText = it.second,
                    optionExplanation = null
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
            
            // Link to set if new
            if (currentQuestionId == null) {
                questionDao.insertSetQuestionCrossRef(SetQuestionCrossRef(setId = sId, questionId = qId))
            }

            // Refresh collection timestamp for smart update logic
            val set = questionDao.getSetWithContent(sId)?.set
            set?.parentCollectionId?.let { colId ->
                collectionDao.updateStudyCollectionTimestamp(colId, System.currentTimeMillis())
                android.util.Log.d("QuestionEditorViewModel", "Updated collection $colId timestamp")
            }
            
            _state.value = _state.value.copy(isSaving = false, isSaved = true)
        }
    }
}
