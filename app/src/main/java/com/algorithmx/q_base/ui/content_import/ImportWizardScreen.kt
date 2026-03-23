package com.algorithmx.q_base.ui.content_import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.tween
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.algorithmx.q_base.ui.components.ProfileIconButton
import com.algorithmx.q_base.data.entity.Collection as AppCollection
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
    val newCollectionName by viewModel.newCollectionName.collectAsStateWithLifecycle()
    
    LaunchedEffect(Unit) {
        viewModel.setInitialSource(source, targetId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Universal Creation", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
                    ProfileIconButton(
                        user = currentUser,
                        onClick = onProfileClick
                    )
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300, delayMillis = 90)) + 
                     slideInHorizontally(initialOffsetX = { it / 2 }))
                        .togetherWith(fadeOut(animationSpec = tween(300)) + 
                                     slideOutHorizontally(targetOffsetX = { -it / 2 }))
                },
                label = "WizardStepTransition"
            ) { step ->
                when (step) {
                    is ImportStep.Welcome -> WelcomeView(
                        onImportGenerate = { viewModel.navigateTo(ImportStep.MediaSelection) },
                        onManualCreate = { name -> onNavigateToManualEditor(targetId ?: "new", name) }
                    )
                    
                    is ImportStep.MediaSelection -> MediaSelectionView(
                        collections = collections,
                        text = extractedText,
                        categoryId = selectedCategoryId,
                        onSelectCategory = { viewModel.selectCategory(it) },
                        onImagePicked = { viewModel.onImagePicked(it) },
                        onPdfPicked = { viewModel.onPdfPicked(it) },
                        onTextUpdated = { viewModel.onRawTextUpdated(it) },
                        onDirectImport = { viewModel.navigateTo(ImportStep.ImportConfig("")) },
                        onAiGenerate = { viewModel.navigateTo(ImportStep.GenerateConfig("")) }
                    )

                    is ImportStep.ImportConfig -> ImportConfigView(
                        onProceed = { types, instructions -> 
                            viewModel.startDirectImport(types, instructions)
                        }
                    )

                    is ImportStep.GenerateConfig -> GenerateConfigView(
                        onProceed = { config ->
                            viewModel.startAiGeneration(config)
                        }
                    )

                    is ImportStep.Generating -> WaitingView(step.message)
                    
                    is ImportStep.Overview -> OverviewView(
                        count = step.questionCount,
                        isNewCollection = selectedCategoryId == null,
                        name = newCollectionName,
                        onNameChanged = { viewModel.updateNewCollectionName(it) },
                        onFinished = { 
                            viewModel.promoteResponse(step.responseId, newCollectionName) { setId, finalName ->
                                onNavigateToManualEditor(setId, finalName)
                            }
                        }
                    )

                    is ImportStep.Error -> ErrorView(
                        message = step.message,
                        onRetry = { viewModel.reset() }
                    )
                    else -> Box(Modifier.fillMaxSize()) // Fallback
                }
            }
        }
    }
}

@Composable
fun WelcomeView(
    onImportGenerate: () -> Unit,
    onManualCreate: (String?) -> Unit
) {
    var showNamingDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }

    if (showNamingDialog) {
        AlertDialog(
            onDismissRequest = { showNamingDialog = false },
            title = { Text("New Collection Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    placeholder = { Text("e.g. Cardiology Basics") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = { 
                        showNamingDialog = false
                        onManualCreate(tempName)
                    },
                    enabled = tempName.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNamingDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Quick Creation",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Text(
            "Choose how you want to build your collection.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Prominent Import/Generate Button
        Surface(
            onClick = onImportGenerate,
            modifier = Modifier.fillMaxWidth().height(160.dp),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(modifier = Modifier.width(20.dp))
                Column {
                    Text("Import / Generate", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Use AI to extract or generate from OCR, PDF, or Plain Text", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        // Manual Creation Button
        OutlinedCard(
            onClick = { showNamingDialog = true },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Manual Creation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Build your collection question by question", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun MediaSelectionView(
    collections: List<AppCollection>,
    text: String,
    categoryId: String?,
    onSelectCategory: (String?) -> Unit,
    onImagePicked: (Uri) -> Unit,
    onPdfPicked: (Uri) -> Unit,
    onTextUpdated: (String) -> Unit,
    onDirectImport: () -> Unit,
    onAiGenerate: () -> Unit
) {
    var rawText by remember(text) { mutableStateOf(text) }
    var selectedCatId by remember(categoryId) { mutableStateOf(categoryId) }
    
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { onImagePicked(it!!) }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { onPdfPicked(it!!) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Import Media", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        
        // Category Picker (Subtle)
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedCard(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(collections.find { it.collectionId == selectedCatId }?.name ?: "New Collection (Master)")
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("New Collection") }, onClick = { selectedCatId = null; onSelectCategory(null); expanded = false })
                collections.forEach { col ->
                    DropdownMenuItem(text = { Text(col.name) }, onClick = { selectedCatId = col.collectionId; onSelectCategory(col.collectionId); expanded = false })
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SmallSourceButton(Icons.Rounded.CameraAlt, "OCR/Photo", Color(0xFF6200EE)) { imagePicker.launch("image/*") }
            SmallSourceButton(Icons.Rounded.PictureAsPdf, "PDF", Color(0xFFF44336)) { pdfPicker.launch("application/pdf") }
        }
        
        // Large Text Area
        OutlinedTextField(
            value = rawText,
            onValueChange = { rawText = it; onTextUpdated(it) },
            modifier = Modifier.weight(1f).fillMaxWidth(),
            placeholder = { Text("Paste imported text here or type manually...") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
            )
        )
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onDirectImport,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = rawText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Direct Import")
            }
            Button(
                onClick = onAiGenerate,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = rawText.isNotBlank()
            ) {
                Text("AI Generate")
            }
        }
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
                    FilterChip(
                        selected = selectedTypes.contains(type),
                        onClick = {
                            val next = selectedTypes.toMutableSet()
                            if (next.contains(type)) next.remove(type) else next.add(type)
                            selectedTypes = next
                        },
                        label = { Text(type) }
                    )
                }
            }
        }
        
        OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            label = { Text("Custom Instructions for AI") },
            placeholder = { Text("e.g. Only extract pathology questions...") }
        )
        
        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = { onProceed(selectedTypes.toList(), instructions) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Begin Import", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GenerateConfigView(onProceed: (ExtractionConfigData) -> Unit) {
    var count by remember { mutableFloatStateOf(10f) }
    var selectedTypes by remember { mutableStateOf(setOf("SBA")) }
    var difficulty by remember { mutableStateOf("Medium") }
    var optionCount by remember { mutableIntStateOf(5) }
    var instructions by remember { mutableStateOf("") }
    
    Column(modifier = Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("AI Generation Parameters", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        
        Column {
            Text("Question Count: ${count.toInt()}", fontWeight = FontWeight.Bold)
            Slider(value = count, onValueChange = { count = it }, valueRange = 1f..50f)
        }
        
        Column {
            Text("Difficulty Level", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Easy", "Medium", "Hard").forEach { level ->
                    FilterChip(
                        selected = difficulty == level,
                        onClick = { difficulty = level },
                        label = { Text(level) }
                    )
                }
            }
        }

        Column {
            Text("Options Per Question", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 5).forEach { opt ->
                    FilterChip(
                        selected = optionCount == opt,
                        onClick = { optionCount = opt },
                        label = { Text("$opt Options") }
                    )
                }
            }
        }

        Column {
            Text("Question Types", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SBA", "MCQ").forEach { type ->
                    FilterChip(
                        selected = selectedTypes.contains(type),
                        onClick = {
                            val next = selectedTypes.toMutableSet()
                            if (next.contains(type)) next.remove(type) else next.add(type)
                            if (next.isEmpty()) next.add("SBA") // Ensure at least one
                            selectedTypes = next
                        },
                        label = { Text(type) }
                    )
                }
            }
        }
        
        OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            label = { Text("Special Context") },
            placeholder = { Text("Focus on clinical features...") }
        )
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = { 
                onProceed(ExtractionConfigData(count.toInt(), selectedTypes.toList(), difficulty, optionCount, customInstructions = instructions))
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Start AI Generation", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WaitingView(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(64.dp), strokeWidth = 6.dp)
        Spacer(Modifier.height(32.dp))
        Text(message, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text("This may take up to a minute.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun OverviewView(
    count: Int, 
    isNewCollection: Boolean,
    name: String,
    onNameChanged: (String) -> Unit,
    onFinished: () -> Unit
) {
    if (!isNewCollection) {
        LaunchedEffect(Unit) {
            delay(3000)
            onFinished()
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("$count Questions Prepared!", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        
        if (isNewCollection) {
            Spacer(Modifier.height(32.dp))
            Text("Name Your Collection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g., Clinical Anatomy") },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onFinished,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = name.isNotBlank()
            ) {
                Text("Proceed to Editor", fontWeight = FontWeight.Bold)
            }
        } else {
            Text("Navigating to editor...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}
@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("Oops! Something went wrong", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onRetry) {
            Text("Try Again")
        }
    }
}
