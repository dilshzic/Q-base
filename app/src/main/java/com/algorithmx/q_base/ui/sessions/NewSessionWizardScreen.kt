package com.algorithmx.q_base.ui.sessions

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.ui.components.reusable.UnifiedTopAppBar
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSessionWizardScreen(
    viewModel: SessionsViewModel,
    collections: List<StudyCollection>,
    onBack: () -> Unit
) {
    val step by viewModel.wizardStep.collectAsStateWithLifecycle()
    val availableQuestions by viewModel.availableQuestions.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedQuestionIds.collectAsStateWithLifecycle()
    val order by viewModel.sessionOrder.collectAsStateWithLifecycle()
    val timingType by viewModel.timingType.collectAsStateWithLifecycle()
    val timeLimitSeconds by viewModel.timeLimitSeconds.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            UnifiedTopAppBar(
                title = when(step) {
                    1 -> "Choose collection"
                    2 -> "Select Questions"
                    3 -> "Configure Session"
                    else -> "New Session"
                },
                subtitle = "Step $step of 3",
                currentUser = currentUser,
                onProfileClick = { /* Maybe navigate to profile? or leave empty if not needed here */ },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (step > 1) viewModel.setWizardStep(step - 1) 
                        else onBack() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                when (step) {
                    1 -> CategoryStep(collections) { viewModel.selectCollection(it) }
                    2 -> QuestionSelectionStep(
                        questions = availableQuestions,
                        selectedIds = selectedIds,
                        lastRandomCount = viewModel.lastRandomCount.collectAsStateWithLifecycle().value,
                        onToggle = { viewModel.toggleQuestionSelection(it) },
                        onSelectAll = { viewModel.selectAllQuestions() },
                        onDeselectAll = { viewModel.deselectAllQuestions() },
                        onRandomSelect = { viewModel.selectRandomQuestions(it) },
                        onNext = { viewModel.setWizardStep(3) }
                    )
                    3 -> {
                        val isAdminOnly by viewModel.sessionIsAdminOnly.collectAsStateWithLifecycle()
                        ConfigurationStep(
                            order = order,
                            timingType = timingType,
                            timeLimitSeconds = timeLimitSeconds,
                            onOrderChange = { viewModel.setOrder(it) },
                            onTimingChange = { viewModel.setTimingType(it) },
                            onTimeLimitChange = { viewModel.setTimeLimit(it) },
                            selectedCount = selectedIds.size,
                            isAdminOnly = isAdminOnly,
                            onIsAdminOnlyChange = { viewModel.setSessionIsAdminOnly(it) },
                            onLaunch = { title -> viewModel.launchSession(title) }
                        )
                    }
                }
            }
        }
    }
}
