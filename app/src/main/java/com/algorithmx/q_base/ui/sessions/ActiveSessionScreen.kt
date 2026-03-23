package com.algorithmx.q_base.ui.sessions

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algorithmx.q_base.ui.components.QuestionViewer
import dev.jeziellago.compose.markdowntext.MarkdownText
import androidx.compose.material.icons.rounded.AutoAwesome

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
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var showNavigator by remember { mutableStateOf(false) }
    var showReportSessionDialog by remember { mutableStateOf(false) }
    var showReportQuestionDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.actionFeedback.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }
    
    val currentAttempt = attempts.getOrNull(currentIndex)
    val isCompleted = session?.isCompleted == true

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                CenterAlignedTopAppBar(
                    title = {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Timer,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isCompleted) "Review Mode" else timerText,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (!isCompleted) {
                            Button(
                                onClick = { viewModel.submitSession() },
                                shape = MaterialTheme.shapes.medium,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("FINISH", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        } else {
                            IconButton(onClick = { onViewResults(viewModel.getSessionId()) }) {
                                Icon(Icons.Default.Assessment, contentDescription = "View Results")
                            }
                        }
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options")
                        }
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

                        com.algorithmx.q_base.ui.components.ProfileIconButton(
                            user = currentUser,
                            onClick = { /* Navigate to profile */ }
                        )
                    }
                )
                
                // Expressive Progress Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question ${currentIndex + 1} of ${attempts.size}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.toggleFlag() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Flag, 
                                contentDescription = "Flag",
                                tint = if (currentAttempt?.attemptStatus == "FLAGGED") Color(0xFFFFA500) else MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showNavigator = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Rounded.GridView, contentDescription = "Navigator")
                        }
                    }
                }
                
                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / attempts.size.coerceAtLeast(1) },
                    modifier = Modifier.fillMaxWidth(),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
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
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PREV")
                    }
                    
                    Button(
                        onClick = { viewModel.navigateToQuestion(currentIndex + 1) },
                        enabled = currentIndex < attempts.size - 1,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("NEXT")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    ) { padding ->
        AnimatedContent(
            targetState = currentIndex,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { it } + fadeIn()).togetherWith(slideOutHorizontally { -it } + fadeOut())
                } else {
                    (slideInHorizontally { -it } + fadeIn()).togetherWith(slideOutHorizontally { it } + fadeOut())
                }.using(SizeTransform(clip = false))
            },
            modifier = Modifier.padding(padding),
            label = "question_transition"
        ) { targetIndex ->
            Box(modifier = Modifier.fillMaxSize()) {
                currentQuestion?.let { question ->
                    QuestionViewer(
                        question = question,
                        options = options,
                        selectedAnswers = currentAttempt?.userSelectedAnswers?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
                        onOptionToggled = { viewModel.onAnswerSelected(it) },
                        isAnswerRevealed = isCompleted,
                        correctAnswers = currentAnswer?.correctAnswerString?.split(",")?.map { it.trim() } ?: emptyList(),
                        explanation = currentAnswer?.generalExplanation,
                        references = currentAnswer?.references,
                        onAskAi = { viewModel.askAi() }
                    )
                }
            }
        }
    }

    if (aiResponse != null) {
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
                            markdown = aiResponse!!,
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
        com.algorithmx.q_base.ui.components.ReportDialog(
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
        com.algorithmx.q_base.ui.components.ReportDialog(
            itemType = "Question",
            itemName = currentQuestion?.stem?.take(30) + "..." ?: "this question",
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

@Composable
fun MasterNavigator(
    dots: List<NavigatorDot>,
    onQuestionClick: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Master Navigator",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Quickly jump between questions and review status.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Adaptive(56.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.heightIn(max = 450.dp)
        ) {
            itemsIndexed(dots) { index, dot ->
                val backgroundColor = when (dot.status) {
                    "ATTEMPTED" -> MaterialTheme.colorScheme.primaryContainer
                    "FLAGGED" -> Color(0xFFFFF4E5)
                    "FINALIZED" -> MaterialTheme.colorScheme.surfaceVariant
                    else -> Color.Transparent
                }
                
                val contentColor = when (dot.status) {
                    "ATTEMPTED" -> MaterialTheme.colorScheme.onPrimaryContainer
                    "FLAGGED" -> Color(0xFFFFA500)
                    "FINALIZED" -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                }
                
                val borderColor = when {
                    dot.isSelected -> MaterialTheme.colorScheme.primary
                    dot.status == "FLAGGED" -> Color(0xFFFFA500)
                    else -> MaterialTheme.colorScheme.outlineVariant
                }

                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            scaleX = if (dot.isSelected) 1.1f else 1f
                            scaleY = if (dot.isSelected) 1.1f else 1f
                        },
                    onClick = { onQuestionClick(index) },
                    shape = RoundedCornerShape(16.dp),
                    color = backgroundColor,
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (dot.isSelected) 3.dp else 1.dp,
                        color = borderColor
                    ),
                    tonalElevation = if (dot.isSelected) 4.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = (index + 1).toString(),
                            color = contentColor,
                            fontWeight = if (dot.isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}
