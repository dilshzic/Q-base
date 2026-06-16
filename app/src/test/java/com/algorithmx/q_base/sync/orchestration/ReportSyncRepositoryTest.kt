package com.algorithmx.q_base.sync.orchestration

import com.algorithmx.q_base.core.data.UserDao
import com.algorithmx.q_base.core.data.UserEntity
import com.algorithmx.q_base.core.data.auth.AuthRepository
import com.algorithmx.q_base.core.data.backend.CoreDatabase
import com.algorithmx.q_base.core.data.chat.ChatEntity
import com.algorithmx.q_base.core.data.chat.ChatLocalDataSource
import com.algorithmx.q_base.core.data.chat.ChatRemoteRepository
import com.algorithmx.q_base.core.data.chat.MessageEntity
import com.algorithmx.q_base.data.collections.Answer
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.data.collections.Question
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.data.collections.QuestionOption
import com.algorithmx.q_base.data.collections.QuestionSet
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.feature.sessions.data.SessionDao
import com.google.gson.Gson
import dagger.Lazy
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReportSyncRepositoryTest {

    private val databases: CoreDatabase = mockk()
    private val authRepository: AuthRepository = mockk()
    private val chatRemoteRepository: ChatRemoteRepository = mockk()
    private val sessionDao: SessionDao = mockk()
    private val collectionDao: CollectionDao = mockk()
    private val questionDao: QuestionDao = mockk()
    private val chatLocalDataSource: ChatLocalDataSource = mockk()
    private val messageSyncRepository: MessageSyncRepository = mockk()
    private val chatManagerRepository: ChatManagerRepository = mockk()
    private val userDao: UserDao = mockk()

    private val repository = ReportSyncRepository(
        databases = databases,
        authRepository = authRepository,
        chatRemoteRepository = chatRemoteRepository,
        sessionDao = sessionDao,
        collectionDao = Lazy { collectionDao },
        questionDao = Lazy { questionDao },
        chatLocalDataSource = Lazy { chatLocalDataSource },
        messageSyncRepository = Lazy { messageSyncRepository },
        chatManagerRepository = Lazy { chatManagerRepository },
        userDao = Lazy { userDao }
    )

    @Before
    fun setup() {
        mockkStatic("android.util.Log")
        every { android.util.Log.d(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>()) } returns 0
        every { android.util.Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { android.util.Log.w(any<String>(), any<String>()) } returns 0

        mockkStatic("com.algorithmx.q_base.sync.orchestration.MessageSyncOutgoingExtensionsKt")
        coEvery { any<MessageSyncRepository>().sendMessage(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `reportCollection should compile all sets and questions and create reported document`() = runTest {
        // Given
        val currentUserIdVal = "reporter_user_id"
        every { authRepository.currentUserId } returns currentUserIdVal

        val collection = StudyCollection(
            collectionId = "col_123",
            name = "Test Collection",
            sharedWithGroupId = "group_123"
        )
        val questionSet = QuestionSet(
            setId = "set_123",
            title = "Set Title",
            parentCollectionId = "col_123"
        )
        val question = Question(
            questionId = "q_123",
            collection = "Test Collection",
            category = "General",
            tags = "math",
            questionType = "SBA",
            stem = "What is 2+2?"
        )
        val option = QuestionOption(
            questionId = "q_123",
            optionLetter = "A",
            optionText = "4",
            optionExplanation = null
        )
        val answer = Answer(
            questionId = "q_123",
            correctAnswerString = "A",
            generalExplanation = "Simple math"
        )

        coEvery { collectionDao.getSetsByStudyCollectionIdOnce("col_123") } returns listOf(questionSet)
        coEvery { collectionDao.getQuestionsForSetOnce("set_123") } returns listOf(question)
        coEvery { questionDao.getOptionsForQuestionOnce("q_123") } returns listOf(option)
        coEvery { questionDao.getAnswerForQuestionOnce("q_123") } returns answer

        val slot = slot<Map<String, Any>>()
        coEvery {
            databases.createDocument(
                collectionId = "reported_collections",
                documentId = any(),
                data = capture(slot)
            )
        } returns Result.success(mockk())

        // When
        repository.reportCollection(collection, "Inappropriate content")

        // Then
        coVerify(exactly = 1) {
            databases.createDocument(
                collectionId = "reported_collections",
                documentId = any(),
                data = any()
            )
        }

        val capturedData = slot.captured
        assertEquals(currentUserIdVal, capturedData["reporterId"])
        assertEquals("Inappropriate content", capturedData["reason"])
        assertEquals("col_123", capturedData["collectionId"])

        val contentJsonStr = capturedData["contentJson"] as String
        val contentMap = Gson().fromJson(contentJsonStr, Map::class.java)
        
        // Assert collection and questions exist in contentJson
        assertTrue(contentMap.containsKey("collection"))
        assertTrue(contentMap.containsKey("questions"))

        val questions = contentMap["questions"] as List<*>
        assertEquals(1, questions.size)
        val questionEntry = questions[0] as Map<*, *>
        assertTrue(questionEntry.containsKey("question"))
        assertTrue(questionEntry.containsKey("options"))
        assertTrue(questionEntry.containsKey("answer"))
    }

    @Test
    fun `reportUser should find direct chat and retrieve 20 sample messages`() = runTest {
        // Given
        val currentUserIdVal = "reporter_user_id"
        every { authRepository.currentUserId } returns currentUserIdVal

        val reportedUser = UserEntity(
            userId = "reported_user_id",
            displayName = "Spammer",
            profilePictureUrl = null,
            friendCode = "SPAM123"
        )

        // Mock chats: reporter_user_id and reported_user_id
        val directChat = ChatEntity(
            chatId = "chat_direct",
            chatName = "Spammer",
            isGroup = false,
            participantIds = "reporter_user_id,reported_user_id",
            adminIds = listOf("reporter_user_id")
        )
        every { chatLocalDataSource.getAllChats() } returns flowOf(listOf(directChat))

        val messages = List(30) { index ->
            MessageEntity(
                messageId = "msg_$index",
                chatId = "chat_direct",
                senderId = "reported_user_id",
                payload = "Spam payload $index",
                type = "TEXT",
                timestamp = System.currentTimeMillis() + index
            )
        }
        every { chatLocalDataSource.getMessagesForChat("chat_direct") } returns flowOf(messages)

        val slot = slot<Map<String, Any>>()
        coEvery {
            databases.createDocument(
                collectionId = "reported_users",
                documentId = any(),
                data = capture(slot)
            )
        } returns Result.success(mockk())

        // Stub warning to user/admins
        coEvery { userDao.getUserById("reported_user_id") } returns reportedUser

        // When
        repository.reportUser(reportedUser, "Abuse")

        // Then
        coVerify(exactly = 1) {
            databases.createDocument(
                collectionId = "reported_users",
                documentId = any(),
                data = any()
            )
        }

        val capturedData = slot.captured
        assertEquals(currentUserIdVal, capturedData["reporterId"])
        assertEquals("reported_user_id", capturedData["reportedUserId"])

        val contentJsonStr = capturedData["contentJson"] as String
        val contentMap = Gson().fromJson(contentJsonStr, Map::class.java)

        assertTrue(contentMap.containsKey("user"))
        assertTrue(contentMap.containsKey("sampleMessages"))

        val sampleMessages = contentMap["sampleMessages"] as List<*>
        // Verify it takes last 20 messages
        assertEquals(20, sampleMessages.size)
        val firstSampleMsg = sampleMessages[0] as Map<*, *>
        // The first of the last 20 messages should be msg_10
        assertEquals("msg_10", firstSampleMsg["messageId"])
    }

    @Test
    fun `reportGroup should retrieve 20 sample messages and delegate to chatRemoteRepository`() = runTest {
        // Given
        val currentUserIdVal = "reporter_user_id"
        every { authRepository.currentUserId } returns currentUserIdVal

        val group = ChatEntity(
            chatId = "group_123",
            chatName = "Study Group",
            isGroup = true,
            participantIds = "reporter_user_id,other_user",
            adminIds = listOf("reporter_user_id")
        )

        val messages = List(25) { index ->
            MessageEntity(
                messageId = "msg_$index",
                chatId = "group_123",
                senderId = "other_user",
                payload = "Group payload $index",
                type = "TEXT",
                timestamp = System.currentTimeMillis() + index
            )
        }
        every { chatLocalDataSource.getMessagesForChat("group_123") } returns flowOf(messages)

        val capturedMessages = slot<List<MessageEntity>>()
        coEvery {
            chatRemoteRepository.reportGroup(
                group = group,
                reason = "Inappropriate chat",
                sampleMessages = capture(capturedMessages)
            )
        } returns Unit

        // When
        repository.reportGroup(group, "Inappropriate chat")

        // Then
        coVerify(exactly = 1) {
            chatRemoteRepository.reportGroup(group, "Inappropriate chat", any())
        }

        val sampleList = capturedMessages.captured
        assertEquals(20, sampleList.size)
        // Verify it takes last 20 messages
        assertEquals("msg_5", sampleList.first().messageId)
        assertEquals("msg_24", sampleList.last().messageId)
    }
}
