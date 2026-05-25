package com.algorithmx.q_base.sync.orchestration

import android.util.Base64
import android.util.Log
import com.algorithmx.q_base.core.data.chat.MessageEntity
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
                profileRepository.syncUserProfile(targetId)
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
    val chat = chatLocalDataSource.getChatById(message.chatId)
    val participants = chat?.participantIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val senderUid = currentUserId ?: throw IllegalStateException("User not authenticated")

    if (participants.isEmpty()) {
        throw IllegalStateException("Cannot send message: No participants in chat.")
    }

    val (ciphertextPayload, sessionKeyHandle) = cryptoManager.encryptWithSessionKey(message.payload)
    val sessionKeyBytes = Base64.decode(sessionKeyHandle, Base64.NO_WRAP)

    val myFingerprint = cryptoManager.getPublicKeyFingerprint()

    suspend fun resolveWrappedKeys(refreshAndRetry: Boolean): Map<String, String> {
        val wrappedKeys = mutableMapOf<String, String>()
        val missingKeyUserIds = mutableListOf<String>()
        val wrapFailures = mutableListOf<String>()

        val resolvedKeys = participants.map { targetId ->
            val publicKeyBase64: String? = if (targetId == senderUid) {
                cryptoManager.initializeAndGetPublicKey()
            } else {
                userDao.getUserById(targetId)?.publicKey
            }

            targetId to publicKeyBase64
        }

        resolvedKeys.forEach { (targetId, publicKeyBase64) ->
            if (publicKeyBase64.isNullOrBlank()) {
                missingKeyUserIds.add(targetId)
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

        repositoryScope.launch {
            chatLocalDataSource.upsertMessage(message.copy(
                keyFingerprint = myFingerprint,
                wrappedKey = wrappedKeys[senderUid],
                decryptionStatus = "SUCCESS"
            ))
        }
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
