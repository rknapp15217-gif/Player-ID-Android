#!/bin/bash
# Quick setup script for running accuracy tests
# Usage: ./setup_accuracy_tests.sh

set -e

echo "Setting up Jersey Detection Accuracy Tests..."
echo ""

# Check if we have test video files
if [ -d "app/src/androidTest/assets" ]; then
    MP4_COUNT=$(find app/src/androidTest/assets -name "*.mp4" 2>/dev/null | wc -l || echo 0)
    MOV_COUNT=$(find app/src/androidTest/assets -name "*.mov" 2>/dev/null | wc -l || echo 0)
    WEBM_COUNT=$(find app/src/androidTest/assets -name "*.webm" 2>/dev/null | wc -l || echo 0)
    
    TOTAL=$((MP4_COUNT + MOV_COUNT + WEBM_COUNT))
    
    echo "Found $TOTAL video files in androidTest/assets:"
    echo "  - MP4: $MP4_COUNT"
    echo "  - MOV: $MOV_COUNT"
    echo "  - WebM: $WEBM_COUNT"
    echo ""
else
    echo "ℹ️  No androidTest/assets directory found yet"
    echo "   Create one and add your test videos there"
    echo ""
fi

echo "Next steps:"
echo "1. Add your test video clips to app/src/androidTest/assets/"
echo "2. Edit app/src/androidTest/java/com/playerid/app/JerseyDetectionAccuracyTest.kt"
echo "3. Update setupTestClips() with your annotations"
echo "4. Run: ./gradlew connectedAndroidTest --tests 'com.playerid.app.JerseyDetectionAccuracyTest'"
echo ""
echo "For detailed instructions, see: ACCURACY_TEST_GUIDE.md"
