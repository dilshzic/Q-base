package com.algorithmx.q_base.core.backend.di

// TODO: RELOCATE_IN_PHASE6 - Network wiring is app-scoped; consider moving
// generic network providers into a `core-network` module and app-specific
// configuration into the app module.

import android.content.Context
import com.algorithmx.q_base.util.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {
        return NetworkMonitor(context)
    }
}