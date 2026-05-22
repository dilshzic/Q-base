package com.algorithmx.q_base

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
// This is the correct package declaration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay
import com.algorithmx.q_base.core.data.DatabaseSeeder
import com.algorithmx.q_base.core.data.auth.AuthRepository
import com.algorithmx.q_base.sync.orchestration.SyncRepository
import com.algorithmx.q_base.ui.state.AppAccessState
import com.algorithmx.q_base.ui.state.LocalAppAccessState
import com.algorithmx.q_base.ui.navigation.*
import com.algorithmx.q_base.ui.theme.QbaseTheme
import com.algorithmx.q_base.util.NetworkMonitor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var universalQueueManager: com.algorithmx.q_base.sync.orchestration.UniversalQueueManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
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

        // Trigger session check when network is restored to re-verify backend readiness
        lifecycleScope.launch {
            networkMonitor.isOnline
                .collect { online ->
                    if (online) {
                        try {
                            authRepository.checkCurrentSession()
                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Session validation failed on network restore", e)
                        }
                    }
                }
        }

        // Start global sync for notifications reactively based on backend readiness
        var restSyncJob: kotlinx.coroutines.Job? = null
        var realtimeSyncJob: kotlinx.coroutines.Job? = null
        lifecycleScope.launch {
            val isBackendReady = combine(
                networkMonitor.isOnline,
                authRepository.isBackendSessionValid
            ) { online, sessionValid -> online && sessionValid }

            combine(
                authRepository.currentUser,
                isBackendReady
            ) { user, ready -> user?.uid to ready }
                .distinctUntilChanged()
                .collect { (userId, isOnline) ->
                    restSyncJob?.cancel()
                    if (userId != null && isOnline) {
                        restSyncJob = launch {
                            try {
                                // Flush pending offline messages first
                                syncRepository.flushQueue()
                                
                                // Flush universal background actions (Profile updates, Moderation, Admin changes)
                                universalQueueManager.flushUniversalQueue()
                                
                                syncRepository.syncUserChatsFromRemote()
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Failed to sync or flush on network restore", e)
                            }
                        }
                    }

                    if (userId == null || !isOnline) {
                        realtimeSyncJob?.cancel()
                        realtimeSyncJob = null
                    } else if (realtimeSyncJob == null || !realtimeSyncJob!!.isActive) {
                        realtimeSyncJob = launch {
                            syncRepository.observeAllIncomingMessages(notificationHelper).collect {}
                        }
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
                val isSessionChecked by authRepository.isSessionChecked.collectAsStateWithLifecycle(initialValue = false)
                val isOnline by remember {
                    combine(
                        networkMonitor.isOnline,
                        authRepository.isBackendSessionValid
                    ) { online, sessionValid -> online && sessionValid }
                }.collectAsStateWithLifecycle(initialValue = false)
                
                // Observe authentication state reactively
                val userFlow = remember { authRepository.currentUser }
                val user by userFlow.collectAsStateWithLifecycle(initialValue = null)
                
                val context = androidx.compose.ui.platform.LocalContext.current
                val sharedPrefs = remember(context) {
                    context.getSharedPreferences("qbase_prefs", android.content.Context.MODE_PRIVATE)
                }
                
                // Track if a persisted login instance exists
                var isLoggedInInstancePersisted by remember {
                    mutableStateOf(sharedPrefs.getBoolean("is_logged_in", false))
                }

                // If user becomes non-null, mark login instance as persisted reactively
                LaunchedEffect(user) {
                    if (user != null && !isLoggedInInstancePersisted) {
                        sharedPrefs.edit().putBoolean("is_logged_in", true).apply()
                        isLoggedInInstancePersisted = true
                    }
                }

                // Decide the starting route based on login instance persistence (guests are not allowed)
                val startRoute = remember(isLoggedInInstancePersisted) {
                    if (isLoggedInInstancePersisted) Screen.Home else Screen.Login
                }

                val appAccessState = remember(isSessionChecked, user, isOnline) {
                    when {
                        !isSessionChecked -> AppAccessState.RestoringSession
                        user != null && isOnline -> AppAccessState.OnlineReady
                        user != null && !isOnline -> AppAccessState.SignedInOffline
                        user == null && isOnline -> AppAccessState.GuestOnline
                        else -> AppAccessState.OfflineGuest
                    }
                }
                
                val topLevelRoutes = remember {
                    setOf(Screen.Login, Screen.Home, Screen.Explore, Screen.Connect, Screen.Sessions())
                }
                
                val navigationState = rememberNavigationState(
                    startRoute = startRoute,
                    topLevelRoutes = topLevelRoutes
                )
                val navigator = remember(navigationState) { Navigator(navigationState) }
                val snackbarHostState = remember { SnackbarHostState() }



                android.util.Log.d("MainActivity", "Composition: startRoute=$startRoute, currentTopLevel=${navigationState.topLevelRoute}")
                android.util.Log.d("MainActivity", "Compose state: seeded=$seeded, isSessionChecked=$isSessionChecked, appAccessState=$appAccessState, user=$user, isOnline=$isOnline")

                // Display a generic long-duration snackbar if background session check fails/user is guest
                LaunchedEffect(isSessionChecked, user) {
                    if (isSessionChecked && user == null) {
                        snackbarHostState.showSnackbar(
                            message = "Running in offline guest mode. Tap profile to log in.",
                            duration = SnackbarDuration.Long
                        )
                    }
                }

                // Redirect to Login if session expired (SharedPrefs says logged in but auth says otherwise)
                LaunchedEffect(isSessionChecked, user, isLoggedInInstancePersisted) {
                    if (isSessionChecked && user == null && isLoggedInInstancePersisted) {
                        android.util.Log.w("MainActivity", "Session expired: clearing persisted login flag, redirecting to Login")
                        sharedPrefs.edit().putBoolean("is_logged_in", false).apply()
                        isLoggedInInstancePersisted = false
                        navigator.resetTo(Screen.Login)
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

                CompositionLocalProvider(LocalAppAccessState provides appAccessState) {
                    if (!seeded || appAccessState == AppAccessState.RestoringSession) {
                        LoadingScreen()
                    } else {
                        MainScreen(navigationState, navigator, snackbarHostState)
                    }
                }
            }
        }
    }
}

/* ARCHIVED: Old Circular Loading Screen
@Composable
fun LoadingScreenCircularArchive() {
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
                text = "Initializing Q-Base...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
*/

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Q-Base Logo",
                modifier = Modifier
                    .size(180.dp)
                    .padding(8.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Q-Base",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Initializing...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MainScreen(
    navigationState: NavigationState, 
    navigator: Navigator, 
    snackbarHostState: SnackbarHostState
) {
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
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
    val entries = navigationState.toEntries(entryProvider)
    
    android.util.Log.d("AppNavDisplay", "Recompose: currentRoute=${navigationState.topLevelRoute}, entries.size=${entries.size}, tabHistory.size=${navigationState.tabHistory.size}")
    
    // Tab-level back: only intercept when NavDisplay has nothing to pop (entries <= 1) but we have tab history
    val canGoBackTab = entries.size <= 1 && navigationState.tabHistory.size > 1
    androidx.activity.compose.BackHandler(enabled = canGoBackTab) {
        android.util.Log.d("AppNavDisplay", "Tab BackHandler FIRED! tabHistory=${navigationState.tabHistory}")
        navigator.goBack()
    }
    
    // NavDisplay handles sub-screen back internally via its own BackHandler
    NavDisplay(
        entries = entries,
        onBack = { 
            android.util.Log.d("AppNavDisplay", "NavDisplay.onBack FIRED!")
            navigator.goBack() 
        }
    )
}