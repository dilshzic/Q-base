package com.algorithmx.q_base.core_ai.brain

import android.util.Log
import com.algorithmx.q_base.core_ai.brain.models.AiGeneratedBlock
import com.algorithmx.q_base.core_ai.brain.models.AiGeneratedTab
import com.algorithmx.q_base.core_ai.brain.models.BlockType
import com.algorithmx.q_base.core_ai.brain.models.NoteContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommonAiService @Inject constructor(
    private val aiBrainManager: AiBrainManager
) {
    private val TAG = "CommonAiService"
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Generates a list of logical tabs, each containing informational content blocks.
     */
    suspend fun generateNoteStructure(
        topic: String,
        context: NoteContext
    ): Result<List<AiGeneratedTab>> {
        val prompt = """
            You are a content architect. Your task is to organize information about a topic into a logical multi-tab structure.
            
            TOPIC: $topic
            CATEGORY: ${context.noteCategory}
            
            INSTRUCTIONS:
            1. Suggest 3 to 5 logical tabs for this topic (e.g., "Overview", "Key Features", "Analysis").
            2. For each tab, generate 2 to 4 high-quality informational content blocks.
            3. Use appropriate types for blocks: "HEADER", "TEXT", "LIST", "TABLE", "ACCORDION", "CALLOUT".
            4. Return ONLY a valid JSON array of "AiGeneratedTab" objects.
            5. Each tab object must have:
               - "tabName" (String)
               - "blocks" (Array of blocks, each with "type" and "content" fields)
            
            FORMATTING RULES:
            - For LIST: "content" is a bulleted list with \n.
            - For ACCORDION: "content" is "Title;Details".
            - For TABLE: "content" is a markdown table.
            
            CRITICAL: Do not include any text outside the JSON array.
        """.trimIndent()

        return aiBrainManager.askBrain(com.algorithmx.q_base.core_ai.brain.models.BrainTask.NOTE_STRUCTURE, prompt).mapCatching { rawResponse ->
            val jsonString = extractJsonFromResponse(rawResponse)
            json.decodeFromString<List<AiGeneratedTab>>(jsonString)
        }
    }

    /**
     * Generates a list of content blocks based on the provided topic and note context.
     */
    suspend fun generateBlocksForTopic(
        topic: String,
        context: NoteContext,
        requestedTypes: List<BlockType> = listOf(BlockType.HEADER, BlockType.TEXT, BlockType.LIST)
    ): Result<List<AiGeneratedBlock>> {
        
        val prompt = constructGenerationPrompt(topic, context, requestedTypes)
        
        return aiBrainManager.askBrain(com.algorithmx.q_base.core_ai.brain.models.BrainTask.NOTE_BLOCKS, prompt).mapCatching { rawResponse ->
            // Extract JSON from the response (in case the AI adds markdown backticks)
            val jsonString = extractJsonFromResponse(rawResponse)
            json.decodeFromString<List<AiGeneratedBlock>>(jsonString)
        }
    }

    private fun constructGenerationPrompt(
        topic: String,
        context: NoteContext,
        requestedTypes: List<BlockType>
    ): String {
        val typesString = requestedTypes.joinToString(", ") { it.name }
        
        return """
            You are an educational content generator. Your task is to generate highly structured and accurate information.
            
            TOPIC: $topic
            
            CONTEXT:
            - Note Title: ${context.noteTitle}
            - Category: ${context.noteCategory}
            ${context.parentNoteTitle?.let { "- Parent Note: $it" } ?: ""}
            ${context.existingContentSummary?.let { "- Existing Content Summary: $it" } ?: ""}
            
            INSTRUCTIONS:
            1. Generate content relevant to the TOPIC while staying consistent with the CONTEXT.
            2. Return ONLY a valid JSON array of objects. 
            3. Each object must have "type" (String) and "content" (String) fields.
            4. The "type" field must be one of: $typesString.
            5. For LIST type, format the "content" as a simple bulleted list with \n separators.
            6. For ACCORDION type, format the "content" as "Title;Detailed content here".
            7. For TABLE type, provide a standard markdown table.
            
            CRITICAL: Do not include any text outside of the JSON array. Do not use markdown backticks unless strictly necessary inside the content.
        """.trimIndent()
    }

    private fun extractJsonFromResponse(response: String): String {
        val startIndex = response.indexOf("[")
        val endIndex = response.lastIndexOf("]")
        if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            return response.substring(startIndex, endIndex + 1)
        }
        return response
    }
}
