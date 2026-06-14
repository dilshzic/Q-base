package com.algorithmx.q_base.feature.explore.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.core.ai.data.AiRepository
import com.algorithmx.q_base.core.ai.data.QuestionAiMessageDao
import com.algorithmx.q_base.core.ai.data.QuestionAiMessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class QuestionAiChatViewModel @Inject constructor(
    private val questionAiMessageDao: QuestionAiMessageDao,
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<QuestionAiMessageEntity>>(emptyList())
    val messages: StateFlow<List<QuestionAiMessageEntity>> = _messages.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private var currentQuestionId: String? = null
    var aiQuestionStem: String? = null
    private var chatJob: kotlinx.coroutines.Job? = null

    fun loadChatForQuestion(questionId: String, initialContext: String? = null) {
        if (currentQuestionId == questionId) return
        currentQuestionId = questionId
        aiQuestionStem = initialContext
        
        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            var isFirstLoad = true
            questionAiMessageDao.getMessagesForQuestion(questionId).collect { msgs ->
                _messages.value = msgs
                
                if (isFirstLoad) {
                    isFirstLoad = false
                    if (msgs.isEmpty()) {
                        sendMessage("Can you provide a brief overview of this question, and then ask me what I would like to explore regarding it?")
                    }
                }
            }
        }
    }

    fun sendMessage(text: String) {
        val questionId = currentQuestionId ?: return
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val userMessage = QuestionAiMessageEntity(
            messageId = messageId,
            questionId = questionId,
            sender = "USER",
            payload = text,
            timestamp = timestamp
        )

        viewModelScope.launch {
            questionAiMessageDao.insertMessage(userMessage)
            
            _isAiLoading.value = true
            try {
                val fullPrompt = buildString {
                    appendLine("You are an expert educational AI tutor. You are helping a user with a specific multiple choice question.")
                    if (!aiQuestionStem.isNullOrBlank()) {
                        appendLine("The question context is:")
                        appendLine(aiQuestionStem)
                    }
                    appendLine("The conversation history is as follows:")
                    _messages.value.forEach { m ->
                        appendLine("${m.sender}: ${m.payload}")
                    }
                    appendLine("USER: $text")
                    appendLine("AI:")
                }

                val result = aiRepository.getAiAssistance(fullPrompt)
                result.onSuccess { reply ->
                    val aiMessage = QuestionAiMessageEntity(
                        messageId = UUID.randomUUID().toString(),
                        questionId = questionId,
                        sender = "AI",
                        payload = reply,
                        timestamp = System.currentTimeMillis()
                    )
                    questionAiMessageDao.insertMessage(aiMessage)
                }.onFailure { e ->
                    val errorMsg = QuestionAiMessageEntity(
                        messageId = UUID.randomUUID().toString(),
                        questionId = questionId,
                        sender = "AI",
                        payload = "I'm sorry, I encountered an error: ${e.message}",
                        timestamp = System.currentTimeMillis()
                    )
                    questionAiMessageDao.insertMessage(errorMsg)
                }
            } catch (e: Exception) {
                // Keep UI simple
            } finally {
                _isAiLoading.value = false
            }
        }
    }
}
