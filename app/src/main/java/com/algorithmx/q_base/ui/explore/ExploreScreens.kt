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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.algorithmx.q_base.data.entity.MasterCategory
import com.algorithmx.q_base.ui.components.QuestionViewer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    categories: List<MasterCategory>,
    onCategoryClick: (String) -> Unit
) {
    val scrollState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(scrollState)

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { 
                    Column {
                        Text(
                            "Categories", 
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Select Your Specialty",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(categories) { index, category ->
                AnimatedCategoryItem(index) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clickable { onCategoryClick(category.name) },
                        shape = MaterialTheme.shapes.extraLarge,
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedCategoryItem(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 40L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.8f) + fadeIn(),
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreQuestionPagerScreen(
    categoryName: String,
    questionStates: List<ExploreQuestionState>,
    onOptionSelected: (Int, String) -> Unit,
    onCheckAnswer: (Int) -> Unit,
    onPageChanged: (Int) -> Unit,
    onPinToggled: (Int) -> Unit,
    onReportSubmitted: (Int, String) -> Unit,
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { questionStates.size })
    val coroutineScope = rememberCoroutineScope()
    var showReportDialog by remember { mutableStateOf(false) }
    var reportText by remember { mutableStateOf("") }

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
                        val currentQuestion = questionStates[pagerState.currentPage].question
                        IconButton(onClick = { onPinToggled(pagerState.currentPage) }) {
                            Icon(
                                imageVector = if (currentQuestion.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin Question",
                                tint = if (currentQuestion.isPinned) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = { showReportDialog = true }) {
                            Icon(Icons.Default.Report, contentDescription = "Report Problem")
                        }
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
                // Expressive Progress Bar
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
                        correctAnswers = state.answer?.correctAnswerString?.split(",") ?: emptyList(),
                        explanation = state.answer?.generalExplanation,
                        references = state.answer?.references,
                        onCheckAnswer = { onCheckAnswer(page) }
                    )
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            shape = MaterialTheme.shapes.extraLarge,
            title = { Text("Report a Problem", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Found an error? Let us know the details.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = reportText,
                        onValueChange = { reportText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = MaterialTheme.shapes.large,
                        placeholder = { Text("Describe the issue...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportText.isNotBlank()) {
                            onReportSubmitted(pagerState.currentPage, reportText)
                            reportText = ""
                            showReportDialog = false
                        }
                    }
                ) {
                    Text("Submit Report")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
