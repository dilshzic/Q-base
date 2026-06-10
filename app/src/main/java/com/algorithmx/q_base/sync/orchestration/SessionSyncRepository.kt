package com.algorithmx.q_base.sync.orchestration

import android.util.Log
import com.algorithmx.q_base.core.data.chat.MessageEntity
import com.algorithmx.q_base.feature.sessions.data.SessionDao
import com.algorithmx.q_base.feature.sessions.data.SessionAttempt
import com.algorithmx.q_base.core.data.auth.AuthRepository
import com.algorithmx.q_base.core.data.backend.CoreDatabase
import com.algorithmx.q_base.core.data.backend.CoreQuery
import com.algorithmx.q_base.core.data.backend.CoreQueryOperator
import io.appwrite.Client
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy

@Singleton
class SessionSyncRepository @Inject constructor(
    private val appwriteClient: Client,
    private val databases: CoreDatabase,
    private val authRepository: AuthRepository,
    private val sessionDao: SessionDao,
    private val messageSyncRepository: Lazy<MessageSyncRepository>
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val currentUserId: String?
        get() = authRepository.currentUserId

    suspend fun sendSessionInvite(
        chatId: String, 
        sessionId: String, 
        sessionTitle: String,
        downloadUrl: String,
        symmetricKey: String
    ) {
        val senderId = currentUserId ?: return
        val messageId = UUID.randomUUID().toString()
        
        val message = MessageEntity(
            messageId = messageId,
            chatId = chatId,
            senderId = senderId,
            payload = "$downloadUrl|E2EE_KEY|$symmetricKey|SESSION_ID|$sessionId|TITLE|$sessionTitle",
            type = "SESSION_INVITE",
            timestamp = System.currentTimeMillis()
        )
        
        messageSyncRepository.get().sendMessage(message)
    }

    suspend fun addSharedSessionToGroup(
        chatId: String, 
        sessionId: String, 
        sessionTitle: String,
        downloadUrl: String,
        symmetricKey: String
    ) {
        try {
            val sessionMetadata = mapOf(
                "sessionId" to sessionId,
                "chatId" to chatId,
                "title" to sessionTitle,
                "downloadUrl" to downloadUrl,
                "symmetricKey" to symmetricKey,
                "adminIds" to listOf(currentUserId ?: "Unknown"),
                "isAdminOnly" to false,
                "timestamp" to System.currentTimeMillis() / 1000
            )

            try {
                databases.createDocument(
                    collectionId = "shared_sessions",
                    documentId = sessionId,
                    data = sessionMetadata
                ).getOrThrow()
            } catch (e: io.appwrite.exceptions.AppwriteException) {
                if (e.code == 409) {
                    databases.updateDocument(
                        collectionId = "shared_sessions",
                        documentId = sessionId,
                        data = sessionMetadata
                    ).getOrThrow()
                } else {
                    throw e
                }
            } catch (e: Exception) {
                val unwrapped = (e as? java.lang.reflect.InvocationTargetException)?.targetException ?: e
                if (unwrapped is io.appwrite.exceptions.AppwriteException && unwrapped.code == 409) {
                    databases.updateDocument(
                        collectionId = "shared_sessions",
                        documentId = sessionId,
                        data = sessionMetadata
                    ).getOrThrow()
                } else {
                    throw e
                }
            }
        } catch (e: Exception) {
            Log.e("SessionSyncRepository", "Failed to share session to group", e)
            throw e
        }
    }

    fun observeSharedSessions(chatId: String): Flow<List<Map<String, Any>>> {
        return callbackFlow {
            repositoryScope.launch {
                try {
                    val docs = databases.queryDocuments(
                        collectionId = "shared_sessions",
                        queries = emptyList()
                    ).getOrThrow().filter { it["chatId"] == chatId }
                    val mapped = docs.map { doc ->
                        val data = doc.toMutableMap()
                        val rawTimestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                        data["timestamp"] = if (rawTimestamp < 1000000000000L) rawTimestamp * 1000 else rawTimestamp
                        data
                    }
                    trySend(mapped).isSuccess
                } catch (e: Exception) {}
            }

            val realtime = io.appwrite.services.Realtime(appwriteClient)
            val subscription = realtime.subscribe("tablesdb.qbase_db.tables.shared_sessions.rows") { event ->
                repositoryScope.launch {
                    try {
                        val docs = databases.queryDocuments(
                            collectionId = "shared_sessions",
                            queries = emptyList()
                        ).getOrThrow().filter { it["chatId"] == chatId }
                        val mapped = docs.map { doc ->
                            val data = doc.toMutableMap()
                            val rawTimestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                            data["timestamp"] = if (rawTimestamp < 1000000000000L) rawTimestamp * 1000 else rawTimestamp
                            data
                        }
                        trySend(mapped).isSuccess
                    } catch (e: Exception) {}
                }
            }
            awaitClose { subscription.close() }
        }
    }

    suspend fun sendSessionPatch(chatId: String, sessionId: String, op: String, data: JSONObject) {
        val patchObj = JSONObject()
        patchObj.put("sessionId", sessionId)
        patchObj.put("op", op)
        patchObj.put("data", data)
        
        val message = MessageEntity(
            messageId = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = currentUserId ?: "",
            payload = patchObj.toString(),
            type = "SESSION_PATCH",
            timestamp = System.currentTimeMillis()
        )
        messageSyncRepository.get().sendMessage(message)
    }

    suspend fun applySessionPatch(jsonString: String) {
        try {
            val patch = JSONObject(jsonString)
            val op = patch.getString("op")
            val sessionId = patch.getString("sessionId")
            val data = patch.getJSONObject("data")

            when (op) {
                "UPSERT_ATTEMPT" -> {
                    val qId = data.getString("questionId")
                    val attemptStatus = data.getString("attemptStatus")
                    val userSelectedAnswers = data.getString("userSelectedAnswers")
                    val marksObtained = data.optDouble("marksObtained", 0.0).toFloat()

                    val attempt = SessionAttempt(
                        sessionId = sessionId,
                        questionId = qId,
                        attemptStatus = attemptStatus,
                        userSelectedAnswers = userSelectedAnswers,
                        marksObtained = marksObtained
                    )
                    sessionDao.insertAttempts(listOf(attempt))
                }
                "UPDATE_SESSION" -> {
                    val title = data.getString("title")
                    val isCompleted = data.getBoolean("isCompleted")
                    val scoreAchieved = data.optDouble("scoreAchieved", 0.0).toFloat()
                    
                    val existing = sessionDao.getSessionById(sessionId)
                    if (existing != null) {
                        sessionDao.updateSession(existing.copy(
                            title = title,
                            isCompleted = isCompleted,
                            scoreAchieved = scoreAchieved
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SessionSyncRepository", "Failed to apply session patch", e)
        }
    }
}