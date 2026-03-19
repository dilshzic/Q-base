package com.algorithmx.q_base.data.repository

import com.algorithmx.q_base.data.dao.ChatDao
import com.algorithmx.q_base.data.dao.MessageDao
import com.algorithmx.q_base.data.entity.MessageEntity
import com.algorithmx.q_base.data.util.MockDownloader
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val mockDownloader: MockDownloader
) {
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun observeAndSyncMessages(chatId: String): Flow<Unit> = callbackFlow {
        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val type = doc.getString("type") ?: "TEXT"
                        val payload = doc.getString("payload") ?: ""
                        
                        val message = MessageEntity(
                            messageId = doc.id,
                            chatId = chatId,
                            senderId = doc.getString("senderId") ?: "",
                            payload = payload,
                            type = type,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                        
                        repositoryScope.launch {
                            messageDao.insertMessage(message)
                            
                            // Automatically download and import if it's a file transfer
                            if (type == "FILE_TRANSFER" && payload.startsWith("http")) {
                                mockDownloader.downloadAndImportMock(payload)
                            }
                        }
                    }
                }
                trySend(Unit)
            }
        
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(message: MessageEntity) {
        val messageMap = hashMapOf(
            "senderId" to message.senderId,
            "payload" to message.payload,
            "type" to message.type,
            "timestamp" to message.timestamp
        )
        
        firestore.collection("chats")
            .document(message.chatId)
            .collection("messages")
            .document(message.messageId)
            .set(messageMap)
    }
}
