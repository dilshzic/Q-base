package com.algorithmx.q_base.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen : NavKey {
    @Serializable data object Login : Screen()
    @Serializable data object Home : Screen()
    @Serializable data object Explore : Screen()
    @Serializable data object Sessions : Screen()
    @Serializable data object Inbox : Screen()
    @Serializable data class ChatDetail(val chatId: String) : Screen()
    @Serializable data class ActiveSession(val sessionId: String) : Screen()
    @Serializable data class SessionResults(val sessionId: String) : Screen()
    
    // Nested Explore Routes
    @Serializable data object Categories : Screen()
    @Serializable data class ExplorePager(val categoryName: String) : Screen()
}
