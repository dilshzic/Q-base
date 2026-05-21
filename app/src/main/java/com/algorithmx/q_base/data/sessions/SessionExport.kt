package com.algorithmx.q_base.data.sessions

import kotlinx.serialization.Serializable

@Serializable
data class SessionExport(
    val session: StudySession,
    val attempts: List<SessionAttempt>
)