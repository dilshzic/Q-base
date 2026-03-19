package com.algorithmx.q_base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.algorithmx.q_base.data.AppDatabase
import com.algorithmx.q_base.data.DatabaseSeeder
import com.algorithmx.q_base.ui.navigation.RootNavGraph
import com.algorithmx.q_base.ui.navigation.Screen
import com.algorithmx.q_base.ui.theme.QbaseTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val isSeeded = MutableStateFlow(false)

    @Inject
    lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            DatabaseSeeder(this@MainActivity, database).seedDatabaseIfNeeded()
            isSeeded.value = true
        }

        setContent {
            QbaseTheme {
                val seeded by isSeeded.collectAsStateWithLifecycle()
                if (seeded) {
                    MainScreen()
                } else {
                    LoadingScreen()
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Preparing Database...",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Check if we should show the bottom bar
            val currentRoute = currentDestination?.route
            val showBottomBar = currentRoute in listOf(
                Screen.Home.route,
                Screen.Explore.route,
                Screen.Sessions.route
            )
            
            if (showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                        label = { Text("Home") },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true,
                        onClick = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.Explore, contentDescription = null) },
                        label = { Text("Explore") },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Explore.route } == true,
                        onClick = {
                            navController.navigate(Screen.Explore.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                        label = { Text("Sessions") },
                        selected = currentDestination?.hierarchy?.any { it.route == Screen.Sessions.route } == true,
                        onClick = {
                            navController.navigate(Screen.Sessions.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            RootNavGraph(navController = navController)
        }
    }
}
