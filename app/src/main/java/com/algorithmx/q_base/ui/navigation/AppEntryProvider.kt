package com.algorithmx.q_base.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import com.algorithmx.q_base.ui.auth.*
import com.algorithmx.q_base.ui.chat.*
import com.algorithmx.q_base.ui.ai.*
import com.algorithmx.q_base.ui.content_import.*
import com.algorithmx.q_base.ui.explore.*
import com.algorithmx.q_base.ui.home.HomeScreen
import com.algorithmx.q_base.ui.sessions.*
import com.algorithmx.q_base.ui.settings.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.algorithmx.q_base.data.entity.Collection as AppCollection

@Composable
fun rememberAppEntryProvider(navigator: Navigator) = remember(navigator) {
    entryProvider<NavKey> {
        entry<Screen.Login> {
            val viewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                onLoginSuccess = { navigator.navigate(Screen.Home) },
                onNavigateToSignup = { navigator.navigate(Screen.Signup) },
                viewModel = viewModel
            )
        }

        entry<Screen.Signup> {
            val viewModel: AuthViewModel = hiltViewModel()
            SignupScreen(
                onSignupSuccess = { navigator.navigate(Screen.Home) },
                onBackToLogin = { navigator.goBack() },
                viewModel = viewModel
            )
        }

        entry<Screen.Home> {
            HomeScreen(
                onNavigateToExplore = { navigator.navigate(Screen.Explore) },
                onNavigateToSessions = { navigator.navigate(Screen.Sessions()) },
                onNavigateToSession = { sessionId -> navigator.navigate(Screen.ActiveSession(sessionId)) },
                onNavigateToCollections = { navigator.navigate(Screen.Collections) },
                onNewSessionWizard = { navigator.navigate(Screen.Sessions(startWizard = true)) },
                onNavigateToUnifiedCreation = { navigator.navigate(Screen.UnifiedCreation) },
                onCollectionClick = { collectionId ->
                    navigator.navigate(Screen.CollectionOverview(collectionId))
                },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onNavigateToNotifications = { navigator.navigate(Screen.Notifications) }
            )
        }

        entry<Screen.Explore> {
            val viewModel: ExploreViewModel = hiltViewModel()
            UnifiedExploreScreen(
                viewModel = viewModel,
                onCollectionClick = { collectionId ->
                    navigator.navigate(Screen.CollectionOverview(collectionId))
                },
                onSetClick = { setId, title ->
                    navigator.navigate(Screen.ExploreSet(setId))
                },
                onNavigateToUnifiedCreation = { navigator.navigate(Screen.UnifiedCreation) },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onBack = { navigator.navigate(Screen.Home) } // Go home from Explore
            )
        }

        entry<Screen.Categories> {
            val viewModel: ExploreViewModel = hiltViewModel()
            UnifiedExploreScreen(
                viewModel = viewModel,
                onCollectionClick = { collectionId ->
                    navigator.navigate(Screen.CollectionOverview(collectionId))
                },
                onSetClick = { setId, title ->
                    navigator.navigate(Screen.ExploreSet(setId))
                },
                onNavigateToUnifiedCreation = { navigator.navigate(Screen.UnifiedCreation) },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onBack = { navigator.navigate(Screen.Home) }
            )
        }

        entry<Screen.ExplorePager> { key ->
            val viewModel: ExploreViewModel = hiltViewModel()
            val questionStates by viewModel.questionStates.collectAsStateWithLifecycle()
            val collections by viewModel.collections.collectAsStateWithLifecycle()
            val sets by viewModel.sets.collectAsStateWithLifecycle()
            val sessions by viewModel.sessions.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            
            LaunchedEffect(key.categoryName) {
                viewModel.loadQuestionsByCollection(key.categoryName)
            }
            
            ExploreQuestionPagerScreen(
                categoryName = key.categoryName,
                questionStates = questionStates,
                collections = sets,
                sessions = sessions,
                onOptionSelected = { index, option ->
                    viewModel.selectOption(index, option)
                },
                onCheckAnswer = { index ->
                    viewModel.revealAnswer(index)
                },
                onPageChanged = { index ->
                    viewModel.loadQuestionDetails(index)
                    viewModel.loadQuestionDetails(index + 1)
                },
                onPinToggled = { index ->
                    viewModel.togglePin(index)
                },
                onAddToCollection = { index, id -> viewModel.addQuestionToSet(index, id) },
                onAddToSession = { index, id -> viewModel.addQuestionToSession(index, id) },
                onReportSubmitted = { index, explanation ->
                    viewModel.reportProblem(index, explanation)
                },
                onAskAi = { index, mode, _ ->
                    viewModel.askAi(index, mode)
                },
                onSaveAiAsOfficial = { index -> viewModel.saveAiResponseToQuestion(index) },
                onClearAiResponse = { index -> viewModel.clearAiResponse(index) },
                onDeleteQuestion = { index -> viewModel.deleteQuestion(index) },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onBack = { navigator.goBack() },
                currentUser = currentUser,
                viewModel = viewModel
            )
        }
        entry<Screen.AiGeneration> { key ->
            AiGenerationScreen(
                collectionId = key.collectionId,
                collectionName = key.collectionName,
                onBack = { navigator.goBack() }
            )
        }

        entry<Screen.Collections> {
            val viewModel: ExploreViewModel = hiltViewModel()
            UnifiedExploreScreen(
                viewModel = viewModel,
                onCollectionClick = { id ->
                    navigator.navigate(Screen.CollectionOverview(id))
                },
                onSetClick = { setId, _ ->
                    navigator.navigate(Screen.ExploreSet(setId))
                },
                onNavigateToUnifiedCreation = { navigator.navigate(Screen.UnifiedCreation) },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onBack = { navigator.goBack() }
            )
        }

        entry<Screen.CollectionOverview> { key ->
            val viewModel: ExploreViewModel = hiltViewModel()
            val collection by viewModel.selectedCollection.collectAsStateWithLifecycle()
            val sets by viewModel.collectionSets.collectAsStateWithLifecycle()
            val lastSession by viewModel.lastSession.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

            val questionCount by viewModel.questionCount.collectAsStateWithLifecycle()

            LaunchedEffect(key.collectionId) {
                viewModel.loadCollectionOverview(key.collectionId)
            }

            CollectionOverviewScreen(
                collection = collection,
                lastSession = lastSession,
                sets = sets,
                questionCount = questionCount,
                onContinueSession = { sessionId, index ->
                    navigator.navigate(Screen.ActiveSession(sessionId))
                },
                onExplore = { categoryName ->
                    navigator.navigate(Screen.ExplorePager(categoryName))
                },
                onStartSet = { setId ->
                    navigator.navigate(Screen.ExploreSet(setId))
                },
                onReportCollection = { reason ->
                    collection?.let { viewModel.reportCollection(it, reason) }
                },
                onDeleteCollection = { col ->
                    viewModel.deleteCollection(col.collectionId)
                    navigator.goBack()
                },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onBack = { navigator.goBack() },
                currentUser = currentUser,
                viewModel = viewModel
            )
        }

        entry<Screen.ExploreSet> { key ->
            val viewModel: ExploreViewModel = hiltViewModel()
            val questionStates by viewModel.questionStates.collectAsStateWithLifecycle()
            val sets by viewModel.sets.collectAsStateWithLifecycle()
            val sessions by viewModel.sessions.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

            LaunchedEffect(key.setId) {
                viewModel.loadQuestionsBySet(key.setId)
            }

            ExploreQuestionPagerScreen(
                categoryName = sets.find { it.setId == key.setId }?.title ?: "Questions",
                questionStates = questionStates,
                collections = sets,
                sessions = sessions,
                onOptionSelected = { index, option -> viewModel.selectOption(index, option) },
                onCheckAnswer = { index -> viewModel.revealAnswer(index) },
                onPinToggled = { index -> viewModel.togglePin(index) },
                onAddToCollection = { index, setId -> viewModel.addQuestionToSet(index, setId) },
                onAddToSession = { index, sessionId -> viewModel.addQuestionToSession(index, sessionId) },
                onReportSubmitted = { index, explanation -> viewModel.reportProblem(index, explanation) },
                onPageChanged = { index ->
                    viewModel.loadQuestionDetails(index)
                    viewModel.loadQuestionDetails(index + 1)
                    key.sessionId?.let { viewModel.updateSessionProgress(it, index) }
                },
                onAskAi = { index, mode, _ -> viewModel.askAi(index, mode) },
                onSaveAiAsOfficial = { index -> viewModel.saveAiResponseToQuestion(index) },
                onClearAiResponse = { index -> viewModel.clearAiResponse(index) },
                onDeleteQuestion = { index -> viewModel.deleteQuestion(index) },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onBack = { navigator.goBack() },
                currentUser = currentUser,
                viewModel = viewModel
            )
        }

        entry<Screen.Sessions> { key ->
            val viewModel: SessionsViewModel = hiltViewModel()
            val sessions by viewModel.sessions.collectAsStateWithLifecycle()
            val collections by viewModel.collections.collectAsStateWithLifecycle()
            
            LaunchedEffect(viewModel.sessionCreated) {
                viewModel.sessionCreated.collect { sessionId ->
                    navigator.navigate(Screen.ActiveSession(sessionId))
                }
            }

            SessionsListScreen(
                sessions = sessions,
                collections = collections,
                onSessionClick = { sessionId ->
                    navigator.navigate(Screen.ActiveSession(sessionId))
                },
                onFabClick = { /* Handled in screen internal state now */ },
                viewModel = viewModel,
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onBack = { navigator.goBack() }
            )
        }

        entry<Screen.ActiveSession> { key ->
            val viewModel: ActiveSessionViewModel = hiltViewModel()
            LaunchedEffect(key.sessionId) {
                viewModel.setSessionId(key.sessionId)
            }
            ActiveSessionScreen(
                viewModel = viewModel,
                onNavigateBack = { navigator.goBack() },
                onViewResults = { sessionId ->
                    navigator.navigate(Screen.SessionResults(sessionId))
                }
            )
        }

        entry<Screen.SessionResults> { key ->
            val viewModel: SessionResultsViewModel = hiltViewModel()
            LaunchedEffect(key.sessionId) {
                viewModel.initSession(key.sessionId)
            }
            SessionResultsScreen(
                viewModel = viewModel,
                onBackToHome = {
                    navigator.navigate(Screen.Home)
                }
            )
        }

        entry<Screen.Connect> {
            ChatListScreen(
                onChatClick = { chatId ->
                    navigator.navigate(Screen.ChatDetail(chatId))
                },
                onNewChat = {
                    navigator.navigate(Screen.NewChat)
                },
                onNewGroup = {
                    navigator.navigate(Screen.NewGroup)
                },
                onNavigateToBlockedList = {
                    navigator.navigate(Screen.BlockedList)
                },
                onProfileClick = { navigator.navigate(Screen.Profile) }
            )
        }

        entry<Screen.NewChat> {
            NewChatScreen(
                onBack = { navigator.goBack() },
                onChatStarted = { chatId: String ->
                    navigator.navigate(Screen.ChatDetail(chatId))
                },
                onNavigateToNewGroup = { navigator.navigate(Screen.NewGroup) },
                onProfileClick = { navigator.navigate(Screen.Profile) }
            )
        }

        entry<Screen.NewGroup> {
            NewGroupScreen(
                onBack = { navigator.goBack() },
                onGroupCreated = { chatId ->
                    navigator.navigate(Screen.ChatDetail(chatId))
                },
                onProfileClick = { navigator.navigate(Screen.Profile) }
            )
        }

        entry<Screen.UnifiedCreation> {
            ImportWizardScreen(
                viewModel = hiltViewModel(),
                source = null,
                targetId = null,
                onBack = { navigator.goBack() },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onNavigateToManualEditor = { targetId, name ->
                    navigator.navigate(Screen.ManualBuilder(targetId, name))
                }
            )
        }

        entry<Screen.ManualBuilder> { key ->
            ManualBuilderScreen(
                targetId = key.targetId,
                name = key.name,
                onBack = { navigator.goBack() },
                onAddQuestion = { setId -> 
                    navigator.navigate(Screen.QuestionEditor(questionId = null, setId = setId))
                },
                onEditQuestion = { questionId, setId ->
                    navigator.navigate(Screen.QuestionEditor(questionId = questionId, setId = setId))
                }
            )
        }

        entry<Screen.QuestionEditor> { key ->
            // Assuming we'll have a QuestionEditorViewModel later
            QuestionEditorScreen(
                questionId = key.questionId,
                setId = key.setId,
                onBack = { navigator.goBack() }
            )
        }

        entry<Screen.ChatDetail> { key ->
            val viewModel: com.algorithmx.q_base.ui.chat.ChatViewModel = hiltViewModel()
            val state by viewModel.chatDetailState.collectAsStateWithLifecycle()
            LaunchedEffect(key.chatId) {
                viewModel.setChatId(key.chatId)
            }
            ChatDetailScreen(
                onBack = { navigator.goBack() },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                viewModel = viewModel,
                onHeaderClick = { chatId ->
                    if (state.chat?.isGroup == true) {
                        navigator.navigate(Screen.GroupOverview(chatId))
                    } else {
                        navigator.navigate(Screen.ContactOverview(chatId))
                    }
                }
            )
        }

        entry<Screen.ContactOverview> { key ->
            val viewModel: ChatViewModel = hiltViewModel()
            LaunchedEffect(key.chatId) {
                viewModel.setChatId(key.chatId)
            }
            ContactOverviewScreen(
                chatId = key.chatId,
                onBack = { navigator.goBack() },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                viewModel = viewModel
            )
        }

        entry<Screen.GroupOverview> { key ->
            val viewModel: ChatViewModel = hiltViewModel()
            LaunchedEffect(key.chatId) {
                viewModel.setChatId(key.chatId)
            }
            GroupOverviewScreen(
                chatId = key.chatId,
                onBack = { navigator.goBack() },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                viewModel = viewModel
            )
        }

        entry<Screen.Settings> {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navigator.goBack() }
            )
        }

        entry<Screen.Profile> {
            val viewModel: ProfileViewModel = hiltViewModel()
            ProfileScreen(
                viewModel = viewModel,
                onBack = { navigator.goBack() },
                onNavigateToSettings = { navigator.navigate(Screen.Settings) },
                onLoggedOut = { 
                    navigator.navigate(Screen.Login)
                }
            )
        }

        entry<Screen.CollectionWizard> {
            val viewModel: ExploreViewModel = hiltViewModel()
            val collections by viewModel.collections.collectAsStateWithLifecycle()
            CollectionCreationWizard(
                onDismiss = { navigator.goBack() },
                categories = collections.map { it.collection },
                viewModel = viewModel
            )
        }

        entry<Screen.ImportWizard> { key ->
            ImportWizardScreen(
                viewModel = hiltViewModel(),
                source = key.source,
                targetId = key.targetId,
                onBack = { navigator.goBack() },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onNavigateToManualEditor = { setId, name ->
                    navigator.navigate(Screen.ManualBuilder(setId, name))
                }
            )
        }

        entry<Screen.Notifications> {
            ChatListScreen(
                onChatClick = { chatId ->
                    navigator.navigate(Screen.ChatDetail(chatId))
                },
                onNewChat = {
                    navigator.navigate(Screen.NewChat)
                },
                onNewGroup = {
                    navigator.navigate(Screen.NewGroup)
                },
                onNavigateToBlockedList = {
                    navigator.navigate(Screen.BlockedList)
                },
                onProfileClick = { navigator.navigate(Screen.Profile) }
            )
        }

        entry<Screen.BlockedList> {
            BlockedListScreen(
                onBack = { navigator.goBack() }
            )
        }
    }
}

@Composable
fun ManualBuilderStub(setId: String?, onBack: () -> Unit) {
    Column(
        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        androidx.compose.material3.Text("Manual Builder")
        androidx.compose.material3.Text("Target setId: $setId", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        androidx.compose.material3.Button(onClick = onBack) {
            androidx.compose.material3.Text("Go Back")
        }
    }
}
