package com.algorithmx.q_base.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.algorithmx.q_base.feature.auth.presentation.*
import com.algorithmx.q_base.feature.chat.presentation.*
import com.algorithmx.q_base.ui.ai.*
import com.algorithmx.q_base.ui.content_import.*
import com.algorithmx.q_base.ui.explore.*
import com.algorithmx.q_base.ui.sessions.*
import com.algorithmx.q_base.ui.settings.*

@Composable
fun ExplorePagerWrapper(key: Screen.ExplorePager, navigator: Navigator) {
    val viewModel: ExploreViewModel = hiltViewModel()
    val questionStates by viewModel.questionStates.collectAsStateWithLifecycle()
    val studyCollections by viewModel.collections.collectAsStateWithLifecycle()
    val sets by viewModel.sets.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    
    LaunchedEffect(key.collectionId) {
        viewModel.resetQuestionStates()
        viewModel.loadQuestionsByStudyCollection(key.collectionId)
    }
    
    val coroutineScope = rememberCoroutineScope()
    
    ExploreQuestionPagerScreen(
        collectionName = studyCollections.find { it.collection.collectionId == key.collectionId }?.collection?.name ?: "Questions",
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

@Composable
fun CollectionOverviewWrapper(key: Screen.CollectionOverview, navigator: Navigator) {
    val viewModel: ExploreViewModel = hiltViewModel()
    val collection by viewModel.selectedCollection.collectAsStateWithLifecycle()
    val sets by viewModel.collectionSets.collectAsStateWithLifecycle()
    val lastSession by viewModel.lastSession.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val questionCount by viewModel.questionCount.collectAsStateWithLifecycle()

    LaunchedEffect(key.collectionId) {
        viewModel.resetQuestionStates()
        viewModel.loadCollectionOverview(key.collectionId)
    }

    CollectionOverviewScreen(
        collection = collection,
        lastSession = lastSession,
        sets = sets,
        questionCount = questionCount,
        onContinueSession = { sessionId, _ ->
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

@Composable
fun PinnedQuestionsWrapper(navigator: Navigator) {
    val viewModel: ExploreViewModel = hiltViewModel()
    val questionStates by viewModel.questionStates.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.resetQuestionStates()
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

@Composable
fun ExploreSetWrapper(key: Screen.ExploreSet, navigator: Navigator) {
    val viewModel: ExploreViewModel = hiltViewModel()
    val questionStates by viewModel.questionStates.collectAsStateWithLifecycle()
    val sets by viewModel.sets.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    LaunchedEffect(key.setId) {
        viewModel.resetQuestionStates()
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

@Composable
fun SessionsWrapper(navigator: Navigator) {
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

@Composable
fun NewSessionWizardWrapper(navigator: Navigator) {
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

@Composable
fun ActiveSessionWrapper(key: Screen.ActiveSession, navigator: Navigator) {
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

@Composable
fun SessionResultsWrapper(key: Screen.SessionResults, navigator: Navigator) {
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

@Composable
fun ChatDetailWrapper(key: Screen.ChatDetail, navigator: Navigator) {
    val viewModel: ChatViewModel = hiltViewModel()
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

@Composable
fun ContactOverviewWrapper(key: Screen.ContactOverview, navigator: Navigator) {
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

@Composable
fun GroupOverviewWrapper(key: Screen.GroupOverview, navigator: Navigator) {
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

@Composable
fun ProfileWrapper(navigator: Navigator) {
    val viewModel: ProfileViewModel = hiltViewModel()
    val context = LocalContext.current
    ProfileScreen(
        viewModel = viewModel,
        onBack = { navigator.goBack() },
        onNavigateToSettings = { navigator.navigate(Screen.Settings) },
        onLoggedOut = { 
            val sharedPrefs = context.getSharedPreferences("qbase_prefs", android.content.Context.MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("is_logged_in", false).apply()
            navigator.resetTo(Screen.Login)
        }
    )
}