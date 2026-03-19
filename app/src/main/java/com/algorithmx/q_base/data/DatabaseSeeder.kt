package com.algorithmx.q_base.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
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
    private fun getColumnString(cursor: android.database.Cursor, columnName: String): String? {
        val index = cursor.getColumnIndex(columnName)
        return if (index != -1 && !cursor.isNull(index)) cursor.getString(index) else null
    }

    suspend fun seedDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        val questionDao = database.questionDao()
        val categoryDao = database.categoryDao()

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
                val batchSize = 100

                // --- Step 1: Seed raw Questions ---
                if (questionDao.getQuestionCount() == 0) {
                    sqliteDb.rawQuery("SELECT * FROM Questions", null).use { cursor ->
                        val batch = mutableListOf<Question>()
                        while (cursor.moveToNext()) {
                            batch.add(Question(
                                questionId = cursor.getString(cursor.getColumnIndexOrThrow("question_id")),
                                masterCategory = getColumnString(cursor, "master_category"),
                                category = getColumnString(cursor, "category"),
                                questionType = getColumnString(cursor, "question_type"),
                                stem = cursor.getString(cursor.getColumnIndexOrThrow("stem")),
                                subject = getColumnString(cursor, "subject"),
                                batch = getColumnString(cursor, "batch"),
                                isPinned = false
                            ))
                            if (batch.size >= batchSize) { questionDao.insertQuestions(batch); batch.clear() }
                        }
                        if (batch.isNotEmpty()) questionDao.insertQuestions(batch)
                    }

                    // Seed Options - JOIN with Option_Explanations table from asset DB
                    val optionsQuery = """
                        SELECT qo.*, oe.specific_explanation 
                        FROM Question_Options qo 
                        LEFT JOIN Option_Explanations oe ON qo.question_id = oe.question_id AND qo.option_letter = oe.option_letter
                    """.trimIndent()

                    sqliteDb.rawQuery(optionsQuery, null).use { cursor ->
                        val batch = mutableListOf<QuestionOption>()
                        while (cursor.moveToNext()) {
                            batch.add(QuestionOption(
                                questionId = cursor.getString(cursor.getColumnIndexOrThrow("question_id")),
                                optionLetter = cursor.getString(cursor.getColumnIndexOrThrow("option_letter")),
                                optionText = cursor.getString(cursor.getColumnIndexOrThrow("option_text")),
                                optionExplanation = getColumnString(cursor, "specific_explanation")
                            ))
                            if (batch.size >= batchSize) { questionDao.insertOptions(batch); batch.clear() }
                        }
                        if (batch.isNotEmpty()) questionDao.insertOptions(batch)
                    }

                    // Seed Answers - Asset DB uses "reference" (singular)
                    sqliteDb.rawQuery("SELECT * FROM Answers", null).use { cursor ->
                        val batch = mutableListOf<Answer>()
                        while (cursor.moveToNext()) {
                            batch.add(Answer(
                                questionId = cursor.getString(cursor.getColumnIndexOrThrow("question_id")),
                                correctAnswerString = cursor.getString(cursor.getColumnIndexOrThrow("correct_answer_string")),
                                generalExplanation = cursor.getString(cursor.getColumnIndexOrThrow("general_explanation")),
                                references = getColumnString(cursor, "reference")
                            ))
                            if (batch.size >= batchSize) { questionDao.insertAnswers(batch); batch.clear() }
                        }
                        if (batch.isNotEmpty()) questionDao.insertAnswers(batch)
                    }
                }

                // Step 2-4: Metadata seeding
                val masterCategoryMap = mutableMapOf<String, String>()
                sqliteDb.rawQuery("SELECT DISTINCT master_category FROM Questions", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(cursor.getColumnIndexOrThrow("master_category"))
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
                    while (cursor.moveToNext()) {
                        val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                        val masterName = cursor.getString(cursor.getColumnIndexOrThrow("master_category"))
                        if (!category.isNullOrEmpty() && !masterName.isNullOrEmpty()) {
                            val masterCategoryId = masterCategoryMap[masterName] ?: continue
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

                sqliteDb.rawQuery("SELECT question_id, category FROM Questions", null).use { cursor ->
                    val batch = mutableListOf<CollectionQuestionCrossRef>()
                    while (cursor.moveToNext()) {
                        val questionId = cursor.getString(cursor.getColumnIndexOrThrow("question_id"))
                        val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                        val collectionId = collectionMap[category] ?: continue
                        batch.add(CollectionQuestionCrossRef(collectionId = collectionId, questionId = questionId))
                        if (batch.size >= batchSize) { categoryDao.insertCrossRefs(batch); batch.clear() }
                    }
                    if (batch.isNotEmpty()) categoryDao.insertCrossRefs(batch)
                }

                Log.d("DatabaseSeeder", "Seeding completed successfully with option explanations.")
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
