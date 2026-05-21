package com.algorithmx_q_base.feature.chat.presentation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.core.UserDao
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.data.auth.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ContactSelectorState(
    val localContacts: List<UserEntity> = emptyList(),
    val searchResult: UserEntity? = null,
    val isSearching: Boolean = false,
    val searchError: String? = null
)

@HiltViewModel
class ContactSelectorViewModel @Inject constructor(
    private val userDao: UserDao,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ContactSelectorState())
    val state = _state.asStateFlow()

    init {
        loadLocalContacts()
    }

    private fun loadLocalContacts() {
        viewModelScope.launch {
            userDao.getAllUsers().collect { users ->
                _state.update { it.copy(localContacts = users) }
            }
        }
    }

    fun searchByFriendCode(code: String, excludedUserId: String? = null) {
        // Normalize: trim whitespace and uppercase for consistent matching
        val normalizedCode = code.trim().uppercase()
        if (normalizedCode.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, searchError = null, searchResult = null) }

            // Check local Room cache first (trim + uppercase normalized comparison)
            val localUser = _state.value.localContacts.find {
                it.friendCode.trim().uppercase() == normalizedCode
            }
            if (localUser != null) {
                if (localUser.userId == excludedUserId) {
                    _state.update { it.copy(isSearching = false, searchError = "You can't start a chat with yourself") }
                } else {
                    _state.update { it.copy(searchResult = localUser, isSearching = false) }
                }
                return@launch
            }

            // Not found locally — query Appwrite
            profileRepository.findUserByFriendCode(normalizedCode)
                .onSuccess { profile ->
                    if (profile != null) {
                        if (profile.userId == excludedUserId) {
                            _state.update { it.copy(isSearching = false, searchError = "You can't start a chat with yourself") }
                            return@onSuccess
                        }
                        val userEntity = UserEntity(
                            userId = profile.userId,
                            displayName = profile.displayName,
                            email = profile.email.ifBlank { null },
                            profilePictureUrl = profile.profilePictureUrl,
                            friendCode = profile.friendCode,
                            publicKey = profile.publicKey,
                            isBanned = profile.isBanned,
                            isPhotoVisible = profile.isPhotoVisible
                        )
                        // Cache locally so subsequent searches hit Room instead of Appwrite
                        userDao.insertUser(userEntity)
                        _state.update { it.copy(searchResult = userEntity, isSearching = false) }
                    } else {
                        _state.update { it.copy(isSearching = false, searchError = "No user found with code \"$normalizedCode\"") }
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(isSearching = false, searchError = error.message ?: "Search failed") }
                }
        }
    }

    fun clearSearchResult() {
        _state.update { it.copy(searchResult = null, searchError = null) }
    }
}
