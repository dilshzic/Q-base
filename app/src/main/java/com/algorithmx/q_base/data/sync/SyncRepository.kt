package com.algorithmx.q_base.data.sync

import com.algorithmx.q_base.data.chat.ChatEntity
import com.algorithmx.q_base.data.chat.MessageEntity
import com.algorithmx.q_base.data.collections.Question
import com.algorithmx.q_base.data.collections.QuestionOption
import com.algorithmx.q_base.data.collections.Answer
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.util.NotificationHelper
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepository @Inject constructor(
    private val chatManagerRepository: ChatManagerRepository,
    private val messageSyncRepository: MessageSyncRepository,
    private val sessionSyncRepository: SessionSyncRepository,
    private val collectionSyncRepository: CollectionSyncRepository,
    private val reportSyncRepository: ReportSyncRepository
) {
    val currentUserId: String?
        get() = messageSyncRepository.currentUserId

    fun observeAndSyncMessages(chatId: String): Flow<MessageEntity?> {
        return messageSyncRepository.observeAndSyncMessages(chatId)
    }

    suspend fun sendMessage(message: MessageEntity) {
        messageSyncRepository.sendMessage(message)
    }

    suspend fun flushQueue() {
        messageSyncRepository.flushQueue()
    }

    suspend fun addParticipantToRemote(chatId: String, userId: String) {
        chatManagerRepository.addParticipantToRemote(chatId, userId)
    }

    suspend fun removeParticipantFromRemote(chatId: String, userId: String) {
        chatManagerRepository.removeParticipantFromRemote(chatId, userId)
    }

    suspend fun promoteParticipantToAdminOnRemote(chatId: String, userId: String) {
        chatManagerRepository.promoteParticipantToAdminOnRemote(chatId, userId)
    }

    suspend fun demoteAdminOnRemote(chatId: String, userId: String) {
        chatManagerRepository.demoteAdminOnRemote(chatId, userId)
    }

    suspend fun createChatOnRemote(chat: ChatEntity) {
        chatManagerRepository.createChatOnRemote(chat)
    }

    fun observeAllIncomingMessages(notificationHelper: NotificationHelper): Flow<Unit> {
        return messageSyncRepository.observeAllIncomingMessages(notificationHelper)
    }

    suspend fun syncUserChatsFromRemote() {
        chatManagerRepository.syncUserChatsFromRemote()
    }

    suspend fun findExistingP2PChat(uid: String, userId: String): ChatEntity? {
        return chatManagerRepository.findExistingP2PChat(uid, userId)
    }

    suspend fun fetchAndSyncMessages(chatId: String) {
        messageSyncRepository.fetchAndSyncMessages(chatId)
    }

    suspend fun clearChatMessagesOnRemote(chatId: String) {
        messageSyncRepository.clearChatMessagesOnRemote(chatId)
    }

    suspend fun deleteChatOnRemote(chatId: String) {
        chatManagerRepository.deleteChatOnRemote(chatId)
    }

    fun deleteChatAndMessagesGlobally(chatId: String) {
        chatManagerRepository.deleteChatAndMessagesGlobally(chatId)
    }

    suspend fun getChatById(chatId: String): ChatEntity? {
        return chatManagerRepository.getChatById(chatId)
    }

    // Sessions
    suspend fun sendSessionInvite(
        chatId: String,
        sessionId: String,
        sessionTitle: String,
        downloadUrl: String,
        symmetricKey: String
    ) {
        sessionSyncRepository.sendSessionInvite(chatId, sessionId, sessionTitle, downloadUrl, symmetricKey)
    }

    suspend fun addSharedSessionToGroup(
        chatId: String,
        sessionId: String,
        sessionTitle: String,
        downloadUrl: String,
        symmetricKey: String
    ) {
        sessionSyncRepository.addSharedSessionToGroup(chatId, sessionId, sessionTitle, downloadUrl, symmetricKey)
    }

    fun observeSharedSessions(chatId: String): Flow<List<Map<String, Any>>> {
        return sessionSyncRepository.observeSharedSessions(chatId)
    }

    suspend fun sendSessionPatch(chatId: String, sessionId: String, op: String, data: JSONObject) {
        sessionSyncRepository.sendSessionPatch(chatId, sessionId, op, data)
    }

    // Collections
    suspend fun sendSyncRequest(targetUserId: String, targetCollectionId: String) {
        collectionSyncRepository.sendSyncRequest(targetUserId, targetCollectionId)
    }

    fun observeIncomingRequests(): Flow<List<SyncRequest>> {
        return collectionSyncRepository.observeIncomingRequests()
    }

    suspend fun uploadQuestionBankZip(zipFile: File): Pair<String, String> {
        return collectionSyncRepository.uploadQuestionBankZip(zipFile)
    }

    suspend fun deleteQuestionBankZip(fileId: String) {
        collectionSyncRepository.deleteQuestionBankZip(fileId)
    }

    suspend fun shareCollectionToGroup(chatId: String, collectionMetadata: Map<String, Any>) {
        collectionSyncRepository.shareCollectionToGroup(chatId, collectionMetadata)
    }

    suspend fun acknowledgeCollectionDownload(collectionId: String) {
        collectionSyncRepository.acknowledgeCollectionDownload(collectionId)
    }

    suspend fun applyCollectionMicroUpdate(payload: String) {
        collectionSyncRepository.applyCollectionMicroUpdate(payload)
    }

    suspend fun broadcastCollectionMicroUpdate(chatId: String, collectionId: String, diff: JSONObject) {
        collectionSyncRepository.broadcastCollectionMicroUpdate(chatId, collectionId, diff)
    }

    fun observeGroupLibrary(chatId: String): Flow<List<Map<String, Any>>> {
        return collectionSyncRepository.observeGroupLibrary(chatId)
    }

    suspend fun sendCollectionPatch(chatId: String, collectionId: String, op: String, data: JSONObject) {
        collectionSyncRepository.sendCollectionPatch(chatId, collectionId, op, data)
    }

    suspend fun requestCollectionAccess(chatId: String, collectionId: String) {
        collectionSyncRepository.requestCollectionAccess(chatId, collectionId)
    }

    fun observeAccessRequests(chatId: String): Flow<List<Map<String, Any>>> {
        return collectionSyncRepository.observeAccessRequests(chatId)
    }

    suspend fun grantCollectionAccess(chatId: String, collectionId: String, requesterId: String) {
        collectionSyncRepository.grantCollectionAccess(chatId, collectionId, requesterId)
    }

    // Reports
    suspend fun reportSession(sessionId: String, reason: String) {
        reportSyncRepository.reportSession(sessionId, reason)
    }

    suspend fun reportQuestion(
        question: Question,
        options: List<QuestionOption>,
        answer: Answer?,
        reason: String
    ) {
        reportSyncRepository.reportQuestion(question, options, answer, reason)
    }

    suspend fun reportGroup(group: ChatEntity, reason: String) {
        reportSyncRepository.reportGroup(group, reason)
    }

    suspend fun reportUser(user: UserEntity, reason: String) {
        reportSyncRepository.reportUser(user, reason)
    }

    suspend fun reportCollection(collection: StudyCollection, reason: String) {
        reportSyncRepository.reportCollection(collection, reason)
    }

    suspend fun reportMessage(message: MessageEntity, reason: String) {
        reportSyncRepository.reportMessage(message, reason)
    }
}
