package com.algorithmx.q_base.data.repository

import android.util.Log
import com.algorithmx.q_base.brain.AiBrainManager
import com.algorithmx.q_base.brain.models.AiCollectionResponse
import com.algorithmx.q_base.data.dao.CollectionDao
import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.entity.*
import com.algorithmx.q_base.data.entity.Collection as AppCollection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import com.algorithmx.q_base.data.entity.AiResponseEntity

@Singleton
class AiRepository @Inject constructor(
    private val aiBrainManager: AiBrainManager,
    private val questionDao: QuestionDao,
    private val collectionDao: CollectionDao,
    private val aiResponseDao: com.algorithmx.q_base.data.dao.AiResponseDao
) {
    private val TAG = "AiRepository"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun generateCollection(
        topic: String,
        count: Int = 5,
        type: String = "SBA",
        difficulty: String = "Medium",
        optionCount: Int = 5,
        collectionId: String,
        collectionName: String
    ): Result<String> {
        val prompt = """
            Generate a question collection for the topic: $topic.
            Count: $count questions.
            Question Type: $type.
            Difficulty Level: $difficulty level clinical scenario.
            Number of Options: Each question must have exactly $optionCount options.
            
            Return ONLY a valid JSON object with this exact structure:
            {
              "collectionTitle": "Cardiology Basics",
              "collectionDescription": "Fundamentals of cardiology",
              "questions": [
                {
                  "id": "generate-a-uuid",
                  "stem": "Question text here...",
                  "type": "$type",
                  "options": [
                    {"letter": "A", "text": "Option text...", "explanation": "Why this is right/wrong"},
                    ...
                  ],
                  "answer": {
                    "correctLetter": "A",
                    "explanation": "Brief explanation...",
                    "references": "Source title"
                  }
                }
              ]
            }
            
            CRITICAL: Do not include ANY text outside the JSON object.
        """.trimIndent()

        return try {
            val brainResult = aiBrainManager.askBrain(com.algorithmx.q_base.brain.models.BrainTask.COLLECTION_GEN, prompt)
            
            if (brainResult.isFailure) {
                return Result.failure(brainResult.exceptionOrNull() ?: Exception("Unknown AI failure"))
            }

            val rawResponse = brainResult.getOrThrow()
            val jsonString = extractJsonFromResponse(rawResponse)
            
            // Validate JSON
            try {
                val response = json.decodeFromString<AiCollectionResponse>(jsonString)
                
                // Save to Cache instead of main DB
                val responseId = UUID.randomUUID().toString()
                aiResponseDao.insertResponse(AiResponseEntity(
                    responseId = responseId,
                    topic = topic,
                    rawJson = jsonString
                ))
                
                Result.success(responseId)
            } catch (e: Exception) {
                Log.e(TAG, "JSON Parsing failed. Raw response: $rawResponse")
                Result.failure(Exception("AI returned invalid JSON structure: ${e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate collection: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun saveAsSet(
        response: AiCollectionResponse,
        parentCollectionId: String,
        parentCollectionName: String
    ): String = withContext(Dispatchers.IO) { // Return the setId
        val setId = UUID.randomUUID().toString()
        
        val set = QuestionSet(
            setId = setId,
            title = response.collectionTitle,
            parentCollectionId = parentCollectionId,
            description = response.collectionDescription,
            isUserCreated = true
        )
        questionDao.insertSet(set)

        saveQuestionsToSet(response.questions, setId, parentCollectionName)
        setId
    }

    suspend fun saveAsCollection(
        response: AiCollectionResponse,
        overrideName: String? = null
    ): String = withContext(Dispatchers.IO) { // Return the setId
        val collectionId = UUID.randomUUID().toString()
        val collectionName = overrideName ?: response.collectionTitle
        
        // 1. Create a new top-level Collection
        val collection = AppCollection(
            collectionId = collectionId,
            name = collectionName,
            description = response.collectionDescription,
            isUserCreated = true,
            updatedAt = System.currentTimeMillis()
        )
        collectionDao.insertCollections(listOf(collection))

        // 2. Also create a default set under this collection to hold the questions
        val setId = UUID.randomUUID().toString()
        val set = QuestionSet(
            setId = setId,
            title = "All Questions",
            parentCollectionId = collectionId,
            description = "Imported questions for $collectionName",
            isUserCreated = true
        )
        questionDao.insertSet(set)

        saveQuestionsToSet(response.questions, setId, collectionName)
        setId
    }

    private suspend fun saveQuestionsToSet(questions: List<com.algorithmx.q_base.brain.models.AiQuestion>, setId: String, collectionName: String) {
        questions.forEach { aiQ ->
            val questionId = UUID.randomUUID().toString()
            
            val question = Question(
                questionId = questionId,
                collection = collectionName, 
                category = collectionName, // Align with subject -> category rename
                tags = "AI Generated",
                questionType = aiQ.type,
                stem = aiQ.stem,
                isPinned = false
            )
            questionDao.insertQuestion(question)

            aiQ.options.forEach { aiOpt ->
                val option = QuestionOption(
                    questionId = questionId,
                    optionLetter = aiOpt.letter,
                    optionText = aiOpt.text,
                    optionExplanation = aiOpt.explanation
                )
                questionDao.insertOption(option)
            }

            val answer = Answer(
                questionId = questionId,
                correctAnswerString = aiQ.answer.correctLetter,
                generalExplanation = aiQ.answer.explanation,
                references = aiQ.answer.references
            )
            questionDao.insertAnswer(answer)

            val crossRef = SetQuestionCrossRef(
                setId = setId,
                questionId = questionId
            )
            questionDao.insertSetQuestionCrossRef(crossRef)
        }
    }

    suspend fun extractQuestionsFromText(
        text: String,
        collectionId: String,
        collectionName: String,
        difficulty: String = "Medium",
        types: List<String> = listOf("SBA"),
        customInstructions: String? = null
    ): Result<String> {
        val prompt = """
            Extract and format questions from the following text:
            ---
            $text
            ---
            
            Question Types: ${types.joinToString(", ")}
            Target Difficulty: $difficulty level questions.
            ${if (!customInstructions.isNullOrBlank()) "ADDITIONAL INSTRUCTIONS: $customInstructions" else ""}
            
            Return ONLY a valid JSON object with this exact structure:
            {
              "collectionTitle": "Extracted Collection",
              "collectionDescription": "Questions extracted from user input",
              "questions": [
                {
                  "id": "generate-a-uuid",
                  "stem": "Question text...",
                  "type": "SBA",
                  "options": [
                    {"letter": "A", "text": "Option...", "explanation": "..."},
                    ...
                  ],
                  "answer": {
                    "correctLetter": "A",
                    "explanation": "...",
                    "references": "..."
                  }
                }
              ]
            }
        """.trimIndent()

        return try {
            val brainResult = aiBrainManager.askBrain(com.algorithmx.q_base.brain.models.BrainTask.QUESTION_EXTRACTION, prompt)
            
            if (brainResult.isFailure) {
                return Result.failure(brainResult.exceptionOrNull() ?: Exception("Extraction AI call failed"))
            }

            val rawResponse = brainResult.getOrThrow()
            val jsonString = extractJsonFromResponse(rawResponse)
            
            // Validate JSON
            try {
                val response = json.decodeFromString<AiCollectionResponse>(jsonString)
                
                // Save to Cache instead of main DB
                val responseId = UUID.randomUUID().toString()
                aiResponseDao.insertResponse(AiResponseEntity(
                    responseId = responseId,
                    topic = "Extracted from text",
                    rawJson = jsonString
                ))
                
                Result.success(responseId)
            } catch (e: Exception) {
                Log.e(TAG, "Extraction JSON Parsing failed. Raw: $rawResponse")
                Result.failure(Exception("AI extraction returned invalid format: ${e.message}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Extraction failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun promoteAiResponseToDatabase(
        responseId: String,
        targetCollectionId: String? = null,
        targetCollectionName: String? = null,
        overrideName: String? = null
    ): Result<String> = withContext(Dispatchers.IO) { // Return setId
        try {
            val aiResponse = aiResponseDao.getResponseById(responseId) 
                ?: return@withContext Result.failure(Exception("AI Response not found"))
            
            val response = json.decodeFromString<AiCollectionResponse>(aiResponse.rawJson)
            
            val setId = if (targetCollectionId != null && targetCollectionName != null) {
                saveAsSet(response, targetCollectionId, targetCollectionName)
            } else {
                saveAsCollection(response, overrideName)
            }
            
            // Mark as promoted
            aiResponseDao.updateResponse(aiResponse.copy(isPromoted = true))
            
            Result.success(setId)

        } catch (e: Exception) {
            Log.e(TAG, "Promotion failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun assistQuestionEditing(stem: String, options: String = "", context: String = ""): Result<String> {
        val prompt = """
            Assist in creating a question.
            Stem: ${if (stem.isBlank()) "[Empty]" else stem}
            Options so far: ${if (options.isBlank()) "[None]" else options}
            Context/Topic: $context
            
            Based on the above, suggest improvements for the stem and provide 5 plausible options (A-E) with high quality explanations.
            Format your response as a helpful AI assistant.
        """.trimIndent()
        
        return aiBrainManager.askBrain(com.algorithmx.q_base.brain.models.BrainTask.EDIT_ASSISTANT, prompt)
    }

    suspend fun getAiAssistance(prompt: String): Result<String> {
        return aiBrainManager.askBrain(com.algorithmx.q_base.brain.models.BrainTask.CHAT_BOT, prompt)
    }

    suspend fun getAiResponseById(responseId: String): AiResponseEntity? {
        return aiResponseDao.getResponseById(responseId)
    }

    private fun extractJsonFromResponse(response: String): String {
        val startIndex = response.indexOf("{")
        val endIndex = response.lastIndexOf("}")
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1)
        }
        return response
    }
}
