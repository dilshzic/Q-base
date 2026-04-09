package com.algorithmx.q_base.core_ai.brain.di

import com.algorithmx.q_base.core_ai.brain.AiBrainManager
import com.algorithmx.q_base.core_ai.brain.BrainDataStoreManager
import com.algorithmx.q_base.core_ai.brain.CommonAiService
import com.algorithmx.q_base.core_ai.brain.AiUsageLogger
import com.algorithmx.q_base.core_ai.brain.BrainConfigProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiCoreModule {

    @Provides
    @Singleton
    fun provideAiBrainManager(
        dataStoreManager: BrainDataStoreManager,
        usageLogger: AiUsageLogger,
        configProvider: BrainConfigProvider
    ): AiBrainManager {
        return AiBrainManager(dataStoreManager, usageLogger, configProvider)
    }

    @Provides
    @Singleton
    fun provideCommonAiService(aiBrainManager: AiBrainManager): CommonAiService {
        return CommonAiService(aiBrainManager)
    }
}
