# Jersey Detection Accuracy Test Harness

## Overview

This test harness measures the accuracy of jersey number detection across real video clips. It helps you quantify detector performance and identify which jersey numbers are problematic before deciding whether to train a custom model.

## Quick Start

### 1. Add Test Video Clips

Place your test video files in `app/src/androidTest/assets/`:

```
app/
  src/
    androidTest/
      assets/
        test_clip_1.mp4
        test_clip_2.mp4
        sample_game.mp4
```

**Best practices:**
- Use 5-10 real game clips from your actual use case
- Include different scenarios:
  - **Easy**: Large, close-up jersey numbers
  - **Medium**: Typical game footage distance
  - **Hard**: Small, distant numbers from far away
  - **Edge**: Difficult lighting, angles, occlusions

### 2. Create Test Annotations

Edit `JerseyDetectionAccuracyTest.kt` and update `setupTestClips()`:

```kotlin
private fun setupTestClips() {
    testAnnotations.add(TestVideoAnnotation(
        clipName = "Game 1 - Close Up",
        clipUri = Uri.parse("file:///android_asset/test_clip_1.mp4"),
        teamName = "Home Team",
        jerseyColor = "#FF0000",  // Red team
        expectedJerseyNumbers = setOf("10", "23", "7", "19"),
        description = "Close-up footage of key players"
    ))
    
    testAnnotations.add(TestVideoAnnotation(
        clipName = "Game 1 - Distance",
        clipUri = Uri.parse("file:///android_asset/test_clip_2.mp4"),
        teamName = "Home Team",
        jerseyColor = "#FF0000",
        expectedJerseyNumbers = setOf("10", "23", "5"),
        description = "Medium distance footage"
    ))
}
```

### 3. Run the Tests

```bash
# Run all accuracy tests
./gradlew connectedAndroidTest --tests "com.playerid.app.JerseyDetectionAccuracyTest"

# Run specific test
./gradlew connectedAndroidTest --tests "com.playerid.app.JerseyDetectionAccuracyTest.testJerseyDetectionAccuracy"

# Run with logging
./gradlew connectedAndroidTest --tests "com.playerid.app.JerseyDetectionAccuracyTest" -i
```

### 4. Review Results

Test results are logged to Android Studio's Logcat. Look for messages tagged with `JerseyAccuracyTest`.

## Available Tests

### `testJerseyDetectionAccuracy()`
- **What:** Comprehensive accuracy test on all configured clips
- **Output:** Detailed report with precision, recall, F1 scores
- **Use:** Primary test to measure overall performance

### `testFastModePerformance()`
- **What:** Tests FAST detection mode (0.8s intervals, low latency)
- **Output:** Speed vs. accuracy tradeoff
- **Use:** Verify fast mode is acceptable for your use case

### `testAccurateModePerformance()`
- **What:** Tests ACCURATE mode (0.5s detection, full variants, high accuracy)
- **Output:** Maximum accuracy metrics
- **Use:** Establish an accuracy ceiling

### `testWithColorFiltering()`
- **What:** Tests detection WITH jersey color filtering enabled
- **Output:** Improvement from color filtering
- **Use:** Quantify color filter effectiveness

## Understanding the Results

### Metrics Explained

- **Precision**: Of the jerseys we detected, how many were correct?
  - Formula: TP / (TP + FP)
  - 1.0 = perfect (no false positives)
  - Low precision = too many wrong detections

- **Recall**: Of the jerseys in the video, how many did we find?
  - Formula: TP / (TP + FN)
  - 1.0 = perfect (no missed detections)
  - Low recall = missing valid jerseys

- **F1 Score**: Harmonic mean of precision and recall
  - Formula: 2 * (P * R) / (P + R)
  - Best single metric for overall accuracy
  - Range: 0.0 to 1.0 (higher is better)

### Confusion Matrix

- **TP (True Positives)**: Correctly detected jerseys ✓
- **FP (False Positives)**: Detected but not in video ✗
- **FN (False Negatives)**: In video but not detected ✗

### Example Output

```
======================================================================
JERSEY DETECTION ACCURACY TEST REPORT
======================================================================

SUMMARY STATISTICS
----------------------------------------------------------------------
Total Tests:           4
Successful Tests:      3 (75%)
Failed Tests:          1

AGGREGATE METRICS
----------------------------------------------------------------------
Average Precision:     0.88
Average Recall:        0.92
Average F1 Score:      0.90

DETAILED RESULTS
======================================================================

Clip: Game 1 - Close Up
----------------------------------------------------------------------
Expected:     [10, 23, 7, 19]
Detected:     [10, 23, 7, 19]
Match:        ✓ PASS
Precision:    1.00
Recall:       1.00
F1 Score:     1.00

Clip: Game 1 - Distance
----------------------------------------------------------------------
Expected:     [10, 23, 5]
Detected:     [10, 23]
Match:        ✗ FAIL
  ✓ TP: 10, 23
  ✗ FN: 5
Precision:    1.00
Recall:       0.67
F1 Score:     0.80
```

## Interpreting Your Results

### If Accuracy is 90%+
✓ ML Kit is performing well for your use case
- Keep current implementation
- Focus on other features
- Consider minimal tuning of confidence thresholds

### If Accuracy is 85-90%
✓ ML Kit is decent but has room for improvement
- Enable color filtering if not already doing so
- Increase ACCURATE mode scanning (slower but more accurate)
- Consider training a small custom model for problematic numbers

### If Accuracy is 75-85%
⚠️ Accuracy needs improvement for reliable use
- Run failure analysis to identify problem numbers
- Implement custom Jersey10Detector-style validation
- Consider training a TensorFlow Lite model
- Optimize preprocessing (image enhancement, upscaling)

### If Accuracy is <75%
✗ Current approach is insufficient
- Identify root causes (lighting, distance, occlusion, etc.)
- Collect more training data
- Train a custom YOLOv8 or TensorFlow model
- Consider combining ML Kit with pattern-based detection

## Improving Accuracy

### Without Model Training

1. **Better Preprocessing**
   - Enhance contrast for dark jerseys
   - Normalize exposure
   - Remove motion blur

2. **Smarter Filtering**
   - Use team-specific jersey color ranges
   - Enforce roster-based validation
   - Check OCR confidence scores

3. **Multi-Model Approach**
   - Combine ML Kit OCR with Jersey10Detector
   - Use voting to reduce false positives
   - Ensemble multiple detection strategies

### With Custom Model Training

1. **Collect Training Data**
   - Record 100+ clips of your target sport/team
   - Annotate jersey locations and numbers

2. **Train TensorFlow Model**
   - Use YOLOv8 for detection + OCR
   - Train on your specific jersey styles
   - Target 95%+ accuracy on your data

3. **Deploy to Device**
   - Convert to TFLite format
   - Integrate into VideoProcessingManager
   - Benchmark performance vs. ML Kit

## Exporting Results

### CSV Export

```kotlin
val csvReport = TestClipManager.exportResultsToCSV(results)
// csvReport contains a spreadsheet-friendly format
```

### Failure Analysis

```kotlin
val analysis = TestClipManager.analyzeFailures(results)
Log.i("Analysis", analysis)
```

## Tips for Accurate Annotation

When creating test cases, ensure you:

1. **Watch the entire clip** and list every jersey number visible
2. **Include all jersey orientations** (front, back, side angles)
3. **Note lighting conditions** (well-lit, backlit, shadowed)
4. **Consider distance variations** (close, medium, far)
5. **Test with and without color filtering** to understand its impact

## Troubleshooting

### Tests Won't Run
- Ensure `androidTest` folder structure is correct
- Check that you have a connected device or emulator
- Verify video files are in `app/src/androidTest/assets/`

### "No test clips configured" warning
- You haven't added any clips to `setupTestClips()`
- Place `.mp4` files in assets folder
- Update `setupTestClips()` with annotations

### Detections are blank
- Video URI might be incorrect
- Check file permissions
- Verify video codec is supported

### Processing takes forever
- Switch to FAST mode for quick feedback
- Use shorter video clips (5-10 seconds)
- Run on an actual device, not emulator

## Next Steps

After measuring accuracy:

1. **If accuracy is good**: Polish other features
2. **If accuracy needs work**: 
   - Run failure analysis to identify problem numbers
   - Decide: optimize current approach vs. train custom model
   - See [Custom Model Training Guide](../ml_training/README.md)

