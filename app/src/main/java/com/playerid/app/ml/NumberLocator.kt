package com.playerid.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * A specialized locator that uses a single-class object detection model to find the
 * bounding boxes of potential jersey numbers.
 * Optimized with GPU acceleration for high-speed tiling passes.
 */
class NumberLocator(context: Context, modelFileName: String = "model.tflite") {

    private var interpreter: Interpreter
    private var gpuDelegate: GpuDelegate? = null

    init {
        val options = Interpreter.Options()
        try {
            // Attempt to use GPU delegate for significantly faster tiled detection
            gpuDelegate = GpuDelegate()
            options.addDelegate(gpuDelegate)
            Log.i("NumberLocator", "TFLite GPU Delegate initialized successfully")
        } catch (e: Exception) {
            Log.w("NumberLocator", "GPU Delegate not supported, falling back to CPU (XNNPACK)", e)
            options.setUseXNNPACK(true)
            options.setNumThreads(4)
        }
        
        interpreter = Interpreter(loadModelFile(context, modelFileName), options)
        
        val inputTensor = interpreter.getInputTensor(0)
        Log.i("NumberLocator", "Model Input: ${inputTensor.dataType()}, Shape: ${inputTensor.shape().contentToString()}")
    }

    @Throws(IOException::class)
    private fun loadModelFile(context: Context, modelFileName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(modelFileName)
        return FileInputStream(assetFileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val declaredLength = assetFileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }
    }

    private data class DetectionResult(val box: RectF, val score: Float)

    private fun runInference(bitmap: Bitmap): List<DetectionResult> {
        val tensorImage = TensorImage(org.tensorflow.lite.DataType.UINT8)
        tensorImage.load(bitmap)

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(320, 320, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val processedImage = imageProcessor.process(tensorImage)
        val inputBuffer = processedImage.buffer
        inputBuffer.rewind()

        val outputBoxes = Array(1) { Array(40) { FloatArray(4) { 0f } } }
        val outputClasses = Array(1) { FloatArray(40) { 0f } }
        val outputScores = Array(1) { FloatArray(40) { 0f } }
        val numDetections = FloatArray(1) { 0f }

        val outputs = mapOf(
            0 to outputBoxes,
            1 to outputClasses,
            2 to outputScores,
            3 to numDetections
        )

        try {
            interpreter.runForMultipleInputsOutputs(arrayOf(inputBuffer), outputs)
        } catch (e: Exception) {
            Log.e("NumberLocator", "Inference failed!", e)
            return emptyList()
        }

        val detections = mutableListOf<DetectionResult>()
        val numDetectionsInt = numDetections[0].toInt().coerceAtMost(40)

        for (i in 0 until numDetectionsInt) {
            val score = outputScores[0][i]
            val box = outputBoxes[0][i]
            detections.add(DetectionResult(RectF(box[1], box[0], box[3], box[2]), score))
        }
        return detections
    }

    fun locate(bitmap: Bitmap, confidenceThreshold: Float = 0.5f): List<RectF> {
        val w = bitmap.width
        val h = bitmap.height

        val rawDetections = runInference(bitmap)
        val candidates = rawDetections.filter { it.score >= confidenceThreshold }

        val sortedCandidates = candidates.sortedByDescending { it.score }
        val suppressedResults = mutableListOf<DetectionResult>()

        for (detection in sortedCandidates) {
            var shouldAdd = true
            for (suppressed in suppressedResults) {
                if (calculateIoU(detection.box, suppressed.box) > 0.3f) {
                    shouldAdd = false
                    break
                }
            }
            if (shouldAdd) {
                suppressedResults.add(detection)
            }
        }

        return suppressedResults.map {
            RectF(it.box.left * w, it.box.top * h, it.box.right * w, it.box.bottom * h)
        }
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val xA = maxOf(box1.left, box2.left)
        val yA = maxOf(box1.top, box2.top)
        val xB = minOf(box1.right, box2.right)
        val yB = minOf(box1.bottom, box2.bottom)
        val interArea = maxOf(0f, xB - xA) * maxOf(0f, yB - yA)
        val box1Area = (box1.right - box1.left) * (box1.bottom - box1.top)
        val box2Area = (box2.right - box2.left) * (box2.bottom - box2.top)
        val unionArea = box1Area + box2Area - interArea
        return if (unionArea <= 0) 0f else interArea / unionArea
    }
    
    fun close() {
        interpreter.close()
        gpuDelegate?.close()
    }
}