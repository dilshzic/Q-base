package com.algorithmx.q_base.ui.content_import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.ui.components.reusable.UnifiedTopAppBar
import com.algorithmx.q_base.data.collections.StudyCollection
import kotlinx.coroutines.delay

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
    val isNotAtStart = uiState !is ImportStep.NameAndDestination
    androidx.activity.compose.BackHandler(enabled = isNotAtStart) {
        when (uiState) {
            is ImportStep.ChooseMethod -> viewModel.navigateTo(ImportStep.NameAndDestination)
            is ImportStep.MediaInput -> viewModel.navigateTo(ImportStep.ChooseMethod)
            is ImportStep.Configure -> viewModel.navigateTo(ImportStep.MediaInput)
            is ImportStep.Review -> viewModel.navigateTo(ImportStep.MediaInput)
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
                                is ImportStep.ChooseMethod -> viewModel.navigateTo(ImportStep.NameAndDestination)
                                is ImportStep.MediaInput -> viewModel.navigateTo(ImportStep.ChooseMethod)
                                is ImportStep.Configure -> viewModel.navigateTo(ImportStep.MediaInput)
                                is ImportStep.Review -> viewModel.navigateTo(ImportStep.MediaInput)
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
            // Progress indicator
            LinearProgressIndicator(
                progress = { stepProgress },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    (fadeIn(tween(300, 90)) + slideInHorizontally { it / 2 })
                        .togetherWith(fadeOut(tween(300)) + slideOutHorizontally { -it / 2 })
                },
                label = "WizardStep"
            ) { step ->
                when (step) {
                    is ImportStep.NameAndDestination -> NameAndDestinationView(
                        name = collectionName,
                        description = collectionDesc,
                        collections = collections,
                        selectedCategoryId = selectedCategoryId,
                        onNameChanged = { viewModel.updateNewCollectionName(it) },
                        onDescriptionChanged = { viewModel.updateNewCollectionDescription(it) },
                        onCategorySelected = { viewModel.selectCategory(it) },
                        onNext = { viewModel.navigateTo(ImportStep.ChooseMethod) }
                    )
                    is ImportStep.ChooseMethod -> ChooseMethodView(
                        collectionName = collectionName.ifBlank { "your collection" },
                        onImport = {
                            viewModel.selectMethod("IMPORT")
                            viewModel.navigateTo(ImportStep.MediaInput)
                        },
                        onGenerate = {
                            viewModel.selectMethod("GENERATE")
                            viewModel.navigateTo(ImportStep.MediaInput)
                        },
                        onManual = {
                            viewModel.selectMethod("MANUAL")
                            onNavigateToManualEditor(targetId ?: "new", collectionName.ifBlank { null })
                        }
                    )
                    is ImportStep.MediaInput -> MediaInputView(
                        text = extractedText,
                        onImagePicked = { viewModel.onImagePicked(it) },
                        onPdfPicked = { viewModel.onPdfPicked(it) },
                        onTextUpdated = { viewModel.onRawTextUpdated(it) },
                        onProceed = {
                            val method = viewModel.selectedMethod.value ?: "IMPORT"
                            viewModel.navigateTo(ImportStep.Configure(method))
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
                    is ImportStep.Processing -> WaitingView(step.message)
                    is ImportStep.Review -> ReviewView(
                        count = step.questionCount,
                        collectionName = collectionName,
                        onFinished = {
                            viewModel.promoteResponse(step.responseId, collectionName) { setId, finalName ->
                                onNavigateToManualEditor(setId, finalName)
                            }
                        }
                    )
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

// ── Step 1: Name & Destination ──────────────────────────────────────────

@Composable
fun NameAndDestinationView(
    name: String,
    description: String,
    collections: List<StudyCollection>,
    selectedCategoryId: String?,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onNext: () -> Unit
) {
    var isNewCollection by remember { mutableStateOf(selectedCategoryId == null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Name Your Collection", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("Give it a name and optionally add to an existing collection.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)

        OutlinedTextField(
            value = name,
            onValueChange = onNameChanged,
            label = { Text("Collection Name") },
            placeholder = { Text("e.g. Pathology Basics") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            minLines = 2
        )

        // Destination toggle
        Text("Destination", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = isNewCollection, onClick = { isNewCollection = true; onCategorySelected(null) }, label = { Text("New Collection") })
            FilterChip(selected = !isNewCollection, onClick = { isNewCollection = false }, label = { Text("Add to Existing") })
        }

        if (!isNewCollection && collections.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(collections.find { it.collectionId == selectedCategoryId }?.name ?: "Select collection")
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    collections.forEach { col ->
                        DropdownMenuItem(text = { Text(col.name) }, onClick = { onCategorySelected(col.collectionId); expanded = false })
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = name.isNotBlank() || selectedCategoryId != null
        ) { Text("Continue", fontWeight = FontWeight.Bold) }
    }
}

// ── Step 2: Choose Method ────────────────────────────────────────────

@Composable
fun ChooseMethodView(
    collectionName: String,
    onImport: () -> Unit,
    onGenerate: () -> Unit,
    onManual: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Build \"$collectionName\"", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("Choose how to add questions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
        Spacer(Modifier.height(40.dp))

        MethodCard(icon = Icons.Rounded.AutoAwesome, title = "Import from Media", subtitle = "Extract from photo, PDF, or pasted text", color = MaterialTheme.colorScheme.primary, onClick = onImport)
        Spacer(Modifier.height(16.dp))
        MethodCard(icon = Icons.Rounded.Psychology, title = "AI Generate", subtitle = "Generate questions from a topic", color = MaterialTheme.colorScheme.tertiary, onClick = onGenerate)
        Spacer(Modifier.height(16.dp))
        MethodCard(icon = Icons.Rounded.EditNote, title = "Build Manually", subtitle = "Add questions one by one", color = MaterialTheme.colorScheme.secondary, onClick = onManual)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MethodCard(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(90.dp),
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = color, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// ── Step 3: Media Input ─────────────────────────────────────────────

@Composable
fun MediaInputView(
    text: String,
    onImagePicked: (Uri) -> Unit,
    onPdfPicked: (Uri) -> Unit,
    onTextUpdated: (String) -> Unit,
    onProceed: () -> Unit
) {
    var rawText by remember(text) { mutableStateOf(text) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { onImagePicked(it) } }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { onPdfPicked(it) } }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Add Source Material", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("Import from a photo/PDF or paste text directly.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallSourceButton(Icons.Rounded.CameraAlt, "OCR/Photo", Color(0xFF6200EE)) { imagePicker.launch("image/*") }
            SmallSourceButton(Icons.Rounded.PictureAsPdf, "PDF", Color(0xFFF44336)) { pdfPicker.launch("application/pdf") }
        }

        OutlinedTextField(
            value = rawText,
            onValueChange = { rawText = it; onTextUpdated(it) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            placeholder = { Text("Paste or type text here...") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            )
        )

        Button(
            onClick = onProceed,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = rawText.isNotBlank()
        ) { Text("Continue", fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun SmallSourceButton(icon: ImageVector, title: String, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.height(56.dp).width(140.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelLarge, color = color, fontWeight = FontWeight.Bold)
        }
    }
}

// ── Step 4a: Import Config ──────────────────────────────────────────

@Composable
fun ImportConfigView(onProceed: (List<String>, String) -> Unit) {
    var selectedTypes by remember { mutableStateOf(setOf("SBA")) }
    var instructions by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Text("Import Configuration", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Column {
            Text("Detect Question Types", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SBA", "MCQ").forEach { type ->
                    FilterChip(selected = selectedTypes.contains(type), onClick = {
                        val next = selectedTypes.toMutableSet()
                        if (next.contains(type)) next.remove(type) else next.add(type)
                        selectedTypes = next
                    }, label = { Text(type) })
                }
            }
        }
        OutlinedTextField(value = instructions, onValueChange = { instructions = it }, modifier = Modifier.fillMaxWidth().height(120.dp), label = { Text("Custom Instructions for AI") }, placeholder = { Text("e.g. Only extract pathology questions...") })
        Spacer(Modifier.weight(1f))
        Button(onClick = { onProceed(selectedTypes.toList(), instructions) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Begin Import", fontWeight = FontWeight.Bold)
        }
    }
}

// ── Step 4b: Generate Config ────────────────────────────────────────

@Composable
fun GenerateConfigView(onProceed: (ExtractionConfigData) -> Unit) {
    var count by remember { mutableFloatStateOf(10f) }
    var selectedTypes by remember { mutableStateOf(setOf("SBA")) }
    var difficulty by remember { mutableStateOf("Medium") }
    var optionCount by remember { mutableIntStateOf(5) }
    var instructions by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("AI Generation Parameters", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Column { Text("Question Count: ${count.toInt()}", fontWeight = FontWeight.Bold); Slider(value = count, onValueChange = { count = it }, valueRange = 1f..50f) }
        Column {
            Text("Difficulty Level", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Easy", "Medium", "Hard").forEach { level -> FilterChip(selected = difficulty == level, onClick = { difficulty = level }, label = { Text(level) }) } }
        }
        Column {
            Text("Options Per Question", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf(4, 5).forEach { opt -> FilterChip(selected = optionCount == opt, onClick = { optionCount = opt }, label = { Text("$opt Options") }) } }
        }
        Column {
            Text("Question Types", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SBA", "MCQ").forEach { type -> FilterChip(selected = selectedTypes.contains(type), onClick = { val next = selectedTypes.toMutableSet(); if (next.contains(type)) next.remove(type) else next.add(type); if (next.isEmpty()) next.add("SBA"); selectedTypes = next }, label = { Text(type) }) }
            }
        }
        OutlinedTextField(value = instructions, onValueChange = { instructions = it }, modifier = Modifier.fillMaxWidth().height(100.dp), label = { Text("Special Context") }, placeholder = { Text("Focus on key features...") })
        Spacer(Modifier.height(16.dp))
        Button(onClick = { onProceed(ExtractionConfigData(count.toInt(), selectedTypes.toList(), difficulty, optionCount, customInstructions = instructions)) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Start AI Generation", fontWeight = FontWeight.Bold)
        }
    }
}

// ── Step 5: Processing ──────────────────────────────────────────────

@Composable
fun WaitingView(message: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
        Spacer(Modifier.height(32.dp))
        Text(message, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("This may take up to a minute.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

// ── Step 6: Review ──────────────────────────────────────────────────

@Composable
fun ReviewView(count: Int, collectionName: String, onFinished: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp)) }
        }
        Spacer(Modifier.height(24.dp))
        Text("$count Questions Prepared!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text("For \"$collectionName\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(40.dp))
        Button(onClick = onFinished, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Proceed to Editor", fontWeight = FontWeight.Bold)
        }
    }
}

// ── Error View ──────────────────────────────────────────────────────

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Oops! Something went wrong", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRetry) { Text("Try Again") }
    }
}
