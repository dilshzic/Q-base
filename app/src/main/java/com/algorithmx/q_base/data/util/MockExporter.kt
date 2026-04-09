package com.algorithmx.q_base.data.util

import android.content.Context
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.collections.CollectionExport
import com.algorithmx.q_base.data.collections.QuestionExport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MockExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val collectionDao: CollectionDao,
    private val questionDao: QuestionDao
) {
    private val json = Json { prettyPrint = true }

    suspend fun exportCollection(collectionId: String): File? = withContext(Dispatchers.IO) {
        val collection: StudyCollection = collectionDao.getStudyCollectionByIdOnce(collectionId) ?: return@withContext null
        val sets = collectionDao.getSetsByStudyCollectionIdOnce(collectionId)
        val setIds = sets.map { it.setId }
        
        val crossRefs = collectionDao.getCrossRefsForSetsBatch(setIds)
        val questionIds = crossRefs.map { it.questionId }.distinct()
        
        val questionExports = questionIds.mapNotNull { qId ->
            val question = questionDao.getQuestionById(qId) ?: return@mapNotNull null
            val options = questionDao.getOptionsForQuestionOnce(qId)
            val answer = questionDao.getAnswerForQuestionOnce(qId)
            QuestionExport(question, options, answer)
        }
        
        val exportData = CollectionExport(collection, sets, questionExports, crossRefs)
        val jsonString = json.encodeToString(exportData)
        
        val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val zipFile = File(exportDir, "collection_${collectionId}.zip")
        
        try {
            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                val entry = ZipEntry("data.json")
                zos.putNextEntry(entry)
                zos.write(jsonString.toByteArray())
                zos.closeEntry()
            }
            android.util.Log.d("MockExporter", "Exported collection $collectionId to ${zipFile.absolutePath} (${zipFile.length()} bytes)")
            zipFile
        } catch (e: Exception) {
            android.util.Log.e("MockExporter", "Failed to zip collection $collectionId", e)
            null
        }
    }

    fun cleanup(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }
}
