package com.algorithmx.q_base.feature.explore.presentation

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.collections.ProblemReport
import com.algorithmx.q_base.data.collections.StudyCollection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

fun ExploreViewModel.reportCollectionToGroup(collection: StudyCollection, reason: String) {
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

fun ExploreViewModel.reportProblem(index: Int, explanation: String) {
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

fun ExploreViewModel.reportCollection(collection: StudyCollection, reason: String) {
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

fun ExploreViewModel.deleteStudyCollection(collectionId: String) {
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

fun ExploreViewModel.reportSet(setId: String, reason: String) {
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