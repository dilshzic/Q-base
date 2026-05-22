package com.algorithmx.q_base.sync.orchestration

import android.util.Base64
import android.util.Log
import com.algorithmx.q_base.data.chat.MessageEntity
import com.algorithmx.q_base.data.core.UserEntity
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch

class MissingEncryptionKeysException(message: String) : IllegalStateException(message)

suspend fun MessageSyncRepository.sendMessage(message: MessageEntity) {
    val chat = chatDao.getChatById(message.chatId)
    val participants = chat?.participantIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val senderUid = currentUserId ?: throw IllegalStateException("User not authenticated")

    if (participants.isEmpty()) {
        throw IllegalStateException("Cannot send message: No participants in chat.")
    }

    val (ciphertextPayload, sessionKeyHandle) = cryptoManager.encryptWithSessionKey(message.payload)
    val sessionKeyBytes = Base64.decode(sessionKeyHandle, Base64.NO_WRAP)

    val wrappedKeys = mutableMapOf<String, String>()
    val myFingerprint = cryptoManager.getPublicKeyFingerprint()
    
    val missingKeyUserIds = mutableListOf<String>()
    val wrapFailures = mutableListOf<String>()

    val deferredKeys = participants.map { targetId ->
        repositoryScope.async {
            val publicKeyBase64: String? = if (targetId == senderUid) {
                cryptoManager.initializeAndGetPublicKey()
            } else {
                val local = userDao.getUserById(targetId)
                var candidateKey: String? = local?.publicKey

                try {
                    val docData = databases.getDocument(
                        collectionId = "users",
                        documentId = targetId
                    ).getOrNull()
                    
                    val remoteKey = docData?.get("publicKey") as? String
                    if (!remoteKey.isNullOrBlank() && docData != null) {
                        candidateKey = remoteKey
                        val cached = UserEntity(
                            userId = targetId,
                            displayName = (docData["displayName"] as? String) ?: (local?.displayName ?: "Unknown"),
                            email = local?.email,
                            intro = (docData["intro"] as? String) ?: local?.intro,
                            profilePictureUrl = (docData["profilePictureUrl"] as? String) ?: local?.profilePictureUrl,
                            friendCode = (docData["friendCode"] as? String) ?: (local?.friendCode ?: ""),
                            publicKey = remoteKey,
                            isBanned = (docData["isBanned"] as? Boolean) ?: (local?.isBanned ?: false),
                            isPhotoVisible = (docData["isPhotoVisible"] as? Boolean) ?: (local?.isPhotoVisible ?: true)
                        )
                        userDao.insertUser(cached)
                    }
                } catch (e: Exception) {
                    Log.w("MessageSyncRepository", "Failed to refresh public key for $targetId", e)
                }
                candidateKey
            }
            targetId to publicKeyBase64
        }
    }

    val resolvedKeys = deferredKeys.awaitAll()
    
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

    if (missingKeyUserIds.isNotEmpty()) {
        throw MissingEncryptionKeysException(
            "Cannot send encrypted message: missing encryption keys for ${missingKeyUserIds.joinToString(", ")}. " +
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
            chatDao.getChatById(message.chatId)?.let { chat ->
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
            messageDao.insertMessage(message.copy(
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
        val pendingMessages = messageDao.getPendingMessages()
        Log.d("MessageSyncRepository", "Flushing ${pendingMessages.size} pending messages...")
        for (message in pendingMessages) {
            try {
                sendMessage(message)
                messageDao.updateMessageStatus(message.messageId, "SENT")
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