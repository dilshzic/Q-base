package com.algorithmx.q_base.data.sync

import kotlinx.serialization.Serializable

@Serializable
data class PatchEvent(
    val type: String, // "SESSION_ATTEMPT", "STUDY_SESSION", "QUESTION", "QUESTION_OPTION"
    val action: String, // "INSERT", "UPDATE", "DELETE"
    val sessionId: String? = null,
    val collectionId: String? = null,
    val dataJson: String // Serialized entity data
)
