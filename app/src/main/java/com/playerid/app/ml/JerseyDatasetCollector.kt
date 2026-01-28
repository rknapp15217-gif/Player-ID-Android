package com.playerid.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 📸 Jersey Number Dataset Collector
 * 
 * Collects and annotates jersey number images for training custom ML models.
 * Integrates with the camera system to gather real-world training data.
 */
class JerseyDatasetCollector(private val context: Context) {
    
    private val datasetDir = File(context.getExternalFilesDir(null), "jersey_dataset")
    private val imagesDir = File(datasetDir, "images")
    private val annotationsDir = File(datasetDir, "annotations")
    private val gson = Gson()
    
    companion object {
        private const val TAG = "DatasetCollector"
    }
    
    init {
        setupDirectories()
    }
    
    private fun setupDirectories() {
        datasetDir.mkdirs()
        imagesDir.mkdirs()
        annotationsDir.mkdirs()
        Log.d(TAG, "📁 Dataset directories created at: ${datasetDir.absolutePath}")
    }
    
    /**
     * 📸 Capture training sample with manual annotation
     */
    fun captureTrainingSample(
        image: Bitmap,
        jerseyNumber: Int,
        boundingBox: RectF,
        metadata: CaptureMetadata = CaptureMetadata()
    ): String? {
        
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val imageFileName = "jersey_${jerseyNumber}_${timestamp}.jpg"
            val annotationFileName = "jersey_${jerseyNumber}_${timestamp}.json"
            
            // Save image
            val imageFile = File(imagesDir, imageFileName)
            val outputStream = FileOutputStream(imageFile)
            image.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.close()
            
            // Create annotation
            val annotation = JerseyAnnotation(
                imagePath = imageFileName,
                imageWidth = image.width,
                imageHeight = image.height,
                jerseyNumber = jerseyNumber,
                boundingBox = BoundingBox(
                    x = boundingBox.left,
                    y = boundingBox.top, 
                    width = boundingBox.width(),
                    height = boundingBox.height()
                ),
                metadata = metadata,
                timestamp = timestamp
            )
            
            // Save annotation
            val annotationFile = File(annotationsDir, annotationFileName)
            annotationFile.writeText(gson.toJson(annotation))
            
            Log.d(TAG, "✅ Captured training sample: $imageFileName")
            return imageFileName
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to capture training sample: ${e.message}")
            return null
        }
    }
    
    /**
     * 🎯 Auto-capture from ML Kit detections for dataset augmentation
     */
    fun autoCapture(
        image: Bitmap,
        detectedPlayers: List<com.playerid.app.data.DetectedPlayer>
    ) {
        for (detectedPlayer in detectedPlayers) {
            // Only capture high-confidence detections
            if (detectedPlayer.confidence > 0.8f) {
                val metadata = CaptureMetadata(
                    captureMode = "auto",
                    confidence = detectedPlayer.confidence,
                    detectionSource = "ml_kit"
                )
                
                captureTrainingSample(
                    image = image,
                    jerseyNumber = detectedPlayer.number.toIntOrNull() ?: 0,
                    boundingBox = detectedPlayer.boundingBox,
                    metadata = metadata
                )
            }
        }
    }
    
    /**
     * 🏷️ Create annotation overlay for manual labeling
     */
    fun createAnnotationOverlay(
        originalImage: Bitmap,
        annotations: List<JerseyAnnotation>
    ): Bitmap {
        
        val overlayBitmap = originalImage.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(overlayBitmap)
        
        val boundingBoxPaint = Paint().apply {
            color = android.graphics.Color.RED
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        
        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 32f
            isAntiAlias = true
        }
        
        for (annotation in annotations) {
            val bbox = annotation.boundingBox
            val rect = RectF(bbox.x, bbox.y, bbox.x + bbox.width, bbox.y + bbox.height)
            
            // Draw bounding box
            canvas.drawRect(rect, boundingBoxPaint)
            
            // Draw jersey number label
            canvas.drawText(
                "#${annotation.jerseyNumber}",
                rect.left,
                rect.top - 8f,
                textPaint
            )
        }
        
        return overlayBitmap
    }
    
    /**
     * 📊 Generate dataset statistics
     */
    fun getDatasetStats(): DatasetStats {
        val annotationFiles = annotationsDir.listFiles { file -> file.extension == "json" }
        val stats = DatasetStats()
        
        annotationFiles?.forEach { file ->
            try {
                val annotation = gson.fromJson(file.readText(), JerseyAnnotation::class.java)
                stats.addSample(annotation)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse annotation: ${file.name}")
            }
        }
        
        return stats
    }
    
    /**
     * 📤 Export dataset in YOLO format
     */
    fun exportToYOLOFormat(): File? {
        try {
            val exportDir = File(datasetDir, "yolo_export")
            exportDir.mkdirs()
            
            val trainDir = File(exportDir, "train")
            val validDir = File(exportDir, "valid") 
            trainDir.mkdirs()
            validDir.mkdirs()
            
            val annotationFiles = annotationsDir.listFiles { file -> file.extension == "json" }
            val shuffledFiles = annotationFiles?.toList()?.shuffled() ?: emptyList()
            
            // 80/20 train/validation split
            val splitIndex = (shuffledFiles.size * 0.8).toInt()
            val trainFiles = shuffledFiles.take(splitIndex)
            val validFiles = shuffledFiles.drop(splitIndex)
            
            exportFiles(trainFiles, trainDir)
            exportFiles(validFiles, validDir)
            
            // Create dataset.yaml for YOLO training
            createYOLOConfig(exportDir)
            
            Log.d(TAG, "📤 Dataset exported to YOLO format: ${exportDir.absolutePath}")
            return exportDir
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to export dataset: ${e.message}")
            return null
        }
    }
    
    private fun exportFiles(files: List<File>, outputDir: File) {
        files.forEach { annotationFile ->
            try {
                val annotation = gson.fromJson(annotationFile.readText(), JerseyAnnotation::class.java)
                
                // Copy image
                val sourceImage = File(imagesDir, annotation.imagePath)
                val destImage = File(outputDir, annotation.imagePath)
                sourceImage.copyTo(destImage, overwrite = true)
                
                // Create YOLO annotation
                val yoloAnnotation = createYOLOAnnotation(annotation)
                val yoloFile = File(outputDir, annotation.imagePath.replace(".jpg", ".txt"))
                yoloFile.writeText(yoloAnnotation)
                
            } catch (e: Exception) {
                Log.w(TAG, "Failed to export file: ${annotationFile.name}")
            }
        }
    }
    
    private fun createYOLOAnnotation(annotation: JerseyAnnotation): String {
        val bbox = annotation.boundingBox
        
        // Convert to YOLO format (normalized coordinates)
        val centerX = (bbox.x + bbox.width / 2) / annotation.imageWidth
        val centerY = (bbox.y + bbox.height / 2) / annotation.imageHeight
        val width = bbox.width / annotation.imageWidth
        val height = bbox.height / annotation.imageHeight
        
        // Format: class_id center_x center_y width height
        return "${annotation.jerseyNumber} $centerX $centerY $width $height"
    }
    
    private fun createYOLOConfig(exportDir: File) {
        val config = """
            # Jersey Number Detection Dataset
            path: ${exportDir.absolutePath}
            train: train
            val: valid
            
            # Classes (jersey numbers 0-99)
            nc: 100
            names: ${(0..99).toList()}
        """.trimIndent()
        
        File(exportDir, "dataset.yaml").writeText(config)
    }
}

/**
 * 🏷️ Jersey annotation data structure
 */
data class JerseyAnnotation(
    @SerializedName("image_path") val imagePath: String,
    @SerializedName("image_width") val imageWidth: Int,
    @SerializedName("image_height") val imageHeight: Int,
    @SerializedName("jersey_number") val jerseyNumber: Int,
    @SerializedName("bounding_box") val boundingBox: BoundingBox,
    @SerializedName("metadata") val metadata: CaptureMetadata,
    @SerializedName("timestamp") val timestamp: String
)

data class BoundingBox(
    val x: Float,
    val y: Float, 
    val width: Float,
    val height: Float
)

data class CaptureMetadata(
    @SerializedName("capture_mode") val captureMode: String = "manual", // "manual" or "auto"
    @SerializedName("confidence") val confidence: Float = 1.0f,
    @SerializedName("detection_source") val detectionSource: String = "manual", // "manual", "ml_kit", "custom"
    @SerializedName("lighting_condition") val lightingCondition: String = "unknown", // "bright", "normal", "dark"
    @SerializedName("distance") val distance: String = "medium", // "close", "medium", "far"
    @SerializedName("angle") val angle: String = "front" // "front", "side", "angled"
)

/**
 * 📊 Dataset statistics
 */
class DatasetStats {
    private val numberCounts = mutableMapOf<Int, Int>()
    private val captureModeCounts = mutableMapOf<String, Int>()
    private var totalSamples = 0
    
    fun addSample(annotation: JerseyAnnotation) {
        totalSamples++
        numberCounts[annotation.jerseyNumber] = numberCounts.getOrDefault(annotation.jerseyNumber, 0) + 1
        captureModeCounts[annotation.metadata.captureMode] = captureModeCounts.getOrDefault(annotation.metadata.captureMode, 0) + 1
    }
    
    fun getTotalSamples(): Int = totalSamples
    
    fun getNumberCounts(): Map<Int, Int> = numberCounts
    
    fun getCaptureModeBreakdown(): Map<String, Int> = captureModeCounts
    
    fun getDataBalance(): Float {
        if (numberCounts.isEmpty()) return 0f
        
        val avgSamplesPerNumber = totalSamples / 100f // Assuming 0-99 numbers
        val variance = numberCounts.values.map { count ->
            (count - avgSamplesPerNumber).let { it * it }
        }.average()
        
        return 1f / (1f + variance.toFloat()) // Higher value = better balance
    }
}