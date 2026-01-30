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
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.tasks.await

@SuppressLint("UnsafeOptInUsageError")
class JerseyDetectionManager(
    private val context: Context,
    private val onPlayersTracked: (List<TrackedPlayer>) -> Unit,
    private val onDetectionProcessing: (() -> Unit)? = null
) : ImageAnalysis.Analyzer {

    private val numberLocator = NumberLocator(context)
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val yuvToRgbConverter = YuvToRgbConverter(context)
    private val playerTracker = PlayerTracker()
    private var bitmapBuffer: Bitmap? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // FIX: Must use SINGLE thread for TFLite as the interpreter is not thread-safe.
    private val tfliteDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val isProcessing = AtomicBoolean(false)
    
    private var frameCounter = 0
    private var activeRosterNumbers: Set<String> = emptySet()

    companion object {
        private const val TAG = "JerseyDetectionManager"
    }

    fun setRosterFilter(numbers: Set<String>) {
        this.activeRosterNumbers = numbers
    }

    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing.get()) {
            imageProxy.close()
            return
        }

        isProcessing.set(true)
        onDetectionProcessing?.invoke()

        scope.launch {
            try {
                val currentBuffer = bitmapBuffer
                if (currentBuffer == null || currentBuffer.width != imageProxy.width || currentBuffer.height != imageProxy.height) {
                    currentBuffer?.recycle()
                    bitmapBuffer = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
                }
                
                val buffer = bitmapBuffer!!
                imageProxy.image?.let { yuvToRgbConverter.yuvToRgb(it, buffer) }
                val rotatedBitmap = rotateBitmap(buffer, imageProxy.imageInfo.rotationDegrees)

                val detectionSize = 320
                val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, detectionSize, detectionSize, true)
                val rawLocations = mutableListOf<RectF>()

                // Process all detection passes sequentially on the background thread to avoid crashes
                withContext(tfliteDispatcher) {
                    // 1. Wide Pass
                    numberLocator.locate(scaledBitmap, 0.15f).forEach { det ->
                        rawLocations.add(RectF(
                            det.left / detectionSize,
                            det.top / detectionSize,
                            det.right / detectionSize,
                            det.bottom / detectionSize
                        ))
                    }

                    // 2. High-Priority Center Tiles (Optimized for speed)
                    val centerTiles = listOf(
                        RectF(0.25f, 0.25f, 0.75f, 0.75f),
                        RectF(0.1f, 0.1f, 0.6f, 0.6f),
                        RectF(0.4f, 0.4f, 0.9f, 0.9f)
                    )
                    
                    centerTiles.forEach { tile ->
                        processTile(rotatedBitmap, tile, rawLocations)
                    }
                }

                val combinedLocations = performNMS(rawLocations, 0.3f)
                val detections = processLocationsParallelOCR(combinedLocations.take(5), rotatedBitmap)

                val trackedPlayers = playerTracker.update(detections, rotatedBitmap.width, rotatedBitmap.height)
                
                withContext(Dispatchers.Main) {
                    onPlayersTracked(trackedPlayers)
                }

                scaledBitmap.recycle()
                rotatedBitmap.recycle()
            } catch (e: Exception) {
                Log.e(TAG, "Analysis failed", e)
            } finally {
                imageProxy.close()
                isProcessing.set(false)
            }
        }
    }

    private fun processTile(fullBitmap: Bitmap, tile: RectF, outLocations: MutableList<RectF>) {
        val tileBitmap = cropNormalizedBitmap(fullBitmap, tile) ?: return
        try {
            val tile320 = Bitmap.createScaledBitmap(tileBitmap, 320, 320, true)
            numberLocator.locate(tile320, 0.15f).forEach { det ->
                outLocations.add(RectF(
                    tile.left + (det.left / 320f * tile.width()),
                    tile.top + (det.top / 320f * tile.height()),
                    tile.left + (det.right / 320f * tile.width()),
                    tile.top + (det.bottom / 320f * tile.height())
                ))
            }
            tile320.recycle()
        } finally {
            tileBitmap.recycle()
        }
    }

    private suspend fun processLocationsParallelOCR(locations: List<RectF>, bitmap: Bitmap): List<Pair<RectF, String>> {
        if (locations.isEmpty()) return emptyList()

        return locations.map { normalizedBox ->
            scope.async {
                val crop = cropNormalizedBitmap(bitmap, normalizedBox) ?: return@async null
                try {
                    val minDim = 32
                    val ocrBitmap = if (crop.width < minDim || crop.height < minDim) {
                        val scale = maxOf(minDim.toFloat() / crop.width, minDim.toFloat() / crop.height)
                        Bitmap.createScaledBitmap(crop, (crop.width * scale).toInt(), (crop.height * scale).toInt(), true)
                    } else {
                        crop
                    }

                    val result = textRecognizer.process(InputImage.fromBitmap(ocrBitmap, 0)).await()
                    if (ocrBitmap != crop) ocrBitmap.recycle()

                    val digits = result.textBlocks.flatMap { it.lines }.map { it.text.filter { c -> c.isDigit() } }
                    val rawNumber = digits.filter { it.length in 1..3 }.maxByOrNull { it.length }
                    
                    if (rawNumber != null) {
                        val matchedNumber = when {
                            activeRosterNumbers.isEmpty() -> rawNumber
                            activeRosterNumbers.contains(rawNumber) -> rawNumber
                            rawNumber == "0" && activeRosterNumbers.contains("00") -> "00"
                            rawNumber == "00" && activeRosterNumbers.contains("0") -> "0"
                            else -> null
                        }
                        if (matchedNumber != null) Pair(normalizedBox, matchedNumber) else null
                    } else null
                } catch (e: Exception) {
                    null
                } finally {
                    crop.recycle()
                }
            }
        }.awaitAll().filterNotNull()
    }

    private fun performNMS(boxes: List<RectF>, threshold: Float): List<RectF> {
        val result = mutableListOf<RectF>()
        val remaining = boxes.sortedByDescending { it.width() * it.height() }.toMutableList()
        while (remaining.isNotEmpty()) {
            val first = remaining.removeAt(0)
            result.add(first)
            remaining.removeAll { box -> calculateIoU(first, box) > threshold }
        }
        return result
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val inter = RectF(maxOf(box1.left, box2.left), maxOf(box1.top, box2.top), minOf(box1.right, box2.right), minOf(box1.bottom, box2.bottom))
        val interArea = maxOf(0f, inter.width()) * maxOf(0f, inter.height())
        val unionArea = box1.width() * box1.height() + box2.width() * box2.height() - interArea
        return if (unionArea <= 0) 0f else interArea / unionArea
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun cropNormalizedBitmap(source: Bitmap, box: RectF): Bitmap? {
        val left = (box.left * source.width).toInt().coerceIn(0, source.width - 1)
        val top = (box.top * source.height).toInt().coerceIn(0, source.height - 1)
        val right = (box.right * source.width).toInt().coerceIn(left + 1, source.width)
        val bottom = (box.bottom * source.height).toInt().coerceIn(top + 1, source.height)
        if (right - left <= 0 || bottom - top <= 0) return null
        return Bitmap.createBitmap(source, left, top, right - left, bottom - top)
    }
}