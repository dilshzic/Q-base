package com.algorithmx.q_base.brain.models

import kotlinx.serialization.Serializable

@Serializable
data class AiQuestion(
    val id: String,
    val stem: String,
    val type: String,
    val options: List<AiOption>,
    val answer: AiAnswer
)

@Serializable
data class AiOption(
    val letter: String,
    val text: String,
    val explanation: String? = null
)

@Serializable
data class AiAnswer(
    val correctLetter: String,
    val explanation: String,
    val references: String? = null
)

@Serializable
data class AiCollectionResponse(
    val collectionTitle: String,
    val collectionDescription: String,
    val questions: List<AiQuestion>
)
