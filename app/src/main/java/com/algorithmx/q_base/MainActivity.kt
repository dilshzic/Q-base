package com.algorithmx.q_base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.algorithmx.q_base.data.AppDatabase
import com.algorithmx.q_base.data.DatabaseSeeder
import com.algorithmx.q_base.data.repository.ExploreRepository
import com.algorithmx.q_base.data.repository.SessionRepository
import com.algorithmx.q_base.ui.explore.ExploreViewModel
import com.algorithmx.q_base.ui.explore.ExploreViewModelFactory
import com.algorithmx.q_base.ui.navigation.ExploreNavGraph
import com.algorithmx.q_base.ui.sessions.*
import com.algorithmx.q_base.ui.theme.QbaseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val database = AppDatabase.getDatabase(this)
        val exploreRepository = ExploreRepository(database.categoryDao(), database.questionDao())
        val exploreViewModelFactory = ExploreViewModelFactory(exploreRepository)
        
        val sessionRepository = SessionRepository(database.sessionDao(), database.categoryDao(), database.questionDao())
        val sessionsViewModelFactory = SessionsViewModelFactory(sessionRepository)

        // Seed database if needed
        lifecycleScope.launch {
            DatabaseSeeder(this@MainActivity, database).seedDatabaseIfNeeded()
        }

        setContent {
            QbaseTheme {
                MainScreen(exploreViewModelFactory, sessionsViewModelFactory, sessionRepository)
            }
        }
    }
}

@Composable
fun MainScreen(
    exploreViewModelFactory: ExploreViewModelFactory,
    sessionsViewModelFactory: SessionsViewModelFactory,
    sessionRepository: SessionRepository
) {
    val navController = rememberNavController()
    val exploreNavController = rememberNavController()
    val exploreViewModel: ExploreViewModel = viewModel(factory = exploreViewModelFactory)
    val sessionsViewModel: SessionsViewModel = viewModel(factory = sessionsViewModelFactory)
    
    var showCreateSessionSheet by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val isSessionActive = currentDestination?.route?.startsWith("active_session") == true
            
            if (!isSessionActive) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.Explore, contentDescription = null) },
                        label = { Text("Explore") },
                        selected = currentDestination?.hierarchy?.any { it.route == "explore_route" } == true,
                        onClick = {
                            navController.navigate("explore_route") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                        label = { Text("Sessions") },
                        selected = currentDestination?.hierarchy?.any { it.route == "sessions_route" } == true,
                        onClick = {
                            navController.navigate("sessions_route") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "explore_route",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("explore_route") {
                ExploreNavGraph(navController = exploreNavController, viewModel = exploreViewModel)
            }
            composable("sessions_route") {
                val sessions by sessionsViewModel.sessions.collectAsState()
                val categories by sessionsViewModel.categories.collectAsState()
                
                SessionsListScreen(
                    sessions = sessions,
                    onSessionClick = { sessionId ->
                        navController.navigate("active_session/$sessionId")
                    },
                    onFabClick = { showCreateSessionSheet = true }
                )
                
                if (showCreateSessionSheet) {
                    CreateSessionBottomSheet(
                        categories = categories,
                        onDismiss = { showCreateSessionSheet = false },
                        onCreateSession = { catId, qCount, isTimed ->
                            sessionsViewModel.createSession(catId, qCount, isTimed)
                            showCreateSessionSheet = false
                        }
                    )
                }
            }
            composable(
                route = "active_session/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
                val activeSessionViewModel: ActiveSessionViewModel = viewModel(
                    factory = ActiveSessionViewModelFactory(sessionRepository, sessionId)
                )
                ActiveSessionScreen(
                    viewModel = activeSessionViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
        
        LaunchedEffect(Unit) {
            sessionsViewModel.sessionCreated.collect { sessionId ->
                navController.navigate("active_session/$sessionId")
            }
        }
    }
}
