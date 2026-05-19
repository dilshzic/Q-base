---
name: android-best-practices
description: "General Android development best practices, code style, architecture patterns, and modern Kotlin conventions. Apply to all Kotlin files in Android projects."
applyTo: ["**/*.kt"]
---

# Android Development Best Practices

## Code Style & Conventions

### Kotlin Conventions
1. **Naming**: `camelCase` for variables/functions, `PascalCase` for classes
2. **Prefer val over var**: Use `val` by default, only `var` when necessary
3. **Extension Functions**: Prefer over utility classes
4. **Scope Functions**: Use `apply`, `let`, `run`, `with`, `also` appropriately
5. **Data Classes**: Use for POJOs with `@Parcelize` for parcelable types

### Code Organization
```kotlin
// ✅ Order in class
class MyViewModel : ViewModel() {
    // Properties (private first, then public)
    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    // init block
    init { }
    
    // Public methods
    fun loadData() { }
    
    // Private methods
    private fun processData() { }
    
    // Companion object (at end)
    companion object { }
}
```

## Architecture Patterns

### MVVM with Repository Pattern
```
UI (Composable/Fragment)
  ↓ (observes StateFlow)
ViewModel (manages state, holds viewModelScope)
  ↓ (uses)
Repository (coordinates data sources)
  ↓ (uses)
DataSource (Database, API, Cache)
```

### Dependency Injection
- Use Hilt for dependency injection
- Scope: `@Singleton` for app-wide, `@ViewModelScoped` for ViewModel dependencies
- Avoid service locators and manual DI

### State Management
```kotlin
// ✅ Sealed class for UI state
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// ✅ ViewModel exposes state
class MyViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<Item>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Item>>> = _uiState.asStateFlow()
}
```

## Coroutines & Async

### Structured Concurrency
- Always use viewModelScope or lifecycleScope
- Never use GlobalScope
- Cancel jobs explicitly if not using scoped launchers

```kotlin
// ✅ Good - automatically cancels when ViewModel cleared
viewModelScope.launch {
    val data = repository.fetchData()
    _uiState.value = UiState.Success(data)
}

// ✅ Good - cancels when lifecycle moves to destroyed
viewLifecycleOwner.lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.uiState.collect { state ->
            updateUI(state)
        }
    }
}
```

### Error Handling
```kotlin
// ✅ Proper try-catch in coroutine
viewModelScope.launch {
    try {
        val result = apiService.fetchUsers()
        _uiState.value = UiState.Success(result)
    } catch (e: IOException) {
        _uiState.value = UiState.Error("Network error")
    } catch (e: Exception) {
        _uiState.value = UiState.Error("Unknown error")
    }
}
```

## Jetpack Compose

### Composable Best Practices
```kotlin
// ✅ Prefer parameters over state access
@Composable
fun UserCard(user: User, onClick: (User) -> Unit = {}) {
    Card(modifier = Modifier.clickable { onClick(user) }) {
        // Content
    }
}

// ✅ State hoisting for testability
@Composable
fun MyScreen(viewModel: MyViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MyScreenContent(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
fun MyScreenContent(uiState: UiState, onAction: (Action) -> Unit) {
    // Pure composable, testable without ViewModel
}
```

### Performance Optimization
1. **Remember**: Cache computations, lambdas
2. **Key**: Explicit keys for list items
3. **Stable**: Mark data classes @Stable if applicable
4. **Recomposition Scope**: Keep recompositions in smallest composable

## Material 3 Guidelines

### Colors & Theming
```kotlin
// ✅ Use Material 3 color scheme
MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    shapes = Shapes,
) {
    // App content
}

// ✅ Reference colors from theme
Text(
    "Hello",
    color = MaterialTheme.colorScheme.onSurface,
    style = MaterialTheme.typography.bodyMedium
)
```

### Components
- Use Material 3 components: `Button`, `TextField`, `Card`, `NavigationBar`
- Follow Material 3 spacing system (4dp grid)
- Implement ripple effects and state layers properly
- Use `ElevatedButton`, `FilledButton`, `OutlinedButton` as needed

## Testing

### Unit Testing
```kotlin
@Test
fun testUserRepository_fetchUsers_success() {
    // Arrange
    val expectedUsers = listOf(User(1, "John"))
    coEvery { apiService.getUsers() } returns expectedUsers
    
    // Act
    val result = repository.fetchUsers()
    
    // Assert
    assertEquals(expectedUsers, result)
}
```

### Compose Testing
```kotlin
@Test
fun userCard_displaysUserInfo() {
    composeRule.setContent {
        UserCard(user = User(1, "John"))
    }
    
    composeRule.onNodeWithText("John").assertIsDisplayed()
}
```

## Navigation

### Navigation Component
- Define navigation graph in XML
- Use type-safe arguments with Safe Args
- Handle deep links properly
- Manage back stack explicitly

```kotlin
// ✅ Type-safe navigation
val action = ListFragmentDirections.actionListToDetail(userId = 123)
findNavController().navigate(action)
```

## Resources & Configuration

### String Resources
- Use string resources instead of hardcoding
- Use plurals for variable counts
- Support multiple languages

### Dimensions
- Use dimension resources (dimen.xml)
- Follow Material 3 spacing scale
- Use `sp` for text sizes, `dp` for layouts

### Colors
- Define in colors.xml or M3 theme
- Use semantic names: `surface`, `onSurface`, `error`, `errorContainer`

## Security

### Sensitive Data
- Use EncryptedSharedPreferences for sensitive preferences
- Never log sensitive data
- Use ProGuard/R8 for obfuscation in release builds

### Permissions
- Request minimum necessary permissions
- Use permission contracts (registerForActivityResult)
- Check permissions before accessing protected resources

```kotlin
// ✅ Modern permission request
private val requestPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) handlePermissionGranted()
}

private fun checkAndRequest() {
    if (ContextCompat.checkSelfPermission(
        context, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED) {
        handlePermissionGranted()
    } else {
        requestPermission.launch(Manifest.permission.CAMERA)
    }
}
```

## Lifecycle Management

### Activity/Fragment Lifecycle
- Initialize UI in onCreate/onViewCreated
- Start collecting state flows in onStart/onViewCreated with repeatOnLifecycle(STARTED)
- Clean up in onDestroy/onDestroyView
- Unbind views in onDestroyView (set to null)

### ViewModel Lifecycle
- Never clear non-UI data in onCleared
- Persist UI state using SavedStateHandle
- Hold no references to Context/View

## Performance Tips

1. **Use Baseline Profiles**: Define startup critical paths
2. **Lazy Layout**: Use LazyColumn/LazyRow for large lists
3. **Image Loading**: Use Coil/Glide with appropriate sizing
4. **Proguard**: Enable in release builds
5. **R8**: Use newer R8 compiler instead of ProGuard
6. **Jetifier**: Ensure all AndroidX dependencies updated

## Logging & Debugging

### Use Timber Over Log
```kotlin
// ✅ Better logging with Timber
Timber.d("User loaded: %s", user.name)
Timber.e(exception, "Failed to load users")
```

### Debug Utilities
- Use `BuildConfig.DEBUG` for debug-only code
- Implement debug drawer for development tools
- Log important state transitions
