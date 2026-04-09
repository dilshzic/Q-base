package com.algorithmx.q_base.data.util

import android.content.Context
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.data.collections.SetQuestionCrossRef
import com.algorithmx.q_base.data.collections.CollectionExport
import com.algorithmx.q_base.data.collections.MockExport
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
    private val cryptoManager: com.algorithmx.q_base.data.util.CryptoManager
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    suspend fun downloadAndImportMock(url: String, symmetricKeyBase64: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Download failed: \${response.code}"))
            
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
                importCollectionData(collectionData)
            } catch (e: Exception) {
                try {
                    val mockData = json.decodeFromString<MockExport>(jsonString)
                    importMockData(mockData)
                } catch (e2: Exception) {
                    return@withContext Result.failure(Exception("Failed to decode data.json as CollectionExport or MockExport"))
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun importCollectionData(exportData: CollectionExport) {
        // 1. Insert Collection
        collectionDao.insertStudyCollections(listOf(exportData.collection))
        
        // 2. Insert Sets
        collectionDao.insertSets(exportData.sets)
        
        // 3. Insert Questions, Options, and Answers
        val questions = exportData.questions.map { it.question }
        val options = exportData.questions.flatMap { it.options }
        val answers = exportData.questions.mapNotNull { it.answer }
        
        questionDao.insertQuestions(questions)
        questionDao.insertOptions(options)
        questionDao.insertAnswers(answers)
        
        // 4. Create CrossRefs
        collectionDao.insertCrossRefs(exportData.crossRefs)
    }

    private suspend fun importMockData(mockData: MockExport) {
        // 1. Insert Set (Was QuestionCollection)
        collectionDao.insertSets(listOf(mockData.collection))
        
        // 2. Insert Questions, Options, and Answers
        val questions = mockData.questions.map { it.question }
        val options = mockData.questions.flatMap { it.options }
        val answers = mockData.questions.mapNotNull { it.answer }
        
        questionDao.insertQuestions(questions)
        questionDao.insertOptions(options)
        questionDao.insertAnswers(answers)
        
        // 3. Create CrossRefs (SetQuestionCrossRef)
        val crossRefs = questions.map { question ->
            SetQuestionCrossRef(
                setId = mockData.collection.setId,
                questionId = question.questionId
            )
        }
        collectionDao.insertCrossRefs(crossRefs)
    }
}
