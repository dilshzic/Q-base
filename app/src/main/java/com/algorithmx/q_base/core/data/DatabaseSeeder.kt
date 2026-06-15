package com.algorithmx.q_base.core.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.withTransaction
import com.algorithmx.q_base.data.collections.*
import com.algorithmx.q_base.core.data.chat.*
import com.algorithmx.q_base.core.data.*
import com.algorithmx.q_base.data.collections.StudyCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import dagger.hilt.android.qualifiers.ApplicationContext

class DatabaseSeeder @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val chatDatabase: ChatDatabase,
    private val chatLocalDataSource: com.algorithmx.q_base.core.data.chat.ChatLocalDataSource,
    private val userDao: UserDao,
    private val dataStoreManager: com.algorithmx.q_base.core.ai.brain.BrainDataStoreManager,
        private val authRepository: com.algorithmx.q_base.core.data.auth.AuthRepository
) {
    private fun getColumnString(cursor: android.database.Cursor, index: Int): String? {
        return if (index != -1 && !cursor.isNull(index)) cursor.getString(index) else null
    }

    suspend fun seedDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        val collectionDao = database.collectionDao()
        val questionDao = database.questionDao()

        val seedApplied = dataStoreManager.isSeedAppliedFlow.first()
        val collectionCount = collectionDao.getStudyCollectionCount()

        if (seedApplied && collectionCount > 0) {
            Log.d("DatabaseSeeder", "Seed already applied and data exists. Skipping.")
            return@withContext
        }

        Log.d("DatabaseSeeder", "Seeding database from assets (seedApplied=$seedApplied, count=$collectionCount)...")

        val tempDbFile = File(context.cacheDir, "seed_temp.db")
        try {
            context.assets.open("database/qbase.db").use { input ->
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
                        val tagIdx = cursor.getColumnIndex("tags")
                        val qtIdx = cursor.getColumnIndex("question_type")
                        val stemIdx = cursor.getColumnIndexOrThrow("stem")

                        val batch = mutableListOf<Question>()
                        while (cursor.moveToNext()) {
                            batch.add(Question(
                                questionId = cursor.getString(idIdx),
                                collection = getColumnString(cursor, mcIdx),
                                category = getColumnString(cursor, catIdx),
                                tags = getColumnString(cursor, tagIdx),
                                questionType = getColumnString(cursor, qtIdx),
                                stem = cursor.getString(stemIdx),
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
                    
                    val collections = masterCategoryMap.map { (name, id) ->
                        StudyCollection(collectionId = id, name = name)
                    }
                    collectionDao.insertStudyCollections(collections)

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
                                    val setId = UUID.randomUUID().toString()
                                    collectionMap[category] = setId
                                    collectionDao.insertSets(listOf(
                                        QuestionSet(
                                            setId = setId,
                                            title = category,
                                            parentCollectionId = masterCategoryId,
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
                        
                        val batch = mutableListOf<SetQuestionCrossRef>()
                        while (cursor.moveToNext()) {
                            val questionId = cursor.getString(idIdx)
                            val category = cursor.getString(catIdx)
                            val setId = collectionMap[category] ?: continue
                            batch.add(SetQuestionCrossRef(setId = setId, questionId = questionId))
                            if (batch.size >= batchSize) {
                                collectionDao.insertCrossRefs(batch)
                                batch.clear()
                            }
                        }
                        if (batch.isNotEmpty()) collectionDao.insertCrossRefs(batch)
                    }
                    
                    dataStoreManager.markSeedAsApplied()
                }
                Log.d("DatabaseSeeder", "Seeding completed successfully.")
            } finally {
                sqliteDb.close()
            }
        } catch (e: Exception) {
            Log.e("DatabaseSeeder", "Error seeding database", e)
        } finally {
            if (tempDbFile.exists()) tempDbFile.delete()
        }
    }
}