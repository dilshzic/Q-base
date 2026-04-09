package com.algorithmx.q_base.di

import com.algorithmx.q_base.data.ai.AiUsageLoggerImpl
import com.algorithmx.q_base.data.ai.BrainConfigProviderImpl
import com.algorithmx.q_base.core_ai.brain.AiUsageLogger
import com.algorithmx.q_base.core_ai.brain.BrainConfigProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiIntegrationModule {

    @Binds
    @Singleton
    abstract fun bindAiUsageLogger(
        impl: AiUsageLoggerImpl
    ): AiUsageLogger

    @Binds
    @Singleton
    abstract fun bindBrainConfigProvider(
        impl: BrainConfigProviderImpl
    ): BrainConfigProvider
}
