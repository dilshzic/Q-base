package com.algorithmx.q_base.ui.content_import

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.collections.StudyCollection

// ── Step 4: Success & Interactive AI Generated Collection Overview ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewView(
    count: Int,
    collectionName: String,
    collections: List<StudyCollection>,
    selectedCategoryId: String?,
    onNameChanged: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onFinished: () -> Unit
) {
    val scale = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f, 
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    var isNewCollection by remember { mutableStateOf(selectedCategoryId == null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Success Orb Badge
        Surface(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }, 
            shape = CircleShape, 
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) { 
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.primary, 
                    modifier = Modifier.size(48.dp)
                ) 
            }
        }

        // 2. Collection Title & Questions Count
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Collection Prepared!", 
                style = MaterialTheme.typography.headlineSmall, 
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            ) {
                Text(
                    text = "$count practice questions created successfully",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Divider
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // 3. AI Suggested Title (Editable Text OutlinedTextField)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Suggested Title",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            OutlinedTextField(
                value = collectionName,
                onValueChange = onNameChanged,
                placeholder = { Text("e.g. Neurology Quiz") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
        }

        // 4. Save Location Selector (Category folders picker)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Organize Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isNewCollection,
                    onClick = { isNewCollection = true; onCategorySelected(null) },
                    label = { Text("Create New Folder", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp)
                )
                FilterChip(
                    selected = !isNewCollection,
                    onClick = { isNewCollection = false },
                    label = { Text("Add to Existing Folder", fontWeight = FontWeight.Bold) },
                    shape = RoundedCornerShape(12.dp),
                    enabled = collections.isNotEmpty()
                )
            }

            if (!isNewCollection && collections.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.FolderOpen, 
                                contentDescription = null, 
                                modifier = Modifier.size(20.dp), 
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = collections.find { it.collectionId == selectedCategoryId }?.name ?: "Select Folder...",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        collections.forEach { col ->
                            DropdownMenuItem(
                                text = { Text(col.name, fontWeight = FontWeight.Medium) },
                                onClick = { 
                                    onCategorySelected(col.collectionId)
                                    expanded = false 
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        // 5. Proceed Button (Edit Collection Screen)
        Button(
            onClick = onFinished, 
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp), 
            shape = RoundedCornerShape(18.dp),
            enabled = collectionName.isNotBlank(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Icon(Icons.Rounded.EditNote, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Proceed to Edit Collection", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

// ── Error View ──────────────────────────────────────────────────────

@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp), 
        verticalArrangement = Arrangement.Center, 
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Oops! Something went wrong", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(16.dp)
        ) { 
            Text("Try Again", fontWeight = FontWeight.Bold) 
        }
    }
}
