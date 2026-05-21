package com.algorithmx.q_base.ui.explore

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.data.collections.QuestionSet
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.data.sessions.StudySession
import com.algorithmx.q_base.ui.components.question.QuestionViewer
import com.algorithmx.q_base.ui.components.reusable.UnifiedTopAppBar
import com.algorithmx.q_base.ui.components.reusable.ReportDialog
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreQuestionPagerScreen(
    collectionName: String,
    questionStates: List<ExploreQuestionState>,
    collections: List<QuestionSet>,
    sessions: List<StudySession>,
    onOptionSelected: (Int, String) -> Unit,
    onCheckAnswer: (Int) -> Unit,
    onPageChanged: (Int) -> Unit,
    onPinToggled: (Int) -> Unit,
    onAddToCollection: (Int, String) -> Unit,
    onAddToSession: (Int, String) -> Unit,
    onReportSubmitted: (Int, String) -> Unit,
    onAskAi: (Int, String) -> Unit,
    onSaveAiAsOfficial: (Int) -> Unit,
    onClearAiResponse: (Int) -> Unit,
    onDeleteQuestion: (Int) -> Unit,
    onEditQuestion: (Int) -> Unit = {},
    onProfileClick: () -> Unit,
    onBack: () -> Unit,
    currentUser: UserEntity? = null,
    viewModel: ExploreViewModel? = null
) {
    val pagerState = rememberPagerState(pageCount = { questionStates.size })
    val coroutineScope = rememberCoroutineScope()
    var showReportDialog by remember { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var showSessionDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Group questions by sub-category for fast navigation
    val subCategories = remember(questionStates) {
        questionStates.map { it.question.category }
            .distinct()
            .filterNotNull()
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
            UnifiedTopAppBar(
                title = collectionName,
                currentUser = currentUser,
                onProfileClick = onProfileClick,
                isLarge = false,
                titleCentered = true,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        /*
        floatingActionButton = {
            if (questionStates.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { onAskAi(pagerState.currentPage, "EXPLAIN") },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                    icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                    text = { Text("Ask AI") },
                    modifier = Modifier.padding(bottom = 72.dp)
                )
            }
        }
        */
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
                    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
                    
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
                        onDelete = { onDeleteQuestion(page) },
                        isEditable = state.isEditable,
                        onEditQuestion = { onEditQuestion(page) },
                        onAskAi = { onAskAi(page, "EXPLAIN") },
                        onCopy = {
                            val content = buildString {
                                appendLine("Q: ${state.question.stem}")
                                appendLine("\nOptions:")
                                state.options.forEachIndexed { i, opt ->
                                    appendLine("${(i + 'A'.code).toChar()}. ${opt.optionText}")
                                }
                            }
                            clipboard.setText(androidx.compose.ui.text.AnnotatedString(content))
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Question copied to clipboard")
                            }
                        }
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

    // AI Response Content Bottom Sheet
    val currentQuestionState = questionStates.getOrNull(pagerState.currentPage)
    if (currentQuestionState?.aiResponse != null) {
        ModalBottomSheet(
            onDismissRequest = { onClearAiResponse(pagerState.currentPage) },
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

                if (currentQuestionState.isAiLoading) {
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
                            markdown = currentQuestionState.aiResponse!!,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp
                            ),
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onClearAiResponse(pagerState.currentPage) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = { onSaveAiAsOfficial(pagerState.currentPage) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save to Question")
                    }
                }
            }
        }
    }

    // Collection Selector Dialog
    if (showCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showCollectionDialog = false },
            title = { Text("Add to Collection") },
            text = {
                LazyColumn {
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
            },
            confirmButton = {}
        )
    }

    // Session Selector Dialog
    if (showSessionDialog) {
        AlertDialog(
            onDismissRequest = { showSessionDialog = false },
            title = { Text("Add to Study Session") },
            text = {
                LazyColumn {
                    itemsIndexed(sessions) { _, session ->
                        ListItem(
                            headlineContent = { Text(session.title) },
                            modifier = Modifier.clickable {
                                onAddToSession(pagerState.currentPage, session.sessionId)
                                showSessionDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}