package com.playerid.app

import android.content.Context
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.playerid.app.data.Player
import com.playerid.app.video.VideoProcessingManager
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log
import kotlin.system.measureTimeMillis

/**
 * Instrumented accuracy test for jersey number detection.
 *
 * To use this test:
 * 1. Add video clips to app/src/androidTest/assets/ as raw video files
 * 2. Create test annotations in setupTestClips()
 * 3. Run: ./gradlew connectedAndroidTest --tests "com.playerid.app.JerseyDetectionAccuracyTest"
 *
 * Reports will be logged with detailed accuracy metrics.
 */
@RunWith(AndroidJUnit4::class)
class JerseyDetectionAccuracyTest {
    
    private lateinit var context: Context
    private lateinit var videoProcessingManager: VideoProcessingManager
    private val testAnnotations = mutableListOf<TestVideoAnnotation>()
    
    companion object {
        private const val TAG = "JerseyAccuracyTest"
    }
    
    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        videoProcessingManager = VideoProcessingManager(context)
        setupTestClips()
    }
    
    /**
     * Configure your test clips and their ground-truth annotations here.
     *
     * Example:
     * testAnnotations.add(TestVideoAnnotation(
     *     clipName = "Sample Game 1",
     *     clipUri = Uri.parse("file:///path/to/clip.mp4"),
     *     teamName = "Home Team",
     *     jerseyColor = "#FF0000",
     *     expectedJerseyNumbers = setOf("10", "23", "7", "19")
     * ))
     */
    private fun setupTestClips() {
        // Add your test clips here
        // Example:
        // testAnnotations.add(TestVideoAnnotation(
        //     clipName = "Test Clip 1",
        //     clipUri = getClipUri("test_clip_1.mp4"),
        //     teamName = "Test Team",
        //     jerseyColor = "#FF0000",
        //     expectedJerseyNumbers = setOf("10", "23", "7"),
        //     description = "Sample clip with jersey #10, #23, #7"
        // ))
        
        // Placeholder test data for demonstration
        val testClip = TestVideoAnnotation(
            clipName = "Demo Clip (add real clips here)",
            clipUri = Uri.parse("file:///dev/null"),  // Replace with actual clip
            teamName = "Demo Team",
            jerseyColor = null,
            expectedJerseyNumbers = setOf()
        )
        testAnnotations.add(testClip)
    }
    
    /**
     * Main accuracy test: runs detection on all clips and generates report.
     */
    @Test
    fun testJerseyDetectionAccuracy() {
        if (testAnnotations.isEmpty()) {
            Log.w(TAG, "No test clips configured. See setupTestClips() to add tests.")
            return
        }
        
        Log.i(TAG, "Starting accuracy test with ${testAnnotations.size} clips...")
        
        val results = mutableListOf<AccuracyTestResult>()
        
        testAnnotations.forEach { annotation ->
            Log.i(TAG, "Processing: ${annotation.clipName}")
            
            val result = runDetectionTest(annotation)
            results.add(result)
            
            Log.d(TAG, """
                Clip: ${annotation.clipName}
                Expected: ${result.expectedNumbers.sorted()}
                Detected: ${result.detectedNumbers.sorted()}
                Precision: ${"%.2f".format(result.precision())}
                Recall: ${"%.2f".format(result.recall())}
                F1: ${"%.2f".format(result.f1Score())}
                Time: ${result.processingTimeMs}ms
            """.trimIndent())
        }
        
        val summary = generateSummary(results)
        val report = summary.toDetailedReport()
        
        Log.i(TAG, report)
        
        // Print report to logcat for easy viewing
        report.split("\n").forEach { line ->
            Log.i(TAG, line)
        }
    }
    
    /**
     * Fast scan test: measures speed and accuracy of FAST mode.
     */
    @Test
    fun testFastModePerformance() {
        if (testAnnotations.isEmpty()) {
            Log.w(TAG, "No test clips configured.")
            return
        }
        
        Log.i(TAG, "Testing FAST detection mode...")
        
        testAnnotations.firstOrNull()?.let { annotation ->
            val result = runDetectionOnClip(
                annotation = annotation,
                mode = VideoProcessingManager.DetectionMode.FAST
            )
            
            Log.i(TAG, """
                FAST Mode Performance:
                Processing Time: ${result.processingTimeMs}ms
                Detections: ${result.detectedNumbers.size}
                Precision: ${"%.2f".format(result.precision())}
                Recall: ${"%.2f".format(result.recall())}
            """.trimIndent())
        }
    }
    
    /**
     * Accurate scan test: measures accuracy vs. latency tradeoff of ACCURATE mode.
     */
    @Test
    fun testAccurateModePerformance() {
        if (testAnnotations.isEmpty()) {
            Log.w(TAG, "No test clips configured.")
            return
        }
        
        Log.i(TAG, "Testing ACCURATE detection mode...")
        
        testAnnotations.firstOrNull()?.let { annotation ->
            val result = runDetectionOnClip(
                annotation = annotation,
                mode = VideoProcessingManager.DetectionMode.ACCURATE
            )
            
            Log.i(TAG, """
                ACCURATE Mode Performance:
                Processing Time: ${result.processingTimeMs}ms
                Detections: ${result.detectedNumbers.size}
                Precision: ${"%.2f".format(result.precision())}
                Recall: ${"%.2f".format(result.recall())}
            """.trimIndent())
        }
    }
    
    /**
     * Test with color filtering enabled.
     */
    @Test
    fun testWithColorFiltering() {
        if (testAnnotations.isEmpty()) {
            Log.w(TAG, "No test clips configured.")
            return
        }
        
        Log.i(TAG, "Testing detection WITH color filtering...")
        
        testAnnotations.filter { it.jerseyColor != null }.forEach { annotation ->
            val result = runDetectionOnClip(
                annotation = annotation,
                mode = VideoProcessingManager.DetectionMode.ACCURATE,
                useColorFilter = true
            )
            
            Log.i(TAG, """
                ${annotation.clipName} (with color filter ${annotation.jerseyColor}):
                Expected: ${result.expectedNumbers.sorted()}
                Detected: ${result.detectedNumbers.sorted()}
                F1: ${"%.2f".format(result.f1Score())}
            """.trimIndent())
        }
    }
    
    /**
     * Runs detection on a single test clip.
     */
    private fun runDetectionTest(annotation: TestVideoAnnotation): AccuracyTestResult {
        return runDetectionOnClip(
            annotation = annotation,
            mode = VideoProcessingManager.DetectionMode.ACCURATE,
            useColorFilter = annotation.jerseyColor != null
        )
    }
    
    /**
     * Executes detection with given parameters.
     */
    private fun runDetectionOnClip(
        annotation: TestVideoAnnotation,
        mode: VideoProcessingManager.DetectionMode,
        useColorFilter: Boolean = false
    ): AccuracyTestResult {
        return try {
            val startTime = System.currentTimeMillis()
            var detectedNumbers = emptySet<String>()
            var detections = emptyList<DetectionMatch>()
            var processingTimeMs = 0L
            
            val timeMs = measureTimeMillis {
                runBlocking {
                    try {
                        val bubbles = videoProcessingManager.autoDetectPlayersInVideo(
                            videoUri = annotation.clipUri,
                            roster = emptyList(),  // No roster constraints for this test
                            mode = mode,
                            jerseyColorHex = if (useColorFilter) annotation.jerseyColor else null,
                            onProgress = { progress ->
                                Log.d(TAG, "Progress: ${(progress * 100).toInt()}%")
                            }
                        )
                        
                        detectedNumbers = bubbles.map { it.jerseyNumber }.toSet()
                        detections = bubbles.map { bubble ->
                            DetectionMatch(
                                jerseyNumber = bubble.jerseyNumber,
                                confidence = 1f,  // ML Kit doesn't expose per-bubble confidence
                                position = bubble.position
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Detection failed for ${annotation.clipName}", e)
                        throw e
                    }
                }
            }
            processingTimeMs = timeMs
            
            val truePositives = annotation.expectedJerseyNumbers.intersect(detectedNumbers)
            val falsePositives = detectedNumbers - annotation.expectedJerseyNumbers
            val falseNegatives = annotation.expectedJerseyNumbers - detectedNumbers
            
            AccuracyTestResult(
                annotation = annotation,
                detectedNumbers = detectedNumbers,
                detections = detections,
                truePositives = truePositives,
                falsePositives = falsePositives,
                falseNegatives = falseNegatives,
                detectionMode = mode.name,
                processingTimeMs = processingTimeMs
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error testing ${annotation.clipName}", e)
            AccuracyTestResult(
                annotation = annotation,
                detectedNumbers = emptySet(),
                truePositives = emptySet(),
                falsePositives = emptySet(),
                falseNegatives = annotation.expectedJerseyNumbers,
                detectionMode = mode.name,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Generates summary statistics from test results.
     */
    private fun generateSummary(results: List<AccuracyTestResult>): AccuracyTestSummary {
        val successfulTests = results.count { it.isFullSuccess() }
        val failedTests = results.size - successfulTests
        
        val avgPrecision = results.map { it.precision() }.average().toFloat()
        val avgRecall = results.map { it.recall() }.average().toFloat()
        val avgF1 = results.map { it.f1Score() }.average().toFloat()
        
        val totalTP = results.sumOf { it.truePositives.size }
        val totalFP = results.sumOf { it.falsePositives.size }
        val totalFN = results.sumOf { it.falseNegatives.size }
        
        return AccuracyTestSummary(
            totalTests = results.size,
            successfulTests = successfulTests,
            failedTests = failedTests,
            averagePrecision = avgPrecision,
            averageRecall = avgRecall,
            averageF1 = avgF1,
            totalTruePositives = totalTP,
            totalFalsePositives = totalFP,
            totalFalseNegatives = totalFN,
            results = results
        )
    }
    
    /**
     * Helper to get clip URI from assets or file system.
     * Override this to point to your test video files.
     */
    private fun getClipUri(filename: String): Uri {
        // Example: copy from assets to temp file
        // For now, return placeholder
        return Uri.parse("file:///data/local/tmp/$filename")
    }
}
