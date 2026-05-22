package com.algorithmx.q_base.feature.explore.presentation

import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.feature.content_import.data.QuestionSet
import com.algorithmx.q_base.core.data.chat.ChatEntity
import com.algorithmx.q_base.core.data.chat.isAdmin
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun ExploreViewModel.toggleSetSelection(setId: String) {
    val current = _selectedSetIds.value.toMutableSet()
    if (current.contains(setId)) {
        current.remove(setId)
    } else {
        current.add(setId)
    }
    _selectedSetIds.value = current
    _isSelectionMode.value = current.isNotEmpty()
}

fun ExploreViewModel.clearSelection() {
    _selectedSetIds.value = emptySet()
    _isSelectionMode.value = false
}

fun ExploreViewModel.deleteCollectionSet(setId: String) {
    viewModelScope.launch {
        questionDao.deleteSetById(setId)
        loadSetsAndSessions()
    }
}

suspend fun ExploreViewModel.getSetIdForQuestion(questionId: String): String? {
    return repository.getSetIdForQuestion(questionId)
}

fun ExploreViewModel.deleteSelectedSets() {
    val idsToDelete = _selectedSetIds.value.toList()
    if (idsToDelete.isEmpty()) return
    
    viewModelScope.launch {
        questionDao.deleteCrossRefsForSets(idsToDelete)
        questionDao.deleteSetsByIds(idsToDelete)
        clearSelection()
        loadSetsAndSessions()
    }
}

fun ExploreViewModel.addQuestionToSet(index: Int, setId: String) {
    val state = _questionStates.value.getOrNull(index) ?: return
    viewModelScope.launch {
        // Verify admin-only restrictions for this question's collection
        val collectionName = state.question.collection
        val collection = if (!collectionName.isNullOrBlank()) repository.getStudyCollectionByNameOnce(collectionName) else null
        if (collection != null && collection.isAdminOnly) {
            val groupId = collection.sharedWithGroupId
            if (groupId != null) {
                val chat = syncRepository.getChatById(groupId)
                val currentUid = authRepository.currentUser.firstOrNull()?.uid
                if (chat == null || currentUid == null || !chat.adminIds.contains(currentUid)) {
                    _actionFeedback.emit("Only a group admin can modify this collection")
                    return@launch
                }
            }
        }

        repository.addQuestionToSet(setId, state.question.questionId)
    }
}

fun ExploreViewModel.addQuestionToSession(index: Int, sessionId: String) {
    val state = _questionStates.value.getOrNull(index) ?: return
    viewModelScope.launch {
        // Verify admin-only restrictions for this question's collection
        val collectionName = state.question.collection
        val collection = if (!collectionName.isNullOrBlank()) repository.getStudyCollectionByNameOnce(collectionName) else null
        if (collection != null && collection.isAdminOnly) {
            val groupId = collection.sharedWithGroupId
            if (groupId != null) {
                val chat = syncRepository.getChatById(groupId)
                val currentUid = authRepository.currentUser.firstOrNull()?.uid
                if (chat == null || currentUid == null || !chat.adminIds.contains(currentUid)) {
                    _actionFeedback.emit("Only a group admin can modify this collection")
                    return@launch
                }
            }
        }

        repository.addQuestionToSession(sessionId, state.question.questionId)
    }
}

fun ExploreViewModel.createSet(title: String, description: String, collectionId: String) {
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

fun ExploreViewModel.deleteQuestion(index: Int) {
    val state = _questionStates.value.getOrNull(index) ?: return
    viewModelScope.launch {
        // Verify admin-only restrictions for this question's collection
        val collectionName = state.question.collection
        val collection = if (!collectionName.isNullOrBlank()) repository.getStudyCollectionByNameOnce(collectionName) else null
        if (collection != null && collection.isAdminOnly) {
            val groupId = collection.sharedWithGroupId
            if (groupId != null) {
                val chat = syncRepository.getChatById(groupId)
                val currentUid = authRepository.currentUser.firstOrNull()?.uid
                if (chat == null || currentUid == null || !chat.adminIds.contains(currentUid)) {
                    _actionFeedback.emit("Only a group admin can modify this collection")
                    return@launch
                }
            }
        }

        questionDao.deleteQuestionById(state.question.questionId)
        _questionStates.update { current ->
            val mutableList = current.toMutableList()
            mutableList.removeAt(index)
            mutableList
        }
    }
}

fun ExploreViewModel.deleteQuestionFromSet(index: Int, setId: String) {
    val state = _questionStates.value.getOrNull(index) ?: return
    viewModelScope.launch {
        // Verify admin-only restrictions for this question's collection
        val collectionName = state.question.collection
        val collection = if (!collectionName.isNullOrBlank()) repository.getStudyCollectionByNameOnce(collectionName) else null
        if (collection != null && collection.isAdminOnly) {
            val groupId = collection.sharedWithGroupId
            if (groupId != null) {
                val chat = syncRepository.getChatById(groupId)
                val currentUid = authRepository.currentUser.firstOrNull()?.uid
                if (chat == null || currentUid == null || !chat.adminIds.contains(currentUid)) {
                    _actionFeedback.emit("Only a group admin can modify this collection")
                    return@launch
                }
            }
        }

        questionDao.removeQuestionFromSet(setId, state.question.questionId)
        _questionStates.update { current ->
            val mutableList = current.toMutableList()
            mutableList.removeAt(index)
            mutableList
        }
    }
}