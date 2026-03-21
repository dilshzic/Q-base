package com.algorithmx.q_base.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.DialogSceneStrategy
import com.algorithmx.q_base.ui.auth.LoginScreen
import com.algorithmx.q_base.ui.chat.ChatDetailScreen
import com.algorithmx.q_base.ui.chat.ChatListScreen
import com.algorithmx.q_base.ui.explore.CategoryListScreen
import com.algorithmx.q_base.ui.explore.ExploreQuestionPagerScreen
import com.algorithmx.q_base.ui.explore.ExploreViewModel
import com.algorithmx.q_base.ui.home.HomeScreen
import com.algorithmx.q_base.ui.sessions.*

@Composable
fun rememberAppEntryProvider(navigator: Navigator) = remember(navigator) {
    entryProvider {
        entry<Screen.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navigator.navigate(Screen.Home)
                }
            )
        }

        entry<Screen.Home> {
            HomeScreen(
                onNavigateToExplore = { navigator.navigate(Screen.Explore) },
                onNavigateToSession = { sessionId -> navigator.navigate(Screen.ActiveSession(sessionId)) },
                onNavigateToCollections = { /* TODO */ },
                onCategoryClick = { categoryName ->
                    navigator.navigate(Screen.ExplorePager(categoryName))
                }
            )
        }

        entry<Screen.Explore> {
            // This is actually showing the Categories by default in the new structure
            // Or we can keep a nested NavDisplay if needed, but simple is better for Nav3
            val viewModel: ExploreViewModel = hiltViewModel()
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            CategoryListScreen(
                categories = categories,
                onCategoryClick = { categoryName ->
                    navigator.navigate(Screen.ExplorePager(categoryName))
                }
            )
        }

        entry<Screen.Categories> {
            val viewModel: ExploreViewModel = hiltViewModel()
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            CategoryListScreen(
                categories = categories,
                onCategoryClick = { categoryName ->
                    navigator.navigate(Screen.ExplorePager(categoryName))
                }
            )
        }

        entry<Screen.ExplorePager> { key ->
            val viewModel: ExploreViewModel = hiltViewModel()
            val questionStates by viewModel.questionStates.collectAsStateWithLifecycle()
            
            // In Nav3, we should ideally trigger side effects in LaunchedEffect within the screen
            // or pass arguments to the ViewModel using the new lifecycle-viewmodel-navigation3
            ExploreQuestionPagerScreen(
                categoryName = key.categoryName,
                questionStates = questionStates,
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
                onReportSubmitted = { index, explanation ->
                    viewModel.reportProblem(index, explanation)
                },
                onBack = { navigator.goBack() }
            )
        }

        entry<Screen.Sessions> {
            val viewModel: SessionsViewModel = hiltViewModel()
            val sessions by viewModel.sessions.collectAsStateWithLifecycle()
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            
            SessionsListScreen(
                sessions = sessions,
                categories = categories,
                onSessionClick = { sessionId ->
                    navigator.navigate(Screen.ActiveSession(sessionId))
                },
                onFabClick = { /* Show sheet logic might need adjustment or be local to Screen */ }
            )
        }

        entry<Screen.ActiveSession> { key ->
            val viewModel: ActiveSessionViewModel = hiltViewModel()
            ActiveSessionScreen(
                viewModel = viewModel,
                onNavigateBack = { navigator.goBack() },
                onViewResults = { sessionId ->
                    navigator.navigate(Screen.SessionResults(sessionId))
                }
            )
        }

        entry<Screen.SessionResults> { key ->
            SessionResultsScreen(
                onBackToHome = {
                    navigator.navigate(Screen.Home)
                }
            )
        }

        entry<Screen.Inbox> {
            ChatListScreen(
                onChatClick = { chatId ->
                    navigator.navigate(Screen.ChatDetail(chatId))
                }
            )
        }

        entry<Screen.ChatDetail> { key ->
            ChatDetailScreen(
                onBack = { navigator.goBack() }
            )
        }
    }
}
