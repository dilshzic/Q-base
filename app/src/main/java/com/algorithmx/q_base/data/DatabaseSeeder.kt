package com.algorithmx.q_base.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.withTransaction
import com.algorithmx.q_base.data.collections.*
import com.algorithmx.q_base.data.chat.*
import com.algorithmx.q_base.data.core.*
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
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val dataStoreManager: com.algorithmx.q_base.core_ai.brain.BrainDataStoreManager
) {
    private fun getColumnString(cursor: android.database.Cursor, index: Int): String? {
        return if (index != -1 && !cursor.isNull(index)) cursor.getString(index) else null
    }

    suspend fun seedDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        val collectionDao = database.collectionDao()
        val questionDao = database.questionDao()

        val seedApplied = dataStoreManager.isSeedAppliedFlow.first()
        val collectionCount = collectionDao.getStudyCollectionCount()

        val chatList = chatDao.getAllChats().first()
        if (chatList.isEmpty()) {
            Log.d("DatabaseSeeder", "Chat database is empty. Seeding sample chats...")
            seedSampleChats()
        }

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
        val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: "current_user"

        try {
            chatDatabase.withTransaction {
                val sampleUsers = listOf(
                    UserEntity(userId = "mentor_1", displayName = "Alice (Mentor)", profilePictureUrl = null, friendCode = "ALICE-77"),
                    UserEntity(userId = "peer_1", displayName = "John Doe", profilePictureUrl = null, friendCode = "JD-1234"),
                    UserEntity(userId = "peer_2", displayName = "Sarah Miller", profilePictureUrl = null, friendCode = "SM-5678")
                )
                sampleUsers.forEach { userDao.insertUser(it) }

                // P2P Chat
                val p2pChat = ChatEntity(
                    chatId = "sample_p2p",
                    chatName = "Alice (Mentor)",
                    isGroup = false,
                    participantIds = "mentor_1,$currentUserId",
                    adminIds = listOf("mentor_1")
                )
                chatDao.insertChat(p2pChat)
                
                messageDao.insertMessage(MessageEntity(
                    messageId = "m1", chatId = "sample_p2p", senderId = "mentor_1",
                    payload = "Hello! I saw your recent progress on the General Knowledge collection. Great work!",
                    type = "TEXT", timestamp = System.currentTimeMillis() - 3600000
                ))

                // Group Chat
                val groupChat = ChatEntity(
                    chatId = "sample_group",
                    chatName = "Global Knowledge Exchange",
                    isGroup = true,
                    participantIds = "mentor_1,peer_1,peer_2,$currentUserId",
                    adminIds = listOf("mentor_1")
                )
                chatDao.insertChat(groupChat)

                messageDao.insertMessage(MessageEntity(
                    messageId = "m2", chatId = "sample_group", senderId = "peer_1",
                    payload = "Has anyone explored the new Space Science module?",
                    type = "TEXT", timestamp = System.currentTimeMillis() - 7200000
                ))
                messageDao.insertMessage(MessageEntity(
                    messageId = "m3", chatId = "sample_group", senderId = "peer_2",
                    payload = "Yes! The section on orbital mechanics is quite detailed.",
                    type = "TEXT", timestamp = System.currentTimeMillis() - 3600000
                ))

                // Shared Collection Message
                val scienceCollectionJson = """
                    {
                      "collectionTitle": "Scientific Foundations",
                      "collectionDescription": "A collection of essential foundational questions for multi-disciplinary study covering core principles and advanced concepts.",
                      "questions": [
                        {
                          "id": "sci_1",
                          "stem": "What is the approximate speed of light in a vacuum?",
                          "type": "SBA",
                          "options": [
                            {"letter": "A", "text": "300,000 km/s"},
                            {"letter": "B", "text": "150,000 km/s"},
                            {"letter": "C", "text": "450,000 km/s"},
                            {"letter": "D", "text": "600,000 km/s"}
                          ],
                          "answer": {
                            "correctLetter": "A",
                            "explanation": "Light travels at approximately 299,792 kilometers per second in a vacuum.",
                            "references": "General Educational Resources"
                          }
                        },
                        {
                          "id": "sci_2",
                          "stem": "Which planet is known as the Red Planet?",
                          "type": "SBA",
                          "options": [
                            {"letter": "A", "text": "Venus"},
                            {"letter": "B", "text": "Jupiter"},
                            {"letter": "C", "text": "Mars"},
                            {"letter": "D", "text": "Saturn"}
                          ],
                          "answer": {
                            "correctLetter": "C",
                            "explanation": "Mars is often called the Red Planet due to iron oxide on its surface.",
                            "references": "Astronomy 101"
                          }
                        }
                      ]
                    }
                """.trimIndent()

                messageDao.insertMessage(MessageEntity(
                    messageId = "m_col_1", chatId = "sample_p2p", senderId = "mentor_1",
                    payload = scienceCollectionJson,
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
            
            database.collectionDao().insertStudyCollections(listOf(
                StudyCollection(collectionId = collectionId, name = collectionName, description = "History of Ancient Sri Lanka", isUserCreated = false)
            ))
            
            val setId = UUID.randomUUID().toString()
            database.collectionDao().insertSets(listOf(
                QuestionSet(setId = setId, title = "Kings and Fortresses", parentCollectionId = collectionId, isUserCreated = false)
            ))
            
            val questionDao = database.questionDao()
            
            val batchQuestions = mutableListOf<Question>()
            val batchOptions = mutableListOf<QuestionOption>()
            val batchAnswers = mutableListOf<Answer>()
            val batchCrossRefs = mutableListOf<SetQuestionCrossRef>()

            questions.forEach { qJson ->
                val qId = UUID.randomUUID().toString()
                val stem = qJson["stem"]?.toString()?.removeSurrounding("\"") ?: ""
                val qType = qJson["questionType"]?.toString()?.removeSurrounding("\"") ?: "SBA"
                
                batchQuestions.add(Question(
                    questionId = qId,
                    collection = collectionName,
                    category = "History",
                    tags = "History, Sample",
                    questionType = qType,
                    stem = stem,
                    isPinned = false
                ))

                // Options
                val optionsArray = qJson["options"]?.jsonArray?.map { it.jsonObject } ?: emptyList()
                optionsArray.forEach { optJson ->
                    val letter = optJson["optionLetter"]?.toString()?.removeSurrounding("\"") ?: ""
                    val text = optJson["optionText"]?.toString()?.removeSurrounding("\"") ?: ""
                    batchOptions.add(QuestionOption(
                        questionId = qId,
                        optionLetter = letter,
                        optionText = text
                    ))
                }

                // Answer
                val correctAnswer = qJson["correctAnswer"]?.toString()?.removeSurrounding("\"") ?: ""
                val explanation = qJson["explanation"]?.toString()?.removeSurrounding("\"") ?: ""
                batchAnswers.add(Answer(
                    questionId = qId,
                    correctAnswerString = correctAnswer,
                    generalExplanation = explanation
                ))

                // Cross Ref
                batchCrossRefs.add(SetQuestionCrossRef(setId = setId, questionId = qId))
            }

            // Perform Batch Inserts
            questionDao.insertQuestions(batchQuestions)
            questionDao.insertOptions(batchOptions)
            questionDao.insertAnswers(batchAnswers)
            database.collectionDao().insertCrossRefs(batchCrossRefs)

        } catch (e: Exception) {
            Log.e("DatabaseSeeder", "Failed to seed Ancient Sri Lanka", e)
        }
    }
}
