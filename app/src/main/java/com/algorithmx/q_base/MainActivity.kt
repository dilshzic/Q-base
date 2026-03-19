package com.algorithmx.q_base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                
                Crossfade(targetState = seeded, label = "loading_transition") { isReady ->
                    if (isReady) {
                        MainScreen()
                    } else {
                        LoadingScreen()
                    }
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
            CircularProgressIndicator(
                strokeWidth = 6.dp,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Preparing Database",
                style = MaterialTheme.typography.displaySmall, // Expressive Typography
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val config = LocalConfiguration.current
    val isExpanded = config.screenWidthDp > 600 // Simple adaptive check

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    
    val showNav = currentRoute in listOf(
        Screen.Home.route,
        Screen.Explore.route,
        Screen.Sessions.route,
        Screen.Inbox.route
    )

    Row(modifier = Modifier.fillMaxSize()) {
        // Expressive Navigation Rail for Large Screens
        if (isExpanded && showNav) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                header = {
                    Icon(
                        Icons.Rounded.Home, 
                        contentDescription = null, 
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                NavigationRailItem(
                    icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Home.route } == true,
                    onClick = { navController.navigate(Screen.Home.route) { launchSingleTop = true } }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Rounded.Explore, contentDescription = null) },
                    label = { Text("Explore") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Explore.route } == true,
                    onClick = { navController.navigate(Screen.Explore.route) { launchSingleTop = true } }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Rounded.Chat, contentDescription = null) },
                    label = { Text("Inbox") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Inbox.route } == true,
                    onClick = { navController.navigate(Screen.Inbox.route) { launchSingleTop = true } }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                    label = { Text("Sessions") },
                    selected = currentDestination?.hierarchy?.any { it.route == Screen.Sessions.route } == true,
                    onClick = { navController.navigate(Screen.Sessions.route) { launchSingleTop = true } }
                )
            }
        }

        Scaffold(
            bottomBar = {
                if (!isExpanded && showNav) {
                    NavigationBar(
                        tonalElevation = 8.dp,
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    ) {
                        val items = listOf(
                            Triple(Screen.Home.route, "Home", Icons.Rounded.Home),
                            Triple(Screen.Explore.route, "Explore", Icons.Rounded.Explore),
                            Triple(Screen.Inbox.route, "Inbox", Icons.Rounded.Chat),
                            Triple(Screen.Sessions.route, "Sessions", Icons.Rounded.History)
                        )
                        
                        items.forEach { (route, label, icon) ->
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = null) },
                                label = { Text(label) },
                                selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                                onClick = {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(if (isExpanded && showNav) PaddingValues(0.dp) else innerPadding)) {
                RootNavGraph(navController = navController)
            }
        }
    }
}
