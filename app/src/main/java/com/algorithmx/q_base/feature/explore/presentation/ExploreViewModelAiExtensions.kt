package com.algorithmx.q_base.feature.explore.presentation

import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.collections.Answer
import com.algorithmx.q_base.data.collections.StudyCollection
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

fun ExploreViewModel.askAi(index: Int, mode: String = "EXPLAIN") {
    val state = _questionStates.value.getOrNull(index) ?: return
    val question = state.question
    
    viewModelScope.launch {
        _questionStates.update { current ->
            current.mapIndexed { i, s -> if (i == index) s.copy(isAiLoading = true) else s }
        }
        
        val prompt = when(mode) {
            "HINT" -> "Give me a subtle hint for this question without revealing the answer: ${question.stem}"
            "DETAILED" -> "Provide a summary related to this question: ${question.stem}"
            else -> "Explain this question and why the options are correct or incorrect: ${question.stem}. Options: ${state.options.joinToString { "${it.optionLetter}: ${it.optionText}" }}"
        }
        
        val result = aiRepository.getAiAssistance(prompt)
        
        _questionStates.update { current ->
            current.mapIndexed { i, s -> 
                if (i == index) s.copy(
                    aiResponse = result.getOrNull() ?: "Failed to get AI assistance: ${result.exceptionOrNull()?.message}",
                    isAiLoading = false
                ) else s 
            }
        }
    }
}

fun ExploreViewModel.askAiAboutCollection(collection: StudyCollection) {
    viewModelScope.launch {
        _isCollectionAiLoading.value = true
        _collectionAiResponse.value = "" // Clear previous
        
        val prompt = """
            Provide a comprehensive summary and key learning points for the study collection: '${collection.name}'.
            Description: ${collection.description ?: "N/A"}
            
            Focus on high-yield information and potential exam topics.
        """.trimIndent()
        
        val result = aiRepository.getAiAssistance(prompt)
        
        _collectionAiResponse.value = result.getOrNull() ?: "Failed to get AI assistance: ${result.exceptionOrNull()?.message}"
        _isCollectionAiLoading.value = false
    }
}

fun ExploreViewModel.clearCollectionAiResponse() {
    _collectionAiResponse.value = null
}

fun ExploreViewModel.clearAiResponse(index: Int) {
    _questionStates.update { current ->
        current.mapIndexed { i, s -> if (i == index) s.copy(aiResponse = null) else s }
    }
}

fun ExploreViewModel.saveAiResponseToQuestion(index: Int) {
    val state = _questionStates.value.getOrNull(index) ?: return
    val aiExp = state.aiResponse ?: return
    
    viewModelScope.launch {
        val currentAnswer = state.answer ?: Answer(
            questionId = state.question.questionId,
            correctAnswerString = "A", // Default if missing
            generalExplanation = aiExp,
            references = ""
        )
        
        val updatedAnswer = currentAnswer.copy(generalExplanation = aiExp)
        repository.saveAnswer(updatedAnswer)
        
        _questionStates.update { current ->
            current.mapIndexed { i, s -> 
                if (i == index) s.copy(answer = updatedAnswer, aiResponse = null) else s 
            }
        }
    }
}