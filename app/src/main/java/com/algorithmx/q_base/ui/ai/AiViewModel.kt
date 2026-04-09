package com.algorithmx.q_base.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.ai.AiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.algorithmx.q_base.core_ai.brain.models.AiCollectionResponse
import com.algorithmx.q_base.data.ai.AiResponseEntity
import kotlinx.serialization.json.Json
import javax.inject.Inject

sealed class AiUiState {
    object Idle : AiUiState()
    object Loading : AiUiState()
    data class Success(val message: String) : AiUiState()
    data class Error(val message: String) : AiUiState()
}

@HiltViewModel
class AiViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    private val _currentResponse = MutableStateFlow<AiCollectionResponse?>(null)
    val currentResponse: StateFlow<AiCollectionResponse?> = _currentResponse.asStateFlow()

    private var lastResponseId: String? = null
    private val json = Json { ignoreUnknownKeys = true }

    fun generateCollection(
        topic: String,
        count: Int,
        type: String,
        collectionId: String,
        collectionName: String
    ) {
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            val result = aiRepository.generateCollection(
                topic = topic,
                count = count,
                type = type,
                collectionId = collectionId,
                collectionName = collectionName
            )
            
            if (result.isSuccess) {
                val responseId = result.getOrThrow()
                lastResponseId = responseId
                
                // Fetch for preview
                val aiResponseEntity = aiRepository.getAiResponseById(responseId)
                if (aiResponseEntity != null) {
                    _currentResponse.value = json.decodeFromString<AiCollectionResponse>(aiResponseEntity.rawJson)
                    _uiState.value = AiUiState.Success("Collection generated successfully. Preview below.")
                } else {
                    _uiState.value = AiUiState.Error("Failed to load generated preview")
                }
            } else {
                _uiState.value = AiUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun promoteResponse(targetCollectionId: String? = null, targetCollectionName: String? = null) {
        val responseId = lastResponseId ?: return
        viewModelScope.launch {
            _uiState.value = AiUiState.Loading
            val result = aiRepository.promoteAiResponseToDatabase(responseId, targetCollectionId, targetCollectionName)
            if (result.isSuccess) {
                _uiState.value = AiUiState.Success("Collection saved to your database!")
                _currentResponse.value = null
            } else {
                _uiState.value = AiUiState.Error(result.exceptionOrNull()?.message ?: "Failed to save")
            }
        }
    }
    
    fun resetState() {
        _uiState.value = AiUiState.Idle
    }
}
