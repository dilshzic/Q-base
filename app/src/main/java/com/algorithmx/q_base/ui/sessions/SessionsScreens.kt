package com.algorithmx.q_base.ui.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.entity.MasterCategory
import com.algorithmx.q_base.data.entity.StudySession
import java.util.Locale

@Composable
fun SessionsListScreen(
    sessions: List<StudySession>,
    onSessionClick: (String) -> Unit,
    onFabClick: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onFabClick) {
                Icon(Icons.Default.Add, contentDescription = "Create Session")
            }
        }
    ) { padding ->
        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No past sessions found.\nTap + to start a new one.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(sessions) { session ->
                    ListItem(
                        headlineContent = { Text("Session ${session.sessionId.take(8)}") },
                        supportingContent = { 
                            val score = String.format(Locale.getDefault(), "%.1f", session.scoreAchieved)
                            Text("Score: $score%") 
                        },
                        trailingContent = {
                            if (session.scoreAchieved >= 80f) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.clickable { onSessionClick(session.sessionId) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSessionBottomSheet(
    categories: List<MasterCategory>,
    onDismiss: () -> Unit,
    onCreateSession: (String, Int, Boolean) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("") }
    var questionCountText by remember { mutableStateOf("10") }
    var isTimed by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Create New Session",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val selectedCategoryName = if (selectedCategory.isEmpty()) "Select Category" else selectedCategory
                OutlinedTextField(
                    value = selectedCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.masterCategory) },
                            onClick = {
                                selectedCategory = category.masterCategory
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = questionCountText,
                onValueChange = { if (it.all { char -> char.isDigit() }) questionCountText = it },
                label = { Text("Number of Questions") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Timed Session", modifier = Modifier.weight(1.0f))
                Switch(
                    checked = isTimed,
                    onCheckedChange = { isTimed = it }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val questionCount = questionCountText.toIntOrNull() ?: 0
            val isInputValid = selectedCategory.isNotEmpty() && questionCount > 0
            
            Button(
                onClick = { onCreateSession(selectedCategory, questionCount, isTimed) },
                modifier = Modifier.fillMaxWidth(),
                enabled = isInputValid
            ) {
                Text("Start Session")
            }
        }
    }
}
