package com.algorithmx.q_base.core_ai.brain

import android.util.Log
import com.algorithmx.q_base.core_ai.brain.implementations.GeminiBrainImpl
import com.algorithmx.q_base.core_ai.brain.implementations.OpenAiCompatibleBrainImpl
import com.algorithmx.q_base.core_ai.brain.models.BrainProvider
import com.algorithmx.q_base.core_ai.brain.models.BrainTask
import com.algorithmx.q_base.core_ai.brain.models.StoredBrainConfig
import com.algorithmx.q_base.core_ai.brain.registry.BrainRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiBrainManager @Inject constructor(
    private val dataStoreManager: BrainDataStoreManager,
    private val usageLogger: AiUsageLogger,
    private val configProvider: BrainConfigProvider
) {
    private val TAG = "AiBrainManager"
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _currentConfig = MutableStateFlow<StoredBrainConfig?>(null)

    init {
        dataStoreManager.brainConfigFlow.onEach { config ->
            _currentConfig.value = config
        }.launchIn(scope)
    }

    private suspend fun buildBrain(modelName: String): AiBrain? {
        val provider = BrainRegistry.getProviderForModel(modelName)
        val apiKey = configProvider.getApiKey(provider)
        
        if (apiKey.isBlank() && provider != BrainProvider.LOCAL_GEMMA) {
            Log.e(TAG, "API key missing for provider: $provider")
            return null
        }

        return when (provider) {
            BrainProvider.GEMINI -> {
                GeminiBrainImpl(apiKey, modelName)
            }
            BrainProvider.GROQ -> {
                OpenAiCompatibleBrainImpl(
                    apiKey = apiKey,
                    modelName = modelName,
                    baseUrl = "https://api.groq.com/openai/v1/chat/completions"
                )
            }
            BrainProvider.LOCAL_GEMMA -> {
                null
            }
            else -> {
                Log.e(TAG, "Unsupported brain provider: $provider")
                null
            }
        }
    }

    suspend fun askBrain(task: BrainTask, prompt: String): Result<String> {
        val config = _currentConfig.value ?: return Result.failure(Exception("AI engine not initialized. Please check your settings."))
        
        if (config.isMasterAiFreeze) {
            return Result.failure(Exception("AI generation is currently disabled by Master Freeze."))
        }

        val taskConfig = config.taskConfigs[task]
        val primaryModel = taskConfig?.modelName ?: config.modelName
        val fallbackModel = taskConfig?.fallbackModelName
        val systemPrompt = taskConfig?.systemPrompt ?: config.systemInstruction

        var currentModel = primaryModel
        var brain = buildBrain(currentModel)

        if (brain == null) {
            return Result.failure(Exception("Failed to initialize AI model ($currentModel)."))
        }

        val isJsonTask = task == BrainTask.COLLECTION_GEN || task == BrainTask.QUESTION_EXTRACTION
        val fullPrompt = if (systemPrompt.isNotBlank()) "$systemPrompt\n\nUser Request: $prompt" else prompt
        
        var result = brain.generateText(fullPrompt, isJsonMode = isJsonTask)
        
        var isSuccess = result.isSuccess
        var errorMessage = result.exceptionOrNull()?.message

        // Fallback Logic
        if (!isSuccess && fallbackModel != null) {
            val isRateLimit = errorMessage?.contains("429") == true || errorMessage?.contains("exhausted") == true || errorMessage?.contains("quota") == true
            val logReason = if (isRateLimit) "RATE LIMIT (429/Quota)" else "Error"
            Log.w(TAG, "Task $task failed with $primaryModel ($logReason). Attempting fallback: $fallbackModel")
            currentModel = fallbackModel
            brain = buildBrain(currentModel)
            if (brain != null) {
                result = brain.generateText(fullPrompt, isJsonMode = isJsonTask)
                isSuccess = result.isSuccess
                errorMessage = result.exceptionOrNull()?.message
            } else {
                errorMessage = "Fallback model ($fallbackModel) initialization failed."
            }
        }

        // Log usage (asynchronously)
        val responseText = if (isSuccess) result.getOrThrow() else "Error: $errorMessage"
        val estimatedTokens = ((fullPrompt.length + responseText.length) / 4.0).toInt()
        scope.launch {
            try {
                usageLogger.logUsage(
                    taskId = task,
                    provider = BrainRegistry.getProviderForModel(currentModel),
                    modelUsed = currentModel,
                    tokensEstimated = estimatedTokens,
                    isSuccess = isSuccess,
                    errorMessage = if (!isSuccess) errorMessage else null
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log usage: ${e.message}")
            }
        }

        return result
    }

    suspend fun streamFromBrain(task: BrainTask, prompt: String): Flow<String> {
        val config = _currentConfig.value ?: return emptyFlow()
        if (config.isMasterAiFreeze) return emptyFlow()

        val taskConfig = config.taskConfigs[task]
        val primaryModel = taskConfig?.modelName ?: config.modelName
        val systemPrompt = taskConfig?.systemPrompt ?: config.systemInstruction
        
        val brain = buildBrain(primaryModel) ?: return emptyFlow()
        val fullPrompt = "$systemPrompt\n\nUser Request: $prompt"
        
        return brain.generateTextStream(fullPrompt)
    }
}
