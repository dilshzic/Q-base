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
    @param:ApplicationContext private val context: Context
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
                val pageCount = document.numberOfPages
                if (pageCount > 80) {
                    document.close()
                    return@withContext Result.failure(
                        Exception("PDF too large ($pageCount pages). Please import max 80 pages at a time.")
                    )
                }
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                val cleaned = cleanRecognizedText(text)
                if (cleaned.isBlank()) {
                    Result.failure(Exception("No readable text found. The PDF may be image-based — try OCR instead."))
                } else {
                    Result.success(cleaned)
                }
            } ?: Result.failure(Exception("Could not open PDF stream"))
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "PDF OOM: ${e.message}")
            Result.failure(Exception("PDF is too large to process. Please try a smaller file."))
        } catch (e: Exception) {
            Log.e(TAG, "PDF extraction failed: ${e.message}")
            Result.failure(e)
        }
    }

    fun cleanRecognizedText(rawText: String): String {
        return rawText
            // Remove isolated page-number lines (e.g. a line that is just "Page 4" or "4")
            .replace(Regex("(?m)^\\s*[Pp]age\\s+\\d+\\s*$"), "")
            // Normalize horizontal whitespace only — preserve newlines for question structure
            .replace(Regex("[ \\t]+"), " ")
            // Collapse 3+ consecutive blank lines into a single blank line
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}
