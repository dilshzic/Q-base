package com.algorithmx.q_base.core.data.chat

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ChatDaoModule {
    @Provides
    @Singleton
    fun provideChatDao(chatDatabase: ChatDatabase): ChatDao = chatDatabase.chatDao()

    @Provides
    @Singleton
    fun provideMessageDao(chatDatabase: ChatDatabase): MessageDao = chatDatabase.messageDao()
}
