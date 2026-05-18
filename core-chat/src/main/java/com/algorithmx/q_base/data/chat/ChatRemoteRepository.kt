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
class ChatRemoteRepository @Inject constructor(
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
            "adminId" to (chat.adminIds.split(",").firstOrNull { it.isNotBlank() } ?: getCurrentUserId().orEmpty()),
            "adminIds" to chat.adminIds.split(",").filter { it.isNotBlank() },
            "createdAt" to System.currentTimeMillis() / 1000
        )

        val chatPermissions = listOf(
            io.appwrite.Permission.read(io.appwrite.Role.users()),
            io.appwrite.Permission.write(io.appwrite.Role.users()),
            io.appwrite.Permission.update(io.appwrite.Role.users()),
            io.appwrite.Permission.delete(io.appwrite.Role.users())
        )

        try {
            databases.createDocument(
                databaseId = "qbase_db",
                collectionId = "chats",
                documentId = chat.chatId,
                data = chatMap,
                permissions = chatPermissions
            )
        } catch (e: io.appwrite.exceptions.AppwriteException) {
            if (e.code == 409) {
                try {
                    databases.updateDocument(
                        databaseId = "qbase_db",
                        collectionId = "chats",
                        documentId = chat.chatId,
                        data = chatMap,
                        permissions = chatPermissions
                    )
                } catch (ex: Exception) {
                    Log.e("ChatRemoteRepository", "Failed to update chat in Appwrite", ex)
                }
            } else {
                Log.e("ChatRemoteRepository", "Failed to create chat in Appwrite", e)
            }
        } catch (e: Exception) {
            Log.e("ChatRemoteRepository", "Failed to create chat in Appwrite", e)
        }
    }

    suspend fun addParticipantToRemote(chatId: String, userId: String) {
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
            Log.e("ChatRemoteRepository", "Failed to add participant in Appwrite", e)
        }
    }

    suspend fun removeParticipantFromRemote(chatId: String, userId: String) {
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
            Log.e("ChatRemoteRepository", "Failed to remove participant in Appwrite", e)
        }
    }

    suspend fun promoteParticipantToAdminOnRemote(chatId: String, userId: String) {
        try {
            val doc = databases.getDocument(
                databaseId = "qbase_db",
                collectionId = "chats",
                documentId = chatId
            )
            @Suppress("UNCHECKED_CAST")
            val admins = (doc.data["adminIds"] as? List<String> ?: emptyList()).toMutableList()
            if (!admins.contains(userId)) {
                admins.add(userId)
                databases.updateDocument(
                    databaseId = "qbase_db",
                    collectionId = "chats",
                    documentId = chatId,
                    data = mapOf("adminIds" to admins)
                )
            }
        } catch (e: Exception) {
            Log.e("ChatRemoteRepository", "Failed to promote participant to admin in Appwrite", e)
        }
    }

    suspend fun demoteAdminOnRemote(chatId: String, userId: String) {
        try {
            val doc = databases.getDocument(
                databaseId = "qbase_db",
                collectionId = "chats",
                documentId = chatId
            )
            @Suppress("UNCHECKED_CAST")
            val admins = (doc.data["adminIds"] as? List<String> ?: emptyList()).toMutableList()
            if (admins.contains(userId)) {
                admins.remove(userId)
                databases.updateDocument(
                    databaseId = "qbase_db",
                    collectionId = "chats",
                    documentId = chatId,
                    data = mapOf("adminIds" to admins)
                )
            }
        } catch (e: Exception) {
            Log.e("ChatRemoteRepository", "Failed to demote admin in Appwrite", e)
        }
    }

    suspend fun clearChatMessagesOnRemote(chatId: String) {
        try {
            var hasMore = true
            while (hasMore) {
                val response = databases.listDocuments(
                    databaseId = "qbase_db",
                    collectionId = "messages",
                    queries = listOf(
                        Query.equal("chatId", chatId),
                        Query.limit(100)
                    )
                )
                if (response.documents.isEmpty()) {
                    hasMore = false
                } else {
                    response.documents.forEach { doc ->
                        try {
                            databases.deleteDocument(
                                databaseId = "qbase_db",
                                collectionId = "messages",
                                documentId = doc.id
                            )
                        } catch (e: Exception) {
                            Log.w("ChatRemoteRepository", "Failed to delete message document ${doc.id}", e)
                        }
                    }
                    if (response.documents.size < 100) {
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
                databaseId = "qbase_db",
                collectionId = "chats",
                documentId = chatId
            )
            Log.d("ChatRemoteRepository", "Successfully deleted chat document from Appwrite")
        } catch (e: Exception) {
            Log.e("ChatRemoteRepository", "Failed to delete chat in Appwrite, trying fallback to remove self", e)
            try {
                val doc = databases.getDocument("qbase_db", "chats", chatId)
                @Suppress("UNCHECKED_CAST")
                val participants = (doc.data["participantIds"] as? List<String> ?: emptyList()).toMutableList()
                val currentUserId = getCurrentUserId()
                if (currentUserId != null && participants.contains(currentUserId)) {
                    participants.remove(currentUserId)
                    databases.updateDocument(
                        databaseId = "qbase_db",
                        collectionId = "chats",
                        documentId = chatId,
                        data = mapOf("participantIds" to participants)
                    )
                    Log.d("ChatRemoteRepository", "Fallback: Successfully removed current user from chat participants list")
                }
            } catch (ex: Exception) {
                Log.e("ChatRemoteRepository", "Fallback failed to remove participant", ex)
            }
        }
    }

    suspend fun reportGroup(group: ChatEntity, reason: String) {
        val reporterId = getCurrentUserId() ?: throw IllegalStateException("User not authenticated")
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis() / 1000,
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
            Log.e("ChatRemoteRepository", "Failed to submit reported group ${group.chatId}", e)
        }
    }

    suspend fun reportMessage(message: MessageEntity, reason: String) {
        val reporterId = getCurrentUserId() ?: return
        val reportMap = mapOf(
            "reporterId" to reporterId,
            "reason" to reason,
            "reportedAt" to System.currentTimeMillis() / 1000,
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
            Log.e("ChatRemoteRepository", "Failed to submit reported message ${message.messageId}", e)
        }
    }
}
