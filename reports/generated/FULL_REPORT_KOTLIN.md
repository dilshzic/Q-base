# KOTLIN & ANDROID DECLARATIONS


## archive/ui/ActiveSessionViewModelFactory.kt

```

L11: class ActiveSessionViewModelFactory(

```


## archive/ui/SessionsViewModelFactory.kt

```

L10: class SessionsViewModelFactory(

```


## archive/ui/ExtractionConfigScreen.kt

```

L20: fun ExtractionConfigScreen(

```


## archive/ui/BaseAiViewModel.kt

```

L13: sealed class AiState {

L14: object Idle : AiState()

L15: object Loading : AiState()

L16: data class Success(val result: String) : AiState()

L17: data class Error(val message: String) : AiState()

L37: protected fun executeAiTask(

L62: fun resetAiState() {

L66: fun cancelCurrentAiTask() {

```


## core-auth/src/main/java/com/algorithmx/q_base/data/di/AuthModule.kt

```

L13: object AuthModule {

L17: fun provideAppwriteAccount(client: Client): Account = Account(client)

```


## core-auth/src/main/java/com/algorithmx/q_base/data/auth/UserProfile.kt

```

L7: data class UserProfile(

```


## core-auth/src/main/java/com/algorithmx/q_base/data/auth/ProfileRepository.kt

```

L14: class ProfileRepository @Inject constructor(

L21: private fun mapToUserProfile(map: Map<String, Any>): UserProfile {

L35: private fun userProfileToMap(profile: UserProfile): Map<String, Any> {

L48: suspend fun createOrUpdateProfile(

L94: suspend fun updateProfile(profile: UserProfile): Result<Unit> {

L107: private suspend fun saveProfile(profile: UserProfile, email: String) {

L129: suspend fun findUserByFriendCode(friendCode: String): Result<UserProfile?> {

L148: suspend fun syncUserProfile(userId: String) {

L219: private suspend fun generateUniqueFriendCode(): String {

L238: private suspend fun isFriendCodeUnique(friendCode: String): Boolean {

L251: private fun generateFriendCode(): String {

L259: suspend fun setupSecureBackup(userId: String, passphrase: String): Result<Unit> {

L277: suspend fun checkHasSecureBackup(userId: String): Boolean {

L287: suspend fun restoreSecureBackup(userId: String, passphrase: String): Result<Unit> {

```


## core-auth/src/main/java/com/algorithmx/q_base/data/auth/ProfileCache.kt

```

L3: interface ProfileCache {

L4: suspend fun upsert(profile: UserProfile)

```


## core-auth/src/main/java/com/algorithmx/q_base/data/auth/AuthRepository.kt

```

L23: data class AppwriteUser(

L31: class AuthRepository @Inject constructor(

L52: private fun saveUserToPrefs(user: AppwriteUser) {

L62: private fun getUserFromPrefs(): AppwriteUser? {

L71: private fun clearUserFromPrefs() {

L81: fun checkCurrentSession() {

L103: private fun mapAppwriteUser(user: User<*>, photoUrl: String? = null): AppwriteUser {

L116: private suspend fun fetchGooglePhotoUrl(): String? {

L153: suspend fun signInWithEmail(email: String, pass: String): Result<AppwriteUser> {

L168: suspend fun signUpWithEmail(email: String, pass: String, username: String, photoUrl: String? = null): Result<AppwriteUser> {

L184: suspend fun signInWithGoogle(activity: ComponentActivity): Result<AppwriteUser> {

L201: fun signOut() {

```


## core-auth/src/main/java/com/algorithmx/q_base/data/backend/FirebaseAuthImpl.kt

```

L21: class FirebaseAuthImpl @Inject constructor(

```


## core-auth/src/main/java/com/algorithmx/q_base/data/backend/CoreDatabase.kt

```

L5: enum class CoreQueryOperator {

L13: data class CoreQuery(

L19: interface CoreDatabase {

L20: suspend fun createDocument(collectionId: String, documentId: String, data: Map<String, Any>): Result<Unit>

L21: suspend fun getDocument(collectionId: String, documentId: String): Result<Map<String, Any>?>

L22: suspend fun updateDocument(collectionId: String, documentId: String, data: Map<String, Any>): Result<Unit>

L23: suspend fun deleteDocument(collectionId: String, documentId: String): Result<Unit>

L24: suspend fun listDocuments(collectionId: String): Result<List<Map<String, Any>>>

L25: suspend fun queryDocuments(collectionId: String, queries: List<CoreQuery>): Result<List<Map<String, Any>>>

L26: fun observeDocument(collectionId: String, documentId: String): Flow<Map<String, Any>?>

L27: fun observeCollection(collectionId: String): Flow<List<Map<String, Any>>>

```


## core-auth/src/main/java/com/algorithmx/q_base/data/backend/AppwriteDatabaseImpl.kt

```

L19: class AppwriteDatabaseImpl @Inject constructor(

L30: private suspend fun invokeSuspendReflective(

L56: private fun mapRow(row: Any): Map<String, Any> {

L132: private fun mapRowList(rowList: Any): List<Map<String, Any>> {

L143: private fun mapDocument(doc: io.appwrite.models.Document<*>): Map<String, Any> {

L393: fun wrapValue(v: Any): List<Any> = if (v is List<*>) v.filterNotNull() else listOf(v)

```


## core-auth/src/main/java/com/algorithmx/q_base/data/backend/FirebaseDatabaseImpl.kt

```

L13: class FirebaseDatabaseImpl @Inject constructor(

```


## core-auth/src/main/java/com/algorithmx/q_base/data/backend/BackendModule.kt

```

L16: annotation class FirebaseBackend

L20: annotation class AppwriteBackend

L24: object BackendModule {

L28: fun provideFirebaseAuth(): FirebaseAuth {

L35: fun provideFirebaseAuthImpl(

L45: fun provideFirebaseDatabaseImpl(

L54: fun provideAppwriteAuthImpl(

L64: fun provideAppwriteDatabaseImpl(

L74: fun provideActiveAuth(

L82: fun provideActiveDatabase(

```


## core-auth/src/main/java/com/algorithmx/q_base/data/backend/CoreAuth.kt

```

L6: data class CoreUser(

L13: interface CoreAuth {

L17: suspend fun signInWithEmail(email: String, pass: String): Result<CoreUser>

L18: suspend fun signUpWithEmail(email: String, pass: String, username: String): Result<CoreUser>

L19: suspend fun signInWithGoogle(activity: ComponentActivity): Result<CoreUser>

L20: suspend fun signOut(): Result<Unit>

L21: suspend fun checkCurrentSession(): Result<CoreUser?>

```


## core-auth/src/main/java/com/algorithmx/q_base/data/backend/AppwriteAuthImpl.kt

```

L18: class AppwriteAuthImpl @Inject constructor(

L37: private fun mapUser(user: User<*>): CoreUser {

```


## core-auth/src/main/java/com/algorithmx/q_base/ui/components/AuthComponents.kt

```

L45: fun AuthFormSection(

L193: fun GoogleSignInButton(

L284: fun GoogleIconGraphic() {

```


## core-chat/src/main/java/com/algorithmx/q_base/data/chat/ChatEntity.kt

```

L9: data class ChatEntity(

L22: fun ChatEntity.isAdmin(userId: String): Boolean {

```


## core-chat/src/main/java/com/algorithmx/q_base/data/chat/MessageEntity.kt

```

L20: data class MessageEntity(

```


## core-chat/src/main/java/com/algorithmx/q_base/data/chat/ChatDao.kt

```

L6: data class ChatSummary(

L19: interface ChatDao {

L21: suspend fun insertChat(chat: ChatEntity)

L24: suspend fun getChatById(chatId: String): ChatEntity?

L27: fun getChatByIdFlow(chatId: String): Flow<ChatEntity?>

L30: suspend fun getP2PChat(ids1: String, ids2: String): ChatEntity?

L33: fun getAllChats(): Flow<List<ChatEntity>>

L46: fun getChatSummaries(): Flow<List<ChatSummary>>

L49: suspend fun updateBlockedStatus(chatId: String, isBlocked: Boolean)

L52: suspend fun updateReportedStatus(chatId: String, isReported: Boolean)

L55: suspend fun updateMutedStatus(chatId: String, isMuted: Boolean)

L58: suspend fun updateParticipants(chatId: String, participantIds: String)

L61: suspend fun deleteChat(chat: ChatEntity)

L64: suspend fun deleteChatById(chatId: String)

L67: suspend fun incrementUnreadCount(chatId: String)

L70: suspend fun clearUnreadCount(chatId: String)

L73: fun getTotalUnreadCount(): Flow<Int?>

L76: suspend fun deleteAllChats()

```


## core-chat/src/main/java/com/algorithmx/q_base/data/chat/ChatDatabase.kt

```

L25: fun getDatabase(context: Context): ChatDatabase {

```


## core-chat/src/main/java/com/algorithmx/q_base/data/chat/ChatTypeConverters.kt

```

L8: object ChatTypeConverters {

L13: fun fromAdminIds(adminIds: List<String>?): String {

L19: fun toAdminIds(value: String?): List<String> {

```


## core-chat/src/main/java/com/algorithmx/q_base/data/chat/ChatRemoteRepository.kt

```

L14: class ChatRemoteRepository @Inject constructor(

L18: private suspend fun getCurrentUserId(): String? {

L26: suspend fun createChatOnRemote(chat: ChatEntity) {

L82: suspend fun addParticipantToRemote(chatId: String, userId: String) {

L104: suspend fun removeParticipantFromRemote(chatId: String, userId: String) {

L126: suspend fun promoteParticipantToAdminOnRemote(chatId: String, userId: String) {

L148: suspend fun demoteAdminOnRemote(chatId: String, userId: String) {

L170: suspend fun clearChatMessagesOnRemote(chatId: String) {

L204: suspend fun deleteChatOnRemote(chatId: String) {

L234: suspend fun reportGroup(group: ChatEntity, reason: String) {

L254: suspend fun reportMessage(message: MessageEntity, reason: String) {

```


## core-chat/src/main/java/com/algorithmx/q_base/data/chat/MessageDao.kt

```

L7: interface MessageDao {

L9: suspend fun insertMessage(message: MessageEntity)

L12: fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

L15: suspend fun getMessageById(messageId: String): MessageEntity?

L18: fun getAllMessages(): Flow<List<MessageEntity>>

L21: suspend fun deleteMessage(message: MessageEntity)

L24: suspend fun deleteMessagesByChatId(chatId: String)

L27: suspend fun deleteAllMessages()

L30: suspend fun getPendingMessages(): List<MessageEntity>

L33: suspend fun updateMessageStatus(messageId: String, status: String)

```


## core-crypto/src/main/java/com/algorithmx/q_base/core_crypto/CryptoManager.kt

```

L21: class CryptoManager @Inject constructor(

L37: fun initializeAndGetPublicKey(): String {

L57: fun encryptMessage(plaintext: String, receiverPublicKeyBase64: String): String {

L69: fun encryptSessionKey(sessionKey: ByteArray, receiverPublicKeyBase64: String): String {

L81: fun decryptMessage(ciphertextBase64: String): Result<String> {

L91: fun decryptSessionKey(wrappedKeyBase64: String): Result<ByteArray> {

L100: private fun decryptRaw(ciphertextBase64: String): ByteArray {

L122: fun clearKeys() {

L130: fun getPublicKeyFingerprint(): String {

L142: fun encryptFileContent(plaintext: ByteArray): Pair<ByteArray, String> {

L158: fun encryptWithSessionKey(plaintext: String): Pair<String, String> {

L174: fun decryptWithSessionKey(ciphertextBase64: String, keyHandleBase64: String): Result<String> {

L190: fun decryptFileContent(ciphertext: ByteArray, keyBase64: String): ByteArray {

L206: fun exportEncryptedKeyset(passphrase: String): String {

L219: fun importEncryptedKeyset(encryptedBackupBase64: String, passphrase: String): Result<Unit> {

L242: private fun deriveKey(passphrase: String, salt: ByteArray): ByteArray {

L248: private fun encryptBackup(data: ByteArray, passphrase: String): String {

L271: private fun decryptBackup(encryptedBase64: String, passphrase: String): ByteArray {

L290: private fun getLocalAead(): Aead {

L305: fun encryptLocalString(plaintext: String): String {

L312: fun decryptLocalString(ciphertextBase64: String): Result<String> {

L325: private fun getGlobalDecryptionKey(): javax.crypto.spec.SecretKeySpec {

L340: fun encryptGlobalKey(plainText: String): String {

L355: fun decryptGlobalKey(combinedBase64: String): Result<String> {

```


## app/src/androidTest/java/com/algorithmx/q_base/ExampleInstrumentedTest.kt

```

L17: class ExampleInstrumentedTest {

L19: fun useAppContext() {

```


## app/src/test/java/com/algorithmx/q_base/ExampleUnitTest.kt

```

L12: class ExampleUnitTest {

L14: fun addition_isCorrect() {

```


## app/src/test/java/com/algorithmx/q_base/ui/settings/ProfileViewModelTest.kt

```

L25: class ProfileViewModelTest {

L40: fun setup() {

L66: fun tearDown() {

```


## app/src/main/java/com/algorithmx/q_base/QbaseApplication.kt

```

L9: class QbaseApplication : Application() {

```


## app/src/main/java/com/algorithmx/q_base/MainActivity.kt

```

L47: class MainActivity : ComponentActivity() {

L60: private fun checkNotificationPermission() {

L275: fun LoadingScreenCircularArchive() {

L299: fun LoadingScreen() {

L339: fun MainScreen(

L453: fun AppNavDisplay(navigationState: NavigationState, navigator: Navigator) {

```


## app/src/main/java/com/algorithmx/q_base/data/AppDatabase.kt

```

L50: fun getDatabase(context: Context): AppDatabase {

```


## app/src/main/java/com/algorithmx/q_base/data/DatabaseSeeder.kt

```

L22: class DatabaseSeeder @Inject constructor(

L32: private fun getColumnString(cursor: android.database.Cursor, index: Int): String? {

L36: suspend fun seedDatabaseIfNeeded() = withContext(Dispatchers.IO) {

L229: private suspend fun seedSampleChats() {

L336: private suspend fun seedAncientSriLanka() {

```


## app/src/main/java/com/algorithmx/q_base/data/di/NetworkModule.kt

```

L23: object NetworkModule {

L27: fun provideOkHttpClient(): OkHttpClient {

L38: fun provideFirestore(): FirebaseFirestore = Firebase.firestore

L42: fun provideAppwriteClient(@ApplicationContext context: Context): Client {

L99: fun shieldJsonObject(obj: org.json.JSONObject) {

L149: fun provideAppwriteStorage(client: Client): Storage {

L155: fun provideAppwriteDatabases(client: Client): Databases {

L161: fun provideAppwriteTables(client: Client): Any? {

L178: fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor {

```


## app/src/main/java/com/algorithmx/q_base/data/di/DatabaseModule.kt

```

L33: object DatabaseModule {

L37: fun provideDatabase(@ApplicationContext context: Context): AppDatabase {

L42: fun provideQuestionDao(database: AppDatabase): QuestionDao = database.questionDao()

L45: fun provideCollectionDao(database: AppDatabase): CollectionDao = database.collectionDao()

L48: fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()

L51: fun provideProblemReportDao(database: AppDatabase): ProblemReportDao = database.problemReportDao()

L54: fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

L58: fun provideChatDatabase(@ApplicationContext context: Context): ChatDatabase {

L63: fun provideChatDao(database: ChatDatabase): ChatDao = database.chatDao()

L66: fun provideMessageDao(database: ChatDatabase): MessageDao = database.messageDao()

L69: fun provideAiResponseDao(database: AppDatabase): AiResponseDao = database.aiResponseDao()

L72: fun provideBrainUsageDao(database: AppDatabase): BrainUsageDao = database.brainUsageDao()

L75: fun provideCollectionVersionLedgerDao(database: AppDatabase): CollectionVersionLedgerDao = database.collectionVersionLedgerDao()

L78: fun provideActionQueueDao(database: AppDatabase): com.algorithmx.q_base.data.sync.ActionQueueDao = database.actionQueueDao()

L82: fun provideExploreRepository(

L94: fun provideSessionRepository(

L105: fun provideHomeRepository(

L117: fun provideImportRepository(@ApplicationContext context: Context): ImportRepository {

L123: fun provideDataClearingRepository(

L141: fun provideProfileCache(userDao: UserDao): ProfileCache {

```


## app/src/main/java/com/algorithmx/q_base/data/di/BrainModule.kt

```

L14: object BrainModule {

L18: fun provideAiRepository(

```


## app/src/main/java/com/algorithmx/q_base/data/util/TypeConverters.kt

```

L7: class TypeConverters {

L9: fun fromBrainTask(task: BrainTask): String {

L14: fun toBrainTask(name: String): BrainTask {

L23: fun fromBrainProvider(provider: BrainProvider): String {

L28: fun toBrainProvider(name: String): BrainProvider {

```


## app/src/main/java/com/algorithmx/q_base/data/util/ExportUtils.kt

```

L22: class ExportUtils @Inject constructor(

L31: suspend fun prepareZip(setWithQuestions: SetWithQuestions): File = withContext(Dispatchers.IO) {

```


## app/src/main/java/com/algorithmx/q_base/data/util/MockExporter.kt

```

L24: class MockExporter @Inject constructor(

L32: suspend fun exportCollection(collectionId: String): File? = withContext(Dispatchers.IO) {

L68: suspend fun exportSession(sessionId: String): File? = withContext(Dispatchers.IO) {

L93: fun cleanup(file: File) {

```


## app/src/main/java/com/algorithmx/q_base/data/util/MockDownloader.kt

```

L22: class MockDownloader @Inject constructor(

L35: suspend fun downloadAndImportMock(

L89: suspend fun downloadAndImportSession(

L134: private suspend fun importCollectionData(

L185: private suspend fun importMockData(mockData: MockExport) {

```


## app/src/main/java/com/algorithmx/q_base/data/sessions/SessionDao.kt

```

L9: interface SessionDao {

L11: suspend fun insertSession(session: StudySession)

L14: suspend fun insertSet(set: QuestionSet)

L17: suspend fun updateSession(session: StudySession)

L20: fun getAllSessions(): Flow<List<StudySession>>

L23: fun getOngoingSessions(): Flow<List<StudySession>>

L26: suspend fun getSessionById(sessionId: String): StudySession?

L29: fun getAllSets(): Flow<List<QuestionSet>>

L32: fun getRecentSets(): Flow<List<QuestionSet>>

L35: fun getPinnedQuestions(): Flow<List<Question>>

L38: suspend fun insertAttempts(attempts: List<SessionAttempt>)

L41: fun getAttemptsForSession(sessionId: String): Flow<List<SessionAttempt>>

L44: suspend fun getAttemptsForSessionOnce(sessionId: String): List<SessionAttempt>

L47: suspend fun updateAttempt(attempt: SessionAttempt)

L50: suspend fun getAttempt(sessionId: String, questionId: String): SessionAttempt?

L53: fun getLastSessionForStudyCollection(collectionId: String): Flow<StudySession?>

L56: suspend fun deleteSessionsByIds(sessionIds: List<String>)

L59: suspend fun deleteAttemptsForSessions(sessionIds: List<String>)

L62: suspend fun deleteAllSessions()

L65: suspend fun deleteAllAttempts()

L68: suspend fun deleteAllSets()

```


## app/src/main/java/com/algorithmx/q_base/data/sessions/SessionExport.kt

```

L6: data class SessionExport(

```


## app/src/main/java/com/algorithmx/q_base/data/sessions/StudySession.kt

```

L10: data class StudySession(

```


## app/src/main/java/com/algorithmx/q_base/data/sessions/SessionAttempt.kt

```

L33: data class SessionAttempt(

```


## app/src/main/java/com/algorithmx/q_base/data/sessions/SessionRepository.kt

```

L19: class SessionRepository @Inject constructor(

L25: fun getCurrentUser(userId: String): Flow<UserEntity?> =

L28: fun getAllSessions(): Flow<List<StudySession>> = sessionDao.getAllSessions()

L30: fun getAllStudyCollections(): Flow<List<StudyCollection>> = collectionDao.getAllStudyCollections()

L32: suspend fun getStudyCollectionByIdOnce(collectionId: String): StudyCollection? =

L35: suspend fun getStudyCollectionByNameOnce(name: String): StudyCollection? =

L38: fun getAllSets(): Flow<List<QuestionSet>> = sessionDao.getAllSets()

L40: suspend fun getSessionById(sessionId: String): StudySession? = sessionDao.getSessionById(sessionId)

L45: suspend fun createNewSession(

L87: suspend fun createNewSessionSmart(

L107: fun getQuestionsByStudyCollection(collection: String): Flow<List<Question>> =

L110: fun getAttemptsForSession(sessionId: String): Flow<List<SessionAttempt>> =

L113: suspend fun getQuestionById(questionId: String): Question? =

L116: fun getOptionsForQuestion(questionId: String): Flow<List<QuestionOption>> =

L119: suspend fun getAnswerForQuestion(questionId: String): Answer? =

L122: suspend fun updateAttemptAndRecalculate(attempt: SessionAttempt) {

L191: suspend fun updateAttempt(attempt: SessionAttempt) {

L195: suspend fun updateSession(session: StudySession) {

L199: suspend fun saveAnswer(answer: Answer) {

L203: suspend fun deleteSessions(sessionIds: List<String>) {

```


## app/src/main/java/com/algorithmx/q_base/data/core/UserDao.kt

```

L7: interface UserDao {

L9: suspend fun insertUser(user: UserEntity)

L12: suspend fun getUserById(userId: String): UserEntity?

L15: fun getCurrentUser(userId: String): Flow<UserEntity?>

L18: fun getAllUsers(): Flow<List<UserEntity>>

L21: suspend fun deleteAllUsers()

```


## app/src/main/java/com/algorithmx/q_base/data/core/HomeRepository.kt

```

L18: class HomeRepository @Inject constructor(

L25: fun getCurrentUser(userId: String): Flow<com.algorithmx.q_base.data.core.UserEntity?> =

L28: fun getOngoingSessions(): Flow<List<StudySession>> = sessionDao.getOngoingSessions()

L30: fun getPinnedQuestions(): Flow<List<Question>> = sessionDao.getPinnedQuestions()

L32: fun getRecentSessions(): Flow<List<StudySession>> = sessionDao.getAllSessions()

L34: fun getAllStudyCollections(): Flow<List<StudyCollection>> = collectionDao.getAllStudyCollections()

L36: fun getAllStudyCollectionsWithCount(): Flow<List<StudyCollectionWithCount>> =

L39: fun getTotalUnreadCount(): Flow<Int> = chatDao.getTotalUnreadCount().map { it ?: 0 }

```


## app/src/main/java/com/algorithmx/q_base/data/core/UserEntity.kt

```

L7: data class UserEntity(

```


## app/src/main/java/com/algorithmx/q_base/data/core/ConfigRepository.kt

```

L19: class ConfigRepository @Inject constructor(

L47: suspend fun saveGeminiKey(key: String) {

L54: suspend fun saveGroqKey(key: String) {

L61: suspend fun fetchRemoteConfig() {

L100: suspend fun getGeminiKeyDirectly(): String = geminiApiKey.first()

L101: suspend fun getGroqKeyDirectly(): String = groqApiKey.first()

L103: suspend fun backupKeysToCloud(userId: String) {

L137: suspend fun restoreKeysFromCloud(userId: String) {

```


## app/src/main/java/com/algorithmx/q_base/data/core/DataClearingRepository.kt

```

L17: class DataClearingRepository @Inject constructor(

L28: suspend fun clearAllData(clearCollections: Boolean) = withContext(Dispatchers.IO) {

```


## app/src/main/java/com/algorithmx/q_base/data/ai/BrainUsageEntity.kt

```

L9: data class BrainUsageEntity(

```


## app/src/main/java/com/algorithmx/q_base/data/ai/AiResponseDao.kt

```

L7: interface AiResponseDao {

L9: fun getAllResponses(): Flow<List<AiResponseEntity>>

L12: suspend fun getResponseById(responseId: String): AiResponseEntity?

L15: suspend fun insertResponse(response: AiResponseEntity)

L18: suspend fun updateResponse(response: AiResponseEntity)

L21: suspend fun deleteResponseById(responseId: String)

L24: suspend fun deleteAllAiResponses()

```


## app/src/main/java/com/algorithmx/q_base/data/ai/AiModuleIntegrator.kt

```

L17: class BrainConfigProviderImpl @Inject constructor(

L32: class AiUsageLoggerImpl @Inject constructor(

```


## app/src/main/java/com/algorithmx/q_base/data/ai/AiResponseEntity.kt

```

L6: data class AiResponseEntity(

```


## app/src/main/java/com/algorithmx/q_base/data/ai/AiRepository.kt

```

L15: class AiRepository @Inject constructor(

L24: suspend fun generateCollection(

L98: suspend fun saveAsSet(

L118: suspend fun saveAsCollection(

L150: private suspend fun saveQuestionsToSet(questions: List<com.algorithmx.q_base.core_ai.brain.models.AiQuestion>, setId: String, collectionName: String) {

L191: suspend fun extractQuestionsFromText(

L265: suspend fun promoteAiResponseToDatabase(

L294: suspend fun assistQuestionEditing(stem: String, options: String = "", context: String = ""): Result<String> {

L308: suspend fun getAiAssistance(prompt: String): Result<String> {

L312: suspend fun getAiResponseById(responseId: String): AiResponseEntity? {

L316: private fun extractJsonFromResponse(response: String): String {

```


## app/src/main/java/com/algorithmx/q_base/data/ai/BrainUsageDao.kt

```

L11: interface BrainUsageDao {

L13: suspend fun insertUsageRecord(record: BrainUsageEntity)

L16: fun getRecentUsage(limit: Int = 100): Flow<List<BrainUsageEntity>>

L19: fun getTotalSuccessfulTokens(): Flow<Int?>

L22: fun getTotalTokensForTask(taskId: BrainTask): Flow<Int?>

L25: suspend fun pruneOldRecords(olderThanMs: Long)

L28: suspend fun deleteAllBrainUsage()

```


## app/src/main/java/com/algorithmx/q_base/data/sync/ChatManagerRepository.kt

```

L23: class ChatManagerRepository @Inject constructor(

L37: suspend fun addParticipantToRemote(chatId: String, userId: String) {

L41: suspend fun removeParticipantFromRemote(chatId: String, userId: String) {

L45: suspend fun promoteParticipantToAdminOnRemote(chatId: String, userId: String) {

L49: suspend fun demoteAdminOnRemote(chatId: String, userId: String) {

L53: suspend fun createChatOnRemote(chat: ChatEntity) {

L57: suspend fun getChatById(chatId: String): ChatEntity? {

L61: suspend fun syncUserChatsFromRemote() {

L126: suspend fun findExistingP2PChat(uid: String, userId: String): ChatEntity? {

L172: suspend fun ensureChatExistsLocally(chatId: String, senderId: String? = null) {

L198: suspend fun fetchAndSyncChatMetadata(chatId: String) {

L237: suspend fun deleteChatOnRemote(chatId: String) {

L241: fun deleteChatAndMessagesGlobally(chatId: String) {

```


## app/src/main/java/com/algorithmx/q_base/data/sync/MessageSyncRepository.kt

```

L21: class MessageSyncRepository @Inject constructor(

L39: fun serializeWrappedKeys(map: Map<String, String>): String {

L45: fun deserializeWrappedKeys(jsonStr: String?): Map<String, String> {

L61: suspend fun acknowledgeMessageDelivery(

```


## app/src/main/java/com/algorithmx/q_base/data/sync/OfflineActionEntity.kt

```

L8: data class OfflineActionEntity(

```


## app/src/main/java/com/algorithmx/q_base/data/sync/MessageSyncIncomingGlobalExtensions.kt

```

L17: fun MessageSyncRepository.observeAllIncomingMessages(notificationHelper: NotificationHelper): Flow<Unit> {

L205: suspend fun MessageSyncRepository.fetchAndSyncMessages(chatId: String) {

```


## app/src/main/java/com/algorithmx/q_base/data/sync/CollectionSyncRequestExtensions.kt

```

L12: suspend fun CollectionSyncRepository.sendSyncRequest(targetUserId: String, targetCollectionId: String) {

L34: fun CollectionSyncRepository.observeIncomingRequests(): Flow<List<SyncRequest>> = callbackFlow {

L86: suspend fun CollectionSyncRepository.requestCollectionAccess(chatId: String, collectionId: String) {

L107: fun CollectionSyncRepository.observeAccessRequests(chatId: String): Flow<List<Map<String, Any>>> {

L155: suspend fun CollectionSyncRepository.grantCollectionAccess(chatId: String, collectionId: String, requesterId: String) {

```


## app/src/main/java/com/algorithmx/q_base/data/sync/MessageSyncOutgoingExtensions.kt

```

L11: suspend fun MessageSyncRepository.sendMessage(message: MessageEntity) {

L143: suspend fun MessageSyncRepository.flushQueue() {

L161: suspend fun MessageSyncRepository.clearChatMessagesOnRemote(chatId: String) {

```


## app/src/main/java/com/algorithmx/q_base/data/sync/SyncRequest.kt

```

L6: data class SyncRequest(

```


## app/src/main/java/com/algorithmx/q_base/data/sync/CollectionSyncPatchExtensions.kt

```

L15: suspend fun CollectionSyncRepository.applyCollectionMicroUpdate(payload: String) {

L128: suspend fun CollectionSyncRepository.broadcastCollectionMicroUpdate(chatId: String, collectionId: String, diff: JSONObject) {

L168: suspend fun CollectionSyncRepository.sendCollectionPatch(chatId: String, collectionId: String, op: String, data: JSONObject) {

L185: suspend fun CollectionSyncRepository.applyCollectionPatch(jsonString: String) {

```


## app/src/main/java/com/algorithmx/q_base/data/sync/CollectionSyncRepository.kt

```

L23: class CollectionSyncRepository @Inject constructor(

```


## app/src/main/java/com/algorithmx/q_base/data/sync/MessageSyncIncomingExtensions.kt

```

L14: fun MessageSyncRepository.observeAndSyncMessages(chatId: String): Flow<MessageEntity?> {

```


## app/src/main/java/com/algorithmx/q_base/data/sync/CollectionSyncFileExtensions.kt

```

L19: suspend fun CollectionSyncRepository.uploadQuestionBankZip(zipFile: File): Pair<String, String> {

L39: suspend fun CollectionSyncRepository.deleteQuestionBankZip(fileId: String) {

L48: suspend fun CollectionSyncRepository.shareCollectionToGroup(chatId: String, collectionMetadata: Map<String, Any>) {

L165: suspend fun CollectionSyncRepository.acknowledgeCollectionDownload(collectionId: String) {

L203: fun CollectionSyncRepository.observeGroupLibrary(chatId: String): Flow<List<Map<String, Any>>> {

L235: private suspend fun CollectionSyncRepository.mapGroupLibrary(list: List<Map<String, Any>>): List<Map<String, Any>> {

```


## app/src/main/java/com/algorithmx/q_base/data/sync/SyncRepository.kt

```

L18: class SyncRepository @Inject constructor(

L28: fun observeAndSyncMessages(chatId: String): Flow<MessageEntity?> {

L32: suspend fun sendMessage(message: MessageEntity) {

L36: suspend fun flushQueue() {

L40: suspend fun addParticipantToRemote(chatId: String, userId: String) {

L44: suspend fun removeParticipantFromRemote(chatId: String, userId: String) {

L48: suspend fun promoteParticipantToAdminOnRemote(chatId: String, userId: String) {

L52: suspend fun demoteAdminOnRemote(chatId: String, userId: String) {

L56: suspend fun createChatOnRemote(chat: ChatEntity) {

L60: fun observeAllIncomingMessages(notificationHelper: NotificationHelper): Flow<Unit> {

L64: suspend fun syncUserChatsFromRemote() {

L68: suspend fun findExistingP2PChat(uid: String, userId: String): ChatEntity? {

L72: suspend fun fetchAndSyncMessages(chatId: String) {

L76: suspend fun clearChatMessagesOnRemote(chatId: String) {

L80: suspend fun deleteChatOnRemote(chatId: String) {

L84: fun deleteChatAndMessagesGlobally(chatId: String) {

L88: suspend fun getChatById(chatId: String): ChatEntity? {

L93: suspend fun sendSessionInvite(

L103: suspend fun addSharedSessionToGroup(

L113: fun observeSharedSessions(chatId: String): Flow<List<Map<String, Any>>> {

L117: suspend fun sendSessionPatch(chatId: String, sessionId: String, op: String, data: JSONObject) {

L122: suspend fun sendSyncRequest(targetUserId: String, targetCollectionId: String) {

L126: fun observeIncomingRequests(): Flow<List<SyncRequest>> {

L130: suspend fun uploadQuestionBankZip(zipFile: File): Pair<String, String> {

L134: suspend fun deleteQuestionBankZip(fileId: String) {

L138: suspend fun shareCollectionToGroup(chatId: String, collectionMetadata: Map<String, Any>) {

L142: suspend fun acknowledgeCollectionDownload(collectionId: String) {

L146: suspend fun applyCollectionMicroUpdate(payload: String) {

L150: suspend fun broadcastCollectionMicroUpdate(chatId: String, collectionId: String, diff: JSONObject) {

L154: fun observeGroupLibrary(chatId: String): Flow<List<Map<String, Any>>> {

L158: suspend fun sendCollectionPatch(chatId: String, collectionId: String, op: String, data: JSONObject) {

L162: suspend fun requestCollectionAccess(chatId: String, collectionId: String) {

L166: fun observeAccessRequests(chatId: String): Flow<List<Map<String, Any>>> {

L170: suspend fun grantCollectionAccess(chatId: String, collectionId: String, requesterId: String) {

L175: suspend fun reportSession(sessionId: String, reason: String) {

L179: suspend fun reportQuestion(

L188: suspend fun reportGroup(group: ChatEntity, reason: String) {

L192: suspend fun reportUser(user: UserEntity, reason: String) {

L196: suspend fun reportCollection(collection: StudyCollection, reason: String) {

L200: suspend fun reportMessage(message: MessageEntity, reason: String) {

```


## app/src/main/java/com/algorithmx/q_base/data/sync/UniversalQueueManager.kt

```

L16: class UniversalQueueManager @Inject constructor(

L22: suspend fun flushUniversalQueue() {

L56: private suspend fun processAction(action: OfflineActionEntity): Boolean {

```


## app/src/main/java/com/algorithmx/q_base/data/sync/ActionQueueDao.kt

```

L6: interface ActionQueueDao {

L8: suspend fun insertAction(action: OfflineActionEntity)

L11: suspend fun getPendingActions(): List<OfflineActionEntity>

L14: suspend fun deleteAction(action: OfflineActionEntity)

L17: suspend fun updateAction(action: OfflineActionEntity)

L20: suspend fun clearAllActions()

```


## app/src/main/java/com/algorithmx/q_base/data/sync/PatchEvent.kt

```

L6: data class PatchEvent(

```


## app/src/main/java/com/algorithmx/q_base/data/sync/SessionSyncRepository.kt

```

L27: class SessionSyncRepository @Inject constructor(

L40: suspend fun sendSessionInvite(

L62: suspend fun addSharedSessionToGroup(

L115: fun observeSharedSessions(chatId: String): Flow<List<Map<String, Any>>> {

L157: suspend fun sendSessionPatch(chatId: String, sessionId: String, op: String, data: JSONObject) {

L174: suspend fun applySessionPatch(jsonString: String) {

```


## app/src/main/java/com/algorithmx/q_base/data/sync/ReportSyncRepository.kt

```

L20: class ReportSyncRepository @Inject constructor(

L29: suspend fun reportSession(sessionId: String, reason: String) {

L52: suspend fun reportQuestion(

L79: suspend fun reportGroup(group: ChatEntity, reason: String) {

L83: suspend fun reportUser(user: UserEntity, reason: String) {

L103: suspend fun reportCollection(collection: StudyCollection, reason: String) {

L123: suspend fun reportMessage(message: MessageEntity, reason: String) {

```


## app/src/main/java/com/algorithmx/q_base/data/collections/ExploreRepository.kt

```

L15: class ExploreRepository @Inject constructor(

L22: fun getCurrentUser(userId: String): Flow<UserEntity?> = userDao.getCurrentUser(userId)

L24: fun getStudyCollections(): Flow<List<StudyCollection>> = collectionDao.getAllStudyCollections()

L26: fun getStudyCollectionsWithCount(): Flow<List<StudyCollectionWithCount>> =

L29: fun getStudyCollectionById(collectionId: String): Flow<StudyCollection?> = collectionDao.getStudyCollectionById(collectionId)

L31: fun getSetsByStudyCollectionId(collectionId: String): Flow<List<QuestionSet>> = collectionDao.getSetsByStudyCollectionId(collectionId)

L33: fun getLastSessionForStudyCollection(collectionId: String): Flow<StudySession?> = sessionDao.getLastSessionForStudyCollection(collectionId)

L35: fun getQuestionsByStudyCollection(collection: String): Flow<List<Question>> =

L38: fun getQuestionsBySet(setId: String): Flow<List<Question>> = collectionDao.getQuestionsForSet(setId)

L40: suspend fun getQuestionById(questionId: String): Question? =

L43: suspend fun getSessionById(sessionId: String): StudySession? = sessionDao.getSessionById(sessionId)

L45: suspend fun updateSession(session: StudySession) = sessionDao.updateSession(session)

L47: fun getOptionsForQuestion(questionId: String): Flow<List<QuestionOption>> =

L50: fun getAnswerForQuestion(questionId: String): Flow<Answer?> =

L53: suspend fun updateQuestion(question: Question) {

L57: suspend fun reportProblem(report: ProblemReport) {

L61: fun getAllSets(): Flow<List<QuestionSet>> = sessionDao.getAllSets()

L63: fun getAllSessions(): Flow<List<StudySession>> = sessionDao.getAllSessions()

L65: fun getPinnedQuestions(): Flow<List<Question>> = questionDao.getPinnedQuestions()

L67: fun getQuestionCountByStudyCollection(collectionName: String): Flow<Int> =

L70: suspend fun addQuestionToSet(setId: String, questionId: String) {

L76: suspend fun addQuestionToSession(sessionId: String, questionId: String) {

L88: suspend fun saveSet(set: QuestionSet) {

L92: suspend fun saveAnswer(answer: Answer) {

L96: suspend fun updateStudyCollection(collection: StudyCollection) {

L100: suspend fun getStudyCollectionByIdOnce(collectionId: String): StudyCollection? {

L104: suspend fun getStudyCollectionByNameOnce(name: String): StudyCollection? {

L108: suspend fun getSetIdForQuestion(questionId: String): String? {

L112: suspend fun deleteStudyCollection(collectionId: String) {

```


## app/src/main/java/com/algorithmx/q_base/data/collections/ProblemReport.kt

```

L21: data class ProblemReport(

```


## app/src/main/java/com/algorithmx/q_base/data/collections/CollectionVersionLedger.kt

```

L11: data class CollectionVersionLedgerEntity(

L18: interface CollectionVersionLedgerDao {

L20: suspend fun getLedgerForCollection(collectionId: String): CollectionVersionLedgerEntity?

L23: suspend fun insertLedger(ledger: CollectionVersionLedgerEntity)

L26: suspend fun deleteLedger(collectionId: String)

```


## app/src/main/java/com/algorithmx/q_base/data/collections/QuestionSet.kt

```

L23: data class QuestionSet(

```


## app/src/main/java/com/algorithmx/q_base/data/collections/ImportRepository.kt

```

L20: class ImportRepository @Inject constructor(

L30: suspend fun recognizeTextFromImage(uri: Uri): Result<String> = withContext(Dispatchers.IO) {

L41: suspend fun extractTextFromPdf(uri: Uri): Result<String> = withContext(Dispatchers.IO) {

L71: fun cleanRecognizedText(rawText: String): String {

```


## app/src/main/java/com/algorithmx/q_base/data/collections/QuestionWithContent.kt

```

L6: data class QuestionWithContent(

```


## app/src/main/java/com/algorithmx/q_base/data/collections/Answer.kt

```

L10: data class Answer(

```


## app/src/main/java/com/algorithmx/q_base/data/collections/CollectionWithSets.kt

```

L6: data class StudyCollectionWithSets(

```


## app/src/main/java/com/algorithmx/q_base/data/collections/ProblemReportDao.kt

```

L7: interface ProblemReportDao {

L9: suspend fun insertReport(report: ProblemReport)

L12: fun getAllReports(): Flow<List<ProblemReport>>

L15: fun getReportsForQuestion(questionId: String): Flow<List<ProblemReport>>

```


## app/src/main/java/com/algorithmx/q_base/data/collections/CollectionDao.kt

```

L7: interface CollectionDao {

L9: fun getAllStudyCollections(): Flow<List<StudyCollection>>

L16: fun getAllStudyCollectionsWithCount(): Flow<List<StudyCollectionWithCount>>

L19: suspend fun getStudyCollectionCount(): Int

L22: fun getStudyCollectionById(collectionId: String): Flow<StudyCollection?>

L25: suspend fun getStudyCollectionByIdOnce(collectionId: String): StudyCollection?

L28: suspend fun getStudyCollectionByNameOnce(name: String): StudyCollection?

L31: fun getSetsByStudyCollectionId(collectionId: String): Flow<List<QuestionSet>>

L34: fun getSetsByStudyCollectionName(collectionName: String): Flow<List<QuestionSet>>

L41: fun getCategoriesByStudyCollectionId(collectionId: String): Flow<List<String>>

L48: fun getQuestionsForSet(setId: String): Flow<List<Question>>

L51: suspend fun getSetsByStudyCollectionIdOnce(collectionId: String): List<QuestionSet>

L58: suspend fun getQuestionsForSetOnce(setId: String): List<Question>

L61: suspend fun getCrossRefsForSetsBatch(setIds: List<String>): List<SetQuestionCrossRef>

L64: suspend fun insertStudyCollections(collections: List<StudyCollection>)

L67: suspend fun updateStudyCollection(collection: StudyCollection)

L70: suspend fun insertSets(sets: List<QuestionSet>)

L73: suspend fun insertCrossRefs(refs: List<SetQuestionCrossRef>)

L76: suspend fun updateStudyCollectionTimestamp(collectionId: String, timestamp: Long)

L79: suspend fun deleteStudyCollectionById(collectionId: String)

L82: suspend fun deleteAllStudyCollections()

```


## app/src/main/java/com/algorithmx/q_base/data/collections/Question.kt

```

L18: data class Question(

```


## app/src/main/java/com/algorithmx/q_base/data/collections/CollectionExport.kt

```

L5: data class CollectionExport(

```


## app/src/main/java/com/algorithmx/q_base/data/collections/SetWithQuestions.kt

```

L7: data class SetWithQuestions(

```


## app/src/main/java/com/algorithmx/q_base/data/collections/QuestionDao.kt

```

L7: interface QuestionDao {

L9: fun getQuestionsByStudyCollection(collection: String): Flow<List<Question>>

L12: fun getQuestionsByCategory(category: String): Flow<List<Question>>

L15: fun getQuestionsByTag(tag: String): Flow<List<Question>>

L18: fun getQuestionsByCategoryAndTag(category: String, tag: String): Flow<List<Question>>

L21: fun getOptionsForQuestion(questionId: String): Flow<List<QuestionOption>>

L24: fun getAnswerForQuestion(questionId: String): Flow<Answer?>

L27: suspend fun getOptionsForQuestionOnce(questionId: String): List<QuestionOption>

L30: suspend fun getAnswerForQuestionOnce(questionId: String): Answer?

L33: suspend fun getQuestionById(questionId: String): Question?

L36: suspend fun updateQuestion(question: Question)

L39: suspend fun insertQuestions(questions: List<Question>)

L42: suspend fun insertOptions(options: List<QuestionOption>)

L45: suspend fun insertAnswers(answers: List<Answer>)

L48: suspend fun insertSet(set: QuestionSet)

L51: suspend fun insertQuestion(question: Question)

L54: suspend fun insertOption(option: QuestionOption)

L57: suspend fun insertAnswer(answer: Answer)

L60: suspend fun insertSetQuestionCrossRef(crossRef: SetQuestionCrossRef)

L63: fun getQuestionCountFlow(): Flow<Int>

L71: fun getUserCreatedQuestionCount(): Flow<Int>

L79: fun getSharedQuestionCount(): Flow<Int>

L82: suspend fun getQuestionCount(): Int

L85: fun getPinnedQuestions(): Flow<List<Question>>

L89: suspend fun getSetWithContent(setId: String): SetWithQuestions?

L92: fun getQuestionCountByStudyCollection(collectionName: String): Flow<Int>

L95: suspend fun getSetIdForQuestion(questionId: String): String?

L98: suspend fun deleteOptionsForQuestion(questionId: String)

L101: suspend fun deleteSetById(setId: String)

L104: suspend fun deleteSetsByIds(setIds: List<String>)

L107: suspend fun deleteCrossRefsForSets(setIds: List<String>)

L110: suspend fun removeQuestionFromSet(setId: String, questionId: String)

L113: suspend fun deleteQuestionById(questionId: String)

L116: suspend fun deleteAllQuestions()

L119: suspend fun deleteAllOptions()

L122: suspend fun deleteAllAnswers()

L125: suspend fun deleteAllCrossRefs()

```


## app/src/main/java/com/algorithmx/q_base/data/collections/MockExport.kt

```

L5: data class QuestionExport(

L12: data class MockExport(

```


## app/src/main/java/com/algorithmx/q_base/data/collections/QuestionOption.kt

```

L23: data class QuestionOption(

```


## app/src/main/java/com/algorithmx/q_base/data/collections/StudyCollection.kt

```

L10: data class StudyCollection(

L37: data class StudyCollectionWithCount(

```


## app/src/main/java/com/algorithmx/q_base/data/collections/SetQuestionCrossRef.kt

```

L32: data class SetQuestionCrossRef(

```


## app/src/main/java/com/algorithmx/q_base/util/NotificationHelper.kt

```

L21: class NotificationHelper @Inject constructor(

L30: fun createNotificationChannels() {

L55: fun showMessageNotification(chatId: String, senderName: String, message: String) {

L87: fun showSessionNotification(sessionId: String, title: String, description: String) {

```


## app/src/main/java/com/algorithmx/q_base/util/NetworkMonitor.kt

```

L21: class NetworkMonitor @Inject constructor(

L32: fun currentConnectivity(): Boolean {

```


## app/src/main/java/com/algorithmx/q_base/ui/navigation/Screen.kt

```

L7: sealed class Screen : NavKey {

```


## app/src/main/java/com/algorithmx/q_base/ui/navigation/Navigator.kt

```

L8: class Navigator(val state: NavigationState) {

L9: private fun normalizeRoute(route: NavKey): NavKey = when (route) {

L14: private fun findTopLevelMatch(route: NavKey): NavKey? =

L19: private fun resetStack(root: NavKey, stack: androidx.navigation3.runtime.NavBackStack<NavKey>) {

L24: fun navigate(route: NavKey) {

L56: fun resetTo(route: NavKey) {

L78: fun goBack() {

```


## app/src/main/java/com/algorithmx/q_base/ui/navigation/NavigationState.kt

```

L27: fun rememberNavigationState(

L83: class NavigationState(

L98: fun NavigationState.toEntries(

```


## app/src/main/java/com/algorithmx/q_base/ui/navigation/AppEntryProvider.kt

```

L16: fun rememberAppEntryProvider(navigator: Navigator) = remember(navigator) {

```


## app/src/main/java/com/algorithmx/q_base/ui/navigation/AppEntryWrappers.kt

```

L20: fun ExplorePagerWrapper(key: Screen.ExplorePager, navigator: Navigator) {

L81: fun CollectionOverviewWrapper(key: Screen.CollectionOverview, navigator: Navigator) {

L123: fun PinnedQuestionsWrapper(navigator: Navigator) {

L147: fun ExploreSetWrapper(key: Screen.ExploreSet, navigator: Navigator) {

L193: fun SessionsWrapper(navigator: Navigator) {

L220: fun NewSessionWizardWrapper(navigator: Navigator) {

L238: fun ActiveSessionWrapper(key: Screen.ActiveSession, navigator: Navigator) {

L253: fun SessionResultsWrapper(key: Screen.SessionResults, navigator: Navigator) {

L267: fun ChatDetailWrapper(key: Screen.ChatDetail, navigator: Navigator) {

L294: fun ContactOverviewWrapper(key: Screen.ContactOverview, navigator: Navigator) {

L308: fun GroupOverviewWrapper(key: Screen.GroupOverview, navigator: Navigator) {

L322: fun ProfileWrapper(navigator: Navigator) {

```


## app/src/main/java/com/algorithmx/q_base/ui/explore/ExploreViewModel.kt

```

L20: data class ExploreQuestionState(

L32: class ExploreViewModel @Inject constructor(

L112: fun resetQuestionStates() {

L141: fun loadQuestionsByStudyCollection(collectionId: String) {

L156: fun loadQuestionsBySet(setId: String) {

L166: private suspend fun checkIsEditable(collection: StudyCollection?): Boolean {

L175: fun loadPinnedQuestions() {

L191: fun updateCollectionAdminOnly(collectionId: String, isAdminOnly: Boolean) {

L213: fun loadCollectionOverview(collectionId: String) {

L246: fun updateSessionProgress(sessionId: String, index: Int) {

L254: fun loadQuestionDetails(index: Int) {

L272: fun selectOption(index: Int, optionLetter: String) {

L309: fun revealAnswer(index: Int) {

L317: fun togglePin(index: Int) {

```


## app/src/main/java/com/algorithmx/q_base/ui/explore/ExploreViewModelSetExtensions.kt

```

L11: fun ExploreViewModel.toggleSetSelection(setId: String) {

L22: fun ExploreViewModel.clearSelection() {

L27: fun ExploreViewModel.deleteCollectionSet(setId: String) {

L34: suspend fun ExploreViewModel.getSetIdForQuestion(questionId: String): String? {

L38: fun ExploreViewModel.deleteSelectedSets() {

L50: fun ExploreViewModel.addQuestionToSet(index: Int, setId: String) {

L72: fun ExploreViewModel.addQuestionToSession(index: Int, sessionId: String) {

L94: fun ExploreViewModel.createSet(title: String, description: String, collectionId: String) {

L110: fun ExploreViewModel.deleteQuestion(index: Int) {

L137: fun ExploreViewModel.deleteQuestionFromSet(index: Int, setId: String) {

```


## app/src/main/java/com/algorithmx/q_base/ui/explore/ExploreViewModelAiExtensions.kt

```

L9: fun ExploreViewModel.askAi(index: Int, mode: String = "EXPLAIN") {

L37: fun ExploreViewModel.askAiAboutCollection(collection: StudyCollection) {

L56: fun ExploreViewModel.clearCollectionAiResponse() {

L60: fun ExploreViewModel.clearAiResponse(index: Int) {

L66: fun ExploreViewModel.saveAiResponseToQuestion(index: Int) {

```


## app/src/main/java/com/algorithmx/q_base/ui/explore/PinnedQuestionsScreen.kt

```

L29: fun PinnedQuestionsScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/explore/ExploreScreens.kt

```

L45: fun ExploreQuestionPagerScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/explore/UnifiedExploreScreen.kt

```

L37: fun UnifiedExploreScreen(

L172: fun MasterCollectionListItem(

L256: fun EmptyLibraryView(modifier: Modifier = Modifier) {

```


## app/src/main/java/com/algorithmx/q_base/ui/explore/ExploreViewModelReportExtensions.kt

```

L10: fun ExploreViewModel.reportCollectionToGroup(collection: StudyCollection, reason: String) {

L32: fun ExploreViewModel.reportProblem(index: Int, explanation: String) {

L62: fun ExploreViewModel.reportCollection(collection: StudyCollection, reason: String) {

L79: fun ExploreViewModel.deleteStudyCollection(collectionId: String) {

L92: fun ExploreViewModel.reportSet(setId: String, reason: String) {

```


## app/src/main/java/com/algorithmx/q_base/ui/explore/CollectionOverviewScreen.kt

```

L39: fun CollectionOverviewScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/explore/CollectionOverviewComponents.kt

```

L21: fun StatCard(

L44: fun SetItem(

```


## app/src/main/java/com/algorithmx/q_base/ui/state/AppAccessState.kt

```

L3: sealed interface AppAccessState {

L4: data object RestoringSession : AppAccessState

L5: data object OnlineReady : AppAccessState

L6: data object SignedInOffline : AppAccessState

L7: data object GuestOnline : AppAccessState

L8: data object OfflineGuest : AppAccessState

```


## app/src/main/java/com/algorithmx/q_base/ui/sessions/NewSessionWizardScreen.kt

```

L26: fun NewSessionWizardScreen(

L140: fun EmptyCollectionsView() {

```


## app/src/main/java/com/algorithmx/q_base/ui/sessions/SessionsScreens.kt

```

L51: fun SessionsListScreen(

L176: fun AnimatedSessionItem(index: Int, content: @Composable () -> Unit) {

L192: fun CategoryChip(name: String) {

L220: fun SessionListItemExpressive(

L319: fun EmptySessionsView() {

```


## app/src/main/java/com/algorithmx/q_base/ui/sessions/SessionWizardSteps.kt

```

L27: fun CategoryStep(

L75: fun QuestionSelectionStep(

L182: fun ConfigurationStep(

L284: fun ConfigToggle(

```


## app/src/main/java/com/algorithmx/q_base/ui/sessions/ActiveSessionViewModel.kt

```

L25: sealed class SessionNavEvent {

L26: data class NavigateToResults(val sessionId: String) : SessionNavEvent()

L29: data class NavigatorDot(

L36: class ActiveSessionViewModel @Inject constructor(

L116: fun setSessionId(id: String, chatId: String? = null) {

L128: fun getSessionId(): String = _sessionId

L130: private fun loadSessionData() {

L156: private fun startTimer() {

L184: private fun loadAttempts() {

L195: fun navigateToQuestion(index: Int) {

L205: private fun loadQuestion(questionId: String) {

L216: fun onAnswerSelected(optionLetter: String) {

L257: fun toggleFlag() {

L264: private fun updateAttempt(attempt: SessionAttempt) {

L290: fun askAi(mode: String = "EXPLAIN") {

L309: fun clearAiResponse() {

L313: fun saveAiResponseToQuestion() {

L331: fun submitSession() {

L366: fun reportSession(reason: String) {

L380: fun reportQuestion(reason: String) {

```


## app/src/main/java/com/algorithmx/q_base/ui/sessions/ActiveSessionScreen.kt

```

L56: fun ActiveSessionScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/sessions/SessionsViewModel.kt

```

L20: class SessionsViewModel @Inject constructor(

L96: fun onSearchQueryChange(query: String) {

L100: fun setWizardStep(step: Int) {

L104: fun selectCollection(name: String) {

L114: fun toggleQuestionSelection(id: String) {

L121: fun selectAllQuestions() {

L125: fun deselectAllQuestions() {

L129: fun selectRandomQuestions(count: Int) {

L140: fun setOrder(order: String) { _sessionOrder.value = order }

L141: fun setTimingType(type: String) { _timingType.value = type }

L142: fun setTimeLimit(seconds: Int) { _timeLimitSeconds.value = seconds }

L143: fun setSessionIsAdminOnly(value: Boolean) { _sessionIsAdminOnly.value = value }

L145: fun launchSession(title: String) {

L163: fun resetWizard() {

L174: fun toggleSessionSelection(sessionId: String) {

L185: fun clearSessionSelection() {

L190: fun deleteSelectedSessions() {

L200: fun reportSession(sessionId: String, reason: String) {

```


## app/src/main/java/com/algorithmx/q_base/ui/sessions/SessionResultsViewModel.kt

```

L20: data class ReviewState(

L27: sealed class ResultsUiState {

L28: object Loading : ResultsUiState()

L29: data class Success(val session: StudySession?, val attempts: List<SessionAttempt>, val score: Float) : ResultsUiState()

L33: class SessionResultsViewModel @Inject constructor(

L66: fun initSession(id: String) {

L72: private fun loadResults() {

L104: fun updateSessionAdminOnly(sessionId: String, isAdminOnly: Boolean) {

L128: fun selectQuestionForReview(attempt: SessionAttempt) {

L143: fun clearReview() {

L147: fun reportCurrentSession(reason: String) {

```


## app/src/main/java/com/algorithmx/q_base/ui/sessions/SessionResultsScreen.kt

```

L39: fun SessionResultsScreen(

L162: fun ResultsContent(

L334: fun AnimatedAttemptDot(

```


## app/src/main/java/com/algorithmx/q_base/ui/sessions/components/MasterNavigator.kt

```

L21: fun MasterNavigator(

```


## app/src/main/java/com/algorithmx/q_base/ui/settings/AiBrainManagerScreen.kt

```

L22: fun AiBrainManagerScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/settings/SecureBackupViewModel.kt

```

L14: data class SecureBackupState(

L22: class SecureBackupViewModel @Inject constructor(

L34: fun checkBackupStatus() {

L43: fun setupBackup(passphrase: String) {

L64: fun restoreBackup(passphrase: String) {

L80: fun clearState() {

```


## app/src/main/java/com/algorithmx/q_base/ui/settings/SettingsScreen.kt

```

L34: fun SettingsScreen(

L58: fun SettingsContent(

```


## app/src/main/java/com/algorithmx/q_base/ui/settings/ProfileViewModel.kt

```

L23: data class UserStats(

L31: class ProfileViewModel @Inject constructor(

L82: private fun loadUser() {

L96: private fun loadBackupStatus() {

L110: private fun loadStats() {

L130: private suspend fun tryUpdateProfile(updatedProfile: UserProfile) {

L142: fun updateDisplayName(newName: String) {

L160: fun updateIntro(newIntro: String) {

L178: fun updateProfilePictureUrl(url: String) {

L196: fun togglePhotoVisibility(isVisible: Boolean) {

L214: fun signOut(clearCollections: Boolean, onComplete: () -> Unit) {

```


## app/src/main/java/com/algorithmx/q_base/ui/settings/ProfileComponents.kt

```

L40: data class AvatarTemplate(

L59: fun Modifier.bounceClick(onClick: () -> Unit = {}) = composed {

L91: fun ProfileParallaxHeader(

```


## app/src/main/java/com/algorithmx/q_base/ui/settings/SettingsViewModel.kt

```

L25: class SettingsViewModel @Inject constructor(

L44: fun updateModel(newModel: String) {

L56: fun updateSystemInstruction(instruction: String) {

L67: fun saveTaskConfig(task: BrainTask, config: TaskConfig) {

L73: fun updateNotifications(enabled: Boolean) {

L79: fun updateTheme(mode: String) {

L85: fun clearAllData(onComplete: () -> Unit) {

L94: private fun calculateDbSize(): Double {

```


## app/src/main/java/com/algorithmx/q_base/ui/settings/AppThemeScreen.kt

```

L26: fun AppThemeScreen(

L85: data class ThemeOption(val id: String, val title: String, val subtitle: String)

L88: fun ThemeCard(

```


## app/src/main/java/com/algorithmx/q_base/ui/settings/SecureBackupDialog.kt

```

L16: fun SecureBackupDialog(

```


## app/src/main/java/com/algorithmx/q_base/ui/settings/ProfileScreen.kt

```

L35: fun ProfileScreen(

L74: fun ProfileContent(

L290: fun ProfileContentPreview() {

```


## app/src/main/java/com/algorithmx/q_base/ui/settings/components/ProfileAvatar.kt

```

L29: fun ProfileAvatar(

```


## app/src/main/java/com/algorithmx/q_base/ui/settings/components/ProfileRowComponents.kt

```

L19: fun ProfileCardSection(

L54: fun ProfilePropertyRow(

L113: fun ProfilePropertyToggleRow(

```


## app/src/main/java/com/algorithmx/q_base/ui/settings/components/SettingsComponents.kt

```

L18: fun SettingsSection(

L31: fun SettingsSectionHeader(title: String) {

L44: fun SettingsToggleCard(

L81: fun SettingsCard(

L117: fun UsageStatsCard(requests: Int, tokens: Int) {

```


## app/src/main/java/com/algorithmx/q_base/ui/settings/components/ProfileStatsComponents.kt

```

L28: fun ProfileAchievementsProperty(stats: UserStats) {

L63: fun BadgeBadge(

L134: fun StatCard(

```


## app/src/main/java/com/algorithmx/q_base/ui/theme/Theme.kt

```

L98: fun QbaseTheme(

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/ContactOverviewScreen.kt

```

L34: fun ContactOverviewScreen(

L196: fun ActionButton(

L221: fun SettingsItem(

L260: fun ReportDialog(

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/ChatDetailScreen.kt

```

L27: fun ChatDetailScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/GroupOverviewScreen.kt

```

L35: fun GroupOverviewScreen(

L334: fun ParticipantItem(user: UserEntity, isAdmin: Boolean, onClick: () -> Unit = {}) {

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/NewChatScreen.kt

```

L10: fun NewChatScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModelSharingExtensions.kt

```

L10: fun ChatViewModel.addSharedCollection(jsonPayload: String) {

L27: fun ChatViewModel.importSharedCollection(payload: String) {

L80: fun ChatViewModel.shareCollection(chatId: String, collectionId: String) {

L132: fun ChatViewModel.resendCollection(collectionId: String) {

L176: fun ChatViewModel.shareSession(chatId: String, sessionId: String) {

L217: fun ChatViewModel.joinSession(sessionIdOrPayload: String, onSessionImported: (String) -> Unit) {

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/ChatListScreen.kt

```

L44: fun ChatListScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/BlockedListScreen.kt

```

L26: fun BlockedListScreen(

L71: fun BlockedChatItem(

L129: fun EmptyBlockedView(modifier: Modifier = Modifier) {

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/AddParticipantDialog.kt

```

L23: fun AddParticipantDialog(

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/NewGroupScreen.kt

```

L29: fun NewGroupScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/ChatModels.kt

```

L10: data class ChatUiModel(

L20: data class ChatListState(

L29: data class ChatDetailState(

L40: sealed class ChatNavEvent {

L41: data class NavigateToChatDetail(val chatId: String) : ChatNavEvent()

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModel.kt

```

L34: class ChatViewModel @Inject constructor(

L72: fun canSendToChat(chatId: String): Flow<Boolean> {

L95: fun requestAccess(collectionId: String) {

L107: fun grantAccess(collectionId: String, requesterId: String) {

L137: fun toggleLibraryMode(enabled: Boolean) {

L176: fun toggleChatSelection(chatId: String) {

L188: fun clearSelection() {

L193: fun deleteSelectedChats() {

L348: fun syncChatsFromRemote() {

L361: fun setChatId(chatId: String) {

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/ContactSelector.kt

```

L38: fun ContactSelector(

L301: fun UserItem(

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModelAdminExtensions.kt

```

L9: fun ChatViewModel.addParticipant(chatId: String, userId: String) {

L48: fun ChatViewModel.removeParticipant(chatId: String, userId: String) {

L86: fun ChatViewModel.promoteParticipantToAdmin(chatId: String, userId: String) {

L109: fun ChatViewModel.demoteAdmin(chatId: String, userId: String) {

L136: fun ChatViewModel.leaveGroup(chatId: String) {

L156: fun ChatViewModel.reportGroup(chatId: String, reason: String) {

L170: fun ChatViewModel.reportUser(userId: String, reason: String) {

L183: fun ChatViewModel.reportMessage(message: MessageEntity, reason: String) {

L194: fun ChatViewModel.toggleMute(chatId: String, isMuted: Boolean) {

L203: fun ChatViewModel.toggleBlock(chatId: String, isBlocked: Boolean) {

L212: fun ChatViewModel.deleteChat(chatId: String) {

L216: fun ChatViewModel.clearChatMessages(chatId: String) {

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/ChatViewModelMessageExtensions.kt

```

L9: fun ChatViewModel.startNewChat(userId: String, userName: String) {

L54: fun ChatViewModel.startNewGroup(participantIds: List<String>, groupName: String) {

L77: fun ChatViewModel.startAiChat() {

L81: fun ChatViewModel.sendMessage(chatId: String, text: String, type: String = "TEXT", senderId: String = currentUserId) {

L130: private fun ChatViewModel.handleAiChatResponse(chatId: String, userMessage: String) {

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/ContactSelectorViewModel.kt

```

L13: data class ContactSelectorState(

L21: class ContactSelectorViewModel @Inject constructor(

L33: private fun loadLocalContacts() {

L41: fun searchByFriendCode(code: String, excludedUserId: String? = null) {

L93: fun clearSearchResult() {

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/components/ChatItem.kt

```

L24: fun AnimatedChatItem(

L49: fun ChatItem(

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/components/MessageBubble.kt

```

L43: fun AnimatedMessageItem(

L82: fun MessageBubble(

L177: private fun AIHeader(isAiLoading: Boolean) {

L210: private fun SenderAvatar(avatarUrl: String?, onClick: () -> Unit) {

L240: private fun SenderNameLabel(name: String, onClick: () -> Unit) {

L253: private fun MessageContent(

L324: fun MessageBubblePreview() {

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/components/EmptyConnectView.kt

```

L15: fun EmptyConnectView(modifier: Modifier = Modifier) {

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/components/ChatDetailDialogs.kt

```

L23: fun SessionPickerSheet(

L85: fun CollectionPickerSheet(

L152: fun ChatDetailConfirmDialogs(

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/components/SharedLibraryView.kt

```

L27: fun SharedLibraryView(

L94: fun CollectionsTabContent(

L151: fun SessionsTabContent(

L183: fun LibraryEmptyState(message: String) {

L199: fun LibraryHeader(title: String, description: String) {

L207: fun SharedSessionCard(title: String, timestamp: Long, onJoin: () -> Unit) {

L231: fun PendingRequestsSection(accessRequests: List<Map<String, Any>>, onGrantAccess: (String, String) -> Unit) {

L245: fun SharedCollectionCard(

L375: fun AccessRequestItem(

L412: fun SharedLibraryViewPreview() {

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/components/ChatDetailHelpers.kt

```

L9: fun accessStateLabel(state: AppAccessState): String = when (state) {

L17: fun formatDateRelatively(timestamp: Long): String {

L28: private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {

L33: private fun isYesterday(now: Calendar, date: Calendar): Boolean {

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/components/GuestConnectView.kt

```

L17: fun GuestConnectView(modifier: Modifier = Modifier) {

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/components/ChatDetailComponents.kt

```

L30: fun DateHeader(date: String) {

L53: fun SystemMessageItem(text: String) {

L76: fun ChatDetailTopBar(

L213: fun ChatDetailBottomBar(

```


## app/src/main/java/com/algorithmx/q_base/ui/chat/components/SpecialBubbleContents.kt

```

L23: fun CollectionBubbleContent(

L64: fun SessionBubbleContent(payload: String, onJoin: (String) -> Unit, isMine: Boolean) {

L94: fun FileTransferBubbleContent(

L144: fun DecryptionErrorContent(status: String, isMine: Boolean, onDeleteChat: () -> Unit) {

L183: fun MessageTimestampAndStatus(timeString: String, isMine: Boolean, status: String) {

```


## app/src/main/java/com/algorithmx/q_base/ui/auth/AuthViewModel.kt

```

L15: data class AuthState(

L24: class AuthViewModel @Inject constructor(

L32: fun signIn(email: String, pass: String) {

L54: fun signUp(email: String, pass: String, username: String, photoUrl: String? = null) {

L76: fun signInWithGoogle(activity: androidx.activity.ComponentActivity) {

L93: fun onGoogleSignInSuccess(user: AppwriteUser) {

L105: fun clearError() {

```


## app/src/main/java/com/algorithmx/q_base/ui/auth/SignupScreen.kt

```

L38: fun SignupScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/auth/LoginScreen.kt

```

L35: fun LoginScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/ai/AiGenerationScreen.kt

```

L20: fun AiGenerationScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/ai/AiViewModel.kt

```

L16: sealed class AiUiState {

L17: object Idle : AiUiState()

L18: object Loading : AiUiState()

L19: data class Success(val message: String) : AiUiState()

L20: data class Error(val message: String) : AiUiState()

L24: class AiViewModel @Inject constructor(

L37: fun generateCollection(

L72: fun promoteResponse(targetCollectionId: String? = null, targetCollectionName: String? = null) {

L86: fun resetState() {

```


## app/src/main/java/com/algorithmx/q_base/ui/home/HomeScreen.kt

```

L40: fun HomeScreen(

L195: fun QuickActionCard(

L230: fun HomeCategoryCard(

L303: fun AnimatedHomeItem(

L324: fun PinnedQuestionItem(

L371: fun EmptyHomeView(onNavigate: () -> Unit) {

```


## app/src/main/java/com/algorithmx/q_base/ui/home/HomeViewModel.kt

```

L20: class HomeViewModel @Inject constructor(

```


## app/src/main/java/com/algorithmx/q_base/ui/content_import/ExtractionWizardSecondScreen.kt

```

L24: fun ExtractionWizardSecondScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/content_import/QuestionEditorScreen.kt

```

L24: fun QuestionEditorScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/content_import/CommonWizardThirdScreen.kt

```

L26: fun WaitingView(message: String) {

```


## app/src/main/java/com/algorithmx/q_base/ui/content_import/ManualBuilderViewModel.kt

```

L18: class ManualBuilderViewModel @Inject constructor(

L31: fun initialize(targetId: String?, name: String? = null) {

L61: private suspend fun createNewCollectionAndSet(name: String? = null) {

L77: private suspend fun createNewSet(collectionId: String, title: String) {

L90: private fun observeQuestions(setId: String) {

L96: fun getTargetSetId(): String? = _targetSetId.value

L98: private fun getCurrentDate(): String {

```


## app/src/main/java/com/algorithmx/q_base/ui/content_import/CommonWizardSecondScreen.kt

```

L17: fun ImportConfigView(onProceed: (List<String>, String) -> Unit) {

L82: fun GenerateConfigView(onProceed: (ExtractionConfigData) -> Unit) {

```


## app/src/main/java/com/algorithmx/q_base/ui/content_import/ImportViewModel.kt

```

L23: data class ExtractionConfigData(

L43: sealed class ImportStep {

L44: data object NameAndDestination : ImportStep()

L45: data object ChooseMethod : ImportStep()

L46: data object MediaInput : ImportStep()

L47: data class Configure(val mode: String) : ImportStep() // "IMPORT" or "GENERATE"

L48: data class Processing(val message: String = "AI is structuring your questions...") : ImportStep()

L49: data class Review(val questionCount: Int, val responseId: String) : ImportStep()

L50: data class Error(val message: String) : ImportStep()

L53: data object ExtractionIngest : ImportStep()

L54: data class ExtractionOverview(val responseId: String, val response: com.algorithmx.q_base.core_ai.brain.models.AiCollectionResponse) : ImportStep()

L57: data class Extracting(val source: String) : ImportStep()

L58: data class Editing(val extractedText: String) : ImportStep()

L59: data class Config(val extractedText: String, val targetId: String? = null) : ImportStep()

L60: data class Preview(val responseId: String, val response: com.algorithmx.q_base.core_ai.brain.models.AiCollectionResponse) : ImportStep()

L61: data class Complete(val message: String) : ImportStep()

L65: class ImportViewModel @Inject constructor(

L116: fun togglePaperType(type: String) {

L142: fun setInitialSource(source: String?, targetId: String? = null) {

L153: private fun loadCollections() {

L161: fun navigateTo(step: ImportStep) {

L171: fun selectCategory(categoryId: String?) {

L175: fun updateNewCollectionName(name: String) {

L179: fun updateNewCollectionDescription(desc: String) {

L183: fun updateCustomInstructions(instructions: String) {

L187: fun selectMethod(method: String) {

L191: fun onImagePicked(uri: Uri) {

L199: fun onPdfPicked(uri: Uri) {

L207: fun onRawTextUpdated(text: String) {

L211: private fun handleExtractionResult(result: Result<String>) {

L220: fun startDirectImport(types: List<String>, instructions: String) {

L238: fun startAiGeneration(config: ExtractionConfigData) {

L256: private fun handleGenerationResult(result: Result<String>) {

L274: fun promoteResponse(responseId: String, customName: String? = null, onFinished: (String, String?) -> Unit) {

L295: fun addPdf(uri: Uri) {

L321: fun addOcr(uri: Uri) {

L346: fun addClipboard(text: String) {

L359: fun removeDoc(id: String) {

L364: fun startPaperExtraction() {

L401: fun handleRetry() {

L409: fun reset() {

L435: fun currentStepNumber(): Int = when (val s = _uiState.value) {

L453: fun totalSteps(): Int = 5

```


## app/src/main/java/com/algorithmx/q_base/ui/content_import/ExtractionWizardThirdScreen.kt

```

L21: fun ExtractionWizardThirdScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/content_import/ManualBuilderScreen.kt

```

L29: fun ManualBuilderScreen(

L131: fun ManualQuestionItem(

```


## app/src/main/java/com/algorithmx/q_base/ui/content_import/ImportWizardScreen.kt

```

L20: fun ImportWizardScreen(

```


## app/src/main/java/com/algorithmx/q_base/ui/content_import/ExtractionWizardFirstScreen.kt

```

L22: data class ExtractedDocumentCard(

L31: fun ExtractionWizardFirstScreen(

L243: fun ExtractedDocItem(

```


## app/src/main/java/com/algorithmx/q_base/ui/content_import/QuestionEditorViewModel.kt

```

L15: data class QuestionEditorState(

L29: class QuestionEditorViewModel @Inject constructor(

L41: fun init(questionId: String?, setId: String) {

L70: fun updateStem(stem: String) {

L74: fun updateOption(letter: String, text: String) {

L81: fun updateCorrectAnswer(answer: String) {

L85: fun updateExplanation(explanation: String) {

L89: fun updateReferences(refs: String) {

L93: fun getAiAssist() {

L107: fun generateAiExplanation() {

L129: fun applyAiExplanation() {

L137: fun discardAiExplanation() {

L141: fun clearAiSuggestions() {

L145: fun saveQuestion() {

```


## app/src/main/java/com/algorithmx/q_base/ui/content_import/CommonWizardFourthScreen.kt

```

L26: fun ReviewView(

L227: fun ErrorView(message: String, onRetry: () -> Unit) {

```


## app/src/main/java/com/algorithmx/q_base/ui/content_import/ExtractionWizardFourthScreen.kt

```

L27: fun ExtractionWizardFourthScreen(

L281: fun CountBadge(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {

```


## app/src/main/java/com/algorithmx/q_base/ui/content_import/CommonWizardFirstScreen.kt

```

L26: fun CommonWizardFirstScreen(

L214: fun SmallSourceButton(icon: ImageVector, title: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {

```


## app/src/main/java/com/algorithmx/q_base/ui/components/question/QuestionHeader.kt

```

L35: fun QuestionHeader(

L246: fun QuestionHeaderPreview() {

```


## app/src/main/java/com/algorithmx/q_base/ui/components/question/QuestionViewer.kt

```

L20: fun QuestionViewer(

L151: fun QuestionViewerPreview() {

```


## app/src/main/java/com/algorithmx/q_base/ui/components/question/TrueFalseToggle.kt

```

L15: fun TrueFalseToggle(

```


## app/src/main/java/com/algorithmx/q_base/ui/components/question/OptionsList.kt

```

L29: fun OptionsList(

L341: fun OptionsListPreview() {

```


## app/src/main/java/com/algorithmx/q_base/ui/components/reusable/ReportDialog.kt

```

L13: fun ReportDialog(

```


## app/src/main/java/com/algorithmx/q_base/ui/components/reusable/ProfileIconButton.kt

```

L24: fun ProfileIconButton(

```


## app/src/main/java/com/algorithmx/q_base/ui/components/reusable/AiConfigSelector.kt

```

L16: fun AiConfigSelector(

```


## app/src/main/java/com/algorithmx/q_base/ui/components/reusable/CommonComponents.kt

```

L32: fun UnifiedTopAppBar(

L125: private fun AppAccessStateBadge(appAccessState: AppAccessState) {

L150: fun SessionCard(

L196: fun SessionListItem(

L230: fun SectionHeader(

L272: fun SessionCardPreview() {

L289: fun SessionListItemPreview() {

L308: fun SectionHeaderPreview() {

```


## app/src/main/java/com/algorithmx/q_base/core_ai/brain/BrainConfigProvider.kt

```

L5: interface BrainConfigProvider {

L6: suspend fun getApiKey(provider: BrainProvider): String

```


## app/src/main/java/com/algorithmx/q_base/core_ai/brain/BrainDataStoreManager.kt

```

L24: class BrainDataStoreManager @Inject constructor(

L29: private object PreferencesKeys {

L73: suspend fun markSeedAsApplied() {

L79: suspend fun resetSeedFlag() {

L85: suspend fun saveEngineConfiguration(

L99: suspend fun incrementUsageStats(tokens: Int) {

L108: suspend fun saveThemeMode(mode: String) {

L114: suspend fun saveNotificationsEnabled(enabled: Boolean) {

L120: suspend fun saveTaskConfig(task: BrainTask, config: TaskConfig) {

L135: suspend fun setMasterAiFreeze(freeze: Boolean) {

```


## app/src/main/java/com/algorithmx/q_base/core_ai/brain/AiUsageLogger.kt

```

L6: interface AiUsageLogger {

L7: suspend fun logUsage(

```


## app/src/main/java/com/algorithmx/q_base/core_ai/brain/CommonAiService.kt

```

L13: class CommonAiService @Inject constructor(

L22: suspend fun generateNoteStructure(

L58: suspend fun generateBlocksForTopic(

L73: private fun constructGenerationPrompt(

L104: private fun extractJsonFromResponse(response: String): String {

```


## app/src/main/java/com/algorithmx/q_base/core_ai/brain/AiBrainManager.kt

```

L24: class AiBrainManager @Inject constructor(

L40: private suspend fun buildBrain(modelName: String): AiBrain? {

L67: suspend fun askBrain(task: BrainTask, prompt: String): Result<String> {

L131: suspend fun streamFromBrain(task: BrainTask, prompt: String): Flow<String> {

```


## app/src/main/java/com/algorithmx/q_base/core_ai/brain/models/BrainTask.kt

```

L3: enum class BrainTask(val displayName: String) {

```


## app/src/main/java/com/algorithmx/q_base/core_ai/brain/models/AiResponseModels.kt

```

L6: data class AiQuestion(

L15: data class AiOption(

L22: data class AiAnswer(

L29: data class AiCollectionResponse(

```


## app/src/main/java/com/algorithmx/q_base/core_ai/brain/models/AiModels.kt

```

L6: enum class BlockType {

L16: data class AiGeneratedBlock(

L22: data class AiGeneratedTab(

L27: data class NoteContext(

```


## app/src/main/java/com/algorithmx/q_base/core_ai/brain/models/BrainModels.kt

```

L10: data class BrainConfig(

L17: data class TaskConfig(

L24: data class StoredBrainConfig(

```


## app/src/main/java/com/algorithmx/q_base/core_ai/brain/di/AiCoreModule.kt

```

L16: object AiCoreModule {

L20: fun provideAiBrainManager(

L30: fun provideCommonAiService(aiBrainManager: AiBrainManager): CommonAiService {

```

