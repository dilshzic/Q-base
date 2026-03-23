package com.algorithmx.q_base.data.di

import android.content.Context
import com.algorithmx.q_base.data.AppDatabase
import com.algorithmx.q_base.data.dao.*
import com.algorithmx.q_base.data.repository.ExploreRepository
import com.algorithmx.q_base.data.repository.SessionRepository
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
        sessionDao: com.algorithmx.q_base.data.dao.SessionDao,
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
}
