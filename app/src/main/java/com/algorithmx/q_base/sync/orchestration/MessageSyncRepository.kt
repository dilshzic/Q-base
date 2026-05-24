package com.algorithmx.q_base.sync.orchestration

import android.util.Log
import com.algorithmx.q_base.core.data.chat.ChatLocalDataSource
import com.algorithmx.q_base.core.data.chat.ChatRemoteRepository
import com.algorithmx.q_base.core.data.UserDao
import com.algorithmx.q_base.core.data.auth.AuthRepository
import com.algorithmx.q_base.core_crypto.CryptoManager
import com.algorithmx.q_base.core.data.backend.CoreDatabase
import io.appwrite.Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy

@Singleton
class MessageSyncRepository @Inject constructor(
    internal val appwriteClient: Client,
    internal val databases: CoreDatabase,
    internal val authRepository: AuthRepository,
    internal val chatRemoteRepository: ChatRemoteRepository,
    internal val chatLocalDataSource: ChatLocalDataSource,
    internal val userDao: UserDao,
    internal val profileRepository: com.algorithmx.q_base.core.data.auth.ProfileRepository,
    internal val cryptoManager: CryptoManager,
    internal val collectionSyncRepository: Lazy<CollectionSyncRepository>,
    internal val sessionSyncRepository: Lazy<SessionSyncRepository>,
    internal val chatManagerRepository: Lazy<ChatManagerRepository>
) {
    internal val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val currentUserId: String?
        get() = authRepository.currentUserId

    fun serializeWrappedKeys(map: Map<String, String>): String {
        val json = JSONObject()
        map.forEach { (k, v) -> json.put(k, v) }
        return json.toString()
    }

    fun deserializeWrappedKeys(jsonStr: String?): Map<String, String> {
        if (jsonStr.isNullOrBlank()) return emptyMap()
        val map = mutableMapOf<String, String>()
        try {
            val json = JSONObject(jsonStr)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.getString(key)
            }
        } catch (e: Exception) {
            Log.e("MessageSyncRepository", "Failed to deserialize wrapped keys", e)
        }
        return map
    }

    suspend fun acknowledgeMessageDelivery(
        messageId: String,
        chatId: String,
        senderId: String,
        wrappedKeyStr: String
    ) {
        if (senderId == currentUserId) return

        val chat = chatLocalDataSource.getChatById(chatId) ?: return
        if (!chat.isGroup) {
            try {
                databases.deleteDocument("messages", messageId).getOrThrow()
                Log.d("MessageSyncRepository", "P2P Ephemeral: Cleared message $messageId")
            } catch (e: Exception) {
                Log.e("MessageSyncRepository", "Failed to clear P2P ephemeral message", e)
            }
            return
        }

        try {
            val wrappedKeysMap = deserializeWrappedKeys(wrappedKeyStr).toMutableMap()
            
            if (!wrappedKeysMap.containsKey(currentUserId)) {
                return
            }
            
            wrappedKeysMap.remove(currentUserId)
            val pendingReceivers = wrappedKeysMap.keys.filter { it != senderId }

            if (pendingReceivers.isEmpty()) {
                databases.deleteDocument("messages", messageId).getOrThrow()
                Log.d("MessageSyncRepository", "Group Ephemeral: All delivered! Deleted message $messageId")
            } else {
                val updatedWrappedKeyStr = serializeWrappedKeys(wrappedKeysMap)
                databases.updateDocument(
                    collectionId = "messages",
                    documentId = messageId,
                    data = mapOf("wrappedKey" to updatedWrappedKeyStr)
                ).getOrThrow()
                Log.d("MessageSyncRepository", "Group Ephemeral: Acknowledged delivery for $messageId. Pending: $pendingReceivers")
            }
        } catch (e: Exception) {
            Log.e("MessageSyncRepository", "Error running group ephemeral acknowledgment for $messageId", e)
        }
    }
}