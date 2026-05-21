package com.algorithmx.q_base.core_ai.brain.models

import kotlinx.serialization.Serializable

import com.algorithmx.androidmodules.coreai.brain.models.BrainProvider
import com.algorithmx.androidmodules.coreai.brain.models.BrainCategory


@Serializable
data class BrainConfig(
    val provider: BrainProvider,
    val modelName: String,
    val category: BrainCategory = BrainCategory.TEXT_TO_TEXT
)

@Serializable
data class TaskConfig(
    val modelName: String,
    val fallbackModelName: String? = null,
    val systemPrompt: String,
    val temperature: Float = 0.7f
)

data class StoredBrainConfig(
    val provider: BrainProvider,
    val modelName: String,
    val systemInstruction: String,
    val totalRequests: Int,
    val totalTokens: Int,
    val category: BrainCategory = BrainCategory.TEXT_TO_TEXT,
    val themeMode: String = "SYSTEM",
    val notificationsEnabled: Boolean = true,
    val isMasterAiFreeze: Boolean = false,
    val taskConfigs: Map<BrainTask, TaskConfig> = emptyMap()
)