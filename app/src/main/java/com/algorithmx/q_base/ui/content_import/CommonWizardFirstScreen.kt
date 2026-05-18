package com.algorithmx.q_base.ui.content_import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.algorithmx.q_base.data.collections.StudyCollection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonWizardFirstScreen(
    name: String,
    description: String,
    extractedText: String,
    collections: List<StudyCollection>,
    selectedCategoryId: String?,
    onNameChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onRawTextUpdated: (String) -> Unit,
    onImagePicked: (Uri) -> Unit,
    onPdfPicked: (Uri) -> Unit,
    onDirectExtractionClick: () -> Unit,
    onNext: () -> Unit
) {
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { onImagePicked(it) } }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { onPdfPicked(it) } }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    // Auto-generate a title based on description if empty when submitting
    LaunchedEffect(description) {
        if (name.isBlank() && description.isNotBlank()) {
            val words = description.trim().split("\\s+".toRegex())
            val autoName = if (words.size >= 2) {
                words.take(3).joinToString(" ").replace("[^a-zA-Z0-9 ]".toRegex(), "")
            } else {
                "Practice Set"
            }
            onNameChanged(autoName)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Question Header
        Text(
            text = "What do you want to practice?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )

        // 2. Multiline Text Area (Describe your ideas)
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            label = { Text("Describe your ideas") },
            placeholder = { Text("e.g. Create practice questions about neurology emphasizing neurotransmitters...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            shape = RoundedCornerShape(16.dp),
            minLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        // 3. Button Set (Import Channels) - All matched in the same cohesive primary color scheme
        val buttonsColor = MaterialTheme.colorScheme.primary
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SmallSourceButton(
                icon = Icons.Rounded.PictureAsPdf,
                title = "Import PDF",
                color = buttonsColor,
                modifier = Modifier.weight(1f)
            ) { pdfPicker.launch("application/pdf") }

            SmallSourceButton(
                icon = Icons.Rounded.CameraAlt,
                title = "Text/OCR",
                color = buttonsColor,
                modifier = Modifier.weight(1f)
            ) { imagePicker.launch("image/*") }

            SmallSourceButton(
                icon = Icons.Rounded.ContentPaste,
                title = "Clipboard",
                color = buttonsColor,
                modifier = Modifier.weight(1f)
            ) {
                val clipText = clipboardManager.getText()?.text
                if (!clipText.isNullOrBlank()) {
                    onRawTextUpdated(clipText)
                }
            }
        }

        // Visual confirmation badge if text is successfully attached/imported
        if (extractedText.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reference Material Attached",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${extractedText.split("\\s+".toRegex()).size} words extracted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(
                        onClick = { onRawTextUpdated("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 4. Direct Extraction Portal - Zip Icon and distinct premium light green color
        val extractionButtonColor = Color(0xFF43A047) // Shade of premium light green
        OutlinedButton(
            onClick = onDirectExtractionClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = extractionButtonColor
            ),
            border = BorderStroke(2.dp, extractionButtonColor.copy(alpha = 0.4f))
        ) {
            Icon(
                imageVector = Icons.Rounded.FolderZip, 
                contentDescription = null, 
                modifier = Modifier.size(22.dp),
                tint = extractionButtonColor
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Direct Exam Paper Extraction",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelLarge,
                color = extractionButtonColor
            )
        }

        Spacer(Modifier.weight(1f))

        // 6. Proceed Button
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            enabled = description.isNotBlank(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text("Proceed to Next Step", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun SmallSourceButton(icon: ImageVector, title: String, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.5.dp, color.copy(alpha = 0.2f))
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp), 
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.ExtraBold)
        }
    }
}
