package com.algorithmx.q_base.ui.explore

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.algorithmx.q_base.ui.components.ProfileIconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.collections.StudyCollectionWithCount
import com.algorithmx.q_base.data.collections.QuestionSet
import com.algorithmx.q_base.data.sessions.StudySession
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.ui.components.QuestionViewer
import com.algorithmx.q_base.ui.components.ReportDialog
import com.algorithmx.q_base.ui.components.ProfileIconButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreQuestionPagerScreen(
    categoryName: String,
    questionStates: List<ExploreQuestionState>,
    collections: List<com.algorithmx.q_base.data.collections.QuestionSet> = emptyList(),
    sessions: List<com.algorithmx.q_base.data.sessions.StudySession> = emptyList(),
    onOptionSelected: (Int, String) -> Unit,
    onCheckAnswer: (Int) -> Unit,
    onPinToggled: (Int) -> Unit,
    onAddToCollection: (Int, String) -> Unit,
    onAddToSession: (Int, String) -> Unit,
    onReportSubmitted: (Int, String) -> Unit,
    onAskAi: (Int, String, String) -> Unit,
    onSaveAiAsOfficial: (Int) -> Unit,
    onClearAiResponse: (Int) -> Unit,
    onDeleteQuestion: (Int) -> Unit,
    onPageChanged: (Int) -> Unit,
    onProfileClick: () -> Unit,
    onBack: () -> Unit,
    currentUser: com.algorithmx.q_base.data.core.UserEntity? = null,
    viewModel: ExploreViewModel? = null
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { questionStates.size })
    val coroutineScope = rememberCoroutineScope()
    var showReportDialog by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var showSessionDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel?.actionFeedback?.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val subCategories = remember(questionStates) {
        questionStates.mapNotNull { it.question.category }
            .distinct()
            .map { category ->
                category to questionStates.indexOfFirst { it.question.category == category }
            }
    }

    val currentSubCategoryIndex = remember(pagerState.currentPage, subCategories) {
        val currentCategory = questionStates.getOrNull(pagerState.currentPage)?.question?.category
        subCategories.indexOfFirst { it.first == currentCategory }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(currentSubCategoryIndex) {
        if (currentSubCategoryIndex != -1) {
            listState.animateScrollToItem(currentSubCategoryIndex)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        categoryName, 
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (questionStates.isNotEmpty()) {
                        val currentQuestionState = questionStates[pagerState.currentPage]
                        val currentQuestion = currentQuestionState.question
                        val clipboardManager = LocalClipboardManager.current
                        
                        IconButton(onClick = {
                            val content = buildString {
                                appendLine("Q: ${currentQuestion.stem}")
                                appendLine("\nOptions:")
                                currentQuestionState.options.forEachIndexed { i, opt ->
                                    appendLine("${(i + 'A'.toInt()).toChar()}. ${opt.optionText}")
                                }
                            }
                            clipboardManager.setText(AnnotatedString(content))
                        }) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Content")
                        }

                        IconButton(onClick = { onDeleteQuestion(pagerState.currentPage) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete Question")
                        }

                        var showMoreMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Report Problem") },
                                leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showMoreMenu = false
                                    showReportDialog = true
                                }
                            )
                        }

                        ProfileIconButton(
                            user = currentUser,
                            onClick = onProfileClick
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (questionStates.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(modifier = Modifier.padding(padding)) {
                LinearProgressIndicator(
                    progress = { (pagerState.currentPage + 1).toFloat() / questionStates.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                if (subCategories.size > 1) {
                    LazyRow(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(subCategories) { index, (name, firstIndex) ->
                            val isSelected = currentSubCategoryIndex == index
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(firstIndex)
                                    }
                                },
                                label = { Text(name) },
                                shape = CircleShape,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top,
                    pageSpacing = 16.dp
                ) { page ->
                    val state = questionStates[page]
                    QuestionViewer(
                        question = state.question,
                        options = state.options,
                        selectedAnswers = state.selectedOption?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
                        onOptionToggled = { onOptionSelected(page, it) },
                        isAnswerRevealed = state.isAnswerRevealed,
                        correctAnswers = state.answer?.correctAnswerString?.split(",")?.map { it.trim() } ?: emptyList(),
                        explanation = state.answer?.generalExplanation,
                        references = state.answer?.references,
                        onCheckAnswer = { onCheckAnswer(page) },
                        onPinToggled = { onPinToggled(page) },
                        onAddToSet = { showCollectionDialog = true },
                        onAddToSession = { showSessionDialog = true },
                        onAskAi = { onAskAi(page, "EXPLAIN", "") }
                    )
                }
            }
        }
    }

    if (showReportDialog) {
        val currentQuestion = questionStates.getOrNull(pagerState.currentPage)?.question
        ReportDialog(
            itemType = "Question",
            itemName = currentQuestion?.stem?.take(30) + "..." ?: "this question",
            onDismiss = { showReportDialog = false },
            onConfirm = { reason ->
                onReportSubmitted(pagerState.currentPage, reason)
                showReportDialog = false
            }
        )
    }

    // AI Response Content Dialog
    val currentQuestionState = questionStates.getOrNull(pagerState.currentPage)
    if (currentQuestionState?.aiResponse != null) {
        AlertDialog(
            onDismissRequest = { onClearAiResponse(pagerState.currentPage) },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = if (currentQuestionState.isAiLoading) 1.3f else 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_scale"
                    )
                    Icon(
                        Icons.Default.AutoAwesome,
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
                    if (currentQuestionState.isAiLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        MarkdownText(
                            markdown = currentQuestionState.aiResponse!!,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        onSaveAiAsOfficial(pagerState.currentPage)
                    },
                    enabled = false // Temporarily disabled per user request
                ) {
                    Text("Save as Official Explanation")
                }
            },
            dismissButton = {
                TextButton(onClick = { onClearAiResponse(pagerState.currentPage) }) {
                    Text("Close")
                }
            }
        )
    }

    if (showCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showCollectionDialog = false },
            title = { Text("Add to Set", fontWeight = FontWeight.Bold) },
            text = {
                if (collections.isEmpty()) {
                    Text("No sets found.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        itemsIndexed(collections) { _, set ->
                            ListItem(
                                headlineContent = { Text(set.title) },
                                modifier = Modifier.clickable {
                                    onAddToCollection(pagerState.currentPage, set.setId)
                                    showCollectionDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCollectionDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showSessionDialog) {
        AlertDialog(
            onDismissRequest = { showSessionDialog = false },
            title = { Text("Add to Session", fontWeight = FontWeight.Bold) },
            text = {
                if (sessions.isEmpty()) {
                    Text("No active sessions found.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        itemsIndexed(sessions) { _, session ->
                            ListItem(
                                headlineContent = { Text(session.title) },
                                supportingContent = { Text("Created ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(session.createdTimestamp))}") },
                                modifier = Modifier.clickable {
                                    onAddToSession(pagerState.currentPage, session.sessionId)
                                    showSessionDialog = false
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSessionDialog = false }) { Text("Cancel") }
            }
        )
    }
}
