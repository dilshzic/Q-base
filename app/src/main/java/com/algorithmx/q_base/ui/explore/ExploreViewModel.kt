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

    // Distinct subjects inside the selected collection
    private val _subjects = MutableStateFlow<List<String>>(emptyList())
    val subjects: StateFlow<List<String>> = _subjects.asStateFlow()

    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()

    private val _currentOptions = MutableStateFlow<List<QuestionOption>>(emptyList())
    val currentOptions: StateFlow<List<QuestionOption>> = _currentOptions.asStateFlow()

    private val _currentAnswer = MutableStateFlow<Answer?>(null)
    val currentAnswer: StateFlow<Answer?> = _currentAnswer.asStateFlow()

    /** Called with the UUID of the clicked Master Category. */
    fun selectCategory(masterCategoryId: String) {
        viewModelScope.launch {
            repository.getCollectionsByMasterCategoryId(masterCategoryId).collect {
                _collections.value = it
            }
        }
    }

    /** Called with the UUID of the clicked Collection. Loads subjects via crossref join. */
    fun selectCollection(collectionId: String) {
        viewModelScope.launch {
            repository.getQuestionsForCollection(collectionId).collect { questions ->
                _subjects.value = questions
                    .mapNotNull { it.subject?.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .sorted()
            }
        }
    }

    /**
     * Called with the collection title (category) and subject name.
     * Questions are still queried by the string fields on the Question entity.
     */
    fun selectSubject(category: String, subject: String) {
        viewModelScope.launch {
            repository.getQuestionsByCategoryAndSubject(category, subject).collect {
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

