package com.algorithmx.q_base.data.chat

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatFirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun createChatOnFirestore(chat: ChatEntity) {
        val participants = chat.participantIds.split(",").filter { it.isNotBlank() }.toMutableList()
        currentUserId?.let { uid ->
            if (!participants.contains(uid)) {
                participants.add(uid)
            }
        }

        val chatMap = hashMapOf(
            "chatName" to chat.chatName,
            "isGroup" to chat.isGroup,
            "participantIds" to participants,
            "adminId" to (chat.adminId.takeIf { it?.isNotBlank() == true } ?: currentUserId),
            "createdAt" to System.currentTimeMillis()
        )

        try {
            firestore.collection("chats")
                .document(chat.chatId)
                .set(chatMap)
                .await()
        } catch (e: Exception) {
            Log.e("ChatFirestoreRepository", "Failed to create chat in Firestore", e)
        }
    }

    suspend fun addParticipantToFirestore(chatId: String, userId: String) {
        try {
            firestore.collection("chats")
                .document(chatId)
                .update("participantIds", userId)
                .await()
        } catch (e: Exception) {
            Log.e("ChatFirestoreRepository", "Failed to add participant in Firestore", e)
        }
    }

    suspend fun removeParticipantFromFirestore(chatId: String, userId: String) {
        try {
            firestore.collection("chats")
                .document(chatId)
                .update("participantIds", userId)
                .await()
        } catch (e: Exception) {
            Log.e("ChatFirestoreRepository", "Failed to remove participant in Firestore", e)
        }
    }

    suspend fun clearChatMessagesOnFirestore(chatId: String) {
        try {
            val messagesRef = firestore.collection("chats").document(chatId).collection("messages")
            val snapshot = messagesRef.get().await()
            val batch = firestore.batch()
            snapshot.documents.forEach { document -> batch.delete(document.reference) }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("ChatFirestoreRepository", "Failed to clear messages on Firestore", e)
        }
    }

    suspend fun deleteChatOnFirestore(chatId: String) {
        try {
            clearChatMessagesOnFirestore(chatId)
            firestore.collection("chats").document(chatId).delete().await()
        } catch (e: Exception) {
            Log.e("ChatFirestoreRepository", "Failed to delete chat on Firestore", e)
        }
    }

    suspend fun reportGroup(group: ChatEntity, reason: String) {
        val reporterId = currentUserId ?: throw IllegalStateException("User not authenticated")
        val reportRef = firestore.collection("reported_groups").document()
        val reportMap = hashMapOf(
            "groupId" to group.chatId,
            "groupName" to group.chatName,
            "participantIds" to group.participantIds,
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis()
        )
        try {
            reportRef.set(reportMap).await()
            Log.d("ChatFirestoreRepository", "Group report submitted successfully: ${reportRef.id}")
        } catch (e: Exception) {
            Log.e("ChatFirestoreRepository", "Failed to submit reported group ${group.chatId}", e)
        }
    }

    suspend fun reportMessage(message: MessageEntity, reason: String) {
        val reporterId = currentUserId ?: return
        val reportRef = firestore.collection("reported_messages").document()
        val reportMap = hashMapOf(
            "messageId" to message.messageId,
            "chatId" to message.chatId,
            "senderId" to message.senderId,
            "payload" to message.payload,
            "type" to message.type,
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis()
        )
        try {
            reportRef.set(reportMap).await()
            Log.d("ChatFirestoreRepository", "Message report submitted successfully: ${reportRef.id}")
        } catch (e: Exception) {
            Log.e("ChatFirestoreRepository", "Failed to submit reported message ${message.messageId}", e)
        }
    }
}