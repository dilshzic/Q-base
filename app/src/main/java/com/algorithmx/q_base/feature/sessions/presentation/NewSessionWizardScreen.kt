package com.algorithmx.q_base.feature.sessions.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.feature.components.reusable.UnifiedTopAppBar
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

    // Intercept OS back to navigate wizard steps before popping the screen
    androidx.activity.compose.BackHandler(enabled = step > 1) {
        viewModel.setWizardStep(step - 1)
    }

    val stepProgress by animateFloatAsState(
        targetValue = step.toFloat() / 3f,
        animationSpec = tween(400), label = "progress"
    )

    Scaffold(
        topBar = {
            UnifiedTopAppBar(
                title = when(step) {
                    1 -> "Choose Collection"
                    2 -> "Select Questions"
                    3 -> "Configure Session"
                    else -> "New Session"
                },
                subtitle = "Step $step of 3",
                currentUser = currentUser,
                onProfileClick = {},
                navigationIcon = {
                    IconButton(onClick = { 
                        if (step > 1) viewModel.setWizardStep(step - 1) 
                        else onBack() 
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
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
            // Progress indicator
            LinearProgressIndicator(
                progress = { stepProgress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        (fadeIn(tween(300, 90)) + slideInHorizontally { it / 2 })
                            .togetherWith(fadeOut(tween(300)) + slideOutHorizontally { -it / 2 })
                    } else {
                        (fadeIn(tween(300, 90)) + slideInHorizontally { -it / 2 })
                            .togetherWith(fadeOut(tween(300)) + slideOutHorizontally { it / 2 })
                    }
                },
                label = "SessionWizardStep"
            ) { currentStep ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (currentStep) {
                        1 -> {
                            if (collections.isEmpty()) {
                                EmptyCollectionsView()
                            } else {
                                CategoryStep(collections) { viewModel.selectCollection(it) }
                            }
                        }
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
}

@Composable
fun EmptyCollectionsView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(modifier = Modifier.size(80.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.LibraryAdd, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("No collections yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Create a collection first to start a practice session.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
    }
}