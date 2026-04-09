package com.algorithmx.q_base.core_ai.brain

import com.algorithmx.q_base.core_ai.brain.models.BrainProvider
import com.algorithmx.q_base.core_ai.brain.models.BrainTask

interface AiUsageLogger {
    suspend fun logUsage(
        taskId: BrainTask,
        provider: BrainProvider,
        modelUsed: String,
        tokensEstimated: Int,
        isSuccess: Boolean,
        errorMessage: String? = null
    )
}
