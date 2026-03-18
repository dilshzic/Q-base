package com.algorithmx.q_base.ui.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.entity.MasterCategory
import com.algorithmx.q_base.ui.components.QuestionViewer

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
    onBack: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { questionStates.size })

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
}
