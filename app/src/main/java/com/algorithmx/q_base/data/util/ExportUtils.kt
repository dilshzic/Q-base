package com.algorithmx.q_base.data.util

import android.content.Context
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.collections.SetWithQuestions
import com.algorithmx.q_base.data.collections.MockExport
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
class ExportUtils @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val questionDao: QuestionDao
) {
    private val json = Json { 
        prettyPrint = false
        encodeDefaults = true
    }

    suspend fun prepareZip(setWithQuestions: SetWithQuestions): File = withContext(Dispatchers.IO) {
        val exportData = MockExport(
            collection = setWithQuestions.set,
            questions = setWithQuestions.questions.map { question ->
                val options = questionDao.getOptionsForQuestionOnce(question.questionId)
                val answer = questionDao.getAnswerForQuestionOnce(question.questionId)
                QuestionExport(question, options, answer)
            }
        )

        val jsonString = json.encodeToString(exportData)
        val zipFile = File(context.cacheDir, "${setWithQuestions.set.setId}.zip")
        
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            val entry = ZipEntry("data.json")
            zos.putNextEntry(entry)
            zos.write(jsonString.toByteArray())
            zos.closeEntry()
        }
        
        zipFile
    }
}
