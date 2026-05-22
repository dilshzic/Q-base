package com.algorithmx.q_base.core.data.di

import android.content.Context
import com.algorithmx.q_base.core.ai.data.AiResponseDao
import com.algorithmx.q_base.core.ai.data.BrainUsageDao
import com.algorithmx.q_base.core.data.chat.ChatDao
import com.algorithmx.q_base.core.data.chat.MessageDao
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.data.collections.ProblemReportDao
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.core.data.UserDao
import com.algorithmx.q_base.feature.sessions.data.SessionDao
import com.algorithmx.q_base.feature.sessions.data.SessionRepository
import com.algorithmx.q_base.core.data.auth.ProfileRepository
import com.algorithmx.q_base.core.data.auth.ProfileCache
import com.algorithmx.q_base.core.data.HomeRepository
import com.algorithmx.q_base.core.data.DataClearingRepository
import com.algorithmx.q_base.data.collections.ExploreRepository
import com.algorithmx.q_base.data.collections.ImportRepository
import com.algorithmx.q_base.data.collections.CollectionVersionLedgerDao
import com.algorithmx.q_base.core_crypto.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
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

    @Provides
    fun provideProfileCache(userDao: UserDao): ProfileCache {
        return object : ProfileCache {
            override suspend fun upsert(profile: com.algorithmx.q_base.core.data.auth.UserProfile) {
                userDao.insertUser(
                    com.algorithmx.q_base.core.data.UserEntity(
                        userId = profile.userId,
                        displayName = profile.displayName,
                        email = profile.email,
                        intro = profile.intro,
                        profilePictureUrl = profile.profilePictureUrl,
                        friendCode = profile.friendCode,
                        publicKey = profile.publicKey,
                        isBanned = profile.isBanned,
                        isPhotoVisible = profile.isPhotoVisible
                    )
                )
            }
        }
    }
}