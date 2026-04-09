package com.algorithmx.q_base.data.di

import com.algorithmx.q_base.core_ai.brain.AiBrainManager
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.data.ai.AiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BrainModule {

    @Provides
    @Singleton
    fun provideAiRepository(
        aiBrainManager: AiBrainManager,
        questionDao: QuestionDao,
        collectionDao: com.algorithmx.q_base.data.collections.CollectionDao,
        aiResponseDao: com.algorithmx.q_base.data.ai.AiResponseDao
    ): AiRepository {
        return AiRepository(aiBrainManager, questionDao, collectionDao, aiResponseDao)
    }
}
