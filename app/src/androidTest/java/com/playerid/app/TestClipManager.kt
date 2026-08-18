package com.playerid.app

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Utility for managing test video clips and annotations.
 *
 * Usage:
 * 1. Place video files in app/src/androidTest/assets/
 * 2. Use TestClipManager to load and annotate them
 * 3. Pass results to accuracy test
 */
object TestClipManager {
    
    /**
     * Loads all test video files from the androidTest/assets/ directory.
     *
     * Video files should be named: test_clip_*.mp4 or sample_*.mp4
     */
    fun discoverTestClips(context: Context): List<Uri> {
        val testClips = mutableListOf<Uri>()
        
        try {
            // Load from assets
            val assetManager = context.assets
            val files = assetManager.list("")
            files?.forEach { file ->
                if (file.endsWith(".mp4") || file.endsWith(".mov") || file.endsWith(".webm")) {
                    testClips.add(Uri.parse("file:///android_asset/$file"))
                }
            }
        } catch (e: Exception) {
            // Silently fail if assets directory doesn't exist
        }
        
        return testClips
    }
    
    /**
     * Creates a test annotation from a clip with auto-detected jersey numbers.
     * 
     * You must manually set the expectedJerseyNumbers based on what's actually in the clip.
     *
     * Example:
     * val testCase = TestClipManager.createTestAnnotation(
     *     context = context,
     *     clipUri = Uri.parse("file:///android_asset/game_footage.mp4"),
     *     clipName = "Game 1 - First Half",
     *     teamName = "Home Team",
     *     jerseyColor = "#FF0000",
     *     expectedNumbers = setOf("10", "23", "7", "19")
     * )
     */
    fun createTestAnnotation(
        context: Context,
        clipUri: Uri,
        clipName: String,
        teamName: String,
        jerseyColor: String? = null,
        expectedNumbers: Set<String>
    ): TestVideoAnnotation {
        return TestVideoAnnotation(
            clipName = clipName,
            clipUri = clipUri,
            teamName = teamName,
            jerseyColor = jerseyColor,
            expectedJerseyNumbers = expectedNumbers,
            description = "Test clip for jersey detection accuracy"
        )
    }
    
    /**
     * Builds a standard test suite with multiple difficulty levels.
     *
     * This is a template you can expand based on your own test videos.
     */
    fun buildStandardTestSuite(context: Context): List<TestVideoAnnotation> {
        return listOf(
            // EASY: Large, close-up jersey numbers
            TestVideoAnnotation(
                clipName = "Easy - Close Up",
                clipUri = Uri.parse("file:///android_asset/test_close_up.mp4"),
                teamName = "Test Team",
                jerseyColor = "#FF0000",
                expectedJerseyNumbers = setOf(),  // Update with actual jersey numbers
                description = "Large, clear jersey numbers at close range"
            ),
            
            // MEDIUM: Medium distance, typical game footage
            TestVideoAnnotation(
                clipName = "Medium - Game Footage",
                clipUri = Uri.parse("file:///android_asset/test_medium.mp4"),
                teamName = "Test Team",
                jerseyColor = "#FF0000",
                expectedJerseyNumbers = setOf(),  // Update with actual jersey numbers
                description = "Typical game footage distance"
            ),
            
            // HARD: Far away, small numbers, various angles
            TestVideoAnnotation(
                clipName = "Hard - Distance",
                clipUri = Uri.parse("file:///android_asset/test_hard.mp4"),
                teamName = "Test Team",
                jerseyColor = "#FF0000",
                expectedJerseyNumbers = setOf(),  // Update with actual jersey numbers
                description = "Small, distant jersey numbers from far away"
            ),
            
            // EDGE: Difficult lighting, angles, occlusion
            TestVideoAnnotation(
                clipName = "Edge - Challenging Conditions",
                clipUri = Uri.parse("file:///android_asset/test_edge.mp4"),
                teamName = "Test Team",
                jerseyColor = "#FF0000",
                expectedJerseyNumbers = setOf(),  // Update with actual jersey numbers
                description = "Back-lit, at angles, partially occluded"
            )
        )
    }
    
    /**
     * Generates a CSV file with test results for analysis.
     */
    fun exportResultsToCSV(results: List<AccuracyTestResult>): String {
        val header = listOf(
            "Clip Name",
            "Expected Numbers",
            "Detected Numbers",
            "True Positives",
            "False Positives",
            "False Negatives",
            "Precision",
            "Recall",
            "F1 Score",
            "Processing Time (ms)",
            "Status"
        ).joinToString(",")
        
        val rows = results.map { result ->
            listOf(
                "\"${result.annotation.clipName}\"",
                "\"${result.expectedNumbers.sorted().joinToString(";")}\"",
                "\"${result.detectedNumbers.sorted().joinToString(";")}\"",
                "\"${result.truePositives.sorted().joinToString(";")}\"",
                "\"${result.falsePositives.sorted().joinToString(";")}\"",
                "\"${result.falseNegatives.sorted().joinToString(";")}\"",
                "%.3f".format(result.precision()),
                "%.3f".format(result.recall()),
                "%.3f".format(result.f1Score()),
                result.processingTimeMs,
                if (result.isFullSuccess()) "PASS" else "FAIL"
            ).joinToString(",")
        }
        
        return (listOf(header) + rows).joinToString("\n")
    }
    
    /**
     * Prints a quick summary to help identify which numbers are problematic.
     */
    fun analyzeFailures(results: List<AccuracyTestResult>): String {
        val allFalsePositives = mutableMapOf<String, Int>()
        val allFalseNegatives = mutableMapOf<String, Int>()
        
        results.forEach { result ->
            result.falsePositives.forEach { num ->
                allFalsePositives[num] = (allFalsePositives[num] ?: 0) + 1
            }
            result.falseNegatives.forEach { num ->
                allFalseNegatives[num] = (allFalseNegatives[num] ?: 0) + 1
            }
        }
        
        return buildString {
            appendLine("FAILURE ANALYSIS")
            appendLine("=".repeat(60))
            
            if (allFalsePositives.isNotEmpty()) {
                appendLine("\nMost Common FALSE POSITIVES (wrongly detected):")
                allFalsePositives.entries
                    .sortedByDescending { it.value }
                    .take(10)
                    .forEach { (num, count) ->
                        appendLine("  #$num: $count times")
                    }
            }
            
            if (allFalseNegatives.isNotEmpty()) {
                appendLine("\nMost Common FALSE NEGATIVES (missed):")
                allFalseNegatives.entries
                    .sortedByDescending { it.value }
                    .take(10)
                    .forEach { (num, count) ->
                        appendLine("  #$num: $count times")
                    }
            }
            
            if (allFalsePositives.isEmpty() && allFalseNegatives.isEmpty()) {
                appendLine("\n✓ No failures detected!")
            }
        }
    }
}
