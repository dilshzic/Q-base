package com.algorithmx.q_base.core.data.chat

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ChatLocalDataSourceModule {
    @Binds
    @Singleton
    abstract fun bindChatLocalDataSource(
        impl: ChatLocalDataSourceImpl
    ): ChatLocalDataSource
}
