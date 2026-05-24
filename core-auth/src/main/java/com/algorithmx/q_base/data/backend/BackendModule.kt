package com.algorithmx.q_base.core.data.backend

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FirebaseBackend

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppwriteBackend

@Module
@InstallIn(SingletonComponent::class)
object BackendModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    @FirebaseBackend
    fun provideFirebaseAuthImpl(
        firebaseAuth: FirebaseAuth,
        @ApplicationContext context: Context
    ): CoreAuth {
        return FirebaseAuthImpl(firebaseAuth, context)
    }

    @Provides
    @Singleton
    @FirebaseBackend
    fun provideFirebaseDatabaseImpl(
        firestore: FirebaseFirestore
    ): CoreDatabase {
        return FirebaseDatabaseImpl(firestore)
    }

    @Provides
    @Singleton
    @AppwriteBackend
    fun provideAppwriteAuthImpl(
        appwriteClient: io.appwrite.Client,
        appwriteAccount: io.appwrite.services.Account
    ): CoreAuth {
        return AppwriteAuthImpl(appwriteClient, appwriteAccount)
    }

    @Provides
    @Singleton
    @AppwriteBackend
    fun provideAppwriteDatabaseImpl(
        client: io.appwrite.Client,
        tablesDB: io.appwrite.services.TablesDB
    ): CoreDatabase {
        return AppwriteDatabaseImpl(client, tablesDB)
    }

    @Provides
    @Singleton
    fun provideActiveAuth(
        @AppwriteBackend appwriteAuth: CoreAuth
    ): CoreAuth {
        return appwriteAuth
    }

    @Provides
    @Singleton
    fun provideActiveDatabase(
        @AppwriteBackend appwriteDatabase: CoreDatabase
    ): CoreDatabase {
        return appwriteDatabase
    }
}
