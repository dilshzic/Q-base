package com.algorithmx.q_base.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.withTransaction
import com.algorithmx.q_base.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class DatabaseSeeder(
    private val context: Context,
    private val database: AppDatabase
) {
    private fun getColumnString(cursor: android.database.Cursor, index: Int): String? {
        return if (index != -1 && !cursor.isNull(index)) cursor.getString(index) else null
    }

    suspend fun seedDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        val categoryDao = database.categoryDao()
        val questionDao = database.questionDao()

        if (categoryDao.getMasterCategoryCount() > 0) {
            Log.d("DatabaseSeeder", "Database already seeded.")
            return@withContext
        }

        Log.d("DatabaseSeeder", "Seeding database...")

        val tempDbFile = File(context.cacheDir, "seed_temp.db")
        try {
            context.assets.open("database/MedicalQuiz.db").use { input ->
                FileOutputStream(tempDbFile).use { output ->
                    input.copyTo(output)
                }
            }

            val sqliteDb = SQLiteDatabase.openDatabase(
                tempDbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
            )

            try {
                database.withTransaction {
                    val batchSize = 500

                    // --- Step 1: Seed raw Questions ---
                    sqliteDb.rawQuery("SELECT * FROM Questions", null).use { cursor ->
                        val idIdx = cursor.getColumnIndexOrThrow("question_id")
                        val mcIdx = cursor.getColumnIndex("master_category")
                        val catIdx = cursor.getColumnIndex("category")
                        val qtIdx = cursor.getColumnIndex("question_type")
                        val stemIdx = cursor.getColumnIndexOrThrow("stem")
                        val subIdx = cursor.getColumnIndex("subject")
                        val batchIdx = cursor.getColumnIndex("batch")

                        val batch = mutableListOf<Question>()
                        while (cursor.moveToNext()) {
                            batch.add(Question(
                                questionId = cursor.getString(idIdx),
                                masterCategory = getColumnString(cursor, mcIdx),
                                category = getColumnString(cursor, catIdx),
                                questionType = getColumnString(cursor, qtIdx),
                                stem = cursor.getString(stemIdx),
                                subject = getColumnString(cursor, subIdx),
                                batch = getColumnString(cursor, batchIdx),
                                isPinned = false
                            ))
                            if (batch.size >= batchSize) {
                                questionDao.insertQuestions(batch)
                                batch.clear()
                            }
                        }
                        if (batch.isNotEmpty()) questionDao.insertQuestions(batch)
                    }

                    // Seed Options
                    val optionsQuery = """
                        SELECT qo.question_id, qo.option_letter, qo.option_text, oe.specific_explanation 
                        FROM Question_Options qo 
                        LEFT JOIN Option_Explanations oe ON qo.question_id = oe.question_id AND qo.option_letter = oe.option_letter
                    """.trimIndent()

                    sqliteDb.rawQuery(optionsQuery, null).use { cursor ->
                        val idIdx = cursor.getColumnIndexOrThrow("question_id")
                        val letIdx = cursor.getColumnIndexOrThrow("option_letter")
                        val txtIdx = cursor.getColumnIndexOrThrow("option_text")
                        val expIdx = cursor.getColumnIndex("specific_explanation")

                        val batch = mutableListOf<QuestionOption>()
                        while (cursor.moveToNext()) {
                            batch.add(QuestionOption(
                                questionId = cursor.getString(idIdx),
                                optionLetter = cursor.getString(letIdx),
                                optionText = cursor.getString(txtIdx),
                                optionExplanation = getColumnString(cursor, expIdx)
                            ))
                            if (batch.size >= batchSize) {
                                questionDao.insertOptions(batch)
                                batch.clear()
                            }
                        }
                        if (batch.isNotEmpty()) questionDao.insertOptions(batch)
                    }

                    // Seed Answers
                    sqliteDb.rawQuery("SELECT * FROM Answers", null).use { cursor ->
                        val idIdx = cursor.getColumnIndexOrThrow("question_id")
                        val ansIdx = cursor.getColumnIndexOrThrow("correct_answer_string")
                        val genIdx = cursor.getColumnIndexOrThrow("general_explanation")
                        val refIdx = cursor.getColumnIndex("reference")

                        val batch = mutableListOf<Answer>()
                        while (cursor.moveToNext()) {
                            batch.add(Answer(
                                questionId = cursor.getString(idIdx),
                                correctAnswerString = cursor.getString(ansIdx),
                                generalExplanation = cursor.getString(genIdx),
                                references = getColumnString(cursor, refIdx)
                            ))
                            if (batch.size >= batchSize) {
                                questionDao.insertAnswers(batch)
                                batch.clear()
                            }
                        }
                        if (batch.isNotEmpty()) questionDao.insertAnswers(batch)
                    }

                    // Step 2-4: Metadata seeding
                    val masterCategoryMap = mutableMapOf<String, String>()
                    sqliteDb.rawQuery("SELECT DISTINCT master_category FROM Questions", null).use { cursor ->
                        val mcIdx = cursor.getColumnIndexOrThrow("master_category")
                        while (cursor.moveToNext()) {
                            val name = cursor.getString(mcIdx)
                            if (!name.isNullOrEmpty()) {
                                masterCategoryMap[name] = UUID.randomUUID().toString()
                            }
                        }
                    }
                    
                    val masterCategories = masterCategoryMap.map { (name, id) ->
                        MasterCategory(masterCategoryId = id, name = name)
                    }
                    categoryDao.insertMasterCategories(masterCategories)

                    val collectionMap = mutableMapOf<String, String>()
                    sqliteDb.rawQuery("SELECT DISTINCT category, master_category FROM Questions", null).use { cursor ->
                        val catIdx = cursor.getColumnIndexOrThrow("category")
                        val mcIdx = cursor.getColumnIndexOrThrow("master_category")
                        
                        while (cursor.moveToNext()) {
                            val category = cursor.getString(catIdx)
                            val masterName = cursor.getString(mcIdx)
                            if (!category.isNullOrEmpty() && !masterName.isNullOrEmpty()) {
                                val masterCategoryId = masterCategoryMap[masterName] ?: continue
                                if (!collectionMap.containsKey(category)) {
                                    val collectionId = UUID.randomUUID().toString()
                                    collectionMap[category] = collectionId
                                    categoryDao.insertCollections(listOf(
                                        QuestionCollection(
                                            collectionId = collectionId,
                                            title = category,
                                            masterCategoryId = masterCategoryId,
                                            createdTimestamp = System.currentTimeMillis()
                                        )
                                    ))
                                }
                            }
                        }
                    }

                    sqliteDb.rawQuery("SELECT question_id, category FROM Questions", null).use { cursor ->
                        val idIdx = cursor.getColumnIndexOrThrow("question_id")
                        val catIdx = cursor.getColumnIndexOrThrow("category")
                        
                        val batch = mutableListOf<CollectionQuestionCrossRef>()
                        while (cursor.moveToNext()) {
                            val questionId = cursor.getString(idIdx)
                            val category = cursor.getString(catIdx)
                            val collectionId = collectionMap[category] ?: continue
                            batch.add(CollectionQuestionCrossRef(collectionId = collectionId, questionId = questionId))
                            if (batch.size >= batchSize) {
                                categoryDao.insertCrossRefs(batch)
                                batch.clear()
                            }
                        }
                        if (batch.isNotEmpty()) categoryDao.insertCrossRefs(batch)
                    }
                }
                Log.d("DatabaseSeeder", "Seeding completed successfully.")
            } finally {
                sqliteDb.close()
            }
        } catch (e: Exception) {
            Log.e("DatabaseSeeder", "Error seeding database", e)
        } finally {
            tempDbFile.delete()
        }
    }
}
