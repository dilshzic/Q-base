package com.algorithmx.q_base.data.sync

import android.util.Log
import com.algorithmx.q_base.data.chat.MessageEntity
import com.algorithmx.q_base.data.collections.Answer
import com.algorithmx.q_base.data.collections.CollectionVersionLedgerEntity
import com.algorithmx.q_base.data.collections.Question
import com.algorithmx.q_base.data.collections.QuestionOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

suspend fun CollectionSyncRepository.applyCollectionMicroUpdate(payload: String) {
    withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(payload)
            val collectionId = json.getString("collectionId")
            val revisionId = json.getInt("revisionId")
            val diff = json.getJSONObject("diff")
            
            val ledger = collectionVersionLedgerDao.getLedgerForCollection(collectionId)
            val localRevision = ledger?.currentRevisionId ?: 0
            
            if (revisionId <= localRevision) {
                Log.d("CollectionSyncRepository", "Micro-update redundant: received $revisionId, local is $localRevision. Skipping.")
                return@withContext
            }
            
            if (revisionId > localRevision + 1) {
                Log.w("CollectionSyncRepository", "Sequence gap detected in collection micro-update: received $revisionId, expected ${localRevision + 1}. Triggering full library re-sync.")
                repositoryScope.launch {
                    try {
                        val doc = databases.getDocument("qbase_db", "shared_collections", collectionId)
                        val url = doc.data["downloadUrl"] as? String
                        val wrappedKeysStr = doc.data["wrappedKeys"] as? String ?: ""
                        val isAdminOnly = doc.data["isAdminOnly"] as? Boolean ?: false
                        val chatId = doc.data["chatId"] as? String ?: ""
                        
                        if (!url.isNullOrBlank() && wrappedKeysStr.isNotBlank()) {
                            val jsonObj = org.json.JSONObject(wrappedKeysStr)
                            val encKey = jsonObj.optString(currentUserId ?: "")
                            if (!encKey.isNullOrBlank()) {
                                val decResult = cryptoManager.decryptMessage(encKey)
                                if (decResult.isSuccess) {
                                    val symmetricKey = decResult.getOrNull()
                                    val result = mockDownloader.downloadAndImportMock(
                                        url = url,
                                        symmetricKeyBase64 = symmetricKey,
                                        sharedWithGroupId = chatId,
                                        isAdminOnly = isAdminOnly
                                    )
                                    if (result.isSuccess) {
                                        Log.d("CollectionSyncRepository", "Full sync completed successfully for collection $collectionId after sequence gap.")
                                    } else {
                                        Log.e("CollectionSyncRepository", "Full sync failed during sequence gap resolution: ${result.exceptionOrNull()?.message}")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CollectionSyncRepository", "Full sync failed during sequence gap resolution", e)
                    }
                }
                return@withContext
            }
            
            val action = diff.optString("action", "UPSERT_QUESTION")
            if (action == "UPSERT_QUESTION") {
                val questionObj = diff.getJSONObject("question")
                val qId = questionObj.getString("questionId")
                val stem = questionObj.getString("stem")
                
                val question = Question(
                    questionId = qId,
                    collection = null,
                    category = "General",
                    tags = "Group Synchronized",
                    questionType = "SBA",
                    stem = stem,
                    isPinned = false
                )
                questionDao.insertQuestion(question)
                questionDao.deleteOptionsForQuestion(qId)
                
                val optionsArr = questionObj.getJSONArray("options")
                val options = mutableListOf<QuestionOption>()
                for (i in 0 until optionsArr.length()) {
                    val optObj = optionsArr.getJSONObject(i)
                    options.add(
                        QuestionOption(
                            questionId = qId,
                            optionLetter = optObj.getString("optionLetter"),
                            optionText = optObj.getString("optionText"),
                            optionExplanation = null
                        )
                    )
                }
                questionDao.insertOptions(options)
                
                if (questionObj.has("answer")) {
                    val ansObj = questionObj.getJSONObject("answer")
                    val answer = Answer(
                        questionId = qId,
                        correctAnswerString = ansObj.getString("correctAnswerString"),
                        generalExplanation = ansObj.optString("generalExplanation", ""),
                        references = ansObj.optString("references", "")
                    )
                    questionDao.insertAnswer(answer)
                }
            }
            
            collectionVersionLedgerDao.insertLedger(
                CollectionVersionLedgerEntity(
                    collectionId = collectionId,
                    currentRevisionId = revisionId,
                    lastAppliedTimestamp = System.currentTimeMillis()
                )
            )
            Log.d("CollectionSyncRepository", "Successfully applied collection micro-update revision $revisionId for collection $collectionId")
        } catch (e: Exception) {
            Log.e("CollectionSyncRepository", "Failed to apply collection micro-update", e)
        }
    }
}

suspend fun CollectionSyncRepository.broadcastCollectionMicroUpdate(chatId: String, collectionId: String, diff: JSONObject) {
    withContext(Dispatchers.IO) {
        try {
            val ledger = collectionVersionLedgerDao.getLedgerForCollection(collectionId)
            val newRevision = (ledger?.currentRevisionId ?: 0) + 1
            
            val payloadObj = JSONObject()
            payloadObj.put("collectionId", collectionId)
            payloadObj.put("revisionId", newRevision)
            payloadObj.put("diff", diff)
            
            val payloadStr = payloadObj.toString()
            
            val message = MessageEntity(
                messageId = UUID.randomUUID().toString(),
                chatId = chatId,
                senderId = currentUserId ?: "",
                payload = payloadStr,
                type = "COLLECTION_MICRO_UPDATE",
                timestamp = System.currentTimeMillis(),
                decryptionStatus = "SUCCESS",
                keyFingerprint = "",
                wrappedKey = ""
            )
            messageSyncRepository.get().sendMessage(message)
            
            collectionVersionLedgerDao.insertLedger(
                CollectionVersionLedgerEntity(
                    collectionId = collectionId,
                    currentRevisionId = newRevision,
                    lastAppliedTimestamp = System.currentTimeMillis()
                )
            )
            Log.d("CollectionSyncRepository", "Broadcasted collection micro-update revision $newRevision for collection $collectionId")
        } catch (e: Exception) {
            Log.e("CollectionSyncRepository", "Failed to broadcast collection micro-update", e)
        }
    }
}

suspend fun CollectionSyncRepository.sendCollectionPatch(chatId: String, collectionId: String, op: String, data: JSONObject) {
    val patchObj = JSONObject()
    patchObj.put("collectionId", collectionId)
    patchObj.put("op", op)
    patchObj.put("data", data)
    
    val message = MessageEntity(
        messageId = UUID.randomUUID().toString(),
        chatId = chatId,
        senderId = currentUserId ?: "",
        payload = patchObj.toString(),
        type = "COLLECTION_PATCH",
        timestamp = System.currentTimeMillis()
    )
    messageSyncRepository.get().sendMessage(message)
}

suspend fun CollectionSyncRepository.applyCollectionPatch(jsonString: String) {
    try {
        val patch = JSONObject(jsonString)
        val op = patch.getString("op")
        val collectionId = patch.getString("collectionId")
        val data = patch.getJSONObject("data")

        when (op) {
            "UPSERT_QUESTION" -> {
                val qId = data.getString("id")
                val collectionName = data.getString("collectionName")
                val qText = data.getString("text")
                val category = data.optString("category", "General")
                val tags = data.optString("tags", "")

                val question = Question(
                    questionId = qId,
                    collection = collectionName,
                    category = category,
                    tags = tags,
                    questionType = "Multiple Choice",
                    stem = qText,
                    isPinned = false
                )
                questionDao.insertQuestion(question)

                questionDao.deleteOptionsForQuestion(qId)
                val optionsArray = data.getJSONArray("options")
                val letters = listOf("A", "B", "C", "D", "E", "F")
                for (i in 0 until optionsArray.length()) {
                    val optText = optionsArray.getString(i)
                    questionDao.insertOption(QuestionOption(
                        questionId = qId,
                        optionLetter = letters.getOrNull(i) ?: "?",
                        optionText = optText,
                        optionExplanation = null
                    ))
                }

                val correctAns = data.getString("correctAnswer")
                questionDao.insertAnswer(Answer(
                    questionId = qId,
                    correctAnswerString = correctAns,
                    generalExplanation = ""
                ))
                
                collectionDao.updateStudyCollectionTimestamp(collectionId, System.currentTimeMillis())
            }
            "DELETE_QUESTION" -> {
                val qId = data.getString("id")
                questionDao.deleteQuestionById(qId)
                collectionDao.updateStudyCollectionTimestamp(collectionId, System.currentTimeMillis())
            }
        }
    } catch (e: Exception) {
        Log.e("CollectionSyncRepository", "Failed to apply collection patch", e)
    }
}
