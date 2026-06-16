package com.algorithmx.q_base.core.data.chat

import android.util.Log
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.ID
import com.algorithmx.q_base.core.data.backend.CoreDatabase
import com.algorithmx.q_base.core.data.backend.CoreQuery
import com.algorithmx.q_base.core.data.backend.CoreQueryOperator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRemoteRepository @Inject constructor(
    private val databases: CoreDatabase,
    private val appwriteAccount: Account
) {
    private suspend fun getCurrentUserId(): String? {
        return try {
            appwriteAccount.get().id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun createChatOnRemote(chat: ChatEntity) {
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
            "adminId" to (chat.adminIds.firstOrNull() ?: getCurrentUserId().orEmpty()),
            "adminIds" to chat.adminIds,
            "createdAt" to System.currentTimeMillis() / 1000
        )

        try {
            databases.createDocument(
                collectionId = "chats",
                documentId = chat.chatId,
                data = chatMap
            ).getOrThrow()
        } catch (e: io.appwrite.exceptions.AppwriteException) {
            if (e.code == 409) {
                try {
                    databases.updateDocument(
                        collectionId = "chats",
                        documentId = chat.chatId,
                        data = chatMap
                    ).getOrThrow()
                } catch (ex: Exception) {
                    Log.e("ChatRemoteRepository", "Failed to update chat in Appwrite", ex)
                }
            } else {
                Log.e("ChatRemoteRepository", "Failed to create chat in Appwrite", e)
            }
        } catch (e: Exception) {
            val unwrapped = (e as? java.lang.reflect.InvocationTargetException)?.targetException ?: e
            if (unwrapped is io.appwrite.exceptions.AppwriteException && unwrapped.code == 409) {
                try {
                    databases.updateDocument(
                        collectionId = "chats",
                        documentId = chat.chatId,
                        data = chatMap
                    ).getOrThrow()
                } catch (ex: Exception) {
                    Log.e("ChatRemoteRepository", "Failed to update chat in Appwrite", ex)
                }
            } else {
                Log.e("ChatRemoteRepository", "Failed to create chat in Appwrite", e)
            }
        }
    }

    suspend fun addParticipantToRemote(chatId: String, userId: String) {
        try {
            val doc = databases.getDocument(
                collectionId = "chats",
                documentId = chatId
            ).getOrThrow() ?: throw IllegalStateException("Chat not found")
            @Suppress("UNCHECKED_CAST")
            val participants = (doc["participantIds"] as? List<String> ?: emptyList()).toMutableList()
            if (!participants.contains(userId)) {
                participants.add(userId)
                databases.updateDocument(
                    collectionId = "chats",
                    documentId = chatId,
                    data = mapOf("participantIds" to participants)
                ).getOrThrow()
            }
        } catch (e: Exception) {
            Log.e("ChatRemoteRepository", "Operation failed in Appwrite", e)
            throw e
        }
    }

    suspend fun removeParticipantFromRemote(chatId: String, userId: String) {
        try {
            val doc = databases.getDocument(
                collectionId = "chats",
                documentId = chatId
            ).getOrThrow() ?: throw IllegalStateException("Chat not found")
            @Suppress("UNCHECKED_CAST")
            val participants = (doc["participantIds"] as? List<String> ?: emptyList()).toMutableList()
            if (participants.contains(userId)) {
                participants.remove(userId)
                databases.updateDocument(
                    collectionId = "chats",
                    documentId = chatId,
                    data = mapOf("participantIds" to participants)
                ).getOrThrow()
            }
        } catch (e: Exception) {
            Log.e("ChatRemoteRepository", "Operation failed in Appwrite", e)
            throw e
        }
    }

    suspend fun promoteParticipantToAdminOnRemote(chatId: String, userId: String) {
        try {
            val doc = databases.getDocument(
                collectionId = "chats",
                documentId = chatId
            ).getOrThrow() ?: throw IllegalStateException("Chat not found")
            @Suppress("UNCHECKED_CAST")
            val admins = (doc["adminIds"] as? List<String> ?: emptyList()).toMutableList()
            if (!admins.contains(userId)) {
                admins.add(userId)
                databases.updateDocument(
                    collectionId = "chats",
                    documentId = chatId,
                    data = mapOf("adminIds" to admins)
                ).getOrThrow()
            }
        } catch (e: Exception) {
            Log.e("ChatRemoteRepository", "Operation failed in Appwrite", e)
            throw e
        }
    }

    suspend fun demoteAdminOnRemote(chatId: String, userId: String) {
        try {
            val doc = databases.getDocument(
                collectionId = "chats",
                documentId = chatId
            ).getOrThrow() ?: throw IllegalStateException("Chat not found")
            @Suppress("UNCHECKED_CAST")
            val admins = (doc["adminIds"] as? List<String> ?: emptyList()).toMutableList()
            if (admins.contains(userId)) {
                admins.remove(userId)
                databases.updateDocument(
                    collectionId = "chats",
                    documentId = chatId,
                    data = mapOf("adminIds" to admins)
                ).getOrThrow()
            }
        } catch (e: Exception) {
            Log.e("ChatRemoteRepository", "Operation failed in Appwrite", e)
            throw e
        }
    }

    suspend fun updateChatNameOnRemote(chatId: String, newName: String) {
        try {
            databases.updateDocument(
                collectionId = "chats",
                documentId = chatId,
                data = mapOf("chatName" to newName)
            ).getOrThrow()
        } catch (e: Exception) {
            Log.e("ChatRemoteRepository", "Failed to update chat name in Appwrite", e)
            throw e
        }
    }

    suspend fun clearChatMessagesOnRemote(chatId: String) {
        try {
            var hasMore = true
            while (hasMore) {
                val queries = listOf(
                    CoreQuery("chatId", CoreQueryOperator.EQUAL, chatId)
                )
                val documents = databases.queryDocuments(
                    collectionId = "messages",
                    queries = queries
                ).getOrThrow()
                if (documents.isEmpty()) {
                    hasMore = false
                } else {
                    documents.forEach { doc ->
                        try {
                            databases.deleteDocument(
                                collectionId = "messages",
                                documentId = doc["\$id"] as String
                            ).getOrThrow()
                        } catch (e: Exception) {
                            Log.w("ChatRemoteRepository", "Failed to delete message document ${doc["\$id"]}", e)
                        }
                    }
                    if (documents.size < 25) {
                        hasMore = false
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ChatRemoteRepository", "Failed to clear messages in Appwrite", e)
        }
    }

    suspend fun deleteChatOnRemote(chatId: String) {
        try {
            clearChatMessagesOnRemote(chatId)
            databases.deleteDocument(
                collectionId = "chats",
                documentId = chatId
            ).getOrThrow()
            Log.d("ChatRemoteRepository", "Successfully deleted chat document from Appwrite")
        } catch (e: Exception) {
            Log.e("ChatRemoteRepository", "Failed to delete chat in Appwrite, trying fallback to remove self", e)
            try {
                val doc = databases.getDocument("chats", chatId).getOrThrow() ?: throw IllegalStateException("Chat not found")
                @Suppress("UNCHECKED_CAST")
                val participants = (doc["participantIds"] as? List<String> ?: emptyList()).toMutableList()
                val currentUserId = getCurrentUserId()
                if (currentUserId != null && participants.contains(currentUserId)) {
                    participants.remove(currentUserId)
                    databases.updateDocument(
                        collectionId = "chats",
                        documentId = chatId,
                        data = mapOf("participantIds" to participants)
                    ).getOrThrow()
                    Log.d("ChatRemoteRepository", "Fallback: Successfully removed current user from chat participants list")
                }
            } catch (ex: Exception) {
                Log.e("ChatRemoteRepository", "Fallback failed to remove participant", ex)
            }
        }
    }

    suspend fun reportGroup(group: ChatEntity, reason: String, sampleMessages: List<MessageEntity> = emptyList()) {
        val reporterId = getCurrentUserId() ?: throw IllegalStateException("User not authenticated")
        val contentMap = mapOf(
            "group" to group,
            "sampleMessages" to sampleMessages
        )
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to (System.currentTimeMillis() / 1000).toInt(),
            "groupId" to group.chatId,
            "contentJson" to com.google.gson.Gson().toJson(contentMap)
        )
        try {
            databases.createDocument(
                collectionId = "reported_groups",
                documentId = ID.unique(),
                data = reportMap
            ).getOrThrow()
        } catch (e: Exception) {
            Log.e("ChatRemoteRepository", "Operation failed in Appwrite", e)
            throw e
        }
    }

    suspend fun reportMessage(message: MessageEntity, reason: String) {
        val reporterId = getCurrentUserId() ?: return
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to (System.currentTimeMillis() / 1000).toInt(),
            "messageId" to message.messageId,
            "contentJson" to com.google.gson.Gson().toJson(message)
        )
        try {
            databases.createDocument(
                collectionId = "reported_messages",
                documentId = ID.unique(),
                data = reportMap
            ).getOrThrow()
        } catch (e: Exception) {
            Log.e("ChatRemoteRepository", "Operation failed in Appwrite", e)
            throw e
        }
    }
}
