package com.playerid.app

import android.net.Uri
import androidx.compose.ui.geometry.Offset

/**
 * Ground truth annotation for a single video test clip.
 * Contains expected jersey detections for validation.
 */
data class TestVideoAnnotation(
    val clipName: String,
    val clipUri: Uri,
    val teamName: String = "Unknown",
    val jerseyColor: String? = null,
    /** Expected jersey numbers present in this clip (e.g., setOf("10", "23", "7")) */
    val expectedJerseyNumbers: Set<String>,
    /** Optional: expected detections with positions for more precise validation */
    val expectedDetections: List<ExpectedDetection> = emptyList(),
    val description: String = ""
)

/**
 * Expected detection result at a specific location in a frame.
 */
data class ExpectedDetection(
    val jerseyNumber: String,
    val approximatePosition: Offset? = null,
    val frameTimeMs: Long? = null
)

/**
 * Result of running detection on a single test clip.
 */
data class AccuracyTestResult(
    val annotation: TestVideoAnnotation,
    val detectedNumbers: Set<String>,
    val detections: List<DetectionMatch> = emptyList(),
    val truePositives: Set<String>,  // Correctly detected
    val falsePositives: Set<String>, // Detected but not in expected
    val falseNegatives: Set<String>, // Expected but not detected
    val detectionMode: String = "UNKNOWN",
    val processingTimeMs: Long = 0,
    val errorMessage: String? = null
) {
    fun precision(): Float = if (truePositives.size + falsePositives.size == 0) {
        1f
    } else {
        truePositives.size.toFloat() / (truePositives.size + falsePositives.size)
    }

    fun recall(): Float = if (truePositives.size + falseNegatives.size == 0) {
        1f
    } else {
        truePositives.size.toFloat() / (truePositives.size + falseNegatives.size)
    }

    fun f1Score(): Float {
        val p = precision()
        val r = recall()
        return if (p + r == 0f) 0f else 2 * (p * r) / (p + r)
    }

    fun accuracy(): Float {
        val total = expectedNumbers.size
        return if (total == 0) 1f else truePositives.size.toFloat() / total
    }

    val expectedNumbers: Set<String>
        get() = annotation.expectedJerseyNumbers

    fun isFullSuccess(): Boolean = falsePositives.isEmpty() && falseNegatives.isEmpty()
}

/**
 * A matched detection with confidence score.
 */
data class DetectionMatch(
    val jerseyNumber: String,
    val confidence: Float,
    val position: Offset
)

/**
 * Summary statistics for a batch of test results.
 */
data class AccuracyTestSummary(
    val totalTests: Int,
    val successfulTests: Int,
    val failedTests: Int,
    val averagePrecision: Float,
    val averageRecall: Float,
    val averageF1: Float,
    val totalTruePositives: Int,
    val totalFalsePositives: Int,
    val totalFalseNegatives: Int,
    val results: List<AccuracyTestResult>
) {
    val successRate: Float
        get() = if (totalTests == 0) 0f else successfulTests.toFloat() / totalTests

    fun toDetailedReport(): String = buildString {
        appendLine("=" * 70)
        appendLine("JERSEY DETECTION ACCURACY TEST REPORT")
        appendLine("=" * 70)
        appendLine()
        appendLine("SUMMARY STATISTICS")
        appendLine("-".repeat(70))
        appendLine("Total Tests:           $totalTests")
        appendLine("Successful Tests:      $successfulTests (${(successRate * 100).toInt()}%)")
        appendLine("Failed Tests:          $failedTests")
        appendLine()
        appendLine("AGGREGATE METRICS")
        appendLine("-".repeat(70))
        appendLine("Average Precision:     ${"%.2f".format(averagePrecision)}")
        appendLine("Average Recall:        ${"%.2f".format(averageRecall)}")
        appendLine("Average F1 Score:      ${"%.2f".format(averageF1)}")
        appendLine()
        appendLine("CONFUSION MATRIX (Aggregate)")
        appendLine("-".repeat(70))
        appendLine("True Positives:        $totalTruePositives")
        appendLine("False Positives:       $totalFalsePositives")
        appendLine("False Negatives:       $totalFalseNegatives")
        appendLine()
        appendLine("DETAILED RESULTS")
        appendLine("=" * 70)
        
        results.forEach { result ->
            appendLine()
            appendLine("Clip: ${result.annotation.clipName}")
            appendLine("-".repeat(70))
            appendLine("Expected:     ${result.expectedNumbers.sorted().joinToString(", ")}")
            appendLine("Detected:     ${result.detectedNumbers.sorted().joinToString(", ")}")
            appendLine("Match:        ${if (result.isFullSuccess()) "✓ PASS" else "✗ FAIL"}")
            if (!result.isFullSuccess()) {
                if (result.truePositives.isNotEmpty()) {
                    appendLine("  ✓ TP: ${result.truePositives.sorted().joinToString(", ")}")
                }
                if (result.falsePositives.isNotEmpty()) {
                    appendLine("  ✗ FP: ${result.falsePositives.sorted().joinToString(", ")}")
                }
                if (result.falseNegatives.isNotEmpty()) {
                    appendLine("  ✗ FN: ${result.falseNegatives.sorted().joinToString(", ")}")
                }
            }
            appendLine("Precision:    ${"%.2f".format(result.precision())}")
            appendLine("Recall:       ${"%.2f".format(result.recall())}")
            appendLine("F1 Score:     ${"%.2f".format(result.f1Score())}")
            appendLine("Mode:         ${result.detectionMode}")
            appendLine("Time:         ${result.processingTimeMs}ms")
            if (result.errorMessage != null) {
                appendLine("ERROR:        ${result.errorMessage}")
            }
        }
        
        appendLine()
        appendLine("=" * 70)
    }
}

private operator fun String.times(count: Int): String = this.repeat(count)
