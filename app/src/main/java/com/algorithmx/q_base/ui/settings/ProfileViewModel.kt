package com.algorithmx.q_base.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.dao.UserDao
import com.algorithmx.q_base.data.dao.SessionDao
import com.algorithmx.q_base.data.dao.CollectionDao
import com.algorithmx.q_base.data.dao.QuestionDao
import com.algorithmx.q_base.data.entity.UserEntity
import com.algorithmx.q_base.data.model.UserProfile
import com.algorithmx.q_base.data.repository.ProfileRepository
import com.algorithmx.q_base.data.repository.AuthRepository
import com.algorithmx.q_base.data.repository.DataClearingRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi

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
    private val dataClearingRepository: DataClearingRepository
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val userState: StateFlow<UserEntity?> = authRepository.currentUser
        .flatMapLatest { user ->
            if (user != null) {
                userDao.getCurrentUser(user.uid)
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

    init {
        loadUser()
        loadStats()
    }

    private fun loadUser() {
        viewModelScope.launch {
            authRepository.currentUser.collectLatest { user ->
                if (user != null) {
                    profileRepository.syncUserProfile(user.uid)
                }
            }
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
            profileRepository.updateProfile(updatedProfile)
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
            profileRepository.updateProfile(updatedProfile)
        }
    }

    fun signOut(clearCollections: Boolean, onComplete: () -> Unit) {
        viewModelScope.launch {
            dataClearingRepository.clearAllData(clearCollections)
            authRepository.signOut()
            onComplete()
        }
    }
}
