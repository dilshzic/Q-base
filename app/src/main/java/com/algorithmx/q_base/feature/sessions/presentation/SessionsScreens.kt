package com.algorithmx.q_base.feature.sessions.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.layout.Layout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.collections.Question
import com.algorithmx.q_base.feature.sessions.data.StudySession
import com.algorithmx.q_base.core.data.UserEntity
import com.algorithmx.q_base.feature.components.reusable.SectionHeader
import com.algorithmx.q_base.feature.components.reusable.ReportDialog
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsListScreen(
    sessions: List<StudySession>,
    collections: List<StudyCollection>,
    onSessionClick: (String) -> Unit,
    onFabClick: () -> Unit,
    viewModel: SessionsViewModel,
    onProfileClick: () -> Unit,
    onBack: () -> Unit,
    startWizard: Boolean = false
) {
    val scrollState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(scrollState)
    val currentUser by viewModel.currentUser.collectAsState()

    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedIds by viewModel.selectedSessionIds.collectAsState()

    var reportingSessionId by remember { mutableStateOf<String?>(null) }
    var reportingSessionTitle by remember { mutableStateOf("") }

    androidx.activity.compose.BackHandler(enabled = isSelectionMode) {
        viewModel.clearSessionSelection()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            com.algorithmx.q_base.ui.components.reusable.UnifiedTopAppBar(
                title = if (isSelectionMode) "${selectedIds.size} Selected" else "Sessions",
                subtitle = if (isSelectionMode) null else "Test your knowledge",
                currentUser = currentUser,
                onProfileClick = onProfileClick,
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.clearSessionSelection() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear Selection")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.deleteSelectedSessions() }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onFabClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Session", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(
                    title = "Past Performance", 
                    icon = Icons.Rounded.History
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (sessions.isEmpty()) {
                item {
                    EmptySessionsView()
                }
            } else {
                itemsIndexed(sessions) { index, session ->
                    val isSelected = selectedIds.contains(session.sessionId)
                    AnimatedSessionItem(index) {
                        SessionListItemExpressive(
                            session = session, 
                            isSelected = isSelected,
                            selectionMode = isSelectionMode,
                            onSessionClick = {
                                if (isSelectionMode) {
                                    viewModel.toggleSessionSelection(session.sessionId)
                                } else {
                                    onSessionClick(session.sessionId)
                                }
                            },
                            onLongClick = {
                                viewModel.toggleSessionSelection(session.sessionId)
                            },
                            onReportClick = {
                                reportingSessionId = session.sessionId
                                reportingSessionTitle = session.title.ifEmpty { "Practice Session" }
                            }
                        )
                    }
                }
            }
        }
    }

    reportingSessionId?.let { sessionId ->
        ReportDialog(
            itemType = "Session",
            itemName = reportingSessionTitle,
            onDismiss = { reportingSessionId = null },
            onConfirm = { reason ->
                viewModel.reportSession(sessionId, reason)
                reportingSessionId = null
            }
        )
    }
}

@Composable
fun AnimatedSessionItem(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { 50 }) + fadeIn(),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
fun CategoryChip(name: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.clickable { }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name, 
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Rounded.ChevronRight, 
                contentDescription = null, 
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SessionListItemExpressive(
    session: StudySession,
    isSelected: Boolean = false,
    selectionMode: Boolean = false,
    onSessionClick: (String) -> Unit,
    onLongClick: () -> Unit = {},
    onReportClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = { onSessionClick(session.sessionId) },
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        tonalElevation = if (isSelected) 4.dp else 1.dp,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val score = session.scoreAchieved
            val scoreColor = if (score >= 70) Color(0xFF4CAF50) else if (score >= 40) Color(0xFFFF9800) else Color(0xFFF44336)
            
            Box(contentAlignment = Alignment.Center) {
                if (selectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onSessionClick(session.sessionId) },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                } else {
                    CircularProgressIndicator(
                        progress = { score.toFloat() / 100f },
                        modifier = Modifier.size(48.dp),
                        color = scoreColor,
                        strokeWidth = 4.dp,
                        trackColor = scoreColor.copy(alpha = 0.1f),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text(
                        text = "${score.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title.ifEmpty { "Practice Session" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Timer, 
                        contentDescription = null, 
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(session.createdTimestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            if (!selectionMode) {
                IconButton(onClick = onReportClick) {
                    Icon(
                        Icons.Rounded.Flag, 
                        contentDescription = "Report",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Icon(
                Icons.Rounded.ChevronRight, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
fun EmptySessionsView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Rounded.History, 
                    contentDescription = null, 
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No sessions yet", 
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Complete a quiz to see your progress here.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}