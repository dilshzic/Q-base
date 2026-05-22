package com.algorithmx.q_base.feature.chat.presentation
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.algorithmx.q_base.core.data.UserEntity
import com.algorithmx.q_base.core.designsystem.components.reusable.UnifiedTopAppBar
import com.algorithmx.q_base.ui.state.AppAccessState
import com.algorithmx.q_base.ui.state.LocalAppAccessState
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSelector(
    onUserSelected: (UserEntity) -> Unit = {},
    onUsersSelected: (List<UserEntity>) -> Unit = {},
    multiSelectMode: Boolean = false,
    onBack: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
    onNavigateToNewGroup: (() -> Unit)? = null,
    currentUser: UserEntity? = null,
    titleCentered: Boolean = false,
    modifier: Modifier = Modifier,
    viewModel: ContactSelectorViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var nameQuery by remember { mutableStateOf("") }
    var friendCodeQuery by remember { mutableStateOf("") }
    val selectedUsers = remember { mutableStateOf(setOf<UserEntity>()) }
    val excludedUserId = currentUser?.userId
    
    val context = LocalContext.current
    val appAccessState = LocalAppAccessState.current
    val isOffline = appAccessState == AppAccessState.SignedInOffline || appAccessState == AppAccessState.OfflineGuest

    val filteredContacts = remember(nameQuery, state.localContacts, excludedUserId) {
        state.localContacts.filter { 
            it.userId != excludedUserId &&
                it.displayName.contains(nameQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            if (onBack != null) {
                UnifiedTopAppBar(
                    title = if (multiSelectMode) "Select Participants" else "New Chat",
                    currentUser = currentUser,
                    onProfileClick = onProfileClick ?: {},
                    isLarge = false,
                    titleCentered = titleCentered,
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(modifier = modifier.fillMaxWidth().padding(padding)) {
            if (isOffline) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Offline Mode: Some functions are restricted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (!multiSelectMode && onNavigateToNewGroup != null) {
                // New Group Option
                val containerColor = if (isOffline) {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                }
                Surface(
                    onClick = { if (!isOffline) onNavigateToNewGroup() },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = containerColor,
                    enabled = !isOffline
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = CircleShape,
                            color = if (isOffline) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.secondary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.Group,
                                    contentDescription = null,
                                    tint = if (isOffline) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "New Group",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isOffline) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                            )
                            if (isOffline) {
                                Text(
                                    "Unavailable offline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // Friend Code Search Section (Distinct)
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOffline) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Search by Friend Code",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isOffline) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = friendCodeQuery,
                            onValueChange = { friendCodeQuery = it.uppercase() },
                            modifier = Modifier.weight(1f),
                            enabled = !isOffline,
                            placeholder = { Text(if (isOffline) "Unavailable offline" else "e.g. QBS-A1B2-C3D4") },
                            leadingIcon = { Icon(Icons.Rounded.QrCodeScanner, contentDescription = null) },
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.searchByFriendCode(friendCodeQuery, excludedUserId) },
                            enabled = friendCodeQuery.isNotBlank() && !state.isSearching && !isOffline,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (state.isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Find")
                            }
                        }
                    }
                    
                    // Search Result from Firestore
                    state.searchResult?.takeIf { it.userId != excludedUserId }?.let { user ->
                        Spacer(modifier = Modifier.height(12.dp))
                        UserItem(
                            user = user, 
                            isSearchResult = true,
                            onClick = { onUserSelected(user) }
                        )
                    }

                    state.searchError?.let { error ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Local Name Search
            OutlinedTextField(
                value = nameQuery,
                onValueChange = { nameQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search local contacts by name...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Suggested Contacts",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredContacts) { user ->
                    UserItem(
                        user = user, 
                        isSelected = selectedUsers.value.contains(user),
                        showCheckbox = multiSelectMode,
                        onClick = { 
                            if (multiSelectMode) {
                                if (selectedUsers.value.contains(user)) {
                                    selectedUsers.value = selectedUsers.value - user
                                } else {
                                    selectedUsers.value = selectedUsers.value + user
                                }
                            } else {
                                if (isOffline) {
                                    android.widget.Toast.makeText(context, "Cannot start new chats while offline", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    onUserSelected(user)
                                }
                            }
                        }
                    )
                }
            }

            if (multiSelectMode) {
                Button(
                    onClick = { onUsersSelected(selectedUsers.value.toList()) },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    enabled = selectedUsers.value.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Confirm Selection (${selectedUsers.value.size})")
                }
            }
        }
    }
}

@Composable
fun UserItem(
    user: UserEntity, 
    isSearchResult: Boolean = false,
    isSelected: Boolean = false,
    showCheckbox: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = if (isSearchResult) 0.dp else 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSearchResult) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
        border = if (isSearchResult) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (isSearchResult) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Person,
                        contentDescription = null,
                        tint = if (isSearchResult) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(user.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                user.friendCode.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            }
            if (isSearchResult) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    "New!", 
                    style = MaterialTheme.typography.labelSmall, 
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )
            } else if (showCheckbox) {
                Spacer(modifier = Modifier.weight(1f))
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() }
                )
            }
        }
    }
}