package com.algorithmx.q_base.data.collections

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "ImportRepository"
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    init {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun recognizeTextFromImage(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            val result = recognizer.process(image).await()
            Result.success(result.text)
        } catch (e: Exception) {
            Log.e(TAG, "OCR failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun extractTextFromPdf(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                Result.success(cleanRecognizedText(text))
            } ?: Result.failure(Exception("Could not open PDF stream"))
        } catch (e: Exception) {
            Log.e(TAG, "PDF extraction failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun cleanRecognizedText(rawText: String): String {
        // Regex patterns for common cleanup
        var cleaned = rawText
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .replace(Regex("(?i)page \\d+"), "") // Remove page numbers
            .trim()
            
        // Add more specific regex as needed based on medical textbook patterns
        return cleaned
    }
}
