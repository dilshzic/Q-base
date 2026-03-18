package com.algorithmx.q_base.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.algorithmx.q_base.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class DatabaseSeeder(
    private val context: Context,
    private val database: AppDatabase
) {
    suspend fun seedDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        val questionDao = database.questionDao()
        val categoryDao = database.categoryDao()

        if (questionDao.getQuestionCount() > 0) {
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

            val sqliteDb = SQLiteDatabase.openDatabase(tempDbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            
            try {
                // Seed Master Categories
                val masterCategories = mutableListOf<MasterCategory>()
                sqliteDb.rawQuery("SELECT DISTINCT master_category FROM Questions", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val name = cursor.getString(cursor.getColumnIndexOrThrow("master_category"))
                        if (!name.isNullOrEmpty()) {
                            masterCategories.add(MasterCategory(masterCategory = name))
                        }
                    }
                }
                categoryDao.insertMasterCategories(masterCategories)

                // Seed Collections (Categories)
                val collections = mutableListOf<QuestionCollection>()
                sqliteDb.rawQuery("SELECT DISTINCT category, master_category FROM Questions", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                        val masterCategory = cursor.getString(cursor.getColumnIndexOrThrow("master_category"))
                        if (!category.isNullOrEmpty() && !masterCategory.isNullOrEmpty()) {
                            collections.add(QuestionCollection(category = category, masterCategory = masterCategory))
                        }
                    }
                }
                categoryDao.insertCollections(collections)

                // Seed Questions
                val questions = mutableListOf<Question>()
                sqliteDb.rawQuery("SELECT * FROM Questions", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        questions.add(Question(
                            questionId = cursor.getString(cursor.getColumnIndexOrThrow("question_id")),
                            masterCategory = cursor.getString(cursor.getColumnIndexOrThrow("master_category")),
                            category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                            questionType = cursor.getString(cursor.getColumnIndexOrThrow("question_type")),
                            stem = cursor.getString(cursor.getColumnIndexOrThrow("stem")),
                            subject = cursor.getString(cursor.getColumnIndexOrThrow("subject")),
                            batch = cursor.getString(cursor.getColumnIndexOrThrow("batch"))
                        ))
                    }
                }
                questionDao.insertQuestions(questions)

                // Seed Question Options
                val options = mutableListOf<QuestionOption>()
                sqliteDb.rawQuery("SELECT * FROM Question_Options", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        options.add(QuestionOption(
                            questionId = cursor.getString(cursor.getColumnIndexOrThrow("question_id")),
                            optionLetter = cursor.getString(cursor.getColumnIndexOrThrow("option_letter")),
                            optionText = cursor.getString(cursor.getColumnIndexOrThrow("option_text"))
                        ))
                    }
                }
                questionDao.insertOptions(options)

                // Seed Answers
                val answers = mutableListOf<Answer>()
                sqliteDb.rawQuery("SELECT * FROM Answers", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        answers.add(Answer(
                            questionId = cursor.getString(cursor.getColumnIndexOrThrow("question_id")),
                            correctAnswerString = cursor.getString(cursor.getColumnIndexOrThrow("correct_answer_string")),
                            generalExplanation = cursor.getString(cursor.getColumnIndexOrThrow("general_explanation"))
                        ))
                    }
                }
                questionDao.insertAnswers(answers)

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
