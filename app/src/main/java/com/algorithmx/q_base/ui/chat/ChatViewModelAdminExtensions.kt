package com.algorithmx.q_base.ui.chat

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.chat.isAdmin
import com.algorithmx.q_base.data.chat.MessageEntity
import kotlinx.coroutines.launch

fun ChatViewModel.addParticipant(chatId: String, userId: String) {
    viewModelScope.launch {
        val chat = chatDao.getChatById(chatId) ?: return@launch
        
        // Admin Check
        if (chat.isGroup && !chat.isAdmin(currentUserId)) {
            _actionFeedback.emit("Only an admin can add participants")
            return@launch
        }

        val currentParticipants = chat.participantIds
        val updatedParticipants = if (currentParticipants.isEmpty()) userId else "$currentParticipants,$userId"
        
        chatDao.updateParticipants(chatId, updatedParticipants)
        syncRepository.addParticipantToRemote(chatId, userId)
        
        // System message for participant added
        sendMessage(chatId, "added a new participant", type = "DB_CHANGE")
        
        _actionFeedback.emit("Participant added successfully")
    }
}

fun ChatViewModel.removeParticipant(chatId: String, userId: String) {
    viewModelScope.launch {
        val chat = chatDao.getChatById(chatId) ?: return@launch
        
        // Admin Check
        if (!chat.isAdmin(currentUserId)) {
            _actionFeedback.emit("Only an admin can remove participants")
            return@launch
        }

        val currentParticipants = chat.participantIds.split(",").toMutableList()
        if (currentParticipants.contains(userId)) {
            currentParticipants.remove(userId)
            val updatedParticipants = currentParticipants.joinToString(",")
            
            val currentAdmins = chat.adminIds.toMutableList()
            currentAdmins.remove(userId)

            chatDao.insertChat(chat.copy(
                participantIds = updatedParticipants,
                adminIds = currentAdmins
            ))
            syncRepository.removeParticipantFromRemote(chatId, userId)
            
            sendMessage(chatId, "removed a participant", type = "DB_CHANGE")
            _actionFeedback.emit("Participant removed successfully")
        }
    }
}

fun ChatViewModel.promoteParticipantToAdmin(chatId: String, userId: String) {
    viewModelScope.launch {
        val chat = chatDao.getChatById(chatId) ?: return@launch
        
        // Admin Check
        if (!chat.isAdmin(currentUserId)) {
            _actionFeedback.emit("Only an admin can promote members")
            return@launch
        }

        val currentAdmins = chat.adminIds.toMutableList()
        if (!currentAdmins.contains(userId)) {
            currentAdmins.add(userId)

            chatDao.insertChat(chat.copy(adminIds = currentAdmins))
            syncRepository.promoteParticipantToAdminOnRemote(chatId, userId)
            
            sendMessage(chatId, "promoted a member to admin", type = "DB_CHANGE")
            _actionFeedback.emit("Participant promoted to Admin successfully")
        }
    }
}

fun ChatViewModel.demoteAdmin(chatId: String, userId: String) {
    viewModelScope.launch {
        val chat = chatDao.getChatById(chatId) ?: return@launch
        
        // Admin Check
        if (!chat.isAdmin(currentUserId)) {
            _actionFeedback.emit("Only an admin can demote admins")
            return@launch
        }

        val currentAdmins = chat.adminIds.toMutableList()
        if (currentAdmins.contains(userId)) {
            currentAdmins.remove(userId)

            chatDao.insertChat(chat.copy(adminIds = currentAdmins))
            syncRepository.demoteAdminOnRemote(chatId, userId)
            
            sendMessage(chatId, "demoted an admin to member", type = "DB_CHANGE")
            _actionFeedback.emit("Admin demoted to member successfully")
        }
    }
}

fun ChatViewModel.leaveGroup(chatId: String) {
    viewModelScope.launch {
        val chat = chatDao.getChatById(chatId) ?: return@launch
        val currentParticipants = chat.participantIds.split(",").toMutableList()
        currentParticipants.remove(currentUserId)
        val updatedParticipants = currentParticipants.joinToString(",")
        
        if (updatedParticipants.isEmpty()) {
            chatDao.deleteChatById(chatId)
            messageDao.deleteMessagesByChatId(chatId)
            syncRepository.deleteChatOnRemote(chatId)
        } else {
            chatDao.deleteChatById(chatId)
            messageDao.deleteMessagesByChatId(chatId)
            syncRepository.removeParticipantFromRemote(chatId, currentUserId)
        }
        _actionFeedback.emit("You left the group")
    }
}

fun ChatViewModel.reportGroup(chatId: String, reason: String) {
    viewModelScope.launch {
        try {
            val chat = chatDao.getChatById(chatId) ?: return@launch
            syncRepository.reportGroup(chat, reason)
            chatDao.updateReportedStatus(chatId, true)
            _actionFeedback.emit("Group reported successfully.")
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to report group", e)
            _actionFeedback.emit("Failed to report group: ${e.message}")
        }
    }
}

fun ChatViewModel.reportUser(userId: String, reason: String) {
    viewModelScope.launch {
        try {
            val user = userDao.getUserById(userId) ?: return@launch
            syncRepository.reportUser(user, reason)
            _actionFeedback.emit("User reported successfully.")
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Failed to report user", e)
            _actionFeedback.emit("Failed to report user: ${e.message}")
        }
    }
}

fun ChatViewModel.reportMessage(message: MessageEntity, reason: String) {
    viewModelScope.launch {
        try {
            syncRepository.reportMessage(message, reason)
            _actionFeedback.emit("Message reported. Our moderation team will review this interaction.")
        } catch (e: Exception) {
            _actionFeedback.emit("Failed to report message: ${e.message}")
        }
    }
}

fun ChatViewModel.toggleMute(chatId: String, isMuted: Boolean) {
    viewModelScope.launch {
        chatDao.updateMutedStatus(chatId, isMuted)
        val chat = chatDao.getChatById(chatId)
        val label = if (chat?.isGroup == true) "Group" else "Chat"
        _actionFeedback.emit(if (isMuted) "$label muted" else "$label unmuted")
    }
}

fun ChatViewModel.toggleBlock(chatId: String, isBlocked: Boolean) {
    viewModelScope.launch {
        chatDao.updateBlockedStatus(chatId, isBlocked)
        val chat = chatDao.getChatById(chatId)
        val label = if (chat?.isGroup == true) "Group" else "Chat"
        _actionFeedback.emit(if (isBlocked) "$label blocked" else "$label unblocked")
    }
}

fun ChatViewModel.deleteChat(chatId: String) {
    syncRepository.deleteChatAndMessagesGlobally(chatId)
}

fun ChatViewModel.clearChatMessages(chatId: String) {
    viewModelScope.launch {
        try {
            messageDao.deleteMessagesByChatId(chatId)
            syncRepository.clearChatMessagesOnRemote(chatId)
            _actionFeedback.emit("Chat history cleared")
        } catch (e: Exception) {
            _actionFeedback.emit("Failed to clear chat: ${e.message}")
        }
    }
}
