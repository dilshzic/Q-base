package com.algorithmx.q_base

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.google.firebase.auth.FirebaseAuth
import com.algorithmx.q_base.data.AppDatabase
import com.algorithmx.q_base.data.DatabaseSeeder
import com.algorithmx.q_base.ui.navigation.*
import com.algorithmx.q_base.ui.theme.QbaseTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val isSeeded = MutableStateFlow(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
        } else {
            // Permission denied
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var syncRepository: com.algorithmx.q_base.data.repository.SyncRepository

    @Inject
    lateinit var notificationHelper: com.algorithmx.q_base.util.NotificationHelper

    @Inject
    lateinit var auth: FirebaseAuth

    @Inject
    lateinit var dataStoreManager: com.algorithmx.q_base.brain.BrainDataStoreManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission()
        enableEdgeToEdge()

        // Start global sync for notifications reactively
        lifecycleScope.launch {
            com.algorithmx.q_base.data.repository.AuthRepository(auth).currentUser.collect { user ->
                if (user != null) {
                    syncRepository.observeAllIncomingEvents(notificationHelper).collect {}
                }
            }
        }

        lifecycleScope.launch {
            DatabaseSeeder(this@MainActivity, database, dataStoreManager).seedDatabaseIfNeeded()
            isSeeded.value = true
        }

        setContent {
            QbaseTheme {
                val seeded by isSeeded.collectAsStateWithLifecycle()
                
                // Define navigation state at the top level to ensure it has the same lifetime as the Activity.
                // This prevents the "NavController has been destroyed" error when switching from LoadingScreen.
                val startRoute = if (auth.currentUser == null) Screen.Login else Screen.Home
                val navigationState = rememberNavigationState(
                    startRoute = startRoute,
                    topLevelRoutes = setOf(Screen.Home, Screen.Explore, Screen.Connect, Screen.Sessions())
                )
                val navigator = remember(navigationState) { Navigator(navigationState) }

                LaunchedEffect(seeded) {
                    if (seeded) {
                        intent?.getStringExtra("CHAT_ID")?.let { chatId ->
                            navigator.navigate(Screen.ChatDetail(chatId))
                        }
                        intent?.getStringExtra("SESSION_ID")?.let { sessionId ->
                            navigator.navigate(Screen.ActiveSession(sessionId))
                        }
                    }
                }

                Crossfade(targetState = seeded, label = "loading_transition") { isReady ->
                    if (isReady) {
                        MainScreen(navigationState, navigator)
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
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MainScreen(navigationState: NavigationState, navigator: Navigator) {
    val config = LocalConfiguration.current
    val isExpanded = config.screenWidthDp > 600

    val currentStack = navigationState.backStacks[navigationState.topLevelRoute]
    val isAtRoot = (currentStack?.size ?: 0) <= 1

    val showNav = isAtRoot && (
        navigationState.topLevelRoute is Screen.Home ||
        navigationState.topLevelRoute is Screen.Explore ||
        navigationState.topLevelRoute is Screen.Sessions ||
        navigationState.topLevelRoute is Screen.Connect
    )

    Row(modifier = Modifier.fillMaxSize()) {
        if (isExpanded && showNav) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                header = null
            ) {
                NavigationRailItem(
                    icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                    label = { Text("Home") },
                    selected = navigationState.topLevelRoute == Screen.Home,
                    onClick = { navigator.navigate(Screen.Home) }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Rounded.Explore, contentDescription = null) },
                    label = { Text("Explore") },
                    selected = navigationState.topLevelRoute == Screen.Explore,
                    onClick = { navigator.navigate(Screen.Explore) }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Rounded.Hub, contentDescription = null) },
                    label = { Text("Connect") },
                    selected = navigationState.topLevelRoute == Screen.Connect,
                    onClick = { navigator.navigate(Screen.Connect) }
                )
                NavigationRailItem(
                    icon = { Icon(Icons.Rounded.Assessment, contentDescription = null) },
                    label = { Text("Sessions") },
                    selected = navigationState.topLevelRoute is Screen.Sessions,
                    onClick = { navigator.navigate(Screen.Sessions()) }
                )
            }
        }

        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (!isExpanded && showNav) {
                    NavigationBar(
                        tonalElevation = 8.dp,
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    ) {
                        val navItems = listOf(
                            Triple(Screen.Home, "Home", Icons.Rounded.Home),
                            Triple(Screen.Explore, "Explore", Icons.Rounded.Explore),
                            Triple(Screen.Connect, "Connect", Icons.Rounded.Hub),
                            Triple(Screen.Sessions(), "Sessions", Icons.Rounded.History)
                        )
                        
                        navItems.forEach { (screen, label, icon) ->
                            NavigationBarItem(
                                icon = { Icon(icon, contentDescription = null) },
                                label = { Text(label) },
                                selected = navigationState.topLevelRoute == screen || (screen is Screen.Sessions && navigationState.topLevelRoute is Screen.Sessions),
                                onClick = { navigator.navigate(screen) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(if (isExpanded && showNav) PaddingValues(0.dp) else innerPadding)) {
                AppNavDisplay(navigationState, navigator)
            }
        }
    }
}

@Composable
fun AppNavDisplay(navigationState: NavigationState, navigator: Navigator) {
    val entryProvider = rememberAppEntryProvider(navigator)
    
    NavDisplay(
        backStack = navigationState.backStacks[navigationState.topLevelRoute]!!,
        entryProvider = entryProvider,
        onBack = { navigator.goBack() }
    )
}
