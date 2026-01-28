package com.playerid.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import java.io.*
import java.net.URL
import java.util.*

/**
 * 🌐 Online Jersey Photo Importer
 * 
 * Downloads jersey photos from URLs and allows manual validation for training data.
 * Much more efficient than capturing photos individually.
 */
class OnlineJerseyImporter(
    private val context: Context,
    private val datasetCollector: JerseyDatasetCollector
) {
    
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    companion object {
        private const val TAG = "OnlineJerseyImporter"
    }
    
    /**
     * 📸 Import single jersey photo from URL
     */
    suspend fun importJerseyPhoto(
        imageUrl: String,
        suggestedNumber: Int? = null,
        teamName: String? = null,
        sport: String? = null
    ): ImportResult = withContext(Dispatchers.IO) {
        
        try {
            Log.d(TAG, "🌐 Downloading jersey photo: $imageUrl")
            
            val request = Request.Builder()
                .url(imageUrl)
                .build()
            
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext ImportResult.Error("Failed to download: ${response.code}")
            }
            
            val inputStream = response.body?.byteStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            
            if (bitmap == null) {
                return@withContext ImportResult.Error("Invalid image format")
            }
            
            // Create import metadata
            val importData = ImportedJerseyData(
                originalUrl = imageUrl,
                bitmap = bitmap,
                suggestedNumber = suggestedNumber,
                teamName = teamName,
                sport = sport,
                downloadTimestamp = System.currentTimeMillis()
            )
            
            Log.d(TAG, "✅ Successfully imported jersey photo: ${bitmap.width}x${bitmap.height}")
            return@withContext ImportResult.Success(importData)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to import jersey photo: ${e.message}")
            return@withContext ImportResult.Error("Download failed: ${e.message}")
        }
    }
    
    /**
     * 📋 Batch import from URL list
     */
    suspend fun batchImport(
        urls: List<String>,
        onProgress: (Int, Int) -> Unit = { _, _ -> },
        onItemCompleted: (String, ImportResult) -> Unit = { _, _ -> }
    ): BatchImportResult = withContext(Dispatchers.IO) {
        
        val results = mutableListOf<Pair<String, ImportResult>>()
        var successCount = 0
        var errorCount = 0
        
        urls.forEachIndexed { index, url ->
            onProgress(index + 1, urls.size)
            
            val result = importJerseyPhoto(url)
            results.add(url to result)
            
            when (result) {
                is ImportResult.Success -> successCount++
                is ImportResult.Error -> errorCount++
            }
            
            onItemCompleted(url, result)
            
            // Small delay to avoid overwhelming servers
            delay(500)
        }
        
        BatchImportResult(
            totalUrls = urls.size,
            successCount = successCount,
            errorCount = errorCount,
            results = results
        )
    }
    
    /**
     * 🔍 Validate and save jersey sample
     */
    suspend fun validateAndSave(
        importedData: ImportedJerseyData,
        validatedNumber: Int,
        boundingBox: android.graphics.RectF,
        metadata: CaptureMetadata
    ): String? = withContext(Dispatchers.IO) {
        
        // Enhanced metadata with import info
        val enhancedMetadata = metadata.copy(
            captureMode = "imported",
            detectionSource = "manual_validation"
        )
        
        // Save to dataset
        datasetCollector.captureTrainingSample(
            image = importedData.bitmap,
            jerseyNumber = validatedNumber,
            boundingBox = boundingBox,
            metadata = enhancedMetadata
        )
    }
}

/**
 * 📊 Import Results
 */
sealed class ImportResult {
    data class Success(val data: ImportedJerseyData) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

data class ImportedJerseyData(
    val originalUrl: String,
    val bitmap: Bitmap,
    val suggestedNumber: Int? = null,
    val teamName: String? = null,
    val sport: String? = null,
    val downloadTimestamp: Long
)

data class BatchImportResult(
    val totalUrls: Int,
    val successCount: Int,
    val errorCount: Int,
    val results: List<Pair<String, ImportResult>>
)



/**
 * 📋 URL List Generators
 */
object JerseyUrlGenerator {
    
    /**
     * Generate search URLs for jersey numbers
     */
    fun generateGoogleImageUrls(sport: String, numbers: List<Int>): List<String> {
        return numbers.map { number ->
            "https://www.google.com/search?tbm=isch&q=$sport+jersey+number+$number"
        }
    }
    
    /**
     * Generate Pinterest search URLs
     */
    fun generatePinterestUrls(sport: String, numbers: List<Int>): List<String> {
        return numbers.map { number ->
            "https://www.pinterest.com/search/pins/?q=$sport+jersey+$number"
        }
    }
    
    /**
     * Common jersey number patterns for different sports
     */
    fun getCommonNumbers(sport: String): List<Int> {
        return when (sport.lowercase()) {
            "soccer", "football" -> (1..11).toList() + (7..50).toList() // Common soccer numbers
            "basketball" -> listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 15, 20, 21, 22, 23, 24, 30, 32, 33, 34, 35, 41, 42, 45, 50)
            "american_football" -> (1..99).toList()
            "hockey" -> (1..99).toList()
            else -> (1..50).toList()
        }
    }
}