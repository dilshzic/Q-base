package com.algorithmx.q_base.core_ai.brain

import com.algorithmx.q_base.core_ai.brain.models.BrainProvider

interface BrainConfigProvider {
    suspend fun getApiKey(provider: BrainProvider): String
}
