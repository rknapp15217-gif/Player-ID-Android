package com.playerid.app.ar

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.playerid.app.data.TrackedPlayer
import com.playerid.app.ml.NumberLocator
import com.playerid.app.utils.PlayerTracker
import com.playerid.app.utils.YuvToRgbConverter
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await

@SuppressLint("UnsafeOptInUsageError")
class JerseyDetectionManager(
    // For saving OCR crops
    private var ocrCropSaveCount: Int = 0,
    private val ocrCropSaveLimit: Int = 10,
    private val context: Context,
    private val onPlayersTracked: (List<TrackedPlayer>) -> Unit,
    private val onDetectionProcessing: (() -> Unit)? = null
) : ImageAnalysis.Analyzer {

    private val numberLocator = NumberLocator(context)
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val yuvToRgbConverter = YuvToRgbConverter(context)
    private val playerTracker = PlayerTracker()
    private lateinit var bitmapBuffer: Bitmap
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var frameCounter = 0
    private var activeRosterNumbers: Set<String> = emptySet()
    
    // Track last detections to skip redundant OCR
    private val lastOcrResults = mutableMapOf<String, String>()

    companion object {
        private const val TAG = "JerseyDetectionManager"
    }

    fun setRosterFilter(numbers: Set<String>) {
        this.activeRosterNumbers = numbers
    }

    override fun analyze(imageProxy: ImageProxy) {
        try {
            Log.d(TAG, "Analyzing frame: ${imageProxy.width}x${imageProxy.height}, frameCounter=$frameCounter")
            frameCounter++
            // Run detection every 2nd frame for performance
            if (frameCounter % 2 != 0) {
                imageProxy.close()
                return
            }
            val runtime = Runtime.getRuntime()
            Log.d(TAG, "[MEMORY] Before detection: free=${runtime.freeMemory() / 1024}KB, total=${runtime.totalMemory() / 1024}KB")
            scope.launch {
                // Indicate detection is in progress (for UI)
                onDetectionProcessing?.invoke()
                try {
                    if (!::bitmapBuffer.isInitialized) {
                        bitmapBuffer = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
                    }
                    imageProxy.image?.let { yuvToRgbConverter.yuvToRgb(it, bitmapBuffer) }
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val rotatedBitmap = rotateBitmap(bitmapBuffer, rotationDegrees)
                    Log.d(TAG, "Rotated bitmap: ${rotatedBitmap.width}x${rotatedBitmap.height}")

                    // Always downscale to 320x320 for TFLite model
                    val detectionSize = 320
                    val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, detectionSize, detectionSize, true)
                    Log.d(TAG, "Scaled bitmap for detection: ${scaledBitmap.width}x${scaledBitmap.height}")

                    val frameW = scaledBitmap.width
                    val frameH = scaledBitmap.height

                    val rawLocations = mutableListOf<RectF>()

                    // Quadrant tiles for distance detection (run every detection frame)
                    Log.d(TAG, "Running detection on quadrant tiles and wide pass")
                    // Expanded tiling: 3x3 grid for full coverage, with overlap
                    val overlap = 0.15f
                    val step = (1f - overlap) / 2f
                    val tiles = mutableListOf<RectF>()
                    for (row in 0..2) {
                        for (col in 0..2) {
                            val left = (col * step).coerceAtMost(1f - step - overlap)
                            val top = (row * step).coerceAtMost(1f - step - overlap)
                            val right = (left + step + overlap).coerceAtMost(1f)
                            val bottom = (top + step + overlap).coerceAtMost(1f)
                            tiles.add(RectF(left, top, right, bottom))
                        }
                    }
                    Log.d(TAG, "Tiles count: ${tiles.size}")
                    tiles.forEach { tile ->
                        val tileBitmap = cropNormalizedBitmap(scaledBitmap, tile)
                        Log.d(TAG, "Tile crop for tile $tile: ${tileBitmap?.width}x${tileBitmap?.height}")
                        if (tileBitmap != null) {
                            try {
                                // Always resize tile to 320x320 for model
                                val tile320 = Bitmap.createScaledBitmap(tileBitmap, 320, 320, true)
                                Log.d(TAG, "Calling numberLocator.locate for tile $tile (resized to 320x320)")
                                numberLocator.locate(tile320, 0.15f).forEach { det ->
                                    rawLocations.add(RectF(
                                        tile.left + (det.left * tile.width()),
                                        tile.top + (det.top * tile.height()),
                                        tile.left + (det.right * tile.width()),
                                        tile.top + (det.bottom * tile.height())
                                    ))
                                }
                                tile320.recycle()
                            } catch (e: Exception) {
                                Log.e(TAG, "Detection failed for tile $tile", e)
                            }
                            tileBitmap.recycle()
                        }
                    }

                    try {
                        Log.d(TAG, "Calling numberLocator.locate for wide pass")
                        // Lowered confidence threshold to 0.15f for wide pass
                        rawLocations.addAll(numberLocator.locate(scaledBitmap, 0.15f))
                    } catch (e: Exception) {
                        Log.e(TAG, "Detection failed for wide pass", e)
                    }

                    // Lowered NMS threshold to 0.3f for more overlapping boxes
                    val combinedLocations = performNMS(rawLocations, 0.3f)
                    Log.d(TAG, "Detections after NMS: ${combinedLocations.size}")

                    // Limit to 3 most prominent detections for OCR
                    val detections = processLocationsSmart(combinedLocations.take(3), scaledBitmap)

                    // If any detection is found, reset frameCounter to allow immediate next detection
                    if (detections.isNotEmpty()) {
                        frameCounter = 0
                    }

                    val trackedPlayers = playerTracker.update(detections, frameW, frameH)
                    Log.d(TAG, "Tracked jersey numbers: ${trackedPlayers.map { it.jerseyNumber }}")

                    withContext(Dispatchers.Main) {
                        onPlayersTracked(trackedPlayers)
                    }

                    scaledBitmap.recycle()
                    rotatedBitmap.recycle()
                } catch (e: Exception) {
                    Log.e(TAG, "Analysis failed", e)
                } finally {
                    val runtime = Runtime.getRuntime()
                    Log.d(TAG, "[MEMORY] After detection: free=${runtime.freeMemory() / 1024}KB, total=${runtime.totalMemory() / 1024}KB")
                    imageProxy.close()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Global analyze() exception", e)
            imageProxy.close()
        }
    }

    private suspend fun processLocationsSmart(locations: List<RectF>, bitmap: Bitmap): List<Pair<RectF, String>> {
        if (locations.isEmpty()) return emptyList()

        val imgW = bitmap.width.toFloat()
        val imgH = bitmap.height.toFloat()

        val results = locations.map { location ->
            scope.async {
                // Log original box
                Log.d(TAG, "Original detection box: $location")

                // Normalize box to [0,1] using image size
                val normalizedBox = RectF(
                    location.left / imgW,
                    location.top / imgH,
                    location.right / imgW,
                    location.bottom / imgH
                )
                Log.d(TAG, "Normalized box: $normalizedBox (imgW=$imgW, imgH=$imgH)")

                // Clamp normalized box to [0,1]
                val clampedBox = RectF(
                    normalizedBox.left.coerceIn(0f, 1f),
                    normalizedBox.top.coerceIn(0f, 1f),
                    normalizedBox.right.coerceIn(0f, 1f),
                    normalizedBox.bottom.coerceIn(0f, 1f)
                )
                Log.d(TAG, "Clamped box: $clampedBox")

                val crop = cropNormalizedBitmap(bitmap, clampedBox)
                if (crop == null) {
                    Log.w(TAG, "Skipping OCR for box $clampedBox: invalid or too small crop")
                    return@async null
                }
                Log.d(TAG, "OCR crop for box $clampedBox: ${crop.width}x${crop.height}")
                try {
                    // ML Kit requires at least 32x32 for OCR
                    val minOcrSize = 32
                    val cropForOcr = if (crop.width < minOcrSize || crop.height < minOcrSize) {
                        Log.d(TAG, "Upscaling crop to ${minOcrSize}x${minOcrSize} for OCR")
                        Bitmap.createScaledBitmap(crop, maxOf(crop.width, minOcrSize), maxOf(crop.height, minOcrSize), true)
                    } else {
                        Bitmap.createScaledBitmap(crop, crop.width, crop.height, true)
                    }

                    // Save up to 10 crops per session for inspection
                    if (ocrCropSaveCount < ocrCropSaveLimit) {
                        try {
                            val dir = context.getExternalFilesDir("ocr_crops")
                            if (dir != null) {
                                if (!dir.exists()) dir.mkdirs()
                                val file = java.io.File(dir, "ocr_crop_${System.currentTimeMillis()}_${ocrCropSaveCount}.png")
                                val out = java.io.FileOutputStream(file)
                                cropForOcr.compress(Bitmap.CompressFormat.PNG, 100, out)
                                out.flush()
                                out.close()
                                ocrCropSaveCount++
                                Log.d(TAG, "Saved OCR crop to ${file.absolutePath}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to save OCR crop", e)
                        }
                    }

                    val image = InputImage.fromBitmap(cropForOcr, 0)
                    val result = textRecognizer.process(image).await()
                    val allRawLines = result.textBlocks.flatMap { it.lines }.map { it.text }
                    val allDigits = allRawLines.map { it.filter { c -> c.isDigit() } }
                    Log.d(TAG, "OCR debug for box $clampedBox: rawLines=$allRawLines, digits=$allDigits")
                    val number = allDigits
                        .filter { it.length in 1..3 }
                        .maxByOrNull { it.length }
                    Log.d(TAG, "OCR result for box $clampedBox: $allRawLines | Parsed number: $number")
                    cropForOcr.recycle()
                    if (number != null) {
                        if (activeRosterNumbers.isEmpty() || activeRosterNumbers.contains(number)) {
                            return@async Pair(clampedBox, number)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "OCR failed for box $clampedBox", e)
                }
                null
            }
        }.awaitAll().filterNotNull()

        if (results.isEmpty() && locations.isNotEmpty()) {
            Log.w(TAG, "All detection boxes produced invalid crops or OCR results. Raw detection boxes: $locations")
        }
        return results
    }

    private fun performNMS(boxes: List<RectF>, threshold: Float): List<RectF> {
        if (boxes.isEmpty()) return emptyList()
        val result = mutableListOf<RectF>()
        val remaining = boxes.toMutableList()
        while (remaining.isNotEmpty()) {
            val first = remaining.removeAt(0)
            result.add(first)
            remaining.removeAll { box -> calculateIoU(first, box) > threshold }
        }
        return result
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val xA = maxOf(box1.left, box2.left)
        val yA = maxOf(box1.top, box2.top)
        val xB = minOf(box1.right, box2.right)
        val yB = minOf(box1.bottom, box2.bottom)
        val interArea = maxOf(0f, xB - xA) * maxOf(0f, yB - yA)
        if (interArea <= 0) return 0f
        val unionArea = (box1.width() * box1.height()) + (box2.width() * box2.height()) - interArea
        return interArea / unionArea
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap
        val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun cropNormalizedBitmap(source: Bitmap, box: RectF): Bitmap? {
        // Calculate initial crop coordinates
        var left = (box.left * source.width).toInt().coerceIn(0, source.width - 1)
        var top = (box.top * source.height).toInt().coerceIn(0, source.height - 1)
        var right = (box.right * source.width).toInt().coerceIn(left + 1, source.width)
        var bottom = (box.bottom * source.height).toInt().coerceIn(top + 1, source.height)
        var width = right - left
        var height = bottom - top

        // Expand box if needed to ensure at least 32x32 crop, clamp to image bounds
        val minCrop = 32
        if (width < minCrop) {
            val expand = minCrop - width
            val expandLeft = expand / 2
            val expandRight = expand - expandLeft
            left = (left - expandLeft).coerceAtLeast(0)
            right = (right + expandRight).coerceAtMost(source.width)
            width = right - left
        }
        if (height < minCrop) {
            val expand = minCrop - height
            val expandTop = expand / 2
            val expandBottom = expand - expandTop
            top = (top - expandTop).coerceAtLeast(0)
            bottom = (bottom + expandBottom).coerceAtMost(source.height)
            height = bottom - top
        }
        // Final check: skip if still invalid
        if (width < minCrop || height < minCrop) return null
        if (left < 0 || top < 0 || right > source.width || bottom > source.height) return null
        return Bitmap.createBitmap(source, left, top, width, height)
    }
}