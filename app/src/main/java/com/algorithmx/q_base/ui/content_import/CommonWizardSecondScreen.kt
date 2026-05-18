package com.algorithmx.q_base.ui.content_import

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ── Step 2: Import Config (Direct extraction layout settings) ────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportConfigView(onProceed: (List<String>, String) -> Unit) {
    var selectedTypes by remember { mutableStateOf(setOf("SBA")) }
    var instructions by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp), 
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Extraction Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        
        Column {
            Text("Target Layout Formats", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("SBA", "MCQ").forEach { type ->
                    FilterChip(
                        selected = selectedTypes.contains(type), 
                        onClick = {
                            val next = selectedTypes.toMutableSet()
                            if (next.contains(type)) next.remove(type) else next.add(type)
                            if (next.isEmpty()) next.add("SBA")
                            selectedTypes = next
                        }, 
                        label = { Text(type, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
        
        OutlinedTextField(
            value = instructions, 
            onValueChange = { instructions = it }, 
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp), 
            label = { Text("Custom Focus Instructions for AI") }, 
            placeholder = { Text("e.g. Only extract pathology questions. Emphasize laboratory markers...") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        
        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = { onProceed(selectedTypes.toList(), instructions) }, 
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp), 
            shape = RoundedCornerShape(18.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text("Begin Import", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ── Step 2: Generate Config (AI processing parameters) ──────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateConfigView(onProceed: (ExtractionConfigData) -> Unit) {
    var count by remember { mutableFloatStateOf(10f) }
    var selectedTypes by remember { mutableStateOf(setOf("SBA")) }
    var difficulty by remember { mutableStateOf("Medium") }
    var optionCount by remember { mutableIntStateOf(5) }
    var instructions by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()), 
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("AI Generation Parameters", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)

        // Study presets
        Text("Quick Study Presets", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presets = listOf(
                Triple("Exam Sim", 30, "Hard"),
                Triple("Quick Quiz", 10, "Medium"),
                Triple("Deep Dive", 15, "Medium")
            )
            presets.forEach { (label, pCount, pDiff) ->
                val isSelected = count.toInt() == pCount && difficulty == pDiff
                ElevatedFilterChip(
                    selected = isSelected,
                    onClick = {
                        count = pCount.toFloat()
                        difficulty = pDiff
                    },
                    label = { Text(label) },
                    colors = FilterChipDefaults.elevatedFilterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
        
        Column { 
            Text("Question Volume: ${count.toInt()}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Slider(
                value = count, 
                onValueChange = { count = it }, 
                valueRange = 5f..50f,
                steps = 9
            ) 
        }
        
        Column {
            Text("Complexity Standard", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { 
                listOf("Easy", "Medium", "Hard").forEach { level -> 
                    FilterChip(
                        selected = difficulty == level, 
                        onClick = { difficulty = level }, 
                        label = { Text(level, fontWeight = FontWeight.Bold) }
                    ) 
                } 
            }
        }
        
        Column {
            Text("Options Per Question", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { 
                listOf(4, 5).forEach { opt -> 
                    FilterChip(
                        selected = optionCount == opt, 
                        onClick = { optionCount = opt }, 
                        label = { Text("$opt Options", fontWeight = FontWeight.Bold) }
                    ) 
                } 
            }
        }
        
        Column {
            Text("Target Question Formats", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("SBA", "MCQ").forEach { type -> 
                    FilterChip(
                        selected = selectedTypes.contains(type), 
                        onClick = { 
                            val next = selectedTypes.toMutableSet()
                            if (next.contains(type)) next.remove(type) else next.add(type)
                            if (next.isEmpty()) next.add("SBA")
                            selectedTypes = next 
                        }, 
                        label = { Text(type, fontWeight = FontWeight.Bold) }
                    ) 
                }
            }
        }
        
        OutlinedTextField(
            value = instructions, 
            onValueChange = { instructions = it }, 
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp), 
            label = { Text("Special Context & Core Focus") }, 
            placeholder = { Text("Focus on mechanism of action, key diagnostics...") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = { onProceed(ExtractionConfigData(count.toInt(), selectedTypes.toList(), difficulty, optionCount, customInstructions = instructions)) }, 
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp), 
            shape = RoundedCornerShape(18.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text("Launch AI Brain Generator", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}
