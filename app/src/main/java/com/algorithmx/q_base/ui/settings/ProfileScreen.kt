package com.algorithmx.q_base.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.data.core.UserEntity
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val user by viewModel.userState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var clearCollections by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out?") },
            text = {
                Column {
                    Text("Are you sure you want to log out? Chats and session history will be cleared from this device.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { clearCollections = !clearCollections }
                    ) {
                        Checkbox(
                            checked = clearCollections,
                            onCheckedChange = { clearCollections = it }
                        )
                        Text("Delete all collections and question content", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.signOut(clearCollections) {
                            onLoggedOut()
                        }
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Profile Picture
            Box(contentAlignment = Alignment.BottomEnd) {
                if (user?.profilePictureUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(user?.profilePictureUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(60.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
            
            var isEditingName by remember { mutableStateOf(false) }
            var editedName by remember { mutableStateOf(user?.displayName ?: "") }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                if (isEditingName) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                viewModel.updateDisplayName(editedName)
                                isEditingName = false
                            }) {
                                Icon(androidx.compose.material.icons.Icons.Default.Check, contentDescription = "Save")
                            }
                        }
                    )
                } else {
                    Text(
                        text = user?.displayName ?: "Knowledge Seeker",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { 
                        editedName = user?.displayName ?: ""
                        isEditingName = true 
                    }) {
                        Icon(androidx.compose.material.icons.Icons.Default.Edit, contentDescription = "Edit Name", modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            user?.email?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            user?.intro?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Privacy Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "PRIVACY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Show profile photo to others", style = MaterialTheme.typography.bodyLarge)
                        Switch(
                            checked = user?.isPhotoVisible ?: true,
                            onCheckedChange = { viewModel.togglePhotoVisibility(it) }
                        )
                    }
                }
            }

            // Statistics Grid
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        label = "User Created",
                        value = stats.userCreatedQuestions.toString(),
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                    StatCard(
                        label = "Shared",
                        value = stats.sharedQuestions.toString(),
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            // Friend Code Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "FRIEND CODE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable {
                                user?.friendCode?.let {
                                    clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(it))
                                    android.widget.Toast.makeText(context, "Code copied!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = user?.friendCode ?: "UOK-XXXX",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Default.ContentCopy, 
                            contentDescription = "Copy", 
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Log out button
            Button(
                onClick = {
                    showLogoutDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Out", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
