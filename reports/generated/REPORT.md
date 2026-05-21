Project-wide code report (generated)

Overview
- Languages scanned: Kotlin (.kt), Python (.py)
- Scope: `app/`, `core-auth/`, `core-chat/`, `core-crypto/`, `archive/`, `Q_base appwrite/`, `docs/`

Structure by module

**app**
- app/src/main/java/com/algorithmx/q_base/
  - QbaseApplication.kt: class QbaseApplication
  - MainActivity.kt: class MainActivity
  - core_ai/brain/
    - AiBrainManager.kt: class AiBrainManager; private suspend fun buildBrain; suspend fun askBrain; suspend fun streamFromBrain
    - CommonAiService.kt: class CommonAiService; suspend fun generateNoteStructure; suspend fun generateBlocksForTopic; private fun constructGenerationPrompt; private fun extractJsonFromResponse
    - AiUsageLogger.kt: interface AiUsageLogger; suspend fun logUsage
    - models/: multiple data classes (BrainConfig, TaskConfig, StoredBrainConfig, AiQuestion, AiOption, AiAnswer, AiCollectionResponse, AiGeneratedBlock, AiGeneratedTab, NoteContext), enum class BrainTask
    - BrainDataStoreManager.kt: class BrainDataStoreManager; companion object PreferencesKeys; suspend functions: markSeedAsApplied, resetSeedFlag, saveEngineConfiguration, incrementUsageStats, saveThemeMode, saveNotificationsEnabled, saveTaskConfig, setMasterAiFreeze
    - di/AiCoreModule.kt: object AiCoreModule; fun provideAiBrainManager; fun provideCommonAiService
  - data/
    - collections/: many data classes and interfaces (Question, QuestionSet, StudyCollection, CollectionDao, QuestionDao, Chat-related entities)
    - ai/: AiRepository, AiResponseEntity, AiResponseDao, BrainUsageEntity, AiModuleIntegrator (BrainConfigProviderImpl, AiUsageLoggerImpl)
    - sync/: SyncRepository, CollectionSyncRepository, MessageSyncRepository, SessionSyncRepository, ReportSyncRepository, UniversalQueueManager, SyncRequest, OfflineActionEntity, PatchEvent, Repositories
    - core/: ConfigRepository, HomeRepository, DatabaseSeeder, UserEntity, DAOs
    - util/: NetworkMonitor, NotificationHelper, ExportUtils, TypeConverters, MockDownloader, MockExporter
  - ui/
    - navigation/Screen.kt: sealed class Screen with many data objects/data classes for navigation targets
    - ai/: AiViewModel.kt: sealed class AiUiState (Idle, Loading, Success, Error); class AiViewModel with generateCollection, promoteResponse, resetState
    - auth/: AuthViewModel.kt: data class AuthState; class AuthViewModel with signIn, signUp, signInWithGoogle, onGoogleSignInSuccess, clearError
    - content_import/: ImportViewModel.kt (class ImportViewModel with many functions: togglePaperType, setInitialSource, loadCollections, navigateTo, selectCategory, updateNewCollectionName/Description, selectMethod, onImagePicked, onPdfPicked, onRawTextUpdated, startDirectImport, startAiGeneration, promoteResponse, addPdf, addOcr, addClipboard, removeDoc, startPaperExtraction, handleRetry, reset, currentStepNumber, totalSteps, and nested sealed classes/data classes for ImportStep and ExtractionConfigData), QuestionEditorViewModel (data class QuestionEditorState; class QuestionEditorViewModel functions: init, updateStem, updateOption, updateCorrectAnswer, updateExplanation, updateReferences, getAiAssist, generateAiExplanation, applyAiExplanation, discardAiExplanation, clearAiSuggestions, saveQuestion)
    - components/: many composable `fun` functions and previews (AuthComponents.kt, CommonComponents.kt, QuestionViewer, OptionsList, etc.)

**core-auth**
- core-auth/src/main/java/com/algorithmx/q_base/data/backend/
  - CoreAuth.kt: interface CoreAuth with suspend functions signInWithEmail, signUpWithEmail, signInWithGoogle, signOut, checkCurrentSession; data class CoreUser
  - CoreDatabase.kt: interface CoreDatabase with suspend CRUD functions, enum class CoreQueryOperator, data class CoreQuery
  - AppwriteAuthImpl.kt, FirebaseAuthImpl.kt: classes implementing CoreAuth (suspend functions implemented), AppwriteDatabaseImpl.kt, FirebaseDatabaseImpl.kt: classes implementing CoreDatabase (suspend functions create/get/update/delete/list/query; observeDocument/observeCollection flows)
  - BackendModule.kt: annotation classes FirebaseBackend, AppwriteBackend; object BackendModule with provider functions
- core-auth/src/main/java/com/algorithmx/q_base/data/auth/
  - AuthRepository.kt: class AuthRepository; data class AppwriteUser
  - ProfileRepository.kt: class ProfileRepository; ProfileCache interface; ProfileCache/Data classes for user profile

**core-chat**
- core-chat/src/main/java/com/algorithmx/q_base/data/chat/
  - MessageDao.kt, ChatDao.kt, Collection-related DAOs and entities
  - ChatDatabase.kt: abstract class ChatDatabase : RoomDatabase
  - ChatRemoteRepository.kt: class ChatRemoteRepository
  - ChatTypeConverters: object

**core-crypto**
- core-crypto/src/main/java/com/algorithmx/q_base/core_crypto/
  - CryptoManager.kt: class CryptoManager

**archive**
- archive/ui/
  - BaseAiViewModel.kt: abstract class BaseAiViewModel; sealed class AiState with Idle, Loading, Success, Error; protected fun executeAiTask, fun resetAiState, fun cancelCurrentAiTask
  - SessionsViewModelFactory.kt, ActiveSessionViewModelFactory.kt
  - ExtractionConfigScreen.kt: composable function ExtractionConfigScreen

**Q_base appwrite (scripts & functions)**
- Q_base appwrite/app_write.py: multiple functions (wait_for_attributes, setup_database, etc.) and scripts
- Q_base appwrite/encrypt_and_sync_keys.py: get_global_decryption_key, encrypt_global_key, main
- Q_base appwrite/create_global_keys_doc.py: init_global_keys_doc
- Q_base appwrite/verify_appwrite_readiness.py: check_readiness
- Q_base appwrite/functions/validate_admin_writes/main.py: read_stdin, make_client, main

**docs**
- docs/generate_pdf.py: class QbasePDF(FPDF) with header, footer; functions sanitize_text, build_pdf

Notes and limitations
- Scans used regex heuristics on `.kt` and `.py` files to capture `class`, `data class`, `interface`, `object`, `enum class`, `sealed class` and `fun` / `suspend fun` / `def` declarations.
- The search was capped by the tool response (200 matches per query). The generated report includes all matches returned by the scans; some very large files or uncommon patterns might not be fully included due to result caps.
- No `.java` source files were found by the search.

Next steps (optional)
- I can re-run targeted per-directory scans and produce a fully expanded file-by-file listing (CSV or JSON), or walk every source file and extract AST-parsed declarations for a guaranteed exhaustive list. Which would you prefer?

Report file: [REPORT.md](REPORT.md)
