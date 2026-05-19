---
name: android-errors
description: "Common Android development errors, their causes, solutions, and prevention strategies. Use when: debugging Android crashes, fixing runtime errors, understanding lifecycle issues, resolving compiler warnings, optimizing performance problems."
applyTo: ["**/*.kt", "**/*.java"]
---

# Android Development Errors & Solutions

This knowledge base documents common errors encountered in Android development with their solutions and prevention strategies.

## Memory Management

### RecyclerView Memory Leaks
**Error**: OutOfMemoryError, growing memory usage
**Cause**: Listeners/observers not removed, view holders holding references
**Solution**:
```kotlin
// ❌ Bad
class MyAdapter : RecyclerAdapter() {
    val clickListener = { item -> /* handle */ }
}

// ✅ Good
override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
    super.onDetachedFromRecyclerView(recyclerView)
    // Clear listeners and unbind
}
```

### Context Memory Leaks
**Error**: Leaked ServiceConnection/BroadcastReceiver, Context memory not released
**Cause**: Static references to context, not unregistering listeners in onDestroy
**Solution**:
```kotlin
// ❌ Bad
companion object {
    var context: Context? = null
}

// ✅ Good - Use applicationContext when needed, clear references
override fun onDestroy() {
    super.onDestroy()
    unregisterReceiver(broadcastReceiver)
    localBroadcastManager.unregisterReceiver(broadcastReceiver)
}
```

### ViewModel Scope Issues
**Error**: IllegalStateException in coroutine, task post from destroyed view
**Cause**: Not using viewModelScope for UI-related coroutines
**Solution**:
```kotlin
// ❌ Bad
GlobalScope.launch {
    fetchData()
}

// ✅ Good
viewModel.fetchData() // inside ViewModel:
fun fetchData() = viewModelScope.launch {
    val data = repository.getData()
    _uiState.value = UiState.Success(data)
}
```

## Fragment & Activity Lifecycle

### Fragment Transaction IllegalStateException
**Error**: Can not perform this action after onSaveInstanceState
**Cause**: Committing fragment transactions after lifecycle.onSaveInstanceState()
**Solution**:
```kotlin
// ❌ Bad
supportFragmentManager.beginTransaction()
    .replace(R.id.container, fragment)
    .commit() // Can fail if called in onStart()

// ✅ Good
supportFragmentManager.beginTransaction()
    .replace(R.id.container, fragment)
    .commitNow() // Or commit() in onCreate/onViewCreated
```

### View Binding Null Pointer
**Error**: NullPointerException on binding.root
**Cause**: Accessing view binding after fragment is destroyed
**Solution**:
```kotlin
// ❌ Bad
override fun onDestroyView() {
    super.onDestroyView()
    // binding still used here
    binding.textView.text = "..."
}

// ✅ Good
private var _binding: FragmentBinding? = null
private val binding get() = _binding!!

override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    _binding = FragmentBinding.bind(view)
}

override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
}
```

### Configuration Change Data Loss
**Error**: Data disappears when device rotates
**Cause**: Not preserving state across configuration changes
**Solution**:
```kotlin
// ✅ Use ViewModel with SavedStateHandle
class MyViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    val name: StateFlow<String> = savedStateHandle.getStateFlow("name", "")
    
    fun updateName(newName: String) {
        savedStateHandle["name"] = newName
    }
}
```

## Threading & Concurrency

### Network on Main Thread (NetworkOnMainThreadException)
**Error**: NetworkOnMainThreadException, ANR (Application Not Responding)
**Cause**: Network calls on main thread instead of background
**Solution**:
```kotlin
// ❌ Bad
val response = retrofitService.getUsers().execute()

// ✅ Good
viewModel.getUsers() // in ViewModel:
fun getUsers() = viewModelScope.launch {
    try {
        val users = apiService.getUsers() // Retrofit suspended function
        _uiState.value = UiState.Success(users)
    } catch (e: Exception) {
        _uiState.value = UiState.Error(e.message)
    }
}
```

### Coroutine Scope Leaks
**Error**: Task posted from destroyed activity, coroutine still running after destroy
**Cause**: Using wrong scope or not canceling coroutines
**Solution**:
```kotlin
// ❌ Bad
GlobalScope.launch { /* never cancelled */ }
lifecycleScope.launch(Dispatchers.Main) { fetchData() } // Can leak

// ✅ Good
// In Activity
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            updateUI(state)
        }
    }
}

// In Fragment
viewLifecycleOwner.lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.data.collect { data ->
            updateUI(data)
        }
    }
}
```

### Callback After Destroy
**Error**: Activity reference in callback after activity destroyed
**Cause**: Async operation holding activity reference
**Solution**:
```kotlin
// ✅ Use ViewModel for state management
class DataViewModel : ViewModel() {
    private val _data = MutableStateFlow<Data?>(null)
    val data: StateFlow<Data?> = _data.asStateFlow()
    
    fun loadData() = viewModelScope.launch {
        _data.value = repository.fetchData()
    }
}
```

## Permissions

### PermissionDenied / SecurityException
**Error**: SecurityException, permission check failed
**Cause**: Not checking permissions at runtime before accessing protected resources
**Solution**:
```kotlin
// ✅ Modern approach with requestPermission activity contract
class MyFragment : Fragment() {
    private val cameraPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        }
    }
    
    private fun requestCameraAccess() {
        if (ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            cameraPermissionRequest.launch(Manifest.permission.CAMERA)
        }
    }
}
```

## Database

### Room Transaction Deadlock
**Error**: Database is locked, database access on wrong thread
**Cause**: Running database operations on main thread or improper transaction handling
**Solution**:
```kotlin
// ❌ Bad
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getUsers(): List<User> // Called on main thread
}

// ✅ Good
@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    suspend fun getUsers(): List<User>
    
    @Query("SELECT * FROM users")
    fun getUsersFlow(): Flow<List<User>>
}

// Usage
viewModel.getUsers() // in ViewModel:
fun getUsers() = viewModelScope.launch {
    val users = userDao.getUsers()
    _uiState.value = UiState.Success(users)
}
```

### Query on Main Thread Exception
**Error**: IllegalStateException: Cannot access database on the main thread
**Cause**: Synchronous database query on main thread
**Solution**:
```kotlin
// ✅ Use suspend functions or Flow
val usersFlow: Flow<List<User>> = userDao.getUsersFlow()

viewModel.users = usersFlow
    .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
```

## Jetpack Compose

### Recomposition Causing Performance Issues
**Error**: Excessive recompositions, choppy UI
**Cause**: Capturing mutable objects, not using remember, unnecessary state updates
**Solution**:
```kotlin
// ❌ Bad
@Composable
fun MyScreen(viewModel: MyViewModel) {
    var counter by remember { mutableStateOf(0) }
    
    Column {
        Text("Count: $counter") // Recomposes entire screen
        Button(onClick = { counter++ }) { Text("Increment") }
    }
}

// ✅ Good
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val counter by viewModel.counter.collectAsStateWithLifecycle()
    
    Column {
        Text("Count: $counter")
        Button(onClick = { viewModel.incrementCounter() }) { Text("Increment") }
    }
}
```

### Material 3 Theming Issues
**Error**: Colors not applying, theme not consistent
**Cause**: Wrong Material 3 API usage, theme not set up properly
**Solution**:
```kotlin
// ✅ Proper Material 3 setup
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> darkColorScheme(
            primary = md_theme_dark_primary,
            secondary = md_theme_dark_secondary,
            // ... other colors
        )
        else -> lightColorScheme(
            primary = md_theme_light_primary,
            secondary = md_theme_light_secondary,
            // ... other colors
        )
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

## Resources & Lint

### Resource Not Found / InflateException
**Error**: android.view.InflateException, Resource not found error
**Cause**: Referencing non-existent resource ID, wrong resource type
**Solution**:
```xml
<!-- ✅ Verify resource exists in res/values/colors.xml -->
<color name="my_color">#FF6200EE</color>

<!-- ✅ Use correct type -->
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:textColor="@color/my_color" />
```

### Lint Warnings (Unused, Missing Translations)
**Error**: Lint warnings accumulate, cause build failures
**Cause**: Ignoring lint issues during development
**Solution**:
```gradle
android {
    lint {
        missingDimensionStrategy 'store', 'play'
        disable 'MissingTranslation'
        warning 'ExtraTranslation'
    }
}
```

## Navigation

### Fragment Not Found / ClassNotFoundException
**Error**: Fragment instantiation fails, navigation doesn't work
**Cause**: Fragment class not found or not exported in manifest
**Solution**:
```kotlin
// ✅ Use Navigation Component with safe args
// In nav_graph.xml
<navigation>
    <fragment
        android:id="@+id/detailFragment"
        android:name="com.example.DetailFragment" />
</navigation>

// ✅ Use type-safe args
val action = ListFragmentDirections.actionListToDetail(id = 123)
findNavController().navigate(action)
```

## Manifest & Build Issues

### Exported Components Not Declared
**Error**: RuntimeException: Unable to start activity ComponentInfo
**Cause**: Not declaring exported components in manifest (API 31+)
**Solution**:
```xml
<!-- ✅ Add android:exported for API 31+ -->
<activity
    android:name=".MainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

### BuildConfig.DEBUG Not Removed
**Error**: Debug code shipped in release build
**Cause**: Using BuildConfig.DEBUG without proper configuration
**Solution**:
```kotlin
// ✅ Use BuildConfig.DEBUG in debug-only code
if (BuildConfig.DEBUG) {
    Log.d("TAG", "Debug log")
}

// Or better - exclude from release
buildTypes {
    release {
        // Strip debug logs, enable proguard
    }
}
```

## Performance

### ANR (Application Not Responding)
**Error**: ANR dialog, application freeze for >5 seconds
**Cause**: Heavy work on main thread, deadlocks, infinite loops
**Prevention**:
```kotlin
// ✅ Move work off main thread
fun expensiveOperation() = viewModelScope.launch(Dispatchers.Default) {
    val result = computeExpensiveValue()
    withContext(Dispatchers.Main) {
        _uiState.value = UiState.Success(result)
    }
}
```

### Garbage Collection Stutter
**Error**: Frequent frame drops, UI stutter every few seconds
**Cause**: Large object allocations in hot paths, unnecessary garbage
**Prevention**:
```kotlin
// ❌ Bad - allocates new list every recomposition
@Composable
fun MyList(items: List<Item>) {
    LazyColumn {
        items(items) { item ->
            ItemRow(item)
        }
    }
}

// ✅ Good - stable list reference
@Composable
fun MyList(items: List<Item>) {
    val stableItems = remember(items) { items }
    LazyColumn {
        items(stableItems) { item ->
            ItemRow(item)
        }
    }
}
```

## Debugging Tips

1. **Check Logcat First**: Filter by app package name, look for stack traces
2. **Use Timber**: Better logging than Log.d
3. **Android Studio Profiler**: Monitor memory, CPU, network, battery
4. **Layout Inspector**: Debug view hierarchy in real-time
5. **Database Inspector**: Browse Room databases directly
6. **Logcat Patterns to Search**:
   - `Exception` - All exceptions
   - `FATAL` - Critical errors
   - `ANR` - Freezes
   - `OutOfMemory` - Memory pressure
   - `Permission denied` - Permission issues

## Material 3 Common Issues

### Theme Colors Not Applying
- Check `@0 index (Theme)` is `Material3`
- Verify `Material3/values/colors.xml` exists
- Use `MaterialTheme` from `material3` package

### Typography Inconsistent
- Import `androidx.compose.material3.MaterialTheme`
- Use `MaterialTheme.typography.bodyMedium` not custom TextStyle

### Components Not Updated
- Some Material 2 components deprecated
- Check deprecation notices in IDE
- Migrate to Material 3 equivalents

## Project-Specific (Q-base)

### Appwrite Integration
- Check Appwrite session lifecycle
- Handle Appwrite permission errors separately
- Verify collection permissions in Appwrite console

### Compose Adoption
- Target Material 3 components
- Use Compose testing framework
- Avoid mixing Compose + View code where possible
