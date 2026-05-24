package com.algorithmx.q_base.feature.content_import.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.core.ai.data.AiRepository
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.collections.ExploreRepository
import com.algorithmx.q_base.data.collections.ImportRepository
// legacy import removed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.algorithmx.q_base.core.data.UserEntity
import com.algorithmx.q_base.core.data.auth.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.UUID

data class ExtractionConfigData(
    val count: Int,
    val types: List<String>,
    val difficulty: String = "Medium",
    val optionCount: Int = 5,
    val stemLength: String = "Medium",
    val customInstructions: String = "",
    val isResearchMode: Boolean = false
)

/**
 * Redesigned import step flow:
 *  1. NameAndDestination - Name, describe, choose new vs existing collection
 *  2. ChooseMethod - Import media, AI generate, or manual build
 *  3. MediaInput - OCR/PDF/text input (for import and AI generate paths)
 *  4. Configure - AI configuration (types, difficulty, count, instructions)
 *  5. Processing - AI generation in progress
 *  6. Review - Show results, proceed to editor
 *  7. Error - Error with retry
 */
sealed class ImportStep {
    data object NameAndDestination : ImportStep()
    data object ChooseMethod : ImportStep()
    data object MediaInput : ImportStep()
    data class Configure(val mode: String) : ImportStep() // "IMPORT" or "GENERATE"
    data class Processing(val message: String = "AI is structuring your questions...") : ImportStep()
    data class Review(val questionCount: Int, val responseId: String) : ImportStep()
    data class Error(val message: String) : ImportStep()

    // Direct Exam Paper Extraction pipeline states
    data object ExtractionIngest : ImportStep()
    data class ExtractionOverview(val responseId: String, val response: com.algorithmx.q_base.core.ai.brain.models.AiCollectionResponse) : ImportStep()

    // Legacy states kept for internal processing
    data class Extracting(val source: String) : ImportStep()
    data class Editing(val extractedText: String) : ImportStep()
    data class Config(val extractedText: String, val targetId: String? = null) : ImportStep()
    data class Preview(val responseId: String, val response: com.algorithmx.q_base.core.ai.brain.models.AiCollectionResponse) : ImportStep()
    data class Complete(val message: String) : ImportStep()
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importRepository: ImportRepository,
    private val aiRepository: AiRepository,
    private val exploreRepository: ExploreRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow<ImportStep>(ImportStep.NameAndDestination)
    val uiState = _uiState.asStateFlow()

    private val _collections = MutableStateFlow<List<StudyCollection>>(emptyList())
    val collections = _collections.asStateFlow()

    private val _customInstructions = MutableStateFlow("")
    val customInstructions = _customInstructions.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()
    
    private val _extractedText = MutableStateFlow("")
    val extractedText = _extractedText.asStateFlow()

    private val _newCollectionName = MutableStateFlow("")
    val newCollectionName = _newCollectionName.asStateFlow()

    private val _newCollectionDescription = MutableStateFlow("")
    val newCollectionDescription = _newCollectionDescription.asStateFlow()

    // Direct Exam Paper Extraction pipeline states
    private val _extractedDocs = MutableStateFlow<List<ExtractedDocumentCard>>(emptyList())
    val extractedDocs = _extractedDocs.asStateFlow()
    
    val docTexts = mutableMapOf<String, String>()

    // Tracks which method the user chose for back-navigation
    private val _selectedMethod = MutableStateFlow<String?>(null) // "IMPORT", "GENERATE", "MANUAL"
    val selectedMethod = _selectedMethod.asStateFlow()

    // Dynamic question type selection for paper extraction
    private val _selectedPaperTypes = MutableStateFlow<List<String>>(listOf("SBA", "MTF", "EMQ"))
    val selectedPaperTypes = _selectedPaperTypes.asStateFlow()

    // Loading flag for in-place doc ingest (PDF/OCR) — avoids transient step jumps
    private val _isAddingDoc = MutableStateFlow(false)
    val isAddingDoc = _isAddingDoc.asStateFlow()

    // Tracks last meaningful wizard step for freezing progress bar on Error
    private var _lastMeaningfulStep = 1

    fun togglePaperType(type: String) {
        val current = _selectedPaperTypes.value
        _selectedPaperTypes.value = if (current.contains(type)) {
            if (current.size > 1) current - type else current // Keep at least one
        } else {
            current + type
        }
    }

    val currentUser = authRepository.currentUser
        .map { firebaseUser ->
            firebaseUser?.let {
                UserEntity(
                    userId = it.uid,
                    displayName = it.displayName ?: "User",
                    email = it.email ?: "",
                    profilePictureUrl = it.photoUrl?.toString(),
                    friendCode = ""
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadCollections()
    }

    fun setInitialSource(source: String?, targetId: String? = null) {
        if (targetId != null) {
            _selectedCategoryId.value = targetId
        }
        // If a source is pre-specified, skip ahead
        when (source?.uppercase()) {
            "IMAGE", "PDF", "TOPIC" -> _uiState.value = ImportStep.MediaInput
            else -> _uiState.value = ImportStep.NameAndDestination
        }
    }

    private fun loadCollections() {
        viewModelScope.launch {
            exploreRepository.getStudyCollections().collect {
                _collections.value = it
            }
        }
    }

    fun navigateTo(step: ImportStep) {
        // Track the last meaningful step so the progress bar can freeze there on Error
        if (step !is ImportStep.Error &&
            step !is ImportStep.Extracting &&
            step !is ImportStep.Processing) {
            _lastMeaningfulStep = currentStepNumber()
        }
        _uiState.value = step
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun updateNewCollectionName(name: String) {
        _newCollectionName.value = name
    }

    fun updateNewCollectionDescription(desc: String) {
        _newCollectionDescription.value = desc
    }

    fun updateCustomInstructions(instructions: String) {
        _customInstructions.value = instructions
    }

    fun selectMethod(method: String) {
        _selectedMethod.value = method
    }

    fun onImagePicked(uri: Uri) {
        // No transient state change — loading is handled inside CommonWizardFirstScreen
        viewModelScope.launch {
            val result = importRepository.recognizeTextFromImage(uri)
            handleExtractionResult(result)
        }
    }

    fun onPdfPicked(uri: Uri) {
        // No transient state change — loading is handled inside CommonWizardFirstScreen
        viewModelScope.launch {
            val result = importRepository.extractTextFromPdf(uri)
            handleExtractionResult(result)
        }
    }

    fun onRawTextUpdated(text: String) {
        _extractedText.value = text
    }

    private fun handleExtractionResult(result: Result<String>) {
        if (result.isSuccess) {
            _extractedText.value = result.getOrDefault("")
            _uiState.value = ImportStep.MediaInput
        } else {
            _uiState.value = ImportStep.Error(result.exceptionOrNull()?.message ?: "Extraction failed")
        }
    }

    fun startDirectImport(types: List<String>, instructions: String) {
        val catId = _selectedCategoryId.value ?: "new"
        _uiState.value = ImportStep.Processing("AI is analyzing text for direct import...")
        
        viewModelScope.launch {
            val catName = _collections.value.find { it.collectionId == catId }?.name ?: _newCollectionName.value.ifBlank { "Imported" }
            val result = aiRepository.extractQuestionsFromText(
                text = _extractedText.value,
                collectionId = catId,
                collectionName = catName,
                difficulty = "Balanced",
                types = types,
                customInstructions = instructions
            )
            handleGenerationResult(result)
        }
    }

    fun startAiGeneration(config: ExtractionConfigData) {
        val catId = _selectedCategoryId.value ?: "new"
        _uiState.value = ImportStep.Processing("AI is generating unique questions...")
        
        viewModelScope.launch {
            val catName = _collections.value.find { it.collectionId == catId }?.name ?: _newCollectionName.value.ifBlank { "Generated" }
            val result = aiRepository.extractQuestionsFromText(
                text = _extractedText.value,
                collectionId = catId,
                collectionName = catName,
                difficulty = config.difficulty,
                types = config.types,
                customInstructions = "Generate ${config.count} questions. ${config.customInstructions}"
            )
            handleGenerationResult(result)
        }
    }

    private fun handleGenerationResult(result: Result<String>) {
        if (result.isSuccess) {
            val responseId = result.getOrThrow()
            viewModelScope.launch {
                val entity = aiRepository.getAiResponseById(responseId)
                val count = try {
                        json.decodeFromString<com.algorithmx.q_base.core.ai.brain.models.AiCollectionResponse>(entity?.rawJson ?: "")
                        .questions.size
                } catch (e: Exception) { 0 }
                
                _uiState.value = ImportStep.Review(count, responseId)
            }
        } else {
            val error = result.exceptionOrNull()?.message ?: "Question generation failed"
            _uiState.value = ImportStep.Error(error)
        }
    }

    fun promoteResponse(responseId: String, customName: String? = null, onFinished: (String, String?) -> Unit) {
        viewModelScope.launch {
            val targetName = _collections.value.find { it.collectionId == _selectedCategoryId.value }?.name
            val finalCustomName = customName ?: _newCollectionName.value.ifBlank { null }
            
            val result = aiRepository.promoteAiResponseToDatabase(
                responseId = responseId, 
                targetCollectionId = _selectedCategoryId.value, 
                targetCollectionName = targetName,
                overrideName = if (_selectedCategoryId.value == null) finalCustomName else null
            )
            
            if (result.isSuccess) {
                val setId = result.getOrThrow()
                onFinished(setId, finalCustomName)
            } else {
                _uiState.value = ImportStep.Error(result.exceptionOrNull()?.message ?: "Promotion failed")
            }
        }
    }

    fun addPdf(uri: Uri) {
        // Use isAddingDoc flag instead of transitioning to Processing
        // so the progress bar doesn't shoot to step 5 and snap back
        _isAddingDoc.value = true
        viewModelScope.launch {
            val result = importRepository.extractTextFromPdf(uri)
            if (result.isSuccess) {
                val text = result.getOrDefault("")
                val id = UUID.randomUUID().toString()
                val wordCount = text.split("\\s+".toRegex()).size
                val card = ExtractedDocumentCard(
                    id = id,
                    name = uri.lastPathSegment ?: "PDF Document",
                    wordCount = wordCount,
                    type = "PDF"
                )
                docTexts[id] = text
                _extractedDocs.value = _extractedDocs.value + card
                _uiState.value = ImportStep.ExtractionIngest
            } else {
                _uiState.value = ImportStep.Error(result.exceptionOrNull()?.message ?: "PDF extraction failed")
            }
            _isAddingDoc.value = false
        }
    }

    fun addOcr(uri: Uri) {
        // Use isAddingDoc flag instead of transitioning to Processing
        _isAddingDoc.value = true
        viewModelScope.launch {
            val result = importRepository.recognizeTextFromImage(uri)
            if (result.isSuccess) {
                val text = result.getOrDefault("")
                val id = UUID.randomUUID().toString()
                val wordCount = text.split("\\s+".toRegex()).size
                val card = ExtractedDocumentCard(
                    id = id,
                    name = "OCR Document",
                    wordCount = wordCount,
                    type = "OCR"
                )
                docTexts[id] = text
                _extractedDocs.value = _extractedDocs.value + card
                _uiState.value = ImportStep.ExtractionIngest
            } else {
                _uiState.value = ImportStep.Error(result.exceptionOrNull()?.message ?: "OCR failed")
            }
            _isAddingDoc.value = false
        }
    }

    fun addClipboard(text: String) {
        val id = UUID.randomUUID().toString()
        val wordCount = text.split("\\s+".toRegex()).size
        val card = ExtractedDocumentCard(
            id = id,
            name = "Clipboard Content",
            wordCount = wordCount,
            type = "CLIPBOARD"
        )
        docTexts[id] = text
        _extractedDocs.value = _extractedDocs.value + card
    }

    fun removeDoc(id: String) {
        _extractedDocs.value = _extractedDocs.value.filterNot { it.id == id }
        docTexts.remove(id)
    }

    fun startPaperExtraction() {
        val combinedText = _extractedDocs.value.joinToString("\n\n---\n\n") { docTexts[it.id] ?: "" }
        _uiState.value = ImportStep.Processing("AI is extracting exam questions...")
        
        viewModelScope.launch {
            val catId = _selectedCategoryId.value ?: "new"
            val catName = _collections.value.find { it.collectionId == catId }?.name ?: _newCollectionName.value.ifBlank { "Extracted Paper" }
            
            val result = aiRepository.extractQuestionsFromText(
                text = combinedText,
                collectionId = catId,
                collectionName = catName,
                difficulty = "Balanced",
                types = _selectedPaperTypes.value,
                customInstructions = "Ensure questions are parsed correctly as SBA, MTF, or EMQ. List any skipped non-question blocks in skippedSegments and any parsing errors/warnings in parsingWarnings."
            )
            
            if (result.isSuccess) {
                val responseId = result.getOrThrow()
                val entity = aiRepository.getAiResponseById(responseId)
                val response = try {
                        json.decodeFromString<com.algorithmx.q_base.core.ai.brain.models.AiCollectionResponse>(entity?.rawJson ?: "")
                } catch (e: Exception) { null }
                
                if (response != null) {
                    _newCollectionName.value = response.collectionTitle
                    _newCollectionDescription.value = response.collectionDescription
                    _uiState.value = ImportStep.ExtractionOverview(responseId, response)
                } else {
                    _uiState.value = ImportStep.Error("Failed to parse AI extraction results.")
                }
            } else {
                _uiState.value = ImportStep.Error(result.exceptionOrNull()?.message ?: "Extraction failed")
            }
        }
    }

    fun handleRetry() {
        if (_extractedDocs.value.isNotEmpty()) {
            _uiState.value = ImportStep.ExtractionIngest
        } else {
            reset()
        }
    }

    fun reset() {
        _uiState.value = ImportStep.NameAndDestination
        _extractedText.value = ""
        _extractedDocs.value = emptyList()
        docTexts.clear()
        _selectedMethod.value = null
    }

    /**
     * Returns the current step number (1-based) for the progress indicator.
     *
     * Standard AI-generate path (5 total steps):
     *   NameAndDestination / MediaInput → 1
     *   Configure → 2
     *   Processing → 3
     *   Review → 4
     *   (complete) → 5
     *
     * Direct Extraction path (3 total steps):
     *   NameAndDestination → 1
     *   ExtractionIngest → 2
     *   ExtractionOverview → 3
     *
     * Error: frozen at last meaningful step via _lastMeaningfulStep.
     * Transient states (Extracting, legacy) never change the bar.
     */
    fun currentStepNumber(): Int = when (val s = _uiState.value) {
        is ImportStep.NameAndDestination,
        is ImportStep.MediaInput -> 1
        is ImportStep.Configure -> 2
        is ImportStep.Processing -> 3
        is ImportStep.Review -> 4
        is ImportStep.ExtractionIngest -> 2
        is ImportStep.ExtractionOverview -> 3
        is ImportStep.Error -> _lastMeaningfulStep
        // Legacy / transient states — don't move the bar
        else -> _lastMeaningfulStep
    }

    /**
     * Total number of steps for progress indicator.
     * The extraction path has 3 steps; the AI-generate path has 5.
     * We use 5 as the denominator so both paths display proportionally.
     */
    fun totalSteps(): Int = 5
}