package com.algorithmx.q_base.sync.orchestration

import com.algorithmx.q_base.BuildConfig
import com.algorithmx.q_base.core.data.chat.ChatDao
import com.algorithmx.q_base.feature.content_import.data.CollectionDao
import com.algorithmx.q_base.feature.content_import.data.CollectionVersionLedgerDao
import com.algorithmx.q_base.feature.content_import.data.QuestionDao
import com.algorithmx.q_base.core.data.UserDao
import com.algorithmx.q_base.core.data.auth.AuthRepository
import com.algorithmx.q_base.core_crypto.CryptoManager
import com.algorithmx.q_base.core.data.util.MockDownloader
import com.algorithmx.q_base.core.data.backend.CoreDatabase
import io.appwrite.Client
import io.appwrite.services.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy

@Singleton
class CollectionSyncRepository @Inject constructor(
    internal val appwriteClient: Client,
    internal val databases: CoreDatabase,
    internal val authRepository: AuthRepository,
    internal val storage: Storage,
    internal val chatDao: ChatDao,
    internal val userDao: UserDao,
    internal val cryptoManager: CryptoManager,
    internal val mockDownloader: MockDownloader,
    internal val collectionDao: CollectionDao,
    internal val questionDao: QuestionDao,
    internal val collectionVersionLedgerDao: CollectionVersionLedgerDao,
    internal val messageSyncRepository: Lazy<MessageSyncRepository>
) {
    internal val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    internal val bucketId = BuildConfig.APPWRITE_BUCKET_ID
    internal val projectId = BuildConfig.APPWRITE_PROJECT_ID

    internal val currentUserId: String?
        get() = authRepository.currentUserId
}