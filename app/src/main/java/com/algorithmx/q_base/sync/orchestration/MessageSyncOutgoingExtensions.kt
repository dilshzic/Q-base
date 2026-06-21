package com.algorithmx.q_base.sync.orchestration

import android.util.Base64
import android.util.Log
import com.algorithmx.q_base.core.data.chat.MessageEntity
import com.algorithmx.q_base.feature.chat.presentation.ChatViewModel
import kotlinx.coroutines.launch

open class MissingEncryptionKeysException(message: String) : IllegalStateException(message)
class UnreadableProfileException(message: String) : MissingEncryptionKeysException(message)

private suspend fun MessageSyncRepository.refreshProfiles(userIds: List<String>) {
    userIds
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .forEach { targetId ->
            try {
                val profile = profileRepository.syncUserProfile(targetId)
                // If it's a real user and we just got a profile, ensure it's in our local UserDao
                // so resolveWrappedKeys can find it in the same transaction/pass.
                profile?.let { p ->
                    userDao.insertUser(
                        com.algorithmx.q_base.core.data.UserEntity(
                            userId = p.userId,
                            displayName = p.displayName,
                            email = p.email,
                            intro = p.intro,
                            profilePictureUrl = p.profilePictureUrl,
                            friendCode = p.friendCode,
                            publicKey = p.publicKey,
                            isBanned = p.isBanned,
                            isPhotoVisible = p.isPhotoVisible
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("MessageSyncRepository", "Failed to refresh profile for key resolution: $targetId", e)
            }
        }
}

private suspend fun MessageSyncRepository.refreshProfilesForMessage(message: MessageEntity) {
    val participantIds = chatLocalDataSource.getChatById(message.chatId)
        ?.participantIds
        ?.split(",")
        .orEmpty()

    refreshProfiles(participantIds)
}

suspend fun MessageSyncRepository.sendMessage(message: MessageEntity) {
    var chat = chatLocalDataSource.getChatById(message.chatId)
    var participants = chat?.participantIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    if (participants.isEmpty()) {
        Log.d("MessageSyncRepository", "Missing chat info for ${message.chatId}. Attempting to fetch...")
        chatManagerRepository.get().fetchAndSyncChatMetadata(message.chatId)
        chat = chatLocalDataSource.getChatById(message.chatId)
        participants = chat?.participantIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }

    val senderUid = currentUserId ?: throw IllegalStateException("User not authenticated")

    if (participants.isEmpty()) {
        Log.w("MessageSyncRepository", "Skipping message flush: No participants found for chat ${message.chatId} after metadata refresh.")
        return
    }

    val (ciphertextPayload, sessionKeyHandle) = cryptoManager.encryptWithSessionKey(message.payload)
    val sessionKeyBytes = Base64.decode(sessionKeyHandle, Base64.NO_WRAP)

    val myFingerprint = cryptoManager.getPublicKeyFingerprint()

    suspend fun resolveWrappedKeys(refreshAndRetry: Boolean): Map<String, String> {
        val wrappedKeys = mutableMapOf<String, String>()
        val missingKeyUserIds = mutableListOf<String>()
        val wrapFailures = mutableListOf<String>()

        val resolvedKeys = participants.map { targetId ->
            val publicKeyBase64: String? = when (targetId) {
                senderUid -> cryptoManager.initializeAndGetPublicKey()
                ChatViewModel.QBASE_AI_BOT_ID -> "AI_BOT_NO_KEY" // Special marker
                else -> userDao.getUserById(targetId)?.publicKey
            }

            targetId to publicKeyBase64
        }

        resolvedKeys.forEach { (targetId, publicKeyBase64) ->
            if (publicKeyBase64.isNullOrBlank()) {
                missingKeyUserIds.add(targetId)
            } else if (publicKeyBase64 == "AI_BOT_NO_KEY") {
                // Skip encryption for AI bot, it will be handled by the server or ignored
                wrappedKeys[targetId] = "plain"
            } else {
                try {
                    wrappedKeys[targetId] = cryptoManager.encryptSessionKey(sessionKeyBytes, publicKeyBase64)
                } catch (e: Exception) {
                    Log.e("MessageSyncRepository", "Key wrap failed for $targetId", e)
                    wrapFailures.add(targetId)
                }
            }
        }

        if (refreshAndRetry && (missingKeyUserIds.isNotEmpty() || wrapFailures.isNotEmpty())) {
            refreshProfiles(missingKeyUserIds + wrapFailures)
            return resolveWrappedKeys(refreshAndRetry = false)
        }

        if (missingKeyUserIds.isNotEmpty()) {
            val unreadableProfileUserIds = missingKeyUserIds.filter { userDao.getUserById(it) == null }
            val missingKeyOnlyUserIds = missingKeyUserIds - unreadableProfileUserIds.toSet()

            if (unreadableProfileUserIds.isNotEmpty()) {
                throw UnreadableProfileException(
                    "Cannot send encrypted message: peer profile is unreadable for ${unreadableProfileUserIds.joinToString(", ")}. " +
                        "Check Appwrite users row permissions."
                )
            }

            throw MissingEncryptionKeysException(
                "Cannot send encrypted message: missing encryption keys for ${missingKeyOnlyUserIds.joinToString(", ")}. " +
                    "Ask them to sign in at least once on the latest app version."
            )
        }

        if (wrapFailures.isNotEmpty()) {
            throw MissingEncryptionKeysException(
                "Cannot send encrypted message: failed to encrypt for ${wrapFailures.joinToString(", ")}."
            )
        }

        val missingWrappedRecipients = participants.filter { wrappedKeys[it].isNullOrBlank() }
        if (missingWrappedRecipients.isNotEmpty()) {
            throw MissingEncryptionKeysException(
                "Security abort: missing wrapped session keys for ${missingWrappedRecipients.joinToString(", ")}."
            )
        }

        return wrappedKeys
    }

    val wrappedKeys = resolveWrappedKeys(refreshAndRetry = true)

    val messageMap = mapOf(
        "chatId" to message.chatId,
        "senderId" to message.senderId,
        "type" to message.type,
        "payload" to ciphertextPayload,
        "wrappedKey" to serializeWrappedKeys(wrappedKeys),
        "keyFingerprint" to myFingerprint.orEmpty(),
        "timestamp" to message.timestamp / 1000
    )
    
    repositoryScope.launch {
        try {
            chatLocalDataSource.getChatById(message.chatId)?.let { chat ->
                chatManagerRepository.get().createChatOnRemote(chat)
            }
        } catch (e: Exception) {
            Log.w("MessageSyncRepository", "Auto-ensure chat document exists failed", e)
        }
    }

    try {
        databases.createDocument(
            collectionId = "messages",
            documentId = message.messageId,
            data = messageMap
        ).getOrThrow()

        chatLocalDataSource.upsertMessage(message.copy(
            keyFingerprint = myFingerprint,
            wrappedKey = wrappedKeys[senderUid],
            decryptionStatus = "SUCCESS",
            status = "SENT"
        ))
    } catch (e: Exception) {
        Log.e("MessageSyncRepository", "Failed to send message to Appwrite", e)
        throw e
    }
}

suspend fun MessageSyncRepository.flushQueue() {
    try {
        val pendingMessages = chatLocalDataSource.getPendingMessages()
        Log.d("MessageSyncRepository", "Flushing ${pendingMessages.size} pending messages...")
        for (message in pendingMessages) {
            try {
                refreshProfilesForMessage(message)
                sendMessage(message)
                chatLocalDataSource.updateMessageStatus(message.messageId, "SENT")
                Log.d("MessageSyncRepository", "Successfully flushed message ${message.messageId}")
            } catch (e: Exception) {
                Log.e("MessageSyncRepository", "Failed to flush pending message ${message.messageId}", e)
            }
        }
    } catch (e: Exception) {
        Log.e("MessageSyncRepository", "Failed to get pending messages from local DB", e)
    }
}

suspend fun MessageSyncRepository.clearChatMessagesOnRemote(chatId: String) {
    chatRemoteRepository.clearChatMessagesOnRemote(chatId)
}
