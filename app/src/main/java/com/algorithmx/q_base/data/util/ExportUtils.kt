package com.algorithmx.q_base.data.util

import android.content.Context
import com.algorithmx.q_base.data.entity.SetWithQuestions
import com.algorithmx.q_base.data.model.MockExport
import com.algorithmx.q_base.data.model.QuestionExport
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
    @param:ApplicationContext private val context: Context
) {
    private val json = Json { 
        prettyPrint = false
        encodeDefaults = true
    }

    suspend fun prepareZip(setWithQuestions: SetWithQuestions): File = withContext(Dispatchers.IO) {
        val exportData = MockExport(
            collection = setWithQuestions.set,
            questions = setWithQuestions.questions.map { 
                QuestionExport(it.question, it.options, it.answer)
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
