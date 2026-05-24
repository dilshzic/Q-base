package com.algorithmx.q_base.feature.sessions.data

import kotlinx.serialization.Serializable

@Serializable
data class SessionExport(
    val session: StudySession,
    val attempts: List<SessionAttempt>
)