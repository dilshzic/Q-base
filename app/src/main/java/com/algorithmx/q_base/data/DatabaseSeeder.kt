package com.algorithmx.q_base.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.withTransaction
import com.algorithmx.q_base.data.entity.*
import com.algorithmx.q_base.data.entity.Collection as AppCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class DatabaseSeeder(
    private val context: Context,
    private val database: AppDatabase,
    private val dataStoreManager: com.algorithmx.q_base.brain.BrainDataStoreManager
) {
    private fun getColumnString(cursor: android.database.Cursor, index: Int): String? {
        return if (index != -1 && !cursor.isNull(index)) cursor.getString(index) else null
    }

    suspend fun seedDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        val collectionDao = database.collectionDao()
        val questionDao = database.questionDao()

        if (dataStoreManager.isSeedAppliedFlow.first()) {
            Log.d("DatabaseSeeder", "Seed already applied according to DataStore.")
            return@withContext
        }

        Log.d("DatabaseSeeder", "Seeding database from assets...")

        val tempDbFile = File(context.cacheDir, "seed_temp.db")
        try {
            context.assets.open("database/q base.db").use { input ->
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
                        AppCollection(collectionId = id, name = name)
                    }
                    collectionDao.insertCollections(collections)

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

                    // --- Step 5: Seed Sample Chats ---
                    seedSampleChats()
                    
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

    private suspend fun seedSampleChats() {
        val chatDao = database.chatDao()
        val messageDao = database.messageDao()
        val userDao = database.userDao()
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "current_user"

        try {
            database.withTransaction {
                val sampleUsers = listOf(
                    UserEntity(userId = "mentor_1", displayName = "Dr. Alice (Mentor)", profilePictureUrl = null, friendCode = "DR-ALICE"),
                    UserEntity(userId = "peer_1", displayName = "John Doe", profilePictureUrl = null, friendCode = "JD-1234"),
                    UserEntity(userId = "peer_2", displayName = "Sarah Miller", profilePictureUrl = null, friendCode = "SM-5678")
                )
                sampleUsers.forEach { userDao.insertUser(it) }

                // P2P Chat
                val p2pChat = ChatEntity(
                    chatId = "sample_p2p",
                    chatName = "Dr. Alice (Mentor)",
                    isGroup = false,
                    participantIds = "mentor_1,$currentUserId"
                )
                chatDao.insertChat(p2pChat)
                
                messageDao.insertMessage(MessageEntity(
                    messageId = "m1", chatId = "sample_p2p", senderId = "mentor_1",
                    payload = "Hello! I saw your recent progress on Cardiology. Great work!",
                    type = "TEXT", timestamp = System.currentTimeMillis() - 3600000
                ))

                // Group Chat
                val groupChat = ChatEntity(
                    chatId = "sample_group",
                    chatName = "Global Knowledge Exchange",
                    isGroup = true,
                    participantIds = "mentor_1,peer_1,peer_2,$currentUserId"
                )
                chatDao.insertChat(groupChat)

                messageDao.insertMessage(MessageEntity(
                    messageId = "m2", chatId = "sample_group", senderId = "peer_1",
                    payload = "Has anyone read the latest paper on CRISPR applications in oncology?",
                    type = "TEXT", timestamp = System.currentTimeMillis() - 7200000
                ))
                messageDao.insertMessage(MessageEntity(
                    messageId = "m3", chatId = "sample_group", senderId = "peer_2",
                    payload = "Yes! It's fascinating. Especially the section on base editing.",
                    type = "TEXT", timestamp = System.currentTimeMillis() - 3600000
                ))

                // Shared Collection Message
                val cardioCollectionJson = """
                    {
                      "collectionTitle": "Cardiology Basics",
                      "collectionDescription": "A collection of essential foundational questions for multi-disciplinary study covering core principles and advanced concepts.",
                      "questions": [
                        {
                          "id": "cardio_1",
                          "stem": "What is the primary pacemaker of the heart responsible for initiating the electrical impulse under normal physiological conditions?",
                          "type": "SBA",
                          "options": [
                            {"letter": "A", "text": "Atrioventricular (AV) Node"},
                            {"letter": "B", "text": "Sinoatrial (SA) Node"},
                            {"letter": "C", "text": "Purkinje Fibers"},
                            {"letter": "D", "text": "Bundle of His"}
                          ],
                          "answer": {
                            "correctLetter": "B",
                            "explanation": "The Sinoatrial (SA) node is the heart's natural pacemaker because it has the fastest intrinsic rate of spontaneous depolarization.",
                            "references": "General Educational Resources"
                          }
                        },
                        {
                          "id": "cardio_2",
                          "stem": "Which heart sound is associated with rapid ventricular filling and is often heard in physiological states in children or pathological states like heart failure in adults?",
                          "type": "SBA",
                          "options": [
                            {"letter": "A", "text": "S1 (First heart sound)"},
                            {"letter": "B", "text": "S2 (Second heart sound)"},
                            {"letter": "C", "text": "S3 (Third heart sound)"},
                            {"letter": "D", "text": "S4 (Fourth heart sound)"}
                          ],
                          "answer": {
                            "correctLetter": "C",
                            "explanation": "S3 occurs during the early part of diastole and is caused by blood rushing into a non-compliant or overly loaded ventricle.",
                            "references": "Lilly, Pathophysiology of Heart Disease"
                          }
                        }
                      ]
                    }
                """.trimIndent()

                messageDao.insertMessage(MessageEntity(
                    messageId = "m_col_1", chatId = "sample_p2p", senderId = "mentor_1",
                    payload = cardioCollectionJson,
                    type = "COLLECTION", timestamp = System.currentTimeMillis()
                ))

                // Seed Ancient Sri Lanka from Assets
                seedAncientSriLanka()
                
                Log.d("DatabaseSeeder", "Seeding complete successfully.")
            }
        } catch (e: Exception) {
            Log.e("DatabaseSeeder", "Critical seeding failure: ${e.message}", e)
        }
    }

    private suspend fun seedAncientSriLanka() {
        try {
            val jsonString = context.assets.open("database/ancient_sri_lanka.json").bufferedReader().readText()
            val questions = kotlinx.serialization.json.Json.decodeFromString<List<kotlinx.serialization.json.JsonObject>>(jsonString)
            
            val collectionId = UUID.randomUUID().toString()
            val collectionName = "Ancient Sri Lanka"
            
            database.collectionDao().insertCollections(listOf(
                AppCollection(collectionId = collectionId, name = collectionName, description = "History of Ancient Sri Lanka", isUserCreated = false)
            ))
            
            val setId = UUID.randomUUID().toString()
            database.collectionDao().insertSets(listOf(
                QuestionSet(setId = setId, title = "Kings and Fortresses", parentCollectionId = collectionId, isUserCreated = false)
            ))
            
            val questionDao = database.questionDao()
            
            questions.forEach { qJson ->
                val qId = qJson["questionId"]?.toString()?.removeSurrounding("\"") ?: UUID.randomUUID().toString()
                val stem = qJson["stem"]?.toString()?.removeSurrounding("\"") ?: ""
                val qType = qJson["questionType"]?.toString()?.removeSurrounding("\"") ?: "SBA"
                
                questionDao.insertQuestion(Question(
                    questionId = qId,
                    collection = collectionName,
                    category = "History",
                    tags = "History, Sample",
                    questionType = qType,
                    stem = stem,
                    isPinned = false
                ))

                // Insert Options
                val optionsArray = qJson["options"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
                
                optionsArray.forEach { optJson ->
                    val letter = optJson["optionLetter"]?.toString()?.removeSurrounding("\"") ?: ""
                    val text = optJson["optionText"]?.toString()?.removeSurrounding("\"") ?: ""
                    questionDao.insertOption(QuestionOption(
                        questionId = qId,
                        optionLetter = letter,
                        optionText = text
                    ))
                }

                // Insert Answer
                val correctAnswer = qJson["correctAnswer"]?.toString()?.removeSurrounding("\"") ?: ""
                val explanation = qJson["explanation"]?.toString()?.removeSurrounding("\"") ?: ""
                questionDao.insertAnswer(Answer(
                    questionId = qId,
                    correctAnswerString = correctAnswer,
                    generalExplanation = explanation
                ))

                // Cross Ref
                database.collectionDao().insertCrossRefs(listOf(
                    SetQuestionCrossRef(setId = setId, questionId = qId)
                ))
            }
        } catch (e: Exception) {
            Log.e("DatabaseSeeder", "Failed to seed Ancient Sri Lanka", e)
        }
    }
}
