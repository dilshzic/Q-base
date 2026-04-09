package com.algorithmx.q_base.core_ai.brain.implementations

import com.algorithmx.q_base.core_ai.brain.AiBrain
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GeminiBrainImpl(
    apiKey: String,
    modelName: String
) : AiBrain {

    private val generativeModel = GenerativeModel(
        modelName = modelName,
        apiKey = apiKey
    )

    override suspend fun generateText(prompt: String, isJsonMode: Boolean): Result<String> {
        return try {
            val response = generativeModel.generateContent(prompt)
            Result.success(response.text ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun generateTextStream(prompt: String, isJsonMode: Boolean): Flow<String> {
        return generativeModel.generateContentStream(prompt).map { it.text ?: "" }
    }
}
