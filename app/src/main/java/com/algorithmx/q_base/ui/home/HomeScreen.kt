package com.algorithmx.q_base.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToExplore: () -> Unit,
    onNavigateToSession: (String) -> Unit,
    onNavigateToCollections: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val ongoingSessions by viewModel.ongoingSessions.collectAsStateWithLifecycle()
    val pinnedQuestions by viewModel.pinnedQuestions.collectAsStateWithLifecycle()
    val recentCollections by viewModel.recentCollections.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Q-Base Home") })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToExplore,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Session") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Section: Ongoing Sessions
            if (ongoingSessions.isNotEmpty()) {
                item {
                    SectionHeader("Ongoing Sessions")
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(ongoingSessions) { session ->
                            SessionCard(session, onClick = { onNavigateToSession(session.sessionId) })
                        }
                    }
                }
            }

            // Section: Recent Collections
            if (recentCollections.isNotEmpty()) {
                item {
                    SectionHeader("Recent Collections", onActionClick = onNavigateToCollections)
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        recentCollections.forEach { collection ->
                            CollectionItem(collection)
                        }
                    }
                }
            }

            // Section: Pinned Questions
            if (pinnedQuestions.isNotEmpty()) {
                item {
                    SectionHeader("Pinned Questions")
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        pinnedQuestions.forEach { question ->
                            PinnedQuestionItem(question)
                        }
                    }
                }
            }
            
            // Empty State
            if (ongoingSessions.isEmpty() && recentCollections.isEmpty() && pinnedQuestions.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Welcome to Q-Base!", style = MaterialTheme.typography.headlineSmall)
                            Text("Start by creating a new study session.")
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onNavigateToExplore) {
                                Text("Explore Categories")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinnedQuestionItem(question: com.algorithmx.q_base.data.entity.Question) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = question.stem ?: "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Category: ${question.masterCategory}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
