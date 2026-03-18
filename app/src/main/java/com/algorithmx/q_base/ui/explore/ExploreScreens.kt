package com.algorithmx.q_base.ui.explore

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            items(categories) { category ->
                ListItem(
                    headlineContent = { Text(category.name) },
                    trailingContent = {
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
                    },
                    modifier = Modifier.clickable { onCategoryClick(category.name) }
                )
                HorizontalDivider()
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
                        onCheckAnswer = { onCheckAnswer(page) }
                    )
                }
            }
        }
    }
}
