package com.algorithmx.q_base.ui.explore

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.algorithmx.q_base.data.entity.Collection as AppCollection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionCreationWizard(
    onDismiss: () -> Unit,
    categories: List<AppCollection>,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    var step by remember { mutableIntStateOf(1) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    IconButton(onClick = { step-- }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
                Text(
                    text = "Create Collection",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$step/2",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider()

            Box(modifier = Modifier.heightIn(min = 300.dp)) {
                when (step) {
                    1 -> DetailsStep(
                        title = title,
                        onTitleChange = { title = it },
                        description = description,
                        onDescriptionChange = { description = it },
                        onNext = { step = 2 }
                    )
                    2 -> CategoryPickerStep(
                        categories = categories,
                        selected = selectedCategory,
                        onSelect = { selectedCategory = it },
                        onFinish = {
                            viewModel.createSet(title, description, selectedCategory ?: "General")
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DetailsStep(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("Collection Title") },
            placeholder = { Text("e.g. Cardiology Essentials") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            singleLine = true
        )
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            minLines = 3
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = title.isNotBlank(),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Next: Choose Category")
        }
    }
}

@Composable
fun CategoryPickerStep(
    categories: List<AppCollection>,
    selected: String?,
    onSelect: (String) -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Which category does this belong to?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        
        categories.forEach { category ->
            FilterChip(
                selected = selected == category.collectionId,
                onClick = { onSelect(category.collectionId) },
                label = { Text(category.name) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = selected != null,
            shape = MaterialTheme.shapes.large
        ) {
            Text("Create Collection")
        }
    }
}
