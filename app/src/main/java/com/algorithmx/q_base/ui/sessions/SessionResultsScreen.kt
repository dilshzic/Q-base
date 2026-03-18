package com.algorithmx.q_base.ui.sessions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.algorithmx.q_base.data.entity.SessionAttempt
import com.algorithmx.q_base.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionResultsViewModel @Inject constructor(
    private val repository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val sessionId: String = checkNotNull(savedStateHandle["sessionId"])

    private val _uiState = MutableStateFlow<ResultsUiState>(ResultsUiState.Loading)
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    init {
        loadResults()
    }

    private fun loadResults() {
        viewModelScope.launch {
            repository.getAttemptsForSession(sessionId).collect { attempts ->
                val attemptedCount = attempts.count { it.attemptStatus == "ATTEMPTED" || it.attemptStatus == "FINALIZED" }
                val score = if (attempts.isNotEmpty()) (attemptedCount.toFloat() / attempts.size) * 100 else 0f
                _uiState.value = ResultsUiState.Success(attempts, score)
            }
        }
    }
}

sealed class ResultsUiState {
    object Loading : ResultsUiState()
    data class Success(val attempts: List<SessionAttempt>, val score: Float) : ResultsUiState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionResultsScreen(
    viewModel: SessionResultsViewModel = hiltViewModel(),
    onBackToHome: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Session Results") })
        }
    ) { padding ->
        when (val state = uiState) {
            is ResultsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ResultsUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Your Score",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${state.score.toInt()}%",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    Text(
                        text = "Question Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(40.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        itemsIndexed(state.attempts) { index, attempt ->
                            val color = when (attempt.attemptStatus) {
                                "FINALIZED", "ATTEMPTED" -> Color(0xFF2E7D32) // Success green
                                "FLAGGED" -> Color(0xFFFFA500) // Orange
                                else -> Color.LightGray
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    
                    Button(
                        onClick = onBackToHome,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Return to Home")
                    }
                }
            }
        }
    }
}
