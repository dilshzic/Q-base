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
import com.algorithmx.q_base.data.DatabaseSeeder
import com.algorithmx.q_base.data.auth.AuthRepository
import com.algorithmx.q_base.data.sync.SyncRepository
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
    lateinit var cryptoManager: com.algorithmx.q_base.core_crypto.CryptoManager

    @Inject
    lateinit var syncRepository: SyncRepository

    @Inject
    lateinit var notificationHelper: com.algorithmx.q_base.util.NotificationHelper

    @Inject
    lateinit var dataStoreManager: com.algorithmx.q_base.core_ai.brain.BrainDataStoreManager

    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission()
        enableEdgeToEdge()

        // Pre-warm Crypto
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Default) {
            try {
                cryptoManager.initializeAndGetPublicKey()
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Crypto pre-warm failed", e)
            }
        }

        // Start global sync for notifications reactively
        lifecycleScope.launch {
            authRepository.currentUser.collect { user ->
                if (user != null) {
                    syncRepository.observeAllIncomingEvents(notificationHelper).collect {}
                }
            }
        }

        lifecycleScope.launch {
            databaseSeeder.seedDatabaseIfNeeded()
            isSeeded.value = true
        }

        setContent {
            val brainConfig by dataStoreManager.brainConfigFlow.collectAsStateWithLifecycle(
                initialValue = com.algorithmx.q_base.core_ai.brain.models.StoredBrainConfig(
                    provider = com.algorithmx.androidmodules.coreai.brain.models.BrainProvider.GEMINI,
                    modelName = "gemini-1.5-flash",
                    systemInstruction = "",
                    totalRequests = 0,
                    totalTokens = 0,
                    category = com.algorithmx.androidmodules.coreai.brain.models.BrainCategory.TEXT_TO_TEXT,
                    themeMode = "SYSTEM",
                    notificationsEnabled = true,
                    isMasterAiFreeze = false,
                    taskConfigs = emptyMap()
                )
            )

            QbaseTheme(themeMode = brainConfig.themeMode) {
                val seeded by isSeeded.collectAsStateWithLifecycle()
                
                // Observe authentication state reactively
                val userFlow = remember { authRepository.currentUser }
                val user by userFlow.collectAsState(initial = null)
                
                // Use a derived state for the preferred start route based on current auth state.
                // We use 'user' as a key for remember to ensure startRoute updates if the process is restored
                // and the user state is eventually resolved.
                val startRoute = remember(user == null) { 
                    if (user == null) Screen.Login else Screen.Home 
                }
                
                val topLevelRoutes = remember(user == null) {
                    if (user == null) {
                        setOf(Screen.Home, Screen.Explore, Screen.Connect, Screen.Sessions(), Screen.Login)
                    } else {
                        setOf(Screen.Home, Screen.Explore, Screen.Connect, Screen.Sessions())
                    }
                }
                
                val navigationState = rememberNavigationState(
                    startRoute = startRoute,
                    topLevelRoutes = topLevelRoutes
                )
                val navigator = remember(navigationState) { Navigator(navigationState) }

                android.util.Log.d("MainActivity", "Composition: startRoute=$startRoute, currentTopLevel=${navigationState.topLevelRoute}")

                // Sync navigation when user state changes
                LaunchedEffect(user) {
                    val currentRoute = navigationState.topLevelRoute
                    if (user != null && currentRoute == Screen.Login) {
                        navigator.navigate(Screen.Home)
                    } else if (user == null && currentRoute != Screen.Login) {
                        navigator.navigate(Screen.Login)
                    }
                }

                LaunchedEffect(seeded) {
                    android.util.Log.d("MainActivity", "Seeded changed: $seeded")
                    if (seeded) {
                        intent?.getStringExtra("CHAT_ID")?.let { chatId ->
                            android.util.Log.d("MainActivity", "Deep link to chat: $chatId")
                            navigator.navigate(Screen.ChatDetail(chatId))
                        }
                        intent?.getStringExtra("SESSION_ID")?.let { sessionId ->
                            android.util.Log.d("MainActivity", "Deep link to session: $sessionId")
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

    val showNav = isAtRoot && (navigationState.topLevelRoute as? Screen)?.isTopLevel == true && navigationState.topLevelRoute !is Screen.Login

    // Global observation of unread messages for the badge
    val homeViewModel: com.algorithmx.q_base.ui.home.HomeViewModel = androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel()
    val totalUnreadCount by homeViewModel.totalUnreadCount.collectAsStateWithLifecycle()

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
                    icon = {
                        BadgedBox(
                            badge = {
                                if (totalUnreadCount > 0) {
                                    Badge {
                                        Text(totalUnreadCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Hub, contentDescription = null)
                        }
                    },
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
                                icon = {
                                    if (screen == Screen.Connect) {
                                        BadgedBox(
                                            badge = {
                                                if (totalUnreadCount > 0) {
                                                    Badge {
                                                        Text(totalUnreadCount.toString())
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(icon, contentDescription = null)
                                        }
                                    } else {
                                        Icon(icon, contentDescription = null)
                                    }
                                },
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
    val currentRoute = navigationState.topLevelRoute
    
    // Animate between top-level destinations
    AnimatedContent(
        targetState = navigationState.toEntries(entryProvider),
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f) togetherWith 
            fadeOut(animationSpec = tween(200))
        },
        label = "nav_display_transition"
    ) { entries ->
        NavDisplay(
            entries = entries,
            onBack = { navigator.goBack() }
        )
    }
}
