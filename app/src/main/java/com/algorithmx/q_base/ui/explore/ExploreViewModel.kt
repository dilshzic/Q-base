package com.algorithmx.q_base.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.entity.Answer
import com.algorithmx.q_base.data.entity.MasterCategory
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionCollection
import com.algorithmx.q_base.data.entity.QuestionOption
import com.algorithmx.q_base.data.repository.ExploreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExploreViewModel(private val repository: ExploreRepository) : ViewModel() {

    val categories: StateFlow<List<MasterCategory>> = repository.getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _collections = MutableStateFlow<List<QuestionCollection>>(emptyList())
    val collections: StateFlow<List<QuestionCollection>> = _collections.asStateFlow()

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()

    private val _currentOptions = MutableStateFlow<List<QuestionOption>>(emptyList())
    val currentOptions: StateFlow<List<QuestionOption>> = _currentOptions.asStateFlow()

    private val _currentAnswer = MutableStateFlow<Answer?>(null)
    val currentAnswer: StateFlow<Answer?> = _currentAnswer.asStateFlow()

    fun selectCategory(masterCategory: String) {
        viewModelScope.launch {
            repository.getCollectionsByCategory(masterCategory).collect {
                _collections.value = it
            }
        }
    }

    fun selectCollection(category: String) {
        viewModelScope.launch {
            repository.getQuestionsByCategory(category).collect {
                _questions.value = it
            }
        }
    }

    fun selectQuestion(questionId: String) {
        viewModelScope.launch {
            repository.getOptionsForQuestion(questionId).collect {
                _currentOptions.value = it
            }
            repository.getAnswerForQuestion(questionId).collect {
                _currentAnswer.value = it
            }
        }
    }
}
