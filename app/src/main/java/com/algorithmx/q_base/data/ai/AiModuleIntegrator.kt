package com.algorithmx.q_base.data.ai

import com.algorithmx.q_base.BuildConfig
import com.algorithmx.q_base.core_ai.brain.AiUsageLogger
import com.algorithmx.q_base.core_ai.brain.BrainConfigProvider
import com.algorithmx.androidmodules.coreai.brain.models.BrainProvider
import com.algorithmx.q_base.core_ai.brain.models.BrainTask
import com.algorithmx.q_base.data.ai.BrainUsageDao
import com.algorithmx.q_base.data.ai.BrainUsageEntity
import io.appwrite.Query
import io.appwrite.services.Databases
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrainConfigProviderImpl @Inject constructor(
    private val databases: Databases
) : BrainConfigProvider {
    private val TAG = "BrainConfigProvider"

    override suspend fun getApiKey(provider: BrainProvider): String {
        val configKey = when (provider) {
            BrainProvider.GEMINI -> "GEMINI_API_KEY"
            BrainProvider.GROQ -> "GROQ_API_KEY"
            else -> return ""
        }

        // Try Appwrite first
        try {
            val response = databases.listDocuments(
                databaseId = BuildConfig.APPWRITE_DATABASE_ID,
                collectionId = "app_config",
                queries = listOf(Query.equal("key", configKey))
            )
            
            val remoteValue = response.documents.firstOrNull()?.data?.get("value") as? String
            if (!remoteValue.isNullOrBlank()) {
                Log.d(TAG, "Loaded $configKey from Appwrite")
                return remoteValue
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load $configKey from Appwrite, using local fallback", e)
        }

        // Fallback to BuildConfig
        return when (provider) {
            BrainProvider.GEMINI -> BuildConfig.GEMINI_API_KEY
            BrainProvider.GROQ -> BuildConfig.GROQ_API_KEY
            else -> ""
        }
    }
}

@Singleton
class AiUsageLoggerImpl @Inject constructor(
    private val brainUsageDao: BrainUsageDao
) : AiUsageLogger {
    override suspend fun logUsage(
        taskId: BrainTask,
        provider: BrainProvider,
        modelUsed: String,
        tokensEstimated: Int,
        isSuccess: Boolean,
        errorMessage: String?
    ) {
        val usageRecord = BrainUsageEntity(
            id = UUID.randomUUID().toString(),
            taskId = taskId,
            timestampMs = System.currentTimeMillis(),
            provider = provider,
            modelUsed = modelUsed,
            tokensEstimated = tokensEstimated,
            isSuccess = isSuccess,
            errorMessage = errorMessage
        )
        brainUsageDao.insertUsageRecord(usageRecord)
    }
}
