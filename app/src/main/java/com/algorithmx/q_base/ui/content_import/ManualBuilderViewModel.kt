package com.algorithmx.q_base.ui.content_import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.dao.CollectionDao
import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.entity.Collection as AppCollection
import com.algorithmx.q_base.data.entity.Question
import com.algorithmx.q_base.data.entity.QuestionSet
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ManualBuilderViewModel @Inject constructor(
    private val collectionDao: CollectionDao,
    private val questionDao: QuestionDao
) : ViewModel() {

    private val _targetCollectionId = MutableStateFlow<String?>(null)
    private val _targetSetId = MutableStateFlow<String?>(null)
    private val _questions = MutableStateFlow<List<Question>>(emptyList())
    val questions: StateFlow<List<Question>> = _questions.asStateFlow()

    private val _targetName = MutableStateFlow("Manual Collection")
    val targetName: StateFlow<String> = _targetName.asStateFlow()

    fun initialize(targetId: String?, name: String? = null) {
        viewModelScope.launch {
            if (targetId != null) {
                // 1. Check if it's a Set
                val setWithQuestions = questionDao.getSetWithContent(targetId)
                if (setWithQuestions != null) {
                    _targetSetId.value = targetId
                    _targetName.value = setWithQuestions.set.title
                    _targetCollectionId.value = setWithQuestions.set.parentCollectionId
                    observeQuestions(targetId)
                    return@launch
                }

                // 2. Check if it's a Collection
                _targetCollectionId.value = targetId
                val collection = collectionDao.getCollectionByIdOnce(targetId)
                if (collection != null) {
                    _targetName.value = collection.name
                    // Create a new set for this manual session
                    createNewSet(targetId, "Manual Entry - ${getCurrentDate()}")
                } else {
                    // Fallback to new collection if ID is invalid
                    createNewCollectionAndSet(name)
                }
            } else {
                createNewCollectionAndSet(name)
            }
        }
    }

    private suspend fun createNewCollectionAndSet(name: String? = null) {
        val colId = UUID.randomUUID().toString()
        val colName = name ?: "Manual Collection ${getCurrentDate()}"
        _targetName.value = colName
        
        val collection = AppCollection(
            collectionId = colId,
            name = colName,
            isUserCreated = true
        )
        collectionDao.insertCollections(listOf(collection))
        _targetCollectionId.value = colId
        
        createNewSet(colId, "Initial Set")
    }

    private suspend fun createNewSet(collectionId: String, title: String) {
        val setId = UUID.randomUUID().toString()
        val set = QuestionSet(
            setId = setId,
            title = title,
            parentCollectionId = collectionId,
            isUserCreated = true
        )
        questionDao.insertSet(set)
        _targetSetId.value = setId
        observeQuestions(setId)
    }

    private fun observeQuestions(setId: String) {
        collectionDao.getQuestionsForSet(setId)
            .onEach { _questions.value = it }
            .launchIn(viewModelScope)
    }

    fun getTargetSetId(): String? = _targetSetId.value

    private fun getCurrentDate(): String {
        return SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())
    }
}
