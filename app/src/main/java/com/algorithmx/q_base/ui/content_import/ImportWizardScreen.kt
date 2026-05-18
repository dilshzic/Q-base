package com.algorithmx.q_base.ui.content_import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.ui.components.reusable.UnifiedTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportWizardScreen(
    viewModel: ImportViewModel,
    source: String? = null,
    targetId: String? = null,
    onBack: () -> Unit,
    onProfileClick: () -> Unit,
    onNavigateToManualEditor: (String, String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val extractedText by viewModel.extractedText.collectAsStateWithLifecycle()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val collectionName by viewModel.newCollectionName.collectAsStateWithLifecycle()
    val collectionDesc by viewModel.newCollectionDescription.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.setInitialSource(source, targetId)
    }

    // BackHandler for wizard step navigation
    val isNotAtStart = uiState !is ImportStep.NameAndDestination && uiState !is ImportStep.MediaInput && uiState !is ImportStep.ExtractionIngest
    androidx.activity.compose.BackHandler(enabled = isNotAtStart) {
        when (uiState) {
            is ImportStep.Configure -> viewModel.navigateTo(ImportStep.NameAndDestination)
            is ImportStep.Review -> viewModel.navigateTo(ImportStep.NameAndDestination)
            is ImportStep.ExtractionOverview -> viewModel.navigateTo(ImportStep.ExtractionIngest)
            is ImportStep.Error -> viewModel.reset()
            else -> onBack()
        }
    }

    val stepProgress by animateFloatAsState(
        targetValue = viewModel.currentStepNumber().toFloat() / viewModel.totalSteps(),
        animationSpec = tween(400), label = "progress"
    )

    Scaffold(
        topBar = {
            UnifiedTopAppBar(
                title = "Create Collection",
                currentUser = currentUser,
                onProfileClick = onProfileClick,
                isLarge = false,
                titleCentered = true,
                navigationIcon = {
                    IconButton(onClick = {
                        if (isNotAtStart) {
                            when (uiState) {
                                is ImportStep.Configure -> viewModel.navigateTo(ImportStep.NameAndDestination)
                                is ImportStep.Review -> viewModel.navigateTo(ImportStep.NameAndDestination)
                                is ImportStep.ExtractionOverview -> viewModel.navigateTo(ImportStep.ExtractionIngest)
                                is ImportStep.Error -> viewModel.reset()
                                else -> onBack()
                            }
                        } else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Premium Progress indicator
            LinearProgressIndicator(
                progress = { stepProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )

            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    (fadeIn(tween(350, 90)) + slideInHorizontally { it / 2 })
                        .togetherWith(fadeOut(tween(250)) + slideOutHorizontally { -it / 2 })
                },
                label = "WizardStep",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    // Map both states to the same CommonWizardFirstScreen so importing references does not proceed
                    is ImportStep.NameAndDestination, is ImportStep.MediaInput -> CommonWizardFirstScreen(
                        name = collectionName,
                        description = collectionDesc,
                        extractedText = extractedText,
                        collections = collections,
                        selectedCategoryId = selectedCategoryId,
                        onNameChanged = { viewModel.updateNewCollectionName(it) },
                        onDescriptionChanged = { viewModel.updateNewCollectionDescription(it) },
                        onCategorySelected = { viewModel.selectCategory(it) },
                        onRawTextUpdated = { viewModel.onRawTextUpdated(it) },
                        onImagePicked = { viewModel.onImagePicked(it) },
                        onPdfPicked = { viewModel.onPdfPicked(it) },
                        onDirectExtractionClick = {
                            viewModel.navigateTo(ImportStep.ExtractionIngest)
                        },
                        onNext = {
                            viewModel.selectMethod("GENERATE")
                            viewModel.navigateTo(ImportStep.Configure("GENERATE"))
                        }
                    )
                    is ImportStep.Configure -> {
                        if (step.mode == "IMPORT") {
                            ImportConfigView(onProceed = { types, instructions ->
                                viewModel.startDirectImport(types, instructions)
                            })
                        } else {
                            GenerateConfigView(onProceed = { config ->
                                viewModel.startAiGeneration(config)
                            })
                        }
                    }
                    is ImportStep.Processing -> {
                        if (step.message.contains("extracting", ignoreCase = true) || 
                            step.message.contains("Analyzing", ignoreCase = true) || 
                            step.message.contains("Extracting", ignoreCase = true)) {
                            ExtractionWizardSecondScreen(statusMessage = step.message)
                        } else {
                            WaitingView(step.message)
                        }
                    }
                    is ImportStep.Review -> ReviewView(
                        count = step.questionCount,
                        collectionName = collectionName,
                        collections = collections,
                        selectedCategoryId = selectedCategoryId,
                        onNameChanged = { viewModel.updateNewCollectionName(it) },
                        onCategorySelected = { viewModel.selectCategory(it) },
                        onFinished = {
                            viewModel.promoteResponse(step.responseId, collectionName) { setId, finalName ->
                                onNavigateToManualEditor(setId, finalName)
                            }
                        }
                    )
                    is ImportStep.ExtractionIngest -> {
                        val docs by viewModel.extractedDocs.collectAsStateWithLifecycle()
                        ExtractionWizardFirstScreen(
                            extractedDocs = docs,
                            onAddPdf = { viewModel.addPdf(it) },
                            onAddOcr = { viewModel.addOcr(it) },
                            onAddClipboard = { viewModel.addClipboard(it) },
                            onRemoveDoc = { viewModel.removeDoc(it) },
                            onProceed = { viewModel.startPaperExtraction() }
                        )
                    }
                    is ImportStep.ExtractionOverview -> {
                        ExtractionWizardFourthScreen(
                            response = step.response,
                            collectionName = collectionName,
                            onNameChanged = { viewModel.updateNewCollectionName(it) },
                            onManualEditClick = {
                                viewModel.promoteResponse(step.responseId, collectionName) { setId, finalName ->
                                    onNavigateToManualEditor(setId, finalName)
                                }
                            },
                            onFinished = {
                                viewModel.promoteResponse(step.responseId, collectionName) { _, _ ->
                                    onBack()
                                }
                            }
                        )
                    }
                    is ImportStep.Error -> ErrorView(
                        message = step.message,
                        onRetry = { viewModel.reset() }
                    )
                    is ImportStep.Extracting -> WaitingView("Extracting text from ${step.source}...")
                    else -> Box(Modifier.fillMaxSize())
                }
            }
        }
    }
}
