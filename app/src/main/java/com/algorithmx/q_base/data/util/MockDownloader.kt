package com.algorithmx.q_base.data.util

import android.content.Context
import com.algorithmx.q_base.data.dao.CategoryDao
import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.entity.CollectionQuestionCrossRef
import com.algorithmx.q_base.data.model.MockExport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockDownloader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val questionDao: QuestionDao,
    private val categoryDao: CategoryDao
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    suspend fun downloadAndImportMock(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) return@withContext Result.failure(Exception("Download failed: \${response.code}"))
            
            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            
            val zipInputStream = ZipInputStream(body.byteStream())
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

            val mockData = json.decodeFromString<MockExport>(jsonString)
            importMockData(mockData)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun importMockData(mockData: MockExport) {
        // 1. Insert Collection
        categoryDao.insertCollections(listOf(mockData.collection))
        
        // 2. Insert Questions, Options, and Answers
        val questions = mockData.questions.map { it.question }
        val options = mockData.questions.flatMap { it.options }
        val answers = mockData.questions.mapNotNull { it.answer }
        
        questionDao.insertQuestions(questions)
        questionDao.insertOptions(options)
        questionDao.insertAnswers(answers)
        
        // 3. Create CrossRefs
        val crossRefs = questions.map { question ->
            CollectionQuestionCrossRef(
                collectionId = mockData.collection.collectionId,
                questionId = question.questionId
            )
        }
        categoryDao.insertCrossRefs(crossRefs)
    }
}
