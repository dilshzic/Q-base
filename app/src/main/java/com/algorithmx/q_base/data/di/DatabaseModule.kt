package com.algorithmx.q_base.data.di

import android.content.Context
import com.algorithmx.q_base.data.AppDatabase
import com.algorithmx.q_base.data.ai.AiResponseDao
import com.algorithmx.q_base.data.ai.BrainUsageDao
import com.algorithmx.q_base.data.chat.ChatDao
import com.algorithmx.q_base.data.chat.MessageDao
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.data.collections.ProblemReportDao
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.data.core.UserDao
import com.algorithmx.q_base.data.collections.ExploreRepository
import com.algorithmx.q_base.data.sessions.SessionDao
import com.algorithmx.q_base.data.sessions.SessionRepository
import com.algorithmx.q_base.data.auth.ProfileRepository
import com.algorithmx.q_base.data.core.HomeRepository
import com.algorithmx.q_base.data.collections.ImportRepository
import com.algorithmx.q_base.data.core.DataClearingRepository
import com.algorithmx.q_base.data.util.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

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

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao = database.chatDao()

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideAiResponseDao(database: AppDatabase): AiResponseDao = database.aiResponseDao()

    @Provides
    fun provideBrainUsageDao(database: AppDatabase): BrainUsageDao = database.brainUsageDao()

    @Provides
    @Singleton
    fun provideExploreRepository(
        collectionDao: CollectionDao,
        questionDao: QuestionDao,
        sessionDao: SessionDao,
        problemReportDao: ProblemReportDao,
        userDao: UserDao
    ): ExploreRepository {
        return ExploreRepository(collectionDao, questionDao, sessionDao, problemReportDao, userDao)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionDao: SessionDao,
        collectionDao: CollectionDao,
        questionDao: QuestionDao,
        userDao: UserDao
    ): SessionRepository {
        return SessionRepository(sessionDao, collectionDao, questionDao, userDao)
    }

    @Provides
    @Singleton
    fun provideHomeRepository(
        sessionDao: SessionDao,
        questionDao: QuestionDao,
        collectionDao: CollectionDao,
        userDao: UserDao,
        chatDao: ChatDao
    ): HomeRepository {
        return HomeRepository(sessionDao, questionDao, collectionDao, userDao, chatDao)
    }

    @Provides
    @Singleton
    fun provideImportRepository(@ApplicationContext context: Context): ImportRepository {
        return ImportRepository(context)
    }

    @Provides
    @Singleton
    fun provideDataClearingRepository(
        chatDao: ChatDao,
        messageDao: MessageDao,
        sessionDao: SessionDao,
        questionDao: QuestionDao,
        userDao: UserDao,
        aiResponseDao: AiResponseDao,
        brainUsageDao: BrainUsageDao,
        collectionDao: CollectionDao,
        cryptoManager: CryptoManager
    ): DataClearingRepository {
        return DataClearingRepository(
            chatDao, messageDao, sessionDao, questionDao, userDao,
            aiResponseDao, brainUsageDao, collectionDao, cryptoManager
        )
    }
}
