package com.algorithmx.q_base.feature.sessions.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algorithmx.q_base.feature.sessions.data.StudySession
import com.algorithmx.q_base.data.collections.Question
import com.algorithmx.q_base.data.collections.QuestionOption
import com.algorithmx.q_base.data.collections.Answer
import com.algorithmx.q_base.feature.sessions.data.SessionAttempt
import com.algorithmx.q_base.core.data.UserEntity
import com.algorithmx.q_base.core.designsystem.components.question.QuestionViewer
import com.algorithmx.q_base.core.designsystem.components.reusable.UnifiedTopAppBar
import com.algorithmx.q_base.core.designsystem.theme.*
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.material.icons.rounded.AutoAwesome
import kotlinx.coroutines.launch
import com.algorithmx.q_base.feature.sessions.presentation.components.MasterNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSessionScreen(
    viewModel: ActiveSessionViewModel,
    onNavigateBack: () -> Unit,
    onViewResults: (String) -> Unit
) {
    val currentQuestion by viewModel.currentQuestion.collectAsStateWithLifecycle()
    val options by viewModel.currentOptions.collectAsStateWithLifecycle()
    val attempts by viewModel.attempts.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val timerText by viewModel.timerDisplay.collectAsStateWithLifecycle()
    val navDots by viewModel.navigatorDots.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val currentAnswer by viewModel.currentAnswer.collectAsStateWithLifecycle()
    val aiResponse by viewModel.aiResponse.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val isReadOnly by viewModel.isReadOnly.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    
    val pagerState = rememberPagerState(pageCount = { attempts.size })
    var showNavigator by remember { mutableStateOf(false) }
    var showReportSessionDialog by remember { mutableStateOf(false) }
    var showReportQuestionDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.actionFeedback.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvents.collect { event ->
            if (event is SessionNavEvent.NavigateToResults) {
                onViewResults(event.sessionId)
            }
        }
    }

    // Sync pagerState with viewModel.currentQuestionIndex
    LaunchedEffect(currentIndex) {
        if (pagerState.currentPage != currentIndex) {
            pagerState.animateScrollToPage(currentIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.navigateToQuestion(pagerState.currentPage)
    }
    
    val currentAttempt = attempts.getOrNull(currentIndex)
    val isCompleted = session?.isCompleted == true

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            UnifiedTopAppBar(
                title = session?.title ?: "Active Session",
                subtitle = if (isReadOnly) "Read-Only (Admin-Only)" else "Practice Progress",
                currentUser = currentUser,
                onProfileClick = { /* Navigate to profile? Or just icon */ },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showNavigator = true }) {
                        Icon(Icons.Rounded.GridView, contentDescription = "Navigator")
                    }
                    if (isCompleted) {
                        IconButton(onClick = { onViewResults(viewModel.getSessionId()) }) {
                            Icon(Icons.Rounded.Assessment, contentDescription = "View Results", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (!isCompleted && !isReadOnly) {
                        Button(
                            onClick = { viewModel.submitSession() },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text("FINISH", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { viewModel.navigateToQuestion(currentIndex - 1) },
                        enabled = currentIndex > 0,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PREV", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Button(
                        onClick = { viewModel.navigateToQuestion(currentIndex + 1) },
                        enabled = currentIndex < attempts.size - 1,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(56.dp)
                    ) {
                        Text("NEXT", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (isReadOnly) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Read-Only Access",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "This session is marked as Admin-Only. Non-admins cannot submit answers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Expressive Progress and Info Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = CircleShape
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCompleted) "Review" else timerText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Q ${currentIndex + 1}/${attempts.size}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.toggleFlag() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Flag, 
                            contentDescription = "Flag",
                            tint = if (currentAttempt?.attemptStatus == "FLAGGED") warningOrange else MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))

                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Options")
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Report Session") },
                                leadingIcon = { Icon(Icons.Rounded.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showReportSessionDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Report Question") },
                                leadingIcon = { Icon(Icons.Rounded.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMenu = false
                                    showReportQuestionDialog = true
                                }
                            )
                        }
                    }
                }
            }

            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / attempts.size.coerceAtLeast(1) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                beyondViewportPageCount = 1
            ) { page ->
                val attemptForIndex = attempts.getOrNull(page)
                val clipboard = LocalClipboardManager.current
                
                Box(modifier = Modifier.fillMaxSize()) {
                    val question: Question? = currentQuestion
                    if (question != null && page == currentIndex) {
                        QuestionViewer(
                            question = question,
                            options = options,
                            selectedAnswers = attemptForIndex?.userSelectedAnswers?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
                            onOptionToggled = { viewModel.onAnswerSelected(it) },
                            isAnswerRevealed = isCompleted,
                            correctAnswers = currentAnswer?.correctAnswerString?.split(",")?.map { it.trim() } ?: emptyList(),
                            explanation = currentAnswer?.generalExplanation,
                            references = currentAnswer?.references,
                            onPinToggled = { viewModel.toggleFlag() },
                            onDelete = { /* Logic? */ },
                            onCopy = {
                                val content = buildString {
                                    appendLine("Q: ${question.stem}")
                                    appendLine("\nOptions:")
                                    options.forEachIndexed { i, opt ->
                                        appendLine("${(i + 'A'.code).toChar()}. ${opt.optionText}")
                                    }
                                }
                                clipboard.setText(androidx.compose.ui.text.AnnotatedString(content))
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Question copied")
                                }
                            },
                            showHeader = false
                        )
                    }
                }
            }
        }
    }

    val aiResp = aiResponse
    if (aiResp != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearAiResponse() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (isAiLoading) 1.3f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp).graphicsLayer(scaleX = scale, scaleY = scale)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Assistance", style = MaterialTheme.typography.titleLarge)
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        MarkdownText(
                            markdown = aiResp,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.saveAiResponseToQuestion() }) {
                    Text("Save as Official Explanation")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearAiResponse() }) {
                    Text("Close")
                }
            }
        )
    }

    if (showReportSessionDialog) {
        com.algorithmx.q_base.core.designsystem.components.reusable.ReportDialog(
            itemType = "Session",
            itemName = session?.title ?: "current session",
            onDismiss = { showReportSessionDialog = false },
            onConfirm = { reason ->
                viewModel.reportSession(reason)
                showReportSessionDialog = false
            }
        )
    }

    if (showReportQuestionDialog) {
        com.algorithmx.q_base.core.designsystem.components.reusable.ReportDialog(
            itemType = "Question",
            itemName = currentQuestion?.stem?.take(30)?.let { "$it..." } ?: "this question",
            onDismiss = { showReportQuestionDialog = false },
            onConfirm = { reason ->
                viewModel.reportQuestion(reason)
                showReportQuestionDialog = false
            }
        )
    }

    if (showNavigator) {
        ModalBottomSheet(
            onDismissRequest = { showNavigator = false },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            MasterNavigator(
                dots = navDots,
                onQuestionClick = { index ->
                    viewModel.navigateToQuestion(index)
                    showNavigator = false
                }
            )
        }
    }
}

