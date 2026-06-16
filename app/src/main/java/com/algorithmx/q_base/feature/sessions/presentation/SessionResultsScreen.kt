package com.algorithmx.q_base.feature.sessions.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.core.designsystem.components.question.QuestionViewer
import com.algorithmx.q_base.core.designsystem.components.reusable.ReportDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionResultsScreen(
    viewModel: SessionResultsViewModel = hiltViewModel(),
    onDone: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reviewState by viewModel.reviewQuestion.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showReportDialog by remember { mutableStateOf(false) }

    val scrollState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(scrollState)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            com.algorithmx.q_base.core.designsystem.components.reusable.UnifiedTopAppBar(
                title = (uiState as? ResultsUiState.Success)?.session?.title?.takeIf { it.isNotBlank() } ?: "Session Results",
                subtitle = "Analysis of Your Performance",
                currentUser = currentUser,
                onProfileClick = { /* Or direct to profile */ },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is ResultsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeCap = androidx.compose.ui.graphics.StrokeCap.Round)
                    }
                }
                is ResultsUiState.Success -> {
                    ResultsContent(
                        state = state,
                        onReviewQuestion = { viewModel.selectQuestionForReview(it) },
                        onReportSession = {
                            showReportDialog = true
                        },
                        onDone = onDone
                    )
                }
            }
        }
    }

    if (reviewState != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.clearReview() },
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Explanatory Review",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(
                        onClick = { viewModel.clearReview() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close")
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp))
                
                Box(modifier = Modifier.weight(1f)) {
                    reviewState?.let { review ->
                        QuestionViewer(
                            question = review.question,
                            options = review.options,
                            selectedAnswers = review.attempt.userSelectedAnswers.split(",").filter { it.isNotEmpty() },
                            onOptionToggled = { /* Read only in review */ },
                            isAnswerRevealed = true,
                            correctAnswers = review.answer?.correctAnswerString?.split(",")?.map { it.trim() } ?: emptyList(),
                            explanation = review.answer?.generalExplanation,
                            references = review.answer?.references
                        )
                    }
                }
            }
        }
    }

    if (showReportDialog) {
        ReportDialog(
            itemType = "Session",
            itemName = "Session Analysis",
            onDismiss = { showReportDialog = false },
            onConfirm = { reason ->
                viewModel.reportCurrentSession(reason)
                showReportDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Session report sent successfully")
                }
            }
        )
    }
}

@Composable
fun ResultsContent(
    state: ResultsUiState.Success,
    onReviewQuestion: (com.algorithmx.q_base.feature.sessions.data.SessionAttempt) -> Unit,
    onReportSession: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Expressive Score Section
        var scoreVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { scoreVisible = true }
        
        AnimatedVisibility(
            visible = scoreVisible,
            enter = scaleIn(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
        ) {
            Surface(
                modifier = Modifier.size(200.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                border = androidx.compose.foundation.BorderStroke(8.dp, MaterialTheme.colorScheme.primaryContainer)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${state.score.toInt()}%",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (state.score >= 50) "SUCCESS" else "PRACTICE MORE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Performance Summary",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = "Tap any question to review detailed explanations.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.Start),
            color = MaterialTheme.colorScheme.secondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Expressive Staggered Grid chunked by 5 to prevent nested scrolling crash
        state.attempts.chunked(5).forEach { rowAttempts ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowAttempts.forEachIndexed { rowIndex, attempt ->
                    val absoluteIndex = state.attempts.indexOf(attempt)
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        AnimatedAttemptDot(absoluteIndex, attempt, onReviewQuestion)
                    }
                }
                if (rowAttempts.size < 5) {
                    repeat(5 - rowAttempts.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.large,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Rounded.History, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Done Reviewing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = onReportSession,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Report Session Issues", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AnimatedAttemptDot(
    index: Int,
    attempt: com.algorithmx.q_base.feature.sessions.data.SessionAttempt,
    onClick: (com.algorithmx.q_base.feature.sessions.data.SessionAttempt) -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 30L)
        visible = true
    }
    
    val color = when {
        attempt.marksObtained >= 1f -> com.algorithmx.q_base.core.designsystem.theme.successGreen
        attempt.marksObtained > 0f -> com.algorithmx.q_base.core.designsystem.theme.warningOrange
        attempt.attemptStatus == "UNATTEMPTED" -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.error
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.5f) + fadeIn(),
    ) {
        Surface(
            modifier = Modifier
                .size(56.dp)
                .clickable { onClick(attempt) },
            shape = RoundedCornerShape(16.dp),
            color = color.copy(alpha = 0.15f),
            border = androidx.compose.foundation.BorderStroke(2.dp, color)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = (index + 1).toString(),
                    color = color,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}