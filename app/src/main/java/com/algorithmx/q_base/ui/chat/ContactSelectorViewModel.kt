package com.algorithmx.q_base.ui.chat

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

    fun searchByFriendCode(code: String) {
        if (code.isBlank()) return
        
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, searchError = null, searchResult = null) }
            
            // Check local first
            val localUser = _state.value.localContacts.find { it.friendCode.equals(code, ignoreCase = true) }
            if (localUser != null) {
                _state.update { it.copy(searchResult = localUser, isSearching = false) }
                return@launch
            }

            // If not found locally, check Firestore
            profileRepository.findUserByFriendCode(code)
                .onSuccess { profile ->
                    if (profile != null) {
                        val userEntity = UserEntity(
                            userId = profile.userId,
                            displayName = profile.displayName,
                            profilePictureUrl = profile.profilePictureUrl,
                            friendCode = profile.friendCode
                        )
                        _state.update { it.copy(searchResult = userEntity, isSearching = false) }
                    } else {
                        _state.update { it.copy(isSearching = false, searchError = "User not found") }
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
