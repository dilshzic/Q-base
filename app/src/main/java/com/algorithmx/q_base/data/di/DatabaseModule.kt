package com.algorithmx.q_base.data.di

import android.content.Context
import com.algorithmx.q_base.data.AppDatabase
import com.algorithmx.q_base.data.dao.CategoryDao
import com.algorithmx.q_base.data.dao.ProblemReportDao
import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.dao.SessionDao
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
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideProblemReportDao(database: AppDatabase): ProblemReportDao = database.problemReportDao()

    @Provides
    @Singleton
    fun provideExploreRepository(
        categoryDao: CategoryDao,
        questionDao: QuestionDao,
        problemReportDao: ProblemReportDao
    ): ExploreRepository {
        return ExploreRepository(categoryDao, questionDao, problemReportDao)
    }

    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionDao: SessionDao,
        categoryDao: CategoryDao,
        questionDao: QuestionDao
    ): SessionRepository {
        return SessionRepository(sessionDao, categoryDao, questionDao)
    }
}
