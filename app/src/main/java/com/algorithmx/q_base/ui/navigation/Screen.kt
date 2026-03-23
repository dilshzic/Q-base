package com.algorithmx.q_base.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class Screen : NavKey {
    @Serializable data object Login : Screen()
    @Serializable data object Home : Screen()
    @Serializable data object Explore : Screen()
    @Serializable data class Sessions(val startWizard: Boolean = false) : Screen()
    @Serializable data object Connect : Screen()
    @Serializable data class ChatDetail(val chatId: String) : Screen()
    @Serializable data class ContactOverview(val chatId: String) : Screen()
    @Serializable data class GroupOverview(val chatId: String) : Screen()
    @Serializable data class ActiveSession(val sessionId: String) : Screen()
    @Serializable data class SessionResults(val sessionId: String) : Screen()
    @Serializable data object UnifiedCreation : Screen()
    
    @Serializable data class ManualBuilder(val targetId: String? = null, val name: String? = null) : Screen()
    @Serializable data class ExtractionConfig(val extractedText: String, val targetId: String? = null) : Screen()

    @Serializable
    data class QuestionEditor(val questionId: String?, val setId: String) : Screen()
    @Serializable data object Settings : Screen()
    @Serializable data object Profile : Screen()
    @Serializable data class ImportWizard(val source: String? = null, val targetId: String? = null) : Screen()
    
    // Nested Explore Routes
    @Serializable data object Categories : Screen()
    @Serializable data class ExplorePager(val categoryName: String) : Screen()
    @Serializable data class AiGeneration(val collectionId: String, val collectionName: String) : Screen()
    @Serializable data object Collections : Screen()
    @Serializable data class CollectionOverview(val collectionId: String) : Screen()
    @Serializable data class ExploreSet(val setId: String, val sessionId: String? = null, val startIndex: Int = 0) : Screen()
    @Serializable data object NewChat : Screen()
    @Serializable data object NewGroup : Screen()
    @Serializable data object Signup : Screen()
    @Serializable data object CollectionWizard : Screen()
    @Serializable data object Notifications : Screen()
    @Serializable data object BlockedList : Screen()
}
