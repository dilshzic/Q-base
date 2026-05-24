package com.algorithmx.q_base.core.backend.di

// TODO: RELOCATE_IN_PHASE6 - Firestore providers live in `app` but are owned
// by backend/auth concerns; move to `core-auth` and make provider configuration
// injectable during Phase 6.

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirestoreModule {

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = Firebase.firestore
}
