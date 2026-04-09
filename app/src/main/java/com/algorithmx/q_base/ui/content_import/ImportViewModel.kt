package com.algorithmx.q_base.ui.content_import

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.collections.StudyCollection
import com.algorithmx.q_base.data.ai.AiRepository
import com.algorithmx.q_base.data.collections.ExploreRepository
import com.algorithmx.q_base.data.collections.ImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import com.algorithmx.q_base.data.core.UserEntity
import com.algorithmx.q_base.data.auth.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ExtractionConfigData(
    val count: Int,
    val types: List<String>,
    val difficulty: String = "Medium",
    val optionCount: Int = 5,
    val stemLength: String = "Medium",
    val customInstructions: String = "",
    val isResearchMode: Boolean = false
)

sealed class ImportStep {
    object Welcome : ImportStep()
    object MediaSelection : ImportStep()
    data class ActionSelection(val text: String) : ImportStep()
    data class ImportConfig(val text: String) : ImportStep()
    data class GenerateConfig(val text: String) : ImportStep()
    data class Extracting(val source: String) : ImportStep()
    data class Generating(val message: String = "AI is structuring your questions...") : ImportStep()
    data class Overview(val questionCount: Int, val responseId: String) : ImportStep()
    
    // Legacy/Internal states
    data class Editing(val extractedText: String) : ImportStep()
    data class Config(val extractedText: String, val targetId: String? = null) : ImportStep()
    data class Preview(val responseId: String, val response: com.algorithmx.q_base.core_ai.brain.models.AiCollectionResponse) : ImportStep()
    data class Complete(val message: String) : ImportStep()
    data class Error(val message: String) : ImportStep()
}

@HiltViewModel
class ImportViewModel @Inject constructor(
    private val importRepository: ImportRepository,
    private val aiRepository: AiRepository,
    private val exploreRepository: ExploreRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow<ImportStep>(ImportStep.Welcome)
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

        when (source?.uppercase()) {
            "IMAGE" -> _uiState.value = ImportStep.MediaSelection // Let UI trigger picker
            "PDF" -> _uiState.value = ImportStep.MediaSelection
            "TOPIC" -> _uiState.value = ImportStep.MediaSelection
            else -> _uiState.value = ImportStep.Welcome
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
        _uiState.value = step
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun updateCustomInstructions(instructions: String) {
        _customInstructions.value = instructions
    }

    fun onImagePicked(uri: Uri) {
        _uiState.value = ImportStep.Extracting("Image (OCR)")
        viewModelScope.launch {
            val result = importRepository.recognizeTextFromImage(uri)
            handleExtractionResult(result)
        }
    }

    fun onPdfPicked(uri: Uri) {
        _uiState.value = ImportStep.Extracting("PDF Document")
        viewModelScope.launch {
            val result = importRepository.extractTextFromPdf(uri)
            handleExtractionResult(result)
        }
    }

    fun onRawTextUpdated(text: String) {
        _extractedText.value = text
    }

    fun updateNewCollectionName(name: String) {
        _newCollectionName.value = name
    }

    private fun handleExtractionResult(result: Result<String>) {
        if (result.isSuccess) {
            _extractedText.value = result.getOrDefault("")
            _uiState.value = ImportStep.MediaSelection
        } else {
            _uiState.value = ImportStep.Error(result.exceptionOrNull()?.message ?: "Extraction failed")
        }
    }

    fun startDirectImport(types: List<String>, instructions: String) {
        val catId = _selectedCategoryId.value ?: "new"
        _uiState.value = ImportStep.Generating("AI is analyzing text for direct import...")
        
        viewModelScope.launch {
            val catName = _collections.value.find { it.collectionId == catId }?.name ?: "Imported"
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
        _uiState.value = ImportStep.Generating("AI is generating unique questions...")
        
        viewModelScope.launch {
            val catName = _collections.value.find { it.collectionId == catId }?.name ?: "Generated"
            
            // If extracted text is present, use it as context. Otherwise it's topic-based.
            // For now, the user prompt implies generating FROM existing text.
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
                    json.decodeFromString<com.algorithmx.q_base.core_ai.brain.models.AiCollectionResponse>(entity?.rawJson ?: "")
                        .questions.size
                } catch (e: Exception) { 0 }
                
                _uiState.value = ImportStep.Overview(count, responseId)
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

    fun reset() {
        _uiState.value = ImportStep.Welcome
        _extractedText.value = ""
    }
}
