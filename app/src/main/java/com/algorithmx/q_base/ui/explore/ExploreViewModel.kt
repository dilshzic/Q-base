package com.algorithmx.q_base.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.entity.*
import com.algorithmx.q_base.data.repository.ExploreRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExploreQuestionState(
    val question: Question,
    val options: List<QuestionOption> = emptyList(),
    val answer: Answer? = null,
    val selectedOption: String? = null,
    val isAnswerRevealed: Boolean = false
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: ExploreRepository
) : ViewModel() {

    private val _categories = MutableStateFlow<List<MasterCategory>>(emptyList())
    val categories: StateFlow<List<MasterCategory>> = _categories.asStateFlow()

    private val _questionStates = MutableStateFlow<List<ExploreQuestionState>>(emptyList())
    val questionStates: StateFlow<List<ExploreQuestionState>> = _questionStates.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            repository.getAllCategories().collect { _categories.value = it }
        }
    }

    fun loadQuestionsByMasterCategory(categoryName: String) {
        viewModelScope.launch {
            repository.getQuestionsByMasterCategory(categoryName).collect { questions ->
                val states = questions.map { ExploreQuestionState(it) }
                _questionStates.value = states
                
                if (states.isNotEmpty()) {
                    loadQuestionDetails(0)
                }
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
            repository.reportProblem(
                ProblemReport(
                    questionId = state.question.questionId,
                    explanation = explanation
                )
            )
        }
    }
}
