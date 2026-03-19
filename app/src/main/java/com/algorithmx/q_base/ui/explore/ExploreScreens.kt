package com.algorithmx.q_base.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.algorithmx.q_base.data.entity.MasterCategory
import com.algorithmx.q_base.ui.components.QuestionViewer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryListScreen(
    categories: List<MasterCategory>,
    onCategoryClick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Medical Categories") })
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
            items(categories) { category ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { onCategoryClick(category.name) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
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
            TopAppBar(
                title = { Text(categoryName) },
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
                }
            )
        }
    ) { padding ->
        if (questionStates.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No questions found.")
            }
        } else {
            Column(modifier = Modifier.padding(padding)) {
                LinearProgressIndicator(
                    progress = { (pagerState.currentPage + 1).toFloat() / questionStates.size },
                    modifier = Modifier.fillMaxWidth()
                )

                if (subCategories.size > 1) {
                    LazyRow(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(subCategories) { index, (name, firstIndex) ->
                            val isSelected = currentSubCategoryIndex == index
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(firstIndex)
                                        }
                                    },
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(
                                    text = name,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top
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
            title = { Text("Report a Problem") },
            text = {
                Column {
                    Text("Please describe the issue with this question:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reportText,
                        onValueChange = { reportText = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        placeholder = { Text("Issue details...") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (reportText.isNotBlank()) {
                            onReportSubmitted(pagerState.currentPage, reportText)
                            reportText = ""
                            showReportDialog = false
                        }
                    }
                ) {
                    Text("Submit")
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
