package com.algorithmx.q_base.data.util

import android.content.Context
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.data.collections.SetQuestionCrossRef
import com.algorithmx.q_base.data.collections.CollectionExport
import com.algorithmx.q_base.data.collections.MockExport
import com.algorithmx.q_base.data.sessions.SessionDao
import com.algorithmx.q_base.data.sessions.SessionExport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val questionDao: QuestionDao,
    private val collectionDao: CollectionDao,
    private val sessionDao: SessionDao,
    private val cryptoManager: com.algorithmx.q_base.core_crypto.CryptoManager
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    suspend fun downloadAndImportMock(
        url: String, 
        symmetricKeyBase64: String? = null,
        sharedWithGroupId: String? = null,
        isAdminOnly: Boolean = false
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Download failed: ${response.code}"))
            
            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            
            var bytes = body.bytes()
            if (symmetricKeyBase64 != null) {
                bytes = cryptoManager.decryptFileContent(bytes, symmetricKeyBase64)
            }
            
            val zipInputStream = ZipInputStream(java.io.ByteArrayInputStream(bytes))
            var entry = zipInputStream.nextEntry
            var jsonString: String? = null
            
            while (entry != null) {
                if (entry.name == "data.json") {
                    jsonString = zipInputStream.bufferedReader().readText()
                    break
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()

            if (jsonString == null) return@withContext Result.failure(Exception("No data.json found in zip"))

            // Try to decode as CollectionExport first, then fallback to MockExport
            try {
                val collectionData = json.decodeFromString<CollectionExport>(jsonString)
                importCollectionData(collectionData, sharedWithGroupId, isAdminOnly)
            } catch (e: Exception) {
                try {
                    val mockData = json.decodeFromString<MockExport>(jsonString)
                    importMockData(mockData) // MockExport (Legacy) doesn't support shared flags yet
                } catch (e2: Exception) {
                    return@withContext Result.failure(Exception("Failed to decode data.json as CollectionExport or MockExport"))
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadAndImportSession(
        url: String,
        symmetricKeyBase64: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Download failed: ${response.code}"))
            
            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            
            var bytes = body.bytes()
            if (symmetricKeyBase64 != null) {
                bytes = cryptoManager.decryptFileContent(bytes, symmetricKeyBase64)
            }
            
            val zipInputStream = ZipInputStream(java.io.ByteArrayInputStream(bytes))
            var entry = zipInputStream.nextEntry
            var jsonString: String? = null
            
            while (entry != null) {
                if (entry.name == "data.json") {
                    jsonString = zipInputStream.bufferedReader().readText()
                    break
                }
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()

            if (jsonString == null) return@withContext Result.failure(Exception("No data.json found in zip"))

            val sessionData = json.decodeFromString<SessionExport>(jsonString)
            
            // Insert the StudySession and its attempts to the DB
            sessionDao.insertSession(sessionData.session)
            sessionDao.insertAttempts(sessionData.attempts)
            
            Result.success(sessionData.session.sessionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun importCollectionData(
        exportData: CollectionExport,
        sharedWithGroupId: String?,
        isAdminOnly: Boolean
    ) {
        // 1. Generate new UUIDs for questions to avoid conflicts
        val oldToNewQuestionIds = mutableMapOf<String, String>()
        val isShared = sharedWithGroupId != null
        
        val newQuestions = exportData.questions.map { qData ->
            val newId = if (isShared) qData.question.questionId else java.util.UUID.randomUUID().toString()
            oldToNewQuestionIds[qData.question.questionId] = newId
            qData.question.copy(questionId = newId)
        }
        
        val newOptions = exportData.questions.flatMap { qData ->
            val newId = oldToNewQuestionIds[qData.question.questionId]!!
            qData.options.map { it.copy(questionId = newId) }
        }
        
        val newAnswers = exportData.questions.mapNotNull { qData ->
            val newId = oldToNewQuestionIds[qData.question.questionId]!!
            qData.answer?.copy(questionId = newId)
        }
        
        val newCrossRefs = exportData.crossRefs.mapNotNull { ref ->
            oldToNewQuestionIds[ref.questionId]?.let { newId ->
                ref.copy(questionId = newId)
            }
        }

        // 2. Insert Collection
        val collection = exportData.collection.copy(
            isShared = sharedWithGroupId != null,
            sharedWithGroupId = sharedWithGroupId,
            isAdminOnly = isAdminOnly
        )
        collectionDao.insertStudyCollections(listOf(collection))
        
        // 3. Insert Sets
        collectionDao.insertSets(exportData.sets)
        
        // 4. Insert Questions, Options, and Answers
        questionDao.insertQuestions(newQuestions)
        questionDao.insertOptions(newOptions)
        questionDao.insertAnswers(newAnswers)
        
        // 5. Create CrossRefs
        collectionDao.insertCrossRefs(newCrossRefs)
    }

    private suspend fun importMockData(mockData: MockExport) {
        // Generate new UUIDs for questions
        val oldToNewQuestionIds = mutableMapOf<String, String>()
        
        val newQuestions = mockData.questions.map { qData ->
            val newId = java.util.UUID.randomUUID().toString()
            oldToNewQuestionIds[qData.question.questionId] = newId
            qData.question.copy(questionId = newId)
        }
        
        val newOptions = mockData.questions.flatMap { qData ->
            val newId = oldToNewQuestionIds[qData.question.questionId]!!
            qData.options.map { it.copy(questionId = newId) }
        }
        
        val newAnswers = mockData.questions.mapNotNull { qData ->
            val newId = oldToNewQuestionIds[qData.question.questionId]!!
            qData.answer?.copy(questionId = newId)
        }

        // 0. Ensure parent StudyCollection exists (FK requirement)
        val parentId = mockData.collection.parentCollectionId
        if (collectionDao.getStudyCollectionByIdOnce(parentId) == null) {
            collectionDao.insertStudyCollections(listOf(
                com.algorithmx.q_base.data.collections.StudyCollection(
                    collectionId = parentId,
                    name = "Imported Collection",
                    isUserCreated = true
                )
            ))
        }

        // 1. Insert Set (Was QuestionCollection)
        collectionDao.insertSets(listOf(mockData.collection))
        
        // 2. Insert Questions, Options, and Answers
        questionDao.insertQuestions(newQuestions)
        questionDao.insertOptions(newOptions)
        questionDao.insertAnswers(newAnswers)
        
        // 3. Create CrossRefs (SetQuestionCrossRef)
        val crossRefs = newQuestions.map { question ->
            SetQuestionCrossRef(
                setId = mockData.collection.setId,
                questionId = question.questionId
            )
        }
        collectionDao.insertCrossRefs(crossRefs)
    }
}
