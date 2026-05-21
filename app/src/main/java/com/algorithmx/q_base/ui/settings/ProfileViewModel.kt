package com.algorithmx.q_base.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.core.UserDao
import com.algorithmx.q_base.data.sessions.SessionDao
import com.algorithmx.q_base.data.collections.CollectionDao
import com.algorithmx.q_base.data.collections.QuestionDao
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.data.auth.UserProfile
import com.algorithmx.q_base.data.auth.ProfileRepository
import com.algorithmx.q_base.data.auth.AuthRepository
import com.algorithmx.q_base.data.core.DataClearingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

import com.algorithmx.q_base.util.NetworkMonitor

data class UserStats(
    val totalQuestions: Int = 0,
    val userCreatedQuestions: Int = 0,
    val sharedQuestions: Int = 0,
    val sessionsCompleted: Int = 0
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userDao: UserDao,
    private val sessionDao: SessionDao,
    private val collectionDao: CollectionDao,
    private val questionDao: QuestionDao,
    private val authRepository: AuthRepository,
    private val profileRepository: ProfileRepository,
    private val dataClearingRepository: DataClearingRepository,
    private val actionQueueDao: com.algorithmx.q_base.data.sync.ActionQueueDao,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val userState: StateFlow<UserEntity?> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user != null) {
                userDao.getCurrentUser(user.uid).map { dbUser ->
                    dbUser ?: UserEntity(
                        userId = user.uid,
                        email = user.email ?: "",
                        displayName = user.displayName ?: "Learner",
                        profilePictureUrl = user.photoUrl?.toString(),
                        friendCode = "",
                        isBanned = false,
                        isPhotoVisible = true,
                        publicKey = null,
                        intro = null
                    )
                }
            } else {
                flowOf(null)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _stats = MutableStateFlow(UserStats())
    val stats = _stats.asStateFlow()

    private val _hasSecureBackup = MutableStateFlow(false)
    val hasSecureBackup = _hasSecureBackup.asStateFlow()

    init {
        loadUser()
        loadStats()
        loadBackupStatus()
    }

    private fun loadUser() {
        viewModelScope.launch {
            combine(
                authRepository.currentUser,
                networkMonitor.isOnline,
                authRepository.isBackendSessionValid
            ) { user, isOnline, isBackendValid ->
                if (user != null && isOnline && isBackendValid) {
                    profileRepository.syncUserProfile(user.uid)
                }
            }
            .collect()
        }
    }

    private fun loadBackupStatus() {
        viewModelScope.launch {
            combine(
                authRepository.currentUser,
                networkMonitor.isOnline,
                authRepository.isBackendSessionValid
            ) { user, isOnline, isBackendValid ->
                if (user != null && isOnline && isBackendValid) {
                    _hasSecureBackup.value = profileRepository.checkHasSecureBackup(user.uid)
                }
            }
            .collect()
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            combine(
                questionDao.getQuestionCountFlow(),
                questionDao.getUserCreatedQuestionCount(),
                questionDao.getSharedQuestionCount(),
                sessionDao.getAllSessions()
            ) { total, userCreated, shared, sessions ->
                UserStats(
                    totalQuestions = total,
                    userCreatedQuestions = userCreated,
                    sharedQuestions = shared,
                    sessionsCompleted = sessions.count { it.isCompleted }
                )
            }.collect {
                _stats.value = it
            }
        }
    }

    private suspend fun tryUpdateProfile(updatedProfile: UserProfile) {
        val result = profileRepository.updateProfile(updatedProfile)
        if (result.isFailure) {
            val payloadJson = org.json.JSONObject().apply {
                put("userId", updatedProfile.userId)
                put("email", updatedProfile.email)
                put("displayName", updatedProfile.displayName)
                put("profilePictureUrl", updatedProfile.profilePictureUrl ?: org.json.JSONObject.NULL)
                put("friendCode", updatedProfile.friendCode)
                put("intro", updatedProfile.intro)
                put("publicKey", updatedProfile.publicKey ?: org.json.JSONObject.NULL)
                put("isBanned", updatedProfile.isBanned)
                put("isPhotoVisible", updatedProfile.isPhotoVisible)
            }.toString()

            actionQueueDao.insertAction(
                com.algorithmx.q_base.data.sync.OfflineActionEntity(
                    actionType = "UPDATE_PROFILE",
                    payloadJson = payloadJson
                )
            )
        }
    }

    fun updateDisplayName(newName: String) {
        val current = userState.value ?: return
        viewModelScope.launch {
            val updatedProfile = UserProfile(
                userId = current.userId,
                email = current.email ?: "",
                displayName = newName,
                profilePictureUrl = current.profilePictureUrl,
                friendCode = current.friendCode,
                intro = current.intro ?: "",
                publicKey = current.publicKey,
                isBanned = current.isBanned,
                isPhotoVisible = current.isPhotoVisible
            )
            tryUpdateProfile(updatedProfile)
        }
    }

    fun updateIntro(newIntro: String) {
        val current = userState.value ?: return
        viewModelScope.launch {
            val updatedProfile = UserProfile(
                userId = current.userId,
                email = current.email ?: "",
                displayName = current.displayName,
                profilePictureUrl = current.profilePictureUrl,
                friendCode = current.friendCode,
                intro = newIntro,
                publicKey = current.publicKey,
                isBanned = current.isBanned,
                isPhotoVisible = current.isPhotoVisible
            )
            tryUpdateProfile(updatedProfile)
        }
    }

    fun updateProfilePictureUrl(url: String) {
        val current = userState.value ?: return
        viewModelScope.launch {
            val updatedProfile = UserProfile(
                userId = current.userId,
                email = current.email ?: "",
                displayName = current.displayName,
                profilePictureUrl = url,
                friendCode = current.friendCode,
                intro = current.intro ?: "",
                publicKey = current.publicKey,
                isBanned = current.isBanned,
                isPhotoVisible = current.isPhotoVisible
            )
            tryUpdateProfile(updatedProfile)
        }
    }

    fun togglePhotoVisibility(isVisible: Boolean) {
        val current = userState.value ?: return
        viewModelScope.launch {
            val updatedProfile = UserProfile(
                userId = current.userId,
                email = current.email ?: "",
                displayName = current.displayName,
                profilePictureUrl = current.profilePictureUrl,
                friendCode = current.friendCode,
                intro = current.intro ?: "",
                publicKey = current.publicKey,
                isBanned = current.isBanned,
                isPhotoVisible = isVisible
            )
            tryUpdateProfile(updatedProfile)
        }
    }

    fun signOut(clearCollections: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            // Sign out first — this nulls currentUser which cancels active sync jobs
            authRepository.signOut()
            // Brief delay to allow sync cancellation to propagate
            kotlinx.coroutines.delay(100)
            // Now safe to clear local data without race conditions
            dataClearingRepository.clearAllData(clearCollections)
            onComplete()
        }
    }
}
