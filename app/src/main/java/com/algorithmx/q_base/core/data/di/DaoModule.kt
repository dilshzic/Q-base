package com.algorithmx.q_base.core.data.di

import com.algorithmx.q_base.core.data.AppDatabase
import com.algorithmx.q_base.core.data.chat.ChatDatabase
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.data.collections.ProblemReportDao
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.core.data.UserDao
import com.algorithmx.q_base.feature.sessions.data.SessionDao
import com.algorithmx.q_base.core.data.chat.ChatDao
import com.algorithmx.q_base.core.data.chat.MessageDao
import com.algorithmx.q_base.core.ai.data.AiResponseDao
import com.algorithmx.q_base.core.ai.data.BrainUsageDao
import com.algorithmx.q_base.data.collections.CollectionVersionLedgerDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Provides
    fun provideQuestionDao(database: AppDatabase): QuestionDao = database.questionDao()

    @Provides
    fun provideCollectionDao(database: AppDatabase): CollectionDao = database.collectionDao()

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideProblemReportDao(database: AppDatabase): ProblemReportDao = database.problemReportDao()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    // Chat DAOs are provided by the core-chat module to avoid duplicate bindings

    @Provides
    fun provideAiResponseDao(database: AppDatabase): AiResponseDao = database.aiResponseDao()

    @Provides
    fun provideBrainUsageDao(database: AppDatabase): BrainUsageDao = database.brainUsageDao()

    @Provides
    fun provideCollectionVersionLedgerDao(database: AppDatabase): CollectionVersionLedgerDao = database.collectionVersionLedgerDao()

    @Provides
    fun provideActionQueueDao(database: AppDatabase): com.algorithmx.q_base.data.sync.ActionQueueDao = database.actionQueueDao()

    @Provides
    fun provideQuestionAiMessageDao(database: AppDatabase): com.algorithmx.q_base.core.ai.data.QuestionAiMessageDao = database.questionAiMessageDao()
}
