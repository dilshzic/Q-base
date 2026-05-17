package com.algorithmx.q_base.data.chat

import android.util.Log
import io.appwrite.Client
import io.appwrite.services.Databases
import io.appwrite.services.Account
import io.appwrite.Query
import io.appwrite.ID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatFirestoreRepository @Inject constructor(
    private val databases: Databases,
    private val appwriteAccount: Account
) {
    private suspend fun getCurrentUserId(): String? {
        return try {
            appwriteAccount.get().id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createChatOnFirestore(chat: ChatEntity) {
        val participants = chat.participantIds.split(",").filter { it.isNotBlank() }.toMutableList()
        getCurrentUserId()?.let { uid ->
            if (!participants.contains(uid)) {
                participants.add(uid)
            }
        }

        val chatMap = mapOf(
            "chatId" to chat.chatId,
            "chatName" to chat.chatName.orEmpty(),
            "isGroup" to chat.isGroup,
            "participantIds" to participants,
            "adminId" to (chat.adminId.takeIf { !it.isNullOrBlank() } ?: getCurrentUserId().orEmpty()),
            "createdAt" to System.currentTimeMillis()
        )

        try {
            databases.createDocument(
                databaseId = "qbase_db",
                collectionId = "chats",
                documentId = chat.chatId,
                data = chatMap
            )
        } catch (e: io.appwrite.exceptions.AppwriteException) {
            if (e.code == 409) {
                try {
                    databases.updateDocument(
                        databaseId = "qbase_db",
                        collectionId = "chats",
                        documentId = chat.chatId,
                        data = chatMap
                    )
                } catch (ex: Exception) {
                    Log.e("ChatFirestoreRepository", "Failed to update chat in Appwrite", ex)
                }
            } else {
                Log.e("ChatFirestoreRepository", "Failed to create chat in Appwrite", e)
            }
        } catch (e: Exception) {
            Log.e("ChatFirestoreRepository", "Failed to create chat in Appwrite", e)
        }
    }

    suspend fun addParticipantToFirestore(chatId: String, userId: String) {
        try {
            val doc = databases.getDocument(
                databaseId = "qbase_db",
                collectionId = "chats",
                documentId = chatId
            )
            @Suppress("UNCHECKED_CAST")
            val participants = (doc.data["participantIds"] as? List<String> ?: emptyList()).toMutableList()
            if (!participants.contains(userId)) {
                participants.add(userId)
                databases.updateDocument(
                    databaseId = "qbase_db",
                    collectionId = "chats",
                    documentId = chatId,
                    data = mapOf("participantIds" to participants)
                )
            }
        } catch (e: Exception) {
            Log.e("ChatFirestoreRepository", "Failed to add participant in Appwrite", e)
        }
    }

    suspend fun removeParticipantFromFirestore(chatId: String, userId: String) {
        try {
            val doc = databases.getDocument(
                databaseId = "qbase_db",
                collectionId = "chats",
                documentId = chatId
            )
            @Suppress("UNCHECKED_CAST")
            val participants = (doc.data["participantIds"] as? List<String> ?: emptyList()).toMutableList()
            if (participants.contains(userId)) {
                participants.remove(userId)
                databases.updateDocument(
                    databaseId = "qbase_db",
                    collectionId = "chats",
                    documentId = chatId,
                    data = mapOf("participantIds" to participants)
                )
            }
        } catch (e: Exception) {
            Log.e("ChatFirestoreRepository", "Failed to remove participant in Appwrite", e)
        }
    }

    suspend fun clearChatMessagesOnFirestore(chatId: String) {
        try {
            val response = databases.listDocuments(
                databaseId = "qbase_db",
                collectionId = "messages",
                queries = listOf(Query.equal("chatId", chatId))
            )
            response.documents.forEach { doc ->
                try {
                    databases.deleteDocument(
                        databaseId = "qbase_db",
                        collectionId = "messages",
                        documentId = doc.id
                    )
                } catch (e: Exception) {
                    Log.w("ChatFirestoreRepository", "Failed to delete message document ${doc.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e("ChatFirestoreRepository", "Failed to clear messages in Appwrite", e)
        }
    }

    suspend fun deleteChatOnFirestore(chatId: String) {
        try {
            clearChatMessagesOnFirestore(chatId)
            databases.deleteDocument(
                databaseId = "qbase_db",
                collectionId = "chats",
                documentId = chatId
            )
        } catch (e: Exception) {
            Log.e("ChatFirestoreRepository", "Failed to delete chat in Appwrite", e)
        }
    }

    suspend fun reportGroup(group: ChatEntity, reason: String) {
        val reporterId = getCurrentUserId() ?: throw IllegalStateException("User not authenticated")
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis(),
            "groupId" to group.chatId
        )
        try {
            databases.createDocument(
                databaseId = "qbase_db",
                collectionId = "reported_groups",
                documentId = ID.unique(),
                data = reportMap
            )
        } catch (e: Exception) {
            Log.e("ChatFirestoreRepository", "Failed to submit reported group ${group.chatId}", e)
        }
    }

    suspend fun reportMessage(message: MessageEntity, reason: String) {
        val reporterId = getCurrentUserId() ?: return
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis(),
            "messageId" to message.messageId
        )
        try {
            databases.createDocument(
                databaseId = "qbase_db",
                collectionId = "reported_messages",
                documentId = ID.unique(),
                data = reportMap
            )
        } catch (e: Exception) {
            Log.e("ChatFirestoreRepository", "Failed to submit reported message ${message.messageId}", e)
        }
    }
}