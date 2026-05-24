package com.algorithmx.q_base.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen : NavKey {
    // --- Authentication ---
    @Serializable data object Login : Screen()
    @Serializable data object Signup : Screen()

    // --- Main Top-Level Destinations ---
    @Serializable data object Home : Screen()
    @Serializable data object Explore : Screen()
    @Serializable data object Connect : Screen()
    @Serializable data class Sessions(val startWizard: Boolean = false) : Screen()

    // --- Study & Explore Flows ---
    @Serializable data class CollectionOverview(val collectionId: String) : Screen()
    @Serializable data class ExploreSet(
        val setId: String, 
        val sessionId: String? = null, 
        val startIndex: Int = 0
    ) : Screen()
    @Serializable data class ExplorePager(val collectionId: String) : Screen()
    @Serializable data object Collections : Screen() // Redundant with Explore but kept for deep links
    
    // --- Content Creation & Import ---
    @Serializable data object CreateNewCollection : Screen()
    @Serializable data class AiGeneration(val collectionId: String, val collectionName: String) : Screen()
    @Serializable data class ManualBuilder(val targetId: String? = null, val name: String? = null) : Screen()
    @Serializable data class QuestionEditor(val questionId: String?, val setId: String) : Screen()
    @Serializable data class ImportWizard(val source: String? = null, val targetId: String? = null) : Screen()

    // --- Chat & Social ---
    @Serializable data object NewChat : Screen()
    @Serializable data object NewGroup : Screen()
    @Serializable data class ChatDetail(val chatId: String) : Screen()
    @Serializable data class ContactOverview(val chatId: String) : Screen()
    @Serializable data class GroupOverview(val chatId: String) : Screen()
    @Serializable data object BlockedList : Screen()

    // --- Session Management ---
    @Serializable data object NewSessionWizard : Screen()
    @Serializable data class ActiveSession(val sessionId: String, val chatId: String? = null) : Screen()
    @Serializable data class SessionResults(val sessionId: String) : Screen()

    // --- User & App Settings ---
    @Serializable data object Profile : Screen()
    @Serializable data object Settings : Screen()
    @Serializable data object AppTheme : Screen()
    @Serializable data object AiBrainManager : Screen()
    @Serializable data object Notifications : Screen()
    @Serializable data object PinnedQuestions : Screen()
}

val Screen.isTopLevel: Boolean
    get() = this is Screen.Home || 
            this is Screen.Explore || 
            this is Screen.Connect || 
            this is Screen.Sessions ||
            this is Screen.Login