package com.algorithmx.q_base.feature.chat.presentation
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.core.data.chat.ChatEntity
import com.algorithmx.q_base.core.data.chat.MessageEntity
import kotlinx.coroutines.launch
import java.util.UUID

fun ChatViewModel.startNewChat(userId: String, userName: String) {
    viewModelScope.launch {
        val uid = authRepository.currentUserId ?: return@launch
        // Check for existing P2P chat locally or on the cloud
        val existingChat = syncRepository.findExistingP2PChat(uid, userId)
        
        if (existingChat != null) {
            _currentChatId.value = existingChat.chatId
            _actionFeedback.emit("Opening existing conversation")
            _navigationEvents.emit(ChatNavEvent.NavigateToChatDetail(existingChat.chatId))
            if (userId != ChatViewModel.QBASE_AI_BOT_ID) {
                viewModelScope.launch {
                    try {
                        syncRepository.createChatOnRemote(existingChat)
                    } catch (_: Exception) {}
                }
            }
            return@launch
        }

        val chatId = UUID.randomUUID().toString()
        val newChat = ChatEntity(
            chatId = chatId,
            chatName = userName,
            isGroup = false,
            participantIds = "$uid,$userId",
            adminIds = listOf(uid)
        )
        chatDao.insertChat(newChat)
        
        // Log onto Firestore as well (don't sync AI chats to global chat list if they are local)
        if (userId != ChatViewModel.QBASE_AI_BOT_ID) {
            syncRepository.createChatOnRemote(newChat)
        }
        
        val initialMessage = if (userId == ChatViewModel.QBASE_AI_BOT_ID) 
            "Hello! I am Qbase AI. How can I assist you with your studies today?"
            else "Hi! I'd like to start a professional conversation."
        
        sendMessage(chatId, initialMessage, senderId = if (userId == ChatViewModel.QBASE_AI_BOT_ID) ChatViewModel.QBASE_AI_BOT_ID else uid)
        _currentChatId.value = chatId
        _navigationEvents.emit(ChatNavEvent.NavigateToChatDetail(chatId))
    }
}

fun ChatViewModel.startNewGroup(participantIds: List<String>, groupName: String) {
    viewModelScope.launch {
        val uid = authRepository.currentUserId ?: return@launch
        val chatId = UUID.randomUUID().toString()
        val allParticipants = (participantIds + uid).filter { it.isNotBlank() }.distinct().joinToString(",")
        
        val newChat = ChatEntity(
            chatId = chatId,
            chatName = groupName,
            isGroup = true,
            participantIds = allParticipants,
            adminIds = listOf(uid)
        )
        
        chatDao.insertChat(newChat)
        syncRepository.createChatOnRemote(newChat)
        
        sendMessage(chatId, "created the group \"$groupName\"", type = "DB_CHANGE", senderId = uid)
        _currentChatId.value = chatId
        _navigationEvents.emit(ChatNavEvent.NavigateToChatDetail(chatId))
    }
}

fun ChatViewModel.startAiChat() {
    startNewChat(ChatViewModel.QBASE_AI_BOT_ID, "Qbase AI")
}

fun ChatViewModel.sendMessage(chatId: String, text: String, type: String = "TEXT", senderId: String = currentUserId) {
    val messageId = UUID.randomUUID().toString()
    val timestamp = System.currentTimeMillis()
    
    viewModelScope.launch {
        val chat = chatDao.getChatById(chatId)
        val isAiChat = chat?.participantIds
            ?.split(",")
            ?.any { it.trim() == ChatViewModel.QBASE_AI_BOT_ID } == true
        
        val isOffline = !isAiChat && !isOnline.value
        
        val message = MessageEntity(
            messageId = messageId,
            chatId = chatId,
            senderId = senderId,
            payload = text,
            type = type,
            timestamp = timestamp,
            status = if (isOffline) "PENDING" else "SENT"
        )
        
        messageDao.insertMessage(message)
        
        if (!isAiChat) {
            if (isOffline) {
                _actionFeedback.emit("Offline: Message queued.")
                return@launch
            }
            
            try {
                syncRepository.sendMessage(message)
            } catch (e: com.algorithmx.q_base.data.sync.MissingEncryptionKeysException) {
                messageDao.updateMessageStatus(message.messageId, "FAILED")
                _actionFeedback.emit("Waiting for recipient encryption keys...")
            } catch (e: Exception) {
                // Generic network/timeout error during sending
                messageDao.updateMessageStatus(message.messageId, "PENDING")
                _actionFeedback.emit("Network error: Message queued.")
            }
        } else if (senderId == currentUserId) {
            // Trigger AI response if user sent a message to the bot
            handleAiChatResponse(chatId, text)
        }
    }
}

private fun ChatViewModel.handleAiChatResponse(chatId: String, userMessage: String) {
    viewModelScope.launch {
        _isAiLoading.value = true
        try {
            _actionFeedback.emit("Qbase AI is thinking...")
            val result = aiRepository.getAiAssistance(userMessage)
            result.onSuccess { reply ->
                sendMessage(chatId, reply, senderId = ChatViewModel.QBASE_AI_BOT_ID)
            }.onFailure { e ->
                sendMessage(chatId, "I'm sorry, I encountered an error: ${e.message}", senderId = ChatViewModel.QBASE_AI_BOT_ID)
            }
        } catch (e: Exception) {
            _actionFeedback.emit("AI Error: ${e.message}")
        } finally {
            _isAiLoading.value = false
        }
    }
}