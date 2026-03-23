package com.algorithmx.q_base.brain.implementations

import com.algorithmx.q_base.brain.AiBrain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OpenAiCompatibleBrainImpl(
    private val apiKey: String,
    private val modelName: String,
    private val baseUrl: String
) : AiBrain {

    private val client = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    override suspend fun generateText(prompt: String, isJsonMode: Boolean): Result<String> = withContext(Dispatchers.IO) {
        try {
            val jsonPayload = JSONObject().apply {
                put("model", modelName)
                if (isJsonMode) {
                    put("response_format", JSONObject().apply {
                        put("type", "json_object")
                    })
                }
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            val request = Request.Builder()
                .url(baseUrl)
                .addHeader("Authorization", "Bearer $apiKey")
                .post(jsonPayload.toString().toRequestBody(mediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP Error: ${response.code}"))
                }

                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response body"))
                val jsonResponse = JSONObject(body)
                val content = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                Result.success(content)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun generateTextStream(prompt: String, isJsonMode: Boolean): Flow<String> = flow {
        // Simple implementation: emit the full result as a single chunk for now
        generateText(prompt, isJsonMode).onSuccess {
            emit(it)
        }.onFailure {
            throw it
        }
    }
}
