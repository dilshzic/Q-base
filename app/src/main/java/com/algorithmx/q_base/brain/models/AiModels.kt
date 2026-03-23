package com.algorithmx.q_base.brain.models

import kotlinx.serialization.Serializable

@Serializable
enum class BlockType {
    HEADER,
    TEXT,
    LIST,
    TABLE,
    ACCORDION,
    CALLOUT
}

@Serializable
data class AiGeneratedBlock(
    val type: BlockType,
    val content: String
)

@Serializable
data class AiGeneratedTab(
    val tabName: String,
    val blocks: List<AiGeneratedBlock>
)

data class NoteContext(
    val noteTitle: String,
    val noteCategory: String,
    val parentNoteTitle: String? = null,
    val existingContentSummary: String? = null
)
