package com.algorithmx.q_base.ui.settings

import com.algorithmx.q_base.data.auth.AppwriteUser
import com.algorithmx.q_base.data.auth.AuthRepository
import com.algorithmx.q_base.data.auth.ProfileRepository
import com.algorithmx.q_base.data.auth.UserProfile
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.data.core.DataClearingRepository
import com.algorithmx.q_base.data.core.UserDao
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.data.sessions.SessionDao
import com.algorithmx.q_base.data.sessions.StudySession
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var viewModel: ProfileViewModel
    private val userDao: UserDao = mockk()
    private val sessionDao: SessionDao = mockk()
    private val collectionDao: CollectionDao = mockk()
    private val questionDao: QuestionDao = mockk()
    private val authRepository: AuthRepository = mockk()
    private val profileRepository: ProfileRepository = mockk()
    private val dataClearingRepository: DataClearingRepository = mockk()
    private val actionQueueDao: com.algorithmx.q_base.data.sync.ActionQueueDao = mockk()

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock initialization flows
        every { authRepository.currentUser } returns flowOf(null)
        every { questionDao.getQuestionCountFlow() } returns flowOf(0)
        every { questionDao.getUserCreatedQuestionCount() } returns flowOf(0)
        every { questionDao.getSharedQuestionCount() } returns flowOf(0)
        every { sessionDao.getAllSessions() } returns flowOf(emptyList<StudySession>())
        
        coEvery { profileRepository.checkHasSecureBackup(any()) } returns false
        coEvery { profileRepository.syncUserProfile(any()) } returns Unit

        viewModel = ProfileViewModel(
            userDao,
            sessionDao,
            collectionDao,
            questionDao,
            authRepository,
            profileRepository,
            dataClearingRepository,
            actionQueueDao
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `updateDisplayName should call profileRepository updateProfile`() = runTest {
        // Given
        val user = UserEntity(
            userId = "uid123",
            displayName = "Old Name",
            friendCode = "CODE123",
            profilePictureUrl = null
        )
        
        // We need to mock authRepository.currentUser to return a user so userState has a value
        every { authRepository.currentUser } returns flowOf(mockk<AppwriteUser> { every { uid } returns "uid123" })
        every { userDao.getCurrentUser("uid123") } returns flowOf(user)
        
        // Re-initialize to pick up the mocked flows
        viewModel = ProfileViewModel(
            userDao,
            sessionDao,
            collectionDao,
            questionDao,
            authRepository,
            profileRepository,
            dataClearingRepository,
            actionQueueDao
        )

        // Ensure userState is collected so it updates
        val job = backgroundScope.launch { viewModel.userState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { profileRepository.updateProfile(any()) } returns Result.success(Unit)

        // When
        viewModel.updateDisplayName("New Name")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify {
            profileRepository.updateProfile(match {
                it.displayName == "New Name" && it.userId == "uid123"
            })
        }
    }

    @Test
    fun `updateIntro should call profileRepository updateProfile`() = runTest {
        // Given
        val user = UserEntity(
            userId = "uid123",
            displayName = "Old Name",
            friendCode = "CODE123",
            profilePictureUrl = null,
            intro = "Old Intro"
        )
        
        every { authRepository.currentUser } returns flowOf(mockk<AppwriteUser> { every { uid } returns "uid123" })
        every { userDao.getCurrentUser("uid123") } returns flowOf(user)
        
        viewModel = ProfileViewModel(
            userDao,
            sessionDao,
            collectionDao,
            questionDao,
            authRepository,
            profileRepository,
            dataClearingRepository,
            actionQueueDao
        )

        backgroundScope.launch { viewModel.userState.collect {} }
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { profileRepository.updateProfile(any()) } returns Result.success(Unit)

        // When
        viewModel.updateIntro("New Intro")
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        coVerify {
            profileRepository.updateProfile(match {
                it.intro == "New Intro" && it.userId == "uid123"
            })
        }
    }
}