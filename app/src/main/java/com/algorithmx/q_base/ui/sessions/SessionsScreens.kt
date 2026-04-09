package com.algorithmx.q_base.ui.sessions

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
import com.algorithmx.q_base.data.sessions.StudySession
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.ui.components.SectionHeader
import com.algorithmx.q_base.ui.components.ReportDialog
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
    var showCreateSheet by remember { mutableStateOf(startWizard) }

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
            LargeTopAppBar(
                title = { 
                    Column {
                        Text(
                            if (isSelectionMode) "${selectedIds.size} Selected" else "Sessions", 
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (!isSelectionMode) {
                            Text(
                                "Your Learning Journey",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.clearSessionSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear Selection")
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { viewModel.deleteSelectedSessions() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        com.algorithmx.q_base.ui.components.ProfileIconButton(
                            user = currentUser,
                            onClick = onProfileClick
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
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
                SectionHeader(
                    title = "Categories"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    items(collections) { category ->
                        CategoryChip(category.name)
                    }
                }
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

    if (showCreateSheet) {
        NewSessionWizard(
            viewModel = viewModel,
            collections = collections,
            onDismiss = { 
                showCreateSheet = false
                viewModel.resetWizard()
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
                Icons.Default.ChevronRight, 
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
                        Icons.Default.Flag, 
                        contentDescription = "Report",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Icon(
                Icons.Default.ChevronRight, 
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSessionWizard(
    viewModel: SessionsViewModel,
    collections: List<StudyCollection>,
    onDismiss: () -> Unit
) {
    val step by viewModel.wizardStep.collectAsState()
    val selectedCollection by viewModel.selectedCollection.collectAsState()
    val availableQuestions by viewModel.availableQuestions.collectAsState()
    val selectedIds by viewModel.selectedQuestionIds.collectAsState()
    val order by viewModel.sessionOrder.collectAsState()
    val timingType by viewModel.timingType.collectAsState()
    val timeLimitSeconds by viewModel.timeLimitSeconds.collectAsState()
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header with Back Button and Step Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    IconButton(onClick = { viewModel.setWizardStep(step - 1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    text = when(step) {
                        1 -> "Choose Subject"
                        2 -> "Select Questions"
                        3 -> "Configure Session"
                        else -> "New Session"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$step/3",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Box(modifier = Modifier.weight(1f, fill = false).heightIn(min = 300.dp)) {
                when (step) {
                    1 -> CategoryStep(collections) { viewModel.selectCollection(it) }
                    2 -> QuestionSelectionStep(
                        questions = availableQuestions,
                        selectedIds = selectedIds,
                        lastRandomCount = viewModel.lastRandomCount.collectAsState().value,
                        onToggle = { viewModel.toggleQuestionSelection(it) },
                        onSelectAll = { viewModel.selectAllQuestions() },
                        onDeselectAll = { viewModel.deselectAllQuestions() },
                        onRandomSelect = { viewModel.selectRandomQuestions(it) },
                        onNext = { viewModel.setWizardStep(3) }
                    )
                    3 -> ConfigurationStep(
                        order = order,
                        timingType = timingType,
                        timeLimitSeconds = timeLimitSeconds,
                        onOrderChange = { viewModel.setOrder(it) },
                        onTimingChange = { viewModel.setTimingType(it) },
                        onTimeLimitChange = { viewModel.setTimeLimit(it) },
                        selectedCount = selectedIds.size,
                        onLaunch = { title -> viewModel.launchSession(title) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryStep(
    categories: List<StudyCollection>,
    onSelect: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories) { category ->
            Surface(
                onClick = { onSelect(category.name) },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.History, // Placeholder icon
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(category.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        if (!category.description.isNullOrEmpty()) {
                            Text(category.description!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
fun QuestionSelectionStep(
    questions: List<com.algorithmx.q_base.data.collections.Question>,
    selectedIds: Set<String>,
    lastRandomCount: Int?,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onRandomSelect: (Int) -> Unit,
    onNext: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                "Quick Select",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 25, 50).forEach { count ->
                    val isSelected = lastRandomCount == count
                    FilterChip(
                        selected = isSelected,
                        onClick = { onRandomSelect(count) },
                        label = { Text("$count") },
                        modifier = Modifier.weight(1f),
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(onClick = onSelectAll, modifier = Modifier.weight(1f)) {
                Text("Select All (${questions.size})")
            }
            TextButton(onClick = onDeselectAll, modifier = Modifier.weight(1f)) {
                Text("Clear Selection")
            }
        }
        
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        ) {
            items(questions) { question ->
                val isSelected = selectedIds.contains(question.questionId)
                Surface(
                    onClick = { onToggle(question.questionId) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = isSelected, onCheckedChange = { onToggle(question.questionId) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = question.stem ?: "Untitled Question",
                                maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = question.questionType ?: "SBA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(56.dp),
            enabled = selectedIds.isNotEmpty(),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Configure Session (${selectedIds.size})", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ConfigurationStep(
    order: String,
    timingType: String,
    timeLimitSeconds: Int,
    onOrderChange: (String) -> Unit,
    onTimingChange: (String) -> Unit,
    onTimeLimitChange: (Int) -> Unit,
    selectedCount: Int,
    onLaunch: (String) -> Unit
) {
    var title by remember { mutableStateOf("Exam Session ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(System.currentTimeMillis())}") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Session Title") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        )

        Column {
            Text("QUESTION ORDER", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConfigToggle(selected = order == "SEQUENTIAL", label = "Sequential", icon = Icons.Rounded.History, onClick = { onOrderChange("SEQUENTIAL") }, modifier = Modifier.weight(1f))
                ConfigToggle(selected = order == "RANDOM", label = "Random", icon = Icons.Rounded.AutoAwesome, onClick = { onOrderChange("RANDOM") }, modifier = Modifier.weight(1f))
            }
        }

        Column {
            Text("TIMING MODE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ConfigToggle(selected = timingType == "NONE", label = "No Timer", icon = null, onClick = { onTimingChange("NONE") }, modifier = Modifier.weight(1f))
                ConfigToggle(selected = timingType == "TOTAL", label = "Total Time", icon = Icons.Rounded.Timer, onClick = { onTimingChange("TOTAL") }, modifier = Modifier.weight(1f))
                ConfigToggle(selected = timingType == "PER_QUESTION", label = "Per Question", icon = Icons.Rounded.Timer, onClick = { onTimingChange("PER_QUESTION") }, modifier = Modifier.weight(1f))
            }
            
            if (timingType != "NONE") {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Time Limit: ${timeLimitSeconds / 60}m", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Slider(
                        value = timeLimitSeconds.toFloat(),
                        onValueChange = { onTimeLimitChange(it.toInt()) },
                        valueRange = 30f..3600f,
                        steps = 59,
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onLaunch(title) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = selectedCount > 0,
            shape = MaterialTheme.shapes.large
        ) {
            Text("Launch $selectedCount Questions", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ConfigToggle(
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
