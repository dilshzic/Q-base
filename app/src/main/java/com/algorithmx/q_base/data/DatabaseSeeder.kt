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
    suspend fun seedDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        val questionDao = database.questionDao()
        val categoryDao = database.categoryDao()

        // Use Master_Categories as the sentinel — it is wiped during v1→v2 migration
        // so it will be empty even if Questions are still present.
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

                // --- Step 1: Seed raw Questions (unchanged in v2) ---
                if (questionDao.getQuestionCount() == 0) {
                    sqliteDb.rawQuery("SELECT * FROM Questions", null).use { cursor ->
                        val batch = mutableListOf<Question>()
                        while (cursor.moveToNext()) {
                            batch.add(Question(
                                questionId = cursor.getString(cursor.getColumnIndexOrThrow("question_id")),
                                masterCategory = cursor.getString(cursor.getColumnIndexOrThrow("master_category")),
                                category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                                questionType = cursor.getString(cursor.getColumnIndexOrThrow("question_type")),
                                stem = cursor.getString(cursor.getColumnIndexOrThrow("stem")),
                                subject = cursor.getString(cursor.getColumnIndexOrThrow("subject")),
                                batch = cursor.getString(cursor.getColumnIndexOrThrow("batch"))
                            ))
                            if (batch.size >= batchSize) { questionDao.insertQuestions(batch); batch.clear() }
                        }
                        if (batch.isNotEmpty()) questionDao.insertQuestions(batch)
                    }

                    // Seed Options in batches
                    sqliteDb.rawQuery("SELECT * FROM Question_Options", null).use { cursor ->
                        val batch = mutableListOf<QuestionOption>()
                        while (cursor.moveToNext()) {
                            batch.add(QuestionOption(
                                questionId = cursor.getString(cursor.getColumnIndexOrThrow("question_id")),
                                optionLetter = cursor.getString(cursor.getColumnIndexOrThrow("option_letter")),
                                optionText = cursor.getString(cursor.getColumnIndexOrThrow("option_text"))
                            ))
                            if (batch.size >= batchSize) { questionDao.insertOptions(batch); batch.clear() }
                        }
                        if (batch.isNotEmpty()) questionDao.insertOptions(batch)
                    }

                    // Seed Answers in batches
                    sqliteDb.rawQuery("SELECT * FROM Answers", null).use { cursor ->
                        val batch = mutableListOf<Answer>()
                        while (cursor.moveToNext()) {
                            batch.add(Answer(
                                questionId = cursor.getString(cursor.getColumnIndexOrThrow("question_id")),
                                correctAnswerString = cursor.getString(cursor.getColumnIndexOrThrow("correct_answer_string")),
                                generalExplanation = cursor.getString(cursor.getColumnIndexOrThrow("general_explanation"))
                            ))
                            if (batch.size >= batchSize) { questionDao.insertAnswers(batch); batch.clear() }
                        }
                        if (batch.isNotEmpty()) questionDao.insertAnswers(batch)
                    }
                }

                // --- Step 2: Build UUID-keyed Master_Categories from distinct names ---
                // Map: raw name string -> UUID
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

                // --- Step 3: Build UUID-keyed Question_Collections (one per distinct category) ---
                // Map: raw category string -> collectionId UUID
                val collectionMap = mutableMapOf<String, String>()
                sqliteDb.rawQuery(
                    "SELECT DISTINCT category, master_category FROM Questions", null
                ).use { cursor ->
                    while (cursor.moveToNext()) {
                        val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                        val masterName = cursor.getString(cursor.getColumnIndexOrThrow("master_category"))
                        if (!category.isNullOrEmpty() && !masterName.isNullOrEmpty()) {
                            val masterCategoryId = masterCategoryMap[masterName] ?: continue
                            val collectionId = UUID.randomUUID().toString()
                            collectionMap[category] = collectionId
                            // Inserted individually; small table — fine without batching
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

                // --- Step 4: Seed Collection_Questions_CrossRef in batches ---
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

