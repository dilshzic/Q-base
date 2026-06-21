package com.algorithmx.q_base.sync.orchestration

import android.util.Log
import com.algorithmx.q_base.data.sync.ActionQueueDao
import com.algorithmx.q_base.core.data.auth.ProfileRepository
import com.algorithmx.q_base.core.data.chat.ChatRemoteRepository
import com.algorithmx.q_base.data.collections.ProblemReportDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UniversalQueueManager @Inject constructor(
    private val actionQueueDao: ActionQueueDao,
    private val chatRemoteRepository: ChatRemoteRepository,
    private val profileRepository: ProfileRepository,
    private val problemReportDao: ProblemReportDao,
    private val reportSyncRepository: Lazy<ReportSyncRepository>
) {
    suspend fun flushUniversalQueue() {
        withContext(Dispatchers.IO) {
            val pendingActions = actionQueueDao.getPendingActions()
            if (pendingActions.isEmpty()) return@withContext

            Log.d("UniversalQueueManager", "Flushing ${pendingActions.size} pending offline actions...")

            for (action in pendingActions) {
                try {
                    val success = processAction(action)
                    if (success) {
                        actionQueueDao.deleteAction(action)
                    } else {
                        val updated = action.copy(retryCount = action.retryCount + 1)
                        if (updated.retryCount >= 5) {
                            Log.e("UniversalQueueManager", "Action ${action.actionId} exceeded max retries. Dropping.")
                            actionQueueDao.deleteAction(action)
                        } else {
                            actionQueueDao.updateAction(updated)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("UniversalQueueManager", "Failed to process action ${action.actionType}", e)
                    val updated = action.copy(retryCount = action.retryCount + 1)
                    if (updated.retryCount >= 5) {
                        actionQueueDao.deleteAction(action)
                    } else {
                        actionQueueDao.updateAction(updated)
                    }
                }
            }
        }
    }

    private suspend fun processAction(action: OfflineActionEntity): Boolean {
        val payload = Json.parseToJsonElement(action.payloadJson).jsonObject
        
        return when (action.actionType) {
            "ADD_PARTICIPANT" -> {
                val chatId = payload["chatId"]?.jsonPrimitive?.content ?: return false
                val userId = payload["userId"]?.jsonPrimitive?.content ?: return false
                chatRemoteRepository.addParticipantToRemote(chatId, userId)
                true
            }
            "REMOVE_PARTICIPANT" -> {
                val chatId = payload["chatId"]?.jsonPrimitive?.content ?: return false
                val userId = payload["userId"]?.jsonPrimitive?.content ?: return false
                chatRemoteRepository.removeParticipantFromRemote(chatId, userId)
                true
            }
            "PROMOTE_ADMIN" -> {
                val chatId = payload["chatId"]?.jsonPrimitive?.content ?: return false
                val userId = payload["userId"]?.jsonPrimitive?.content ?: return false
                chatRemoteRepository.promoteParticipantToAdminOnRemote(chatId, userId)
                true
            }
            "DEMOTE_ADMIN" -> {
                val chatId = payload["chatId"]?.jsonPrimitive?.content ?: return false
                val userId = payload["userId"]?.jsonPrimitive?.content ?: return false
                chatRemoteRepository.demoteAdminOnRemote(chatId, userId)
                true
            }
            "DELETE_CHAT" -> {
                val chatId = payload["chatId"]?.jsonPrimitive?.content ?: return false
                chatRemoteRepository.deleteChatOnRemote(chatId)
                true
            }
            "REPORT_GROUP" -> {
                val chatId = payload["chatId"]?.jsonPrimitive?.content ?: return false
                val reason = payload["reason"]?.jsonPrimitive?.content ?: return false
                // Dummy ChatEntity just to carry the ID for reportGroup
                val dummyGroup = com.algorithmx.q_base.core.data.chat.ChatEntity(
                    chatId = chatId,
                    chatName = null,
                    isGroup = true,
                    participantIds = ""
                )
                chatRemoteRepository.reportGroup(dummyGroup, reason)
                true
            }
            "REPORT_MESSAGE" -> {
                val messageId = payload["messageId"]?.jsonPrimitive?.content ?: return false
                val reason = payload["reason"]?.jsonPrimitive?.content ?: return false
                // Dummy MessageEntity just to carry the ID for reportMessage
                val dummyMessage = com.algorithmx.q_base.core.data.chat.MessageEntity(
                    messageId = messageId, chatId = "", senderId = "", payload = "", type = "TEXT", timestamp = 0L
                )
                chatRemoteRepository.reportMessage(dummyMessage, reason)
                true
            }
            "UPDATE_PROFILE" -> {
                val userId = payload["userId"]?.jsonPrimitive?.content ?: return false
                val email = payload["email"]?.jsonPrimitive?.content ?: ""
                val displayName = payload["displayName"]?.jsonPrimitive?.content ?: ""
                val profilePictureUrl = payload["profilePictureUrl"]?.jsonPrimitive?.content?.takeIf { it != "null" }
                val friendCode = payload["friendCode"]?.jsonPrimitive?.content ?: ""
                val intro = payload["intro"]?.jsonPrimitive?.content ?: ""
                val publicKey = payload["publicKey"]?.jsonPrimitive?.content?.takeIf { it != "null" }
                val isBanned = payload["isBanned"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
                val isPhotoVisible = payload["isPhotoVisible"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true

                val updatedProfile = com.algorithmx.q_base.core.data.auth.UserProfile(
                    userId = userId,
                    email = email,
                    displayName = displayName,
                    profilePictureUrl = profilePictureUrl,
                    friendCode = friendCode,
                    intro = intro,
                    publicKey = publicKey,
                    isBanned = isBanned,
                    isPhotoVisible = isPhotoVisible
                )

                val result = profileRepository.updateProfile(updatedProfile)
                result.isSuccess
            }
            "REPORT_SESSION" -> {
                val sessionId = payload["sessionId"]?.jsonPrimitive?.content ?: return false
                val reason = payload["reason"]?.jsonPrimitive?.content ?: return false
                try {
                    reportSyncRepository.get().submitSessionReportInternal(sessionId, reason)
                    true
                } catch (e: Exception) {
                    false
                }
            }
            "REPORT_QUESTION" -> {
                val questionId = payload["questionId"]?.jsonPrimitive?.content ?: return false
                val reason = payload["reason"]?.jsonPrimitive?.content ?: return false
                val contentJson = payload["contentJson"]?.jsonPrimitive?.content ?: return false
                try {
                    reportSyncRepository.get().submitQuestionReportInternal(questionId, reason, contentJson)
                    true
                } catch (e: Exception) {
                    false
                }
            }
            "REPORT_COLLECTION" -> {
                val collectionId = payload["collectionId"]?.jsonPrimitive?.content ?: return false
                val reason = payload["reason"]?.jsonPrimitive?.content ?: return false
                val contentJson = payload["contentJson"]?.jsonPrimitive?.content ?: return false
                try {
                    reportSyncRepository.get().submitCollectionReportInternal(collectionId, reason, contentJson)
                    true
                } catch (e: Exception) {
                    false
                }
            }
            else -> {
                Log.w("UniversalQueueManager", "Unknown action type: ${action.actionType}")
                false // Unknown action, drop it by returning false (it'll eventually expire)
            }
        }
    }
}