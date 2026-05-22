package com.algorithmx.q_base.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.entryProvider
import com.algorithmx.q_base.feature.auth.presentation.*
import com.algorithmx.q_base.feature.chat.presentation.*
import com.algorithmx.q_base.feature.ai.*
import com.algorithmx.q_base.feature.content_import.*
import com.algorithmx.q_base.feature.explore.*
import com.algorithmx.q_base.feature.home.HomeScreen
import com.algorithmx.q_base.feature.settings.*

@Composable
fun rememberAppEntryProvider(navigator: Navigator) = remember(navigator) {
    entryProvider<androidx.navigation3.runtime.NavKey> {
        entry<Screen.Login> {
            val viewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                onLoginSuccess = { navigator.resetTo(Screen.Home) },
                onNavigateToSignup = { navigator.navigate(Screen.Signup) },
                viewModel = viewModel
            )
        }

        entry<Screen.Signup> {
            val viewModel: AuthViewModel = hiltViewModel()
            SignupScreen(
                onSignupSuccess = { navigator.resetTo(Screen.Home) },
                onBackToLogin = { navigator.goBack() },
                viewModel = viewModel
            )
        }

        entry<Screen.Home> {
            HomeScreen(
                onNavigateToCollections = { navigator.navigate(Screen.Explore) },
                onNavigateToSessions = { navigator.navigate(Screen.Sessions()) },
                onNavigateToSession = { sessionId -> navigator.navigate(Screen.ActiveSession(sessionId)) },
                onNavigateToSessionResults = { sessionId -> navigator.navigate(Screen.SessionResults(sessionId)) },
                onNewSessionWizard = { navigator.navigate(Screen.NewSessionWizard) },
                onNavigateToCreateNewCollection = { navigator.navigate(Screen.CreateNewCollection) },
                onCollectionClick = { collectionId ->
                    navigator.navigate(Screen.CollectionOverview(collectionId))
                },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onNavigateToNotifications = { navigator.navigate(Screen.Notifications) },
                onNavigateToPinnedQuestions = { navigator.navigate(Screen.PinnedQuestions) }
            )
        }

        entry<Screen.Explore> {
            val viewModel: ExploreViewModel = hiltViewModel()
            UnifiedExploreScreen(
                viewModel = viewModel,
                onCollectionClick = { collectionId ->
                    navigator.navigate(Screen.CollectionOverview(collectionId))
                },
                onSetClick = { setId, _ ->
                    navigator.navigate(Screen.ExploreSet(setId))
                },
                onNavigateToCreateNewCollection = { navigator.navigate(Screen.CreateNewCollection) },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onBack = { navigator.goBack() }
            )
        }

        entry<Screen.ExplorePager> { key ->
            ExplorePagerWrapper(key, navigator)
        }

        entry<Screen.AiGeneration> { key ->
            AiGenerationScreen(
                collectionId = key.collectionId,
                collectionName = key.collectionName,
                onBack = { navigator.goBack() }
            )
        }

        entry<Screen.CollectionOverview> { key ->
            CollectionOverviewWrapper(key, navigator)
        }

        entry<Screen.PinnedQuestions> {
            PinnedQuestionsWrapper(navigator)
        }

        entry<Screen.ExploreSet> { key ->
            ExploreSetWrapper(key, navigator)
        }

        entry<Screen.Sessions> {
            SessionsWrapper(navigator)
        }

        entry<Screen.NewSessionWizard> {
            NewSessionWizardWrapper(navigator)
        }

        entry<Screen.ActiveSession> { key ->
            ActiveSessionWrapper(key, navigator)
        }

        entry<Screen.SessionResults> { key ->
            SessionResultsWrapper(key, navigator)
        }

        entry<Screen.Connect> {
            ChatListScreen(
                onChatClick = { chatId ->
                    navigator.navigate(Screen.ChatDetail(chatId))
                },
                onNewChat = {
                    navigator.navigate(Screen.NewChat)
                },
                onNavigateToBlockedList = {
                    navigator.navigate(Screen.BlockedList)
                },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onNavigateToLogin = { navigator.resetTo(Screen.Login) }
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

        entry<Screen.CreateNewCollection> {
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
            QuestionEditorScreen(
                questionId = key.questionId,
                setId = key.setId,
                onBack = { navigator.goBack() }
            )
        }

        entry<Screen.ChatDetail> { key ->
            ChatDetailWrapper(key, navigator)
        }

        entry<Screen.ContactOverview> { key ->
            ContactOverviewWrapper(key, navigator)
        }

        entry<Screen.GroupOverview> { key ->
            GroupOverviewWrapper(key, navigator)
        }

        entry<Screen.Settings> {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navigator.goBack() },
                onNavigateToBrainManager = { navigator.navigate(Screen.AiBrainManager) },
                onNavigateToAppTheme = { navigator.navigate(Screen.AppTheme) }
            )
        }

        entry<Screen.AppTheme> {
            val viewModel: SettingsViewModel = hiltViewModel()
            AppThemeScreen(
                viewModel = viewModel,
                onBack = { navigator.goBack() }
            )
        }

        entry<Screen.AiBrainManager> {
            val viewModel: SettingsViewModel = hiltViewModel()
            AiBrainManagerScreen(
                viewModel = viewModel,
                onBack = { navigator.goBack() }
            )
        }

        entry<Screen.Profile> {
            ProfileWrapper(navigator)
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
            // Notifications are surfaced via unread badges on the Connect tab.
            // Redirect there instead of rendering a duplicate ChatListScreen.
            androidx.compose.runtime.LaunchedEffect(Unit) {
                navigator.navigate(Screen.Connect)
            }
        }

        entry<Screen.BlockedList> {
            BlockedListScreen(
                onBack = { navigator.goBack() }
            )
        }
    }
}