package com.algorithmx.q_base.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.core_ai.brain.AiBrainManager
import com.algorithmx.q_base.core_ai.brain.models.BrainTask
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AiState {
    object Idle : AiState()
    object Loading : AiState()
    data class Success(val result: String) : AiState()
    data class Error(val message: String) : AiState()
}

/**
 * A reusable base ViewModel for any feature that requires AI interaction.
 * Provides standardized state management (Idle, Loading, Success, Error)
 * and safely delegates execution to the centralized AiBrainManager.
 */
abstract class BaseAiViewModel(
    protected val aiBrainManager: AiBrainManager
) : ViewModel() {

    private val _aiState = MutableStateFlow<AiState>(AiState.Idle)
    val aiState: StateFlow<AiState> = _aiState.asStateFlow()
    
    private var currentJob: Job? = null

    /**
     * Executes a standardized AI task and manages the UI state automatically.
     */
    protected fun executeAiTask(
        task: BrainTask,
        prompt: String,
        onSuccess: ((String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        currentJob?.cancel() // Cancel previous task if still running
        
        currentJob = viewModelScope.launch {
            _aiState.value = AiState.Loading
            
            aiBrainManager.askBrain(task, prompt).fold(
                onSuccess = { resultText ->
                    _aiState.value = AiState.Success(resultText)
                    onSuccess?.invoke(resultText)
                },
                onFailure = { throwable ->
                    val errorMsg = throwable.message ?: "An unexpected AI error occurred."
                    _aiState.value = AiState.Error(errorMsg)
                    onError?.invoke(errorMsg)
                }
            )
        }
    }

    fun resetAiState() {
        _aiState.value = AiState.Idle
    }
    
    fun cancelCurrentAiTask() {
        currentJob?.cancel()
        resetAiState()
    }
}
