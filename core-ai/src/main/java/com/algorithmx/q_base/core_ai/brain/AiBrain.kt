package com.algorithmx.q_base.core_ai.brain

import kotlinx.coroutines.flow.Flow

interface AiBrain {
    suspend fun generateText(prompt: String, isJsonMode: Boolean = false): Result<String>
    fun generateTextStream(prompt: String, isJsonMode: Boolean = false): Flow<String>
}
