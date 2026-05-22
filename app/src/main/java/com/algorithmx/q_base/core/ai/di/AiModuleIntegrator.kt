package com.algorithmx.q_base.core.ai.di

import android.util.Log
import com.algorithmx.q_base.BuildConfig
import com.algorithmx.androidmodules.coreai.brain.AiUsageLogger
import com.algorithmx.androidmodules.coreai.brain.BrainConfigProvider
import com.algorithmx.androidmodules.coreai.brain.models.BrainProvider
import com.algorithmx.q_base.core.ai.brain.models.BrainTask
import com.algorithmx.q_base.core.ai.data.BrainUsageDao
import com.algorithmx.q_base.core.ai.data.BrainUsageEntity
import com.algorithmx.q_base.core.data.ConfigRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrainConfigProviderImpl @Inject constructor(
    private val configRepository: ConfigRepository
) : BrainConfigProvider {
    private val TAG = "BrainConfigProvider"

    override suspend fun getApiKey(provider: BrainProvider): String {
        return when (provider) {
            BrainProvider.GEMINI -> configRepository.getGeminiKeyDirectly()
            BrainProvider.GROQ -> configRepository.getGroqKeyDirectly()
            else -> ""
        }
    }
}

@Singleton
class AiUsageLoggerImpl @Inject constructor(
    private val brainUsageDao: BrainUsageDao
) : AiUsageLogger {
    override suspend fun logUsage(
        provider: BrainProvider,
        modelUsed: String,
        tokensEstimated: Int,
        isSuccess: Boolean,
        errorMessage: String?
    ) {
        // Library no longer provides taskId; store a default fallback task
        val usageRecord = BrainUsageEntity(
            id = UUID.randomUUID().toString(),
            taskId = BrainTask.CHAT_BOT,
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
