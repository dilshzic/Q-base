package com.algorithmx.q_base.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.algorithmx.q_base.ui.explore.*
import com.algorithmx.q_base.ui.home.HomeScreen
import com.algorithmx.q_base.ui.sessions.*
import com.algorithmx.q_base.ui.auth.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import com.google.firebase.auth.FirebaseAuth

sealed class Screen(val route: String) {
    object Login : Screen("login_route")
    object Home : Screen("home_route")
    object Explore : Screen("explore_route")
    object Sessions : Screen("sessions_route")
    object ActiveSession : Screen("active_session/{sessionId}") {
        fun createRoute(sessionId: String) = "active_session/$sessionId"
    }
    object SessionResults : Screen("session_results/{sessionId}") {
        fun createRoute(sessionId: String) = "session_results/$sessionId"
    }
    
    // Nested Explore Routes
    object Categories : Screen("categories")
    object ExplorePager : Screen("explore_pager/{categoryName}") {
        fun createRoute(categoryName: String) = "explore_pager/${categoryName.encodeForNav()}"
    }
}

private fun String.encodeForNav() = this.replace("/", "%2F")

@Composable
fun RootNavGraph(navController: NavHostController) {
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val startDestination = if (currentUser == null) Screen.Login.route else Screen.Home.route

    val sessionsViewModel: SessionsViewModel = hiltViewModel()
    var showCreateSessionSheet by remember { mutableStateOf(false) }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToExplore = { navController.navigate(Screen.Explore.route) },
                onNavigateToSession = { sessionId -> navController.navigate(Screen.ActiveSession.createRoute(sessionId)) },
                onNavigateToCollections = { /* TODO */ }
            )
        }

        composable(Screen.Explore.route) {
            val exploreNavController = rememberNavController()
            val exploreViewModel: ExploreViewModel = hiltViewModel()
            ExploreNavGraph(navController = exploreNavController, viewModel = exploreViewModel)
        }

        composable(Screen.Sessions.route) {
            val sessions by sessionsViewModel.sessions.collectAsStateWithLifecycle()
            val categories by sessionsViewModel.categories.collectAsStateWithLifecycle()
            
            SessionsListScreen(
                sessions = sessions,
                categories = categories,
                onSessionClick = { sessionId ->
                    navController.navigate(Screen.ActiveSession.createRoute(sessionId))
                },
                onFabClick = { showCreateSessionSheet = true }
            )
            
            if (showCreateSessionSheet) {
                CreateSessionBottomSheet(
                    categories = categories,
                    onDismiss = { showCreateSessionSheet = false },
                    onCreateSession = { catName, qCount, isTimed ->
                        sessionsViewModel.createSession(catName, qCount, isTimed)
                        showCreateSessionSheet = false
                    }
                )
            }
        }

        composable(
            route = Screen.ActiveSession.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            val activeSessionViewModel: ActiveSessionViewModel = hiltViewModel()
            ActiveSessionScreen(
                viewModel = activeSessionViewModel,
                onNavigateBack = { navController.popBackStack() },
                onViewResults = { sessionId ->
                    navController.navigate(Screen.SessionResults.createRoute(sessionId)) {
                        popUpTo(Screen.ActiveSession.createRoute(sessionId)) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.SessionResults.route,
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) {
            SessionResultsScreen(
                onBackToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    }
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        sessionsViewModel.sessionCreated.collect { sessionId ->
            navController.navigate(Screen.ActiveSession.createRoute(sessionId))
        }
    }
}

@Composable
fun ExploreNavGraph(
    navController: NavHostController,
    viewModel: ExploreViewModel
) {
    NavHost(navController = navController, startDestination = Screen.Categories.route) {
        composable(Screen.Categories.route) {
            val categories by viewModel.categories.collectAsStateWithLifecycle()
            CategoryListScreen(
                categories = categories,
                onCategoryClick = { categoryName ->
                    navController.navigate(Screen.ExplorePager.createRoute(categoryName))
                }
            )
        }

        composable(
            route = Screen.ExplorePager.route,
            arguments = listOf(
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("categoryName")
                ?.replace("%2F", "/") ?: ""
            
            val questionStates by viewModel.questionStates.collectAsStateWithLifecycle()
            
            LaunchedEffect(categoryName) {
                viewModel.loadQuestionsByMasterCategory(categoryName)
            }

            ExploreQuestionPagerScreen(
                categoryName = categoryName,
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
                onBack = { navController.popBackStack() }
            )
        }
    }
}
