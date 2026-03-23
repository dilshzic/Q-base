package com.algorithmx.q_base.data.di

import com.algorithmx.q_base.brain.AiBrainManager
import com.algorithmx.q_base.brain.BrainDataStoreManager
import com.algorithmx.q_base.brain.CommonAiService
import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.repository.AiRepository
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
    fun provideAiBrainManager(
        dataStoreManager: BrainDataStoreManager,
        brainUsageDao: com.algorithmx.q_base.data.dao.BrainUsageDao
    ): AiBrainManager {
        return AiBrainManager(dataStoreManager, brainUsageDao)
    }

    @Provides
    @Singleton
    fun provideCommonAiService(aiBrainManager: AiBrainManager): CommonAiService {
        return CommonAiService(aiBrainManager)
    }

    @Provides
    @Singleton
    fun provideAiRepository(
        aiBrainManager: AiBrainManager,
        questionDao: QuestionDao,
        collectionDao: com.algorithmx.q_base.data.dao.CollectionDao,
        aiResponseDao: com.algorithmx.q_base.data.dao.AiResponseDao
    ): AiRepository {
        return AiRepository(aiBrainManager, questionDao, collectionDao, aiResponseDao)
    }
}
