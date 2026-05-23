package com.algorithmx.q_base.feature.explore.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import com.algorithmx.q_base.core.designsystem.components.reusable.UnifiedTopAppBar
import androidx.compose.material3.*
import androidx.compose.material3.rememberModalBottomSheetState
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.collections.QuestionSet
import com.algorithmx.q_base.feature.sessions.data.StudySession
import com.algorithmx.q_base.core.designsystem.components.reusable.ReportDialog
import com.algorithmx.q_base.core.designsystem.components.reusable.ProfileIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionOverviewScreen(
    collection: StudyCollection?,
    lastSession: StudySession?,
    sets: List<QuestionSet>,
    questionCount: Int = 0,
    onContinueSession: (String, Int) -> Unit,
    onExplore: (String) -> Unit,
    onStartSet: (String) -> Unit,
    onReportCollection: (String) -> Unit,
    onDeleteCollection: (StudyCollection) -> Unit = {},
    onProfileClick: () -> Unit,
    onBack: () -> Unit,
    currentUser: com.algorithmx.q_base.core.data.UserEntity? = null,
    viewModel: ExploreViewModel? = null
) {
    if (collection == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val scrollState = rememberScrollState()
    var showReportDialog by remember { mutableStateOf(false) }
    var showReportToGroupDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    val sourceGroupName by viewModel?.sourceGroupName?.collectAsState() ?: remember { mutableStateOf(null) }

    LaunchedEffect(viewModel) {
        viewModel?.actionFeedback?.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            UnifiedTopAppBar(
                title = collection.name,
                currentUser = currentUser,
                onProfileClick = onProfileClick,
                isLarge = false,
                titleCentered = true,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    var showMoreMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More Options")
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Report Collection") },
                            leadingIcon = { Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMoreMenu = false
                                showReportDialog = true
                            }
                        )
                        if (collection.sharedWithGroupId != null) {
                            DropdownMenuItem(
                                text = { Text("Report to Group") },
                                leadingIcon = { Icon(Icons.Rounded.Groups, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showMoreMenu = false
                                    showReportToGroupDialog = true
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Delete Collection") },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMoreMenu = false
                                onDeleteCollection(collection)
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            val accessState = com.algorithmx.q_base.core.state.LocalAppAccessState.current
            if (accessState == com.algorithmx.q_base.core.state.AppAccessState.Online) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel?.askAiAboutCollection(collection) },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                    text = { Text("Ask AI") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp)
            ) {
                // Collection Info Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = collection.name,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.weight(1f)
                            )
                            if (collection.isShared) {
                                Surface(
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                    shape = CircleShape
                                ) {
                                    Text(
                                        "SHARED",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        }
                        
                        if (sourceGroupName != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Source: $sourceGroupName",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = collection.description ?: "No description provided.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Pick Up Where You Left Off Section
                        if (lastSession != null) {
                            Text(
                                "PICK UP WHERE YOU LEFT OFF",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.5.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onContinueSession(lastSession.sessionId, lastSession.lastQuestionIndex) },
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text("Continue: ${lastSession.title}", fontWeight = FontWeight.Bold)
                                    Text(
                                        "Question ${lastSession.lastQuestionIndex + 1}", 
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                            }
                        } else {
                            // Start New
                            Button(
                                onClick = { onExplore(collection.name) },
                                modifier = Modifier.fillMaxWidth().height(64.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Rounded.Explore, contentDescription = null)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Explore Collection", fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }

                // Stats Overview
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        title = "Sets",
                        value = sets.size.toString(),
                        icon = Icons.AutoMirrored.Rounded.ListAlt,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Questions",
                        value = questionCount.toString(),
                        icon = Icons.Rounded.Quiz,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                val isUserGroupAdmin by viewModel?.isUserGroupAdmin?.collectAsState() ?: remember { mutableStateOf(false) }
                if (collection.isShared && isUserGroupAdmin) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Rounded.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Access Settings (Admin Only)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Admin-Only Restrictions",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (collection.isAdminOnly) "Only admins can edit and share this collection" else "All group members can edit & share",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = collection.isAdminOnly,
                                    onCheckedChange = { isAdminOnly ->
                                        viewModel?.updateCollectionAdminOnly(collection.collectionId, isAdminOnly)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Question Sets List
                Text(
                    "Question Sets",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                sets.forEach { set ->
                    SetItem(
                        set = set,
                        onClick = { onStartSet(set.setId) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showReportDialog) {
        ReportDialog(
            itemType = "Collection",
            itemName = collection.name,
            onDismiss = { showReportDialog = false },
            onConfirm = { reason ->
                onReportCollection(reason)
                showReportDialog = false
            }
        )
    }

    if (showReportToGroupDialog) {
        ReportDialog(
            itemType = "Group Report",
            itemName = sourceGroupName ?: "Source Group",
            onDismiss = { showReportToGroupDialog = false },
            onConfirm = { reason ->
                viewModel?.reportCollectionToGroup(collection, reason)
                showReportToGroupDialog = false
            }
        )
    }

    // AI Response Content Bottom Sheet
    val collectionAiResponse by viewModel?.collectionAiResponse?.collectAsState() ?: remember { mutableStateOf(null) }
    val isCollectionAiLoading by viewModel?.isCollectionAiLoading?.collectAsState() ?: remember { mutableStateOf(false) }

    if (collectionAiResponse != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel?.clearCollectionAiResponse() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    Icon(
                        Icons.Rounded.AutoAwesome, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "AI Insights", 
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isCollectionAiLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MarkdownText(
                            markdown = collectionAiResponse!!,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp
                            ),
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { viewModel?.clearCollectionAiResponse() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Close")
                }
            }
        }
    }
}
