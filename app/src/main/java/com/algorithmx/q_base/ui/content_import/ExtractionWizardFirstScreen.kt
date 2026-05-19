package com.algorithmx.q_base.ui.content_import

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ExtractedDocumentCard(
    val id: String,
    val name: String,
    val wordCount: Int,
    val type: String // "PDF", "OCR", "CLIPBOARD"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractionWizardFirstScreen(
    extractedDocs: List<ExtractedDocumentCard>,
    selectedPaperTypes: List<String>,
    onTogglePaperType: (String) -> Unit,
    onAddPdf: (Uri) -> Unit,
    onAddOcr: (Uri) -> Unit,
    onAddClipboard: (String) -> Unit,
    onRemoveDoc: (String) -> Unit,
    onProceed: () -> Unit
) {
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { onAddPdf(it) } }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { it?.let { onAddOcr(it) } }
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Text(
            text = "Direct Paper Extraction",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
        Text(
            text = "Ingest structured exam papers, question sheets, or past textbooks to extract formatted practice tests instantly.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Cohesive primary-tinted channel buttons
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
                    onAddClipboard(clipText)
                }
            }
        }

        Text(
            text = "Ingested Reference Materials (${extractedDocs.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Ingested Documents Lazy Column
        if (extractedDocs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(54.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "No documents added yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(extractedDocs, key = { it.id }) { doc ->
                    ExtractedDocItem(
                        doc = doc,
                        onClearClick = { onRemoveDoc(doc.id) }
                    )
                }
            }
        }

        // Question formats selection row
        Text(
            text = "Select formats to extract",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("SBA", "MTF", "EMQ").forEach { format ->
                val isSelected = selectedPaperTypes.contains(format)
                FilterChip(
                    selected = isSelected,
                    onClick = { onTogglePaperType(format) },
                    label = { Text(format, fontWeight = FontWeight.Bold) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.5.dp
                    )
                )
            }
        }

        // Proceed Button
        Button(
            onClick = onProceed,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(18.dp),
            enabled = extractedDocs.isNotEmpty(),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text("Begin Document Extraction", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ExtractedDocItem(
    doc: ExtractedDocumentCard,
    onClearClick: () -> Unit
) {
    val icon = when (doc.type) {
        "PDF" -> Icons.Rounded.PictureAsPdf
        "OCR" -> Icons.Rounded.CameraAlt
        else -> Icons.Rounded.ContentPaste
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = "${doc.wordCount} words extracted",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onClearClick,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
