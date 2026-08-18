# Jersey Detection Accuracy Test Harness

## What You Now Have

I've built a **complete instrumented test harness** that measures how accurately your app detects jersey numbers from real video. This helps you understand if ML Kit is "good enough" or if you should invest in training a custom model.

## Files Created

```
app/src/androidTest/java/com/playerid/app/
├── AccuracyTestData.kt           # Data classes for test results
├── JerseyDetectionAccuracyTest.kt # Main test class (edit this!)
├── TestClipManager.kt            # Utilities for managing test clips
└── ACCURACY_TEST_GUIDE.md        # Detailed documentation

app/src/androidTest/assets/       # Where you put test videos
```

## The 3-Step Setup Process

### Step 1: Add Test Videos
1. Create `app/src/androidTest/assets/` folder (if not exists)
2. Add your test `.mp4`, `.mov`, or `.webm` files there
   - Use real game footage with jersey #'s visible
   - Different distances and lighting (easy, medium, hard)

Example file structure:
```
app/src/androidTest/assets/
├── test_close_up.mp4        # Jersey numbers large and clear
├── test_medium_distance.mp4  # Typical game footage distance
└── test_far_away.mp4         # Small, distant jersey numbers
```

### Step 2: Annotate Your Tests
Edit `JerseyDetectionAccuracyTest.kt` and update `setupTestClips()`:

```kotlin
private fun setupTestClips() {
    // Example 1: Close-up footage
    testAnnotations.add(TestVideoAnnotation(
        clipName = "Close-Up - High Quality",
        clipUri = Uri.parse("file:///android_asset/test_close_up.mp4"),
        teamName = "Home Team",
        jerseyColor = "#FF0000",  // Red jersey
        expectedJerseyNumbers = setOf("10", "23", "7"),
        description = "Large, clear jersey numbers"
    ))
    
    // Example 2: Medium distance
    testAnnotations.add(TestVideoAnnotation(
        clipName = "Medium Distance - Typical",
        clipUri = Uri.parse("file:///android_asset/test_medium_distance.mp4"),
        teamName = "Home Team",
        jerseyColor = "#FF0000",
        expectedJerseyNumbers = setOf("10", "5", "19"),
        description = "Normal game footage distance"
    ))
    
    // Example 3: Far away / challenging
    testAnnotations.add(TestVideoAnnotation(
        clipName = "Far Away - Challenge",
        clipUri = Uri.parse("file:///android_asset/test_far_away.mp4"),
        teamName = "Home Team",
        jerseyColor = "#FF0000",
        expectedJerseyNumbers = setOf("10"),
        description = "Small, distant numbers from far field"
    ))
}
```

**IMPORTANT**: For each video, you need to list ALL jersey numbers that are visible in that clip.

### Step 3: Run the Tests

```bash
# Build and run all accuracy tests on connected device/emulator
./gradlew connectedAndroidTest --tests "com.playerid.app.JerseyDetectionAccuracyTest"

# Watch the output in logcat for detailed results
```

## What You'll Get

The test harness produces a detailed accuracy report:

```
======================================================================
JERSEY DETECTION ACCURACY TEST REPORT
======================================================================

SUMMARY STATISTICS
----------------------------------------------------------------------
Total Tests:           3
Successful Tests:      2 (67%)
Failed Tests:          1

AGGREGATE METRICS
----------------------------------------------------------------------
Average Precision:     0.89
Average Recall:        0.85
Average F1 Score:      0.87

DETAILED RESULTS
======================================================================

Clip: Close-Up - High Quality
----------------------------------------------------------------------
Expected:     [7, 10, 23]
Detected:     [7, 10, 23]
Match:        ✓ PASS
Precision:    1.00
Recall:       1.00
F1 Score:     1.00
```

## Understanding the Metrics

| Metric | Formula | What It Means |
|--------|---------|--------------|
| **Precision** | TP / (TP + FP) | Of what we detected, how many were correct? (1.0 = perfect) |
| **Recall** | TP / (TP + FN) | Of what should be detected, how many did we find? (1.0 = perfect) |
| **F1 Score** | 2×(P×R)/(P+R) | Balanced accuracy score (0-1, higher is better) |

- **TP** = True Positives (correctly detected)
- **FP** = False Positives (wrong detections)
- **FN** = False Negatives (missed detections)

## Decision Matrix: What Your Results Mean

| Accuracy | Recommendation |
|----------|---|
| **>92%** | ✓ ML Kit is excellent. Ship as-is. |
| **87-92%** | ✓ ML Kit is very good. Consider minor tweaks if needed. |
| **80-87%** | ⚠️ ML Kit is decent but not great. Fine-tune filtering or consider Jersey10Detector. |
| **70-80%** | ✗ Consider training a custom model. Current approach is marginal. |
| **<70%** | ✗ Need a custom model. ML Kit alone isn't sufficient. |

## Available Test Methods

### `testJerseyDetectionAccuracy()`
- Tests all clips with ACCURATE mode
- Produces full detailed report
- Best for overall evaluation

### `testFastModePerformance()`
- Measures speed vs accuracy of FAST mode
- Good for understanding latency tradeoff

### `testAccurateModePerformance()`
- Measures maximum possible accuracy
- Establishes a ceiling for current pipeline

### `testWithColorFiltering()`
- Tests with team jersey color filtering
- Shows how much color filtering helps

## Tips for Success

1. **Use real game footage** - Not synthetic or test data
2. **Include variety** - Different distances, angles, lighting
3. **Be accurate with annotations** - Watch entire clip, list every visible #
4. **Start small** - 3-5 test clips, then expand
5. **Test incrementally** - Run after each improvement

## Next Steps After Testing

**If accuracy is 85%+:**
- Keep current ML Kit approach
- Focus on other features
- Occasional tuning as needed

**If accuracy is 75-85%:**
- Integrate Jersey10Detector's validation logic
- Enable color filtering if not already
- Test again to see improvement

**If accuracy is <75%:**
- Consider training a custom TensorFlow model
- Use YOLOv8 for jersey detection
- See `ml_training/README.md` for model training guide

## Troubleshooting

**Tests won't compile?**
- Check gradle sync succeeded
- Ensure `androidTest` folder structure is correct

**Tests won't run?**
- Connect device or start emulator
- Try: `./gradlew connectedAndroidTest`

**Getting "No test clips configured" warning?**
- You haven't added any videos to `setupTestClips()`
- Add at least one TestVideoAnnotation

**Results are all 0%?**
- Check video URIs are correct
- Verify video files are in assets folder
- Check file permissions

## For More Details

See `ACCURACY_TEST_GUIDE.md` for:
- Detailed metric explanations
- CSV export and analysis
- Failure analysis tools
- Advanced usage patterns

---

**Summary**: You now have a professional accuracy testing framework. Use it to measure performance with real game footage, then decide whether to stick with ML Kit or invest in custom model training.
