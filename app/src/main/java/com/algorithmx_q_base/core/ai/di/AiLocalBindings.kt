package com.algorithmx_q_base.core.ai.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.algorithmx_q_base.di.AiUsageLoggerImpl
import com.algorithmx_q_base.di.BrainConfigProviderImpl
import com.algorithmx_q_base.core.ai.AiUsageLogger
import com.algorithmx_q_base.core.ai.BrainConfigProvider

/**
 * Local binding module to bind existing implementation classes without moving them.
 * This keeps implementations in-place while exposing bindings from the owner package.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AiLocalBindings {
    @Binds
    abstract fun bindAiUsageLogger(impl: AiUsageLoggerImpl): AiUsageLogger

    @Binds
    abstract fun bindBrainConfigProvider(impl: BrainConfigProviderImpl): BrainConfigProvider
}
