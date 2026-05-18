package com.algorithmx.q_base.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.algorithmx.q_base.data.collections.StudyCollection

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
                onNavigateToCollections = { navigator.navigate(Screen.Collections) },
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
                onSetClick = { setId, title ->
                    navigator.navigate(Screen.ExploreSet(setId))
                },
                onNavigateToCreateNewCollection = { navigator.navigate(Screen.CreateNewCollection) },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onBack = { navigator.goBack() }
            )
        }

        entry<Screen.ExplorePager> { key ->
            val viewModel: ExploreViewModel = hiltViewModel()
            val questionStates by viewModel.questionStates.collectAsStateWithLifecycle()
            val collections by viewModel.collections.collectAsStateWithLifecycle()
            val sets by viewModel.sets.collectAsStateWithLifecycle()
            val sessions by viewModel.sessions.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
            
            LaunchedEffect(key.collectionName) {
                viewModel.loadQuestionsByStudyCollection(key.collectionName)
            }
            
            val coroutineScope = rememberCoroutineScope()
            
            ExploreQuestionPagerScreen(
                collectionName = key.collectionName,
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
                onAskAi = { index, mode ->
                    viewModel.askAi(index, mode)
                },
                onSaveAiAsOfficial = { index -> viewModel.saveAiResponseToQuestion(index) },
                onClearAiResponse = { index -> viewModel.clearAiResponse(index) },
                onDeleteQuestion = { index -> viewModel.deleteQuestion(index) },
                onEditQuestion = { index ->
                    val qState = questionStates.getOrNull(index)
                    if (qState != null) {
                        coroutineScope.launch {
                            val setId = viewModel.getSetIdForQuestion(qState.question.questionId) ?: ""
                            navigator.navigate(Screen.QuestionEditor(questionId = qState.question.questionId, setId = setId))
                        }
                    }
                },
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
            navigator.navigate(Screen.Explore)
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
                onExplore = { _ ->
                    navigator.navigate(Screen.ExplorePager(key.collectionId))
                },
                onStartSet = { setId ->
                    navigator.navigate(Screen.ExploreSet(setId))
                },
                onReportCollection = { reason ->
                    collection?.let { viewModel.reportCollection(it, reason) }
                },
                onDeleteCollection = { col ->
                    viewModel.deleteStudyCollection(col.collectionId)
                    navigator.goBack()
                },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onBack = { navigator.goBack() },
                currentUser = currentUser,
                viewModel = viewModel
            )
        }

        entry<Screen.PinnedQuestions> {
            val viewModel: ExploreViewModel = hiltViewModel()
            val questionStates by viewModel.questionStates.collectAsStateWithLifecycle()
            val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

            LaunchedEffect(Unit) {
                viewModel.loadPinnedQuestions()
            }

            PinnedQuestionsScreen(
                questionStates = questionStates,
                onPinToggled = { index ->
                    viewModel.togglePin(index)
                },
                onBack = { navigator.goBack() },
                onNavigateToExplore = { navigator.navigate(Screen.Explore) },
                currentUser = currentUser,
                onProfileClick = { navigator.navigate(Screen.Profile) },
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
                collectionName = sets.find { it.setId == key.setId }?.title ?: "Questions",
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
                onAskAi = { index, mode -> viewModel.askAi(index, mode) },
                onSaveAiAsOfficial = { index -> viewModel.saveAiResponseToQuestion(index) },
                onClearAiResponse = { index -> viewModel.clearAiResponse(index) },
                onDeleteQuestion = { index -> viewModel.deleteQuestion(index) },
                onEditQuestion = { index ->
                    val questionId = questionStates.getOrNull(index)?.question?.questionId
                    if (questionId != null) {
                        navigator.navigate(Screen.QuestionEditor(questionId = questionId, setId = key.setId))
                    }
                },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onBack = { navigator.goBack() },
                currentUser = currentUser,
                viewModel = viewModel
            )
        }

        entry<Screen.Sessions> {
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
                onFabClick = { 
                    navigator.navigate(Screen.NewSessionWizard)
                },
                viewModel = viewModel,
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onBack = { navigator.goBack() }
            )
        }

        entry<Screen.NewSessionWizard> {
            val viewModel: SessionsViewModel = hiltViewModel()
            val collections by viewModel.collections.collectAsStateWithLifecycle()

            LaunchedEffect(viewModel.sessionCreated) {
                viewModel.sessionCreated.collect { sessionId ->
                    navigator.navigate(Screen.ActiveSession(sessionId))
                }
            }

            NewSessionWizardScreen(
                viewModel = viewModel,
                collections = collections,
                onBack = { navigator.goBack() }
            )
        }

        entry<Screen.ActiveSession> { key ->
            val viewModel: ActiveSessionViewModel = hiltViewModel()
            LaunchedEffect(key.sessionId) {
                viewModel.setSessionId(key.sessionId, key.chatId)
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
                onDone = {
                    navigator.goBack()
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
                onNavigateToBlockedList = {
                    navigator.navigate(Screen.BlockedList)
                },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onNavigateToLogin = { navigator.navigate(Screen.Login) }
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
                },
                onJoinSession = { sessionId ->
                    navigator.navigate(Screen.ActiveSession(sessionId = sessionId, chatId = key.chatId))
                },
                onDeleteAndRestart = {
                    navigator.navigate(Screen.NewChat)
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
            val viewModel: ProfileViewModel = hiltViewModel()
            val context = androidx.compose.ui.platform.LocalContext.current
            ProfileScreen(
                viewModel = viewModel,
                onBack = { navigator.goBack() },
                onNavigateToSettings = { navigator.navigate(Screen.Settings) },
                onLoggedOut = { 
                    val sharedPrefs = context.getSharedPreferences("qbase_prefs", android.content.Context.MODE_PRIVATE)
                    sharedPrefs.edit().putBoolean("is_logged_in", false).apply()
                    navigator.navigate(Screen.Login)
                }
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
                onNavigateToBlockedList = {
                    navigator.navigate(Screen.BlockedList)
                },
                onProfileClick = { navigator.navigate(Screen.Profile) },
                onNavigateToLogin = { navigator.navigate(Screen.Login) }
            )
        }

        entry<Screen.BlockedList> {
            BlockedListScreen(
                onBack = { navigator.goBack() }
            )
        }
    }
}

