package com.playerid.app.video

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.playerid.app.data.Player
import com.playerid.app.ui.screens.NameBubble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class VideoProcessingManager(private val context: Context) {

    private data class TrackerState(
        val playerId: String,
        val jerseyNumber: String,
        var center: Offset,
        var sampleWidth: Int,
        var sampleHeight: Int,
        var templatePatch: FloatArray,
        var lastTimestampMs: Long,
        var lastConfidence: Float,
        var consecutiveMissCount: Int = 0  // tracks frames where jersey was not visible
    )

    enum class DetectionMode {
        FAST,
        ACCURATE
    }
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val executor = Executors.newSingleThreadExecutor()

    private data class FrameVariant(
        val bitmap: Bitmap,
        val left: Int,
        val top: Int,
        val scale: Float,
        val shouldRecycle: Boolean
    )

    suspend fun autoDetectPlayersInVideo(
        videoUri: Uri,
        roster: List<Player>,
        mode: DetectionMode = DetectionMode.ACCURATE,
        onProgress: (Float) -> Unit = {}
    ): List<NameBubble> {
        return autoDetectPlayersWithTracksInVideo(
            videoUri = videoUri,
            roster = roster,
            mode = mode,
            onProgress = onProgress
        ).bubbles
    }

    suspend fun autoDetectPlayersWithTracksInVideo(
        videoUri: Uri,
        roster: List<Player>,
        mode: DetectionMode = DetectionMode.ACCURATE,
        onProgress: (Float) -> Unit = {}
    ): VideoPlayerDetectionResult = withContext(Dispatchers.IO) {
        val detectedBubbles = mutableListOf<NameBubble>()
        val trackingSamples = mutableMapOf<String, MutableList<PlayerTrackSample>>()
        val trackerStates = mutableMapOf<String, TrackerState>()
        val singleTargetPlayer = roster.singleOrNull()
        var frameWidth = 0
        var frameHeight = 0
        
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoUri)
            
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
            val detectionIntervalUs = when (mode) {
                DetectionMode.FAST -> 1_200_000L
                DetectionMode.ACCURATE -> 500000L
            }
            val trackingIntervalUs = when (mode) {
                DetectionMode.FAST -> detectionIntervalUs
                DetectionMode.ACCURATE -> 250000L
            }
            val totalFrames = (durationMs * 1000 / trackingIntervalUs).toInt()
            
            var frameCount = 0
            var currentTimeUs = 0L
            var nextDetectionTimeUs = 0L
            
            while (currentTimeUs < durationMs * 1000) {
                try {
                    val bitmap = retriever.getFrameAtTime(currentTimeUs, MediaMetadataRetriever.OPTION_CLOSEST)
                    bitmap?.let { frame ->
                        if (frameWidth == 0 || frameHeight == 0) {
                            frameWidth = frame.width
                            frameHeight = frame.height
                        }

                        val timestampMs = currentTimeUs / 1000
                        if (currentTimeUs >= nextDetectionTimeUs) {
                            val detectedNumbers = detectJerseyNumbers(frame, mode)

                            if (singleTargetPlayer != null) {
                                val currentTracker = trackerStates[singleTargetPlayer.id]
                                val targetDetections = detectedNumbers.filter { it.number == singleTargetPlayer.number }
                                val chosenDetection = chooseBestDetectionForTracker(targetDetections, currentTracker)

                                if (chosenDetection != null) {
                                    val existingBubble = detectedBubbles.find {
                                        it.jerseyNumber == chosenDetection.number
                                    }
                                    if (existingBubble == null) {
                                        detectedBubbles.add(
                                            NameBubble(
                                                id = "auto_${chosenDetection.number}_${System.currentTimeMillis()}",
                                                playerName = singleTargetPlayer.name,
                                                jerseyNumber = chosenDetection.number,
                                                position = chosenDetection.position,
                                                isVisible = true
                                            )
                                        )
                                    }

                                    val mergedTracker = mergeDetectionIntoTracker(
                                        frame = frame,
                                        existingTracker = currentTracker,
                                        playerId = singleTargetPlayer.id,
                                        detection = chosenDetection,
                                        timestampMs = timestampMs
                                    )
                                    if (mergedTracker != null) {
                                        trackerStates[singleTargetPlayer.id] = mergedTracker
                                        appendTrackingSample(
                                            trackingSamples = trackingSamples,
                                            playerId = singleTargetPlayer.id,
                                            sample = PlayerTrackSample(
                                                timestampMs = timestampMs,
                                                position = mergedTracker.center,
                                                confidence = mergedTracker.lastConfidence
                                            )
                                        )
                                    }
                                } else {
                                    currentTracker?.let { tracker ->
                                        val msSince = timestampMs - tracker.lastTimestampMs
                                        if (msSince <= 1800L) {
                                            appendTrackingSample(
                                                trackingSamples = trackingSamples,
                                                playerId = singleTargetPlayer.id,
                                                sample = PlayerTrackSample(
                                                    timestampMs = timestampMs,
                                                    position = tracker.center,
                                                    confidence = (tracker.lastConfidence * 0.9f).coerceAtLeast(0.2f)
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                detectedNumbers.forEach { detection ->
                                    val player = roster.find { it.number == detection.number }
                                    player?.let {
                                        val existingBubble = detectedBubbles.find {
                                            it.jerseyNumber == detection.number
                                        }

                                        if (existingBubble == null) {
                                            detectedBubbles.add(
                                                NameBubble(
                                                    id = "auto_${detection.number}_${System.currentTimeMillis()}",
                                                    playerName = player.name,
                                                    jerseyNumber = detection.number,
                                                    position = detection.position,
                                                    isVisible = true
                                                )
                                            )
                                        }

                                        appendTrackingSample(
                                            trackingSamples = trackingSamples,
                                            playerId = player.id,
                                            sample = PlayerTrackSample(
                                                timestampMs = timestampMs,
                                                position = detection.position,
                                                confidence = detection.confidence
                                            )
                                        )

                                        createTrackerState(frame, player.id, detection, timestampMs)?.let { tracker ->
                                            trackerStates[player.id] = tracker
                                        }
                                    }
                                }
                            }

                            nextDetectionTimeUs += detectionIntervalUs
                        } else {
                            if (singleTargetPlayer != null) {
                                trackerStates[singleTargetPlayer.id]?.let { tracker ->
                                    val trackedSample = trackPlayerInFrame(frame, tracker, timestampMs)
                                    if (trackedSample != null) {
                                        appendTrackingSample(
                                            trackingSamples = trackingSamples,
                                            playerId = tracker.playerId,
                                            sample = trackedSample
                                        )
                                    }
                                }
                            } else {
                                trackerStates.values.forEach { tracker ->
                                    val trackedSample = trackPlayerInFrame(frame, tracker, timestampMs)
                                    if (trackedSample != null) {
                                        appendTrackingSample(
                                            trackingSamples = trackingSamples,
                                            playerId = tracker.playerId,
                                            sample = trackedSample
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    frameCount++
                    val normalizedTotal = if (totalFrames <= 0) 1 else totalFrames
                    onProgress(frameCount.toFloat() / normalizedTotal)
                    
                } catch (e: Exception) {
                    // Skip this frame if there's an error
                }
                
                currentTimeUs += trackingIntervalUs
            }
            
            retriever.release()
            
        } catch (e: Exception) {
            // Handle video processing error
        }

        val tracks = trackingSamples.map { (playerId, samples) ->
            PlayerDetectionTrack(
                playerId = playerId,
                samples = samples.sortedBy { it.timestampMs }
            )
        }

        return@withContext VideoPlayerDetectionResult(
            bubbles = detectedBubbles.distinctBy { it.jerseyNumber },
            tracks = tracks,
            frameWidth = frameWidth,
            frameHeight = frameHeight
        )
    }

    private fun distanceSquared(a: Offset, b: Offset): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return dx * dx + dy * dy
    }

    private fun appendTrackingSample(
        trackingSamples: MutableMap<String, MutableList<PlayerTrackSample>>,
        playerId: String,
        sample: PlayerTrackSample
    ) {
        val samples = trackingSamples.getOrPut(playerId) { mutableListOf() }
        val latest = samples.lastOrNull()
        val shouldAppend = latest == null ||
            abs(sample.timestampMs - latest.timestampMs) > 80 ||
            distanceSquared(sample.position, latest.position) > 16f
        if (shouldAppend) {
            samples.add(sample)
        }
    }

    private fun createTrackerState(
        frame: Bitmap,
        playerId: String,
        detection: JerseyDetection,
        timestampMs: Long
    ): TrackerState? {
        val sampleWidth = (detection.boxWidth * 2.2f).toInt().coerceAtLeast(24)
        val sampleHeight = (detection.boxHeight * 2.8f).toInt().coerceAtLeast(32)
        val patch = extractPatchSignature(
            bitmap = frame,
            center = detection.position,
            sampleWidth = sampleWidth,
            sampleHeight = sampleHeight,
            patchSize = 18
        ) ?: return null

        return TrackerState(
            playerId = playerId,
            jerseyNumber = detection.number,
            center = detection.position,
            sampleWidth = sampleWidth,
            sampleHeight = sampleHeight,
            templatePatch = patch,
            lastTimestampMs = timestampMs,
            lastConfidence = detection.confidence
        )
    }

    private fun chooseBestDetectionForTracker(
        detections: List<JerseyDetection>,
        tracker: TrackerState?
    ): JerseyDetection? {
        if (detections.isEmpty()) return null
        if (tracker == null) {
            return detections.maxByOrNull { it.confidence }
        }

        return detections.minByOrNull { detection ->
            val distance = sqrt(distanceSquared(tracker.center, detection.position))
            val distanceNorm = (distance / (max(tracker.sampleWidth, tracker.sampleHeight).coerceAtLeast(1).toFloat() * 2f))
                .coerceIn(0f, 1.5f)
            distanceNorm * 0.72f + (1f - detection.confidence) * 0.28f
        }
    }

    private fun mergeDetectionIntoTracker(
        frame: Bitmap,
        existingTracker: TrackerState?,
        playerId: String,
        detection: JerseyDetection,
        timestampMs: Long
    ): TrackerState? {
        if (existingTracker == null) {
            return createTrackerState(frame, playerId, detection, timestampMs)
        }

        val proposedWidth = (detection.boxWidth * 2.2f).toInt().coerceAtLeast(24)
        val proposedHeight = (detection.boxHeight * 2.8f).toInt().coerceAtLeast(32)
        val newPatch = extractPatchSignature(
            bitmap = frame,
            center = detection.position,
            sampleWidth = proposedWidth,
            sampleHeight = proposedHeight,
            patchSize = 18
        ) ?: return existingTracker

        val distance = sqrt(distanceSquared(existingTracker.center, detection.position))
        val maxReanchorDistance = max(existingTracker.sampleWidth, existingTracker.sampleHeight).toFloat() * 1.6f
        // After a miss streak, accept re-anchor at lower confidence so recovery fires sooner
        val reanchorConfidenceGate = if (existingTracker.consecutiveMissCount > 0) 0.72f else 0.88f
        if (distance > maxReanchorDistance && detection.confidence < reanchorConfidenceGate) {
            return existingTracker
        }

        val blend = if (distance > maxReanchorDistance * 0.6f) 0.2f else 0.42f
        existingTracker.center = Offset(
            x = blendFloat(existingTracker.center.x, detection.position.x, blend),
            y = blendFloat(existingTracker.center.y, detection.position.y, blend)
        )
        existingTracker.sampleWidth = blendFloat(existingTracker.sampleWidth.toFloat(), proposedWidth.toFloat(), 0.35f).toInt()
        existingTracker.sampleHeight = blendFloat(existingTracker.sampleHeight.toFloat(), proposedHeight.toFloat(), 0.35f).toInt()
        existingTracker.templatePatch = blendPatches(existingTracker.templatePatch, newPatch, 0.28f)
        existingTracker.lastTimestampMs = timestampMs
        existingTracker.lastConfidence = max(existingTracker.lastConfidence * 0.75f, detection.confidence)
        existingTracker.consecutiveMissCount = 0  // OCR confirmed player - reset miss streak
        return existingTracker
    }

    private fun trackPlayerInFrame(
        frame: Bitmap,
        tracker: TrackerState,
        timestampMs: Long
    ): PlayerTrackSample? {
        val step = max(4, min(tracker.sampleWidth, tracker.sampleHeight) / 8)
        val searchRadiusX = max(18, tracker.sampleWidth / 2)
        val searchRadiusY = max(18, tracker.sampleHeight / 2)

        var bestCenter: Offset? = null
        var bestPatch: FloatArray? = null
        var bestScore = Float.MAX_VALUE

        for (dy in -searchRadiusY..searchRadiusY step step) {
            for (dx in -searchRadiusX..searchRadiusX step step) {
                val candidateCenter = Offset(
                    x = tracker.center.x + dx,
                    y = tracker.center.y + dy
                )
                val candidatePatch = extractPatchSignature(
                    bitmap = frame,
                    center = candidateCenter,
                    sampleWidth = tracker.sampleWidth,
                    sampleHeight = tracker.sampleHeight,
                    patchSize = 18
                ) ?: continue
                val score = patchDifference(tracker.templatePatch, candidatePatch)
                if (score < bestScore) {
                    bestScore = score
                    bestCenter = candidateCenter
                    bestPatch = candidatePatch
                }
            }
        }

        // Player turned away or patch lost - hold last known position for continuity
        // This prevents the freeze-then-snap when the jersey briefly disappears
        if (bestCenter == null || bestScore > 0.22f) {
            tracker.consecutiveMissCount++
            tracker.lastTimestampMs = timestampMs
            tracker.lastConfidence = (tracker.lastConfidence * 0.88f).coerceAtLeast(0.08f)
            // Hold position for up to 8 misses (~2 seconds at 250ms/frame); give up after that
            return if (tracker.consecutiveMissCount <= 8) {
                PlayerTrackSample(
                    timestampMs = timestampMs,
                    position = tracker.center,
                    confidence = tracker.lastConfidence
                )
            } else {
                null
            }
        }

        val resolvedCenter = bestCenter!!
        val resolvedPatch = bestPatch!!

        val maxStep = max(tracker.sampleWidth, tracker.sampleHeight).toFloat() * 0.28f
        val deltaX = resolvedCenter.x - tracker.center.x
        val deltaY = resolvedCenter.y - tracker.center.y
        val distance = sqrt((deltaX * deltaX + deltaY * deltaY).toDouble()).toFloat()
        val limitedCenter = if (distance > maxStep && distance > 0f) {
            val scale = maxStep / distance
            Offset(
                x = tracker.center.x + deltaX * scale,
                y = tracker.center.y + deltaY * scale
            )
        } else {
            resolvedCenter
        }

        // Good match - reset miss counter
        tracker.consecutiveMissCount = 0
        tracker.center = limitedCenter
        // Only blend the template on clean matches to preserve the jersey-facing reference view.
        // Marginal matches (0.14-0.22) may occur during transition; updating template there would
        // corrupt it and make the tracker drift to the wrong body region on the next miss.
        if (bestScore < 0.14f) {
            tracker.templatePatch = blendPatches(tracker.templatePatch, resolvedPatch, 0.22f)
        }
        tracker.lastTimestampMs = timestampMs
        tracker.lastConfidence = (1f - bestScore * 2.5f).coerceIn(0.15f, 0.95f)

        return PlayerTrackSample(
            timestampMs = timestampMs,
            position = limitedCenter,
            confidence = tracker.lastConfidence
        )
    }

    private fun extractPatchSignature(
        bitmap: Bitmap,
        center: Offset,
        sampleWidth: Int,
        sampleHeight: Int,
        patchSize: Int
    ): FloatArray? {
        val usableWidth = sampleWidth.coerceAtMost((bitmap.width - 1).coerceAtLeast(8))
        val usableHeight = sampleHeight.coerceAtMost((bitmap.height - 1).coerceAtLeast(8))
        val left = (center.x - usableWidth / 2f).toInt().coerceIn(0, (bitmap.width - usableWidth).coerceAtLeast(0))
        val top = (center.y - usableHeight / 2f).toInt().coerceIn(0, (bitmap.height - usableHeight).coerceAtLeast(0))

        val patch = FloatArray(patchSize * patchSize)
        val xScale = usableWidth.toFloat() / patchSize.toFloat()
        val yScale = usableHeight.toFloat() / patchSize.toFloat()

        var index = 0
        for (py in 0 until patchSize) {
            for (px in 0 until patchSize) {
                val sampleX = (left + (px + 0.5f) * xScale).toInt().coerceIn(0, bitmap.width - 1)
                val sampleY = (top + (py + 0.5f) * yScale).toInt().coerceIn(0, bitmap.height - 1)
                val pixel = bitmap.getPixel(sampleX, sampleY)
                val red = (pixel shr 16) and 0xFF
                val green = (pixel shr 8) and 0xFF
                val blue = pixel and 0xFF
                patch[index++] = (0.299f * red + 0.587f * green + 0.114f * blue) / 255f
            }
        }

        val mean = patch.average().toFloat()
        for (i in patch.indices) {
            patch[i] = patch[i] - mean
        }
        return patch
    }

    private fun patchDifference(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return Float.MAX_VALUE
        var total = 0f
        for (i in a.indices) {
            total += abs(a[i] - b[i])
        }
        return total / a.size.toFloat()
    }

    private fun blendPatches(base: FloatArray, update: FloatArray, updateWeight: Float): FloatArray {
        val blended = FloatArray(base.size)
        val baseWeight = 1f - updateWeight
        for (i in base.indices) {
            blended[i] = base[i] * baseWeight + update[i] * updateWeight
        }
        return blended
    }

    private fun blendFloat(start: Float, end: Float, fraction: Float): Float {
        return start + (end - start) * fraction
    }

    private suspend fun detectJerseyNumbers(
        bitmap: Bitmap,
        mode: DetectionMode
    ): List<JerseyDetection> {
        val variants = when (mode) {
            DetectionMode.FAST -> buildFrameVariantsFast(bitmap)
            DetectionMode.ACCURATE -> buildFrameVariants(bitmap)
        }
        val mergedDetections = mutableListOf<JerseyDetection>()
        val seen = mutableSetOf<String>()

        return try {
            if (mode == DetectionMode.FAST) {
                // FAST mode prioritizes latency over recall: run OCR once on the base variant.
                detectJerseyNumbersInVariant(
                    variant = variants.first(),
                    includeElementLevelOcr = false
                )
            } else {
                for (variant in variants) {
                    val detections = detectJerseyNumbersInVariant(
                        variant = variant,
                        includeElementLevelOcr = true
                    )
                    for (detection in detections) {
                        val dedupeKey = "${detection.number}_${(detection.position.x / 24f).toInt()}_${(detection.position.y / 24f).toInt()}"
                        if (seen.add(dedupeKey)) {
                            mergedDetections.add(detection)
                        }
                    }
                }
                mergedDetections
            }
        } finally {
            variants.filter { it.shouldRecycle }.forEach { it.bitmap.recycle() }
        }
    }

    private fun buildFrameVariants(original: Bitmap): List<FrameVariant> {
        val variants = mutableListOf<FrameVariant>()
        val width = original.width
        val height = original.height

        variants.add(
            FrameVariant(
                bitmap = original,
                left = 0,
                top = 0,
                scale = 1f,
                shouldRecycle = false
            )
        )

        // Full-frame upscale improves detection of smaller, far-away jersey numbers.
        val fullScale = min(2f, 1920f / max(width, height).toFloat())
        if (fullScale > 1f) {
            val scaled = Bitmap.createScaledBitmap(
                original,
                (width * fullScale).toInt(),
                (height * fullScale).toInt(),
                true
            )
            variants.add(
                FrameVariant(
                    bitmap = scaled,
                    left = 0,
                    top = 0,
                    scale = fullScale,
                    shouldRecycle = true
                )
            )
        }

        // Cropped and upscaled tiles give OCR a closer view of distant players.
        val tileW = (width * 0.6f).toInt().coerceAtLeast(1)
        val tileH = (height * 0.6f).toInt().coerceAtLeast(1)
        val xOffsets = listOf(0, ((width - tileW) / 2).coerceAtLeast(0), (width - tileW).coerceAtLeast(0))
        val yOffsets = listOf(0, ((height - tileH) / 2).coerceAtLeast(0), (height - tileH).coerceAtLeast(0))

        xOffsets.forEach { left ->
            yOffsets.forEach { top ->
                val cropped = Bitmap.createBitmap(original, left, top, tileW, tileH)
                val tileScale = min(2f, 1280f / max(tileW, tileH).toFloat())
                val scaledTile = if (tileScale > 1f) {
                    Bitmap.createScaledBitmap(
                        cropped,
                        (tileW * tileScale).toInt(),
                        (tileH * tileScale).toInt(),
                        true
                    )
                } else {
                    cropped
                }

                if (scaledTile !== cropped) {
                    cropped.recycle()
                }

                variants.add(
                    FrameVariant(
                        bitmap = scaledTile,
                        left = left,
                        top = top,
                        scale = tileScale,
                        shouldRecycle = true
                    )
                )
            }
        }

        return variants
    }

    private fun buildFrameVariantsFast(original: Bitmap): List<FrameVariant> {
        val variants = mutableListOf<FrameVariant>()
        val width = original.width
        val height = original.height

        variants.add(
            FrameVariant(
                bitmap = original,
                left = 0,
                top = 0,
                scale = 1f,
                shouldRecycle = false
            )
        )

        val fullScale = min(1.4f, 1280f / max(width, height).toFloat())
        if (fullScale > 1f) {
            val scaled = Bitmap.createScaledBitmap(
                original,
                (width * fullScale).toInt(),
                (height * fullScale).toInt(),
                true
            )
            variants.add(
                FrameVariant(
                    bitmap = scaled,
                    left = 0,
                    top = 0,
                    scale = fullScale,
                    shouldRecycle = true
                )
            )
        }

        return variants
    }

    private suspend fun detectJerseyNumbersInVariant(
        variant: FrameVariant,
        includeElementLevelOcr: Boolean
    ): List<JerseyDetection> =
        suspendCoroutine { continuation ->
            val image = InputImage.fromBitmap(variant.bitmap, 0)
            val detections = mutableListOf<JerseyDetection>()
            val numberRegex = Regex("\\b#?(\\d{1,2})\\b")
            val seenCandidates = mutableSetOf<String>()

            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    for (block in visionText.textBlocks) {
                        for (line in block.lines) {
                            val lineText = line.text.trim()
                            numberRegex.findAll(lineText).forEach { match ->
                                val text = match.groupValues[1]
                                line.boundingBox?.let { box ->
                                    val key = "${text}_${box.centerX()}_${box.centerY()}"
                                    if (seenCandidates.add(key)) {
                                        val aspectRatio = box.height().toFloat() / box.width().toFloat()
                                        detections.add(
                                            JerseyDetection(
                                                number = text,
                                                position = Offset(
                                                    x = variant.left + (box.centerX() / variant.scale),
                                                    y = variant.top + (box.centerY() / variant.scale)
                                                ),
                                                confidence = calculateConfidence(text, aspectRatio),
                                                boxWidth = box.width() / variant.scale,
                                                boxHeight = box.height() / variant.scale
                                            )
                                        )
                                    }
                                }
                            }

                            if (includeElementLevelOcr) {
                                line.elements.forEach { element ->
                                    val elementText = element.text.trim()
                                    numberRegex.findAll(elementText).forEach { match ->
                                        val text = match.groupValues[1]
                                        element.boundingBox?.let { box ->
                                            val key = "${text}_${box.centerX()}_${box.centerY()}"
                                            if (seenCandidates.add(key)) {
                                                val aspectRatio = box.height().toFloat() / box.width().toFloat()
                                                detections.add(
                                                    JerseyDetection(
                                                        number = text,
                                                        position = Offset(
                                                            x = variant.left + (box.centerX() / variant.scale),
                                                            y = variant.top + (box.centerY() / variant.scale)
                                                        ),
                                                        confidence = calculateConfidence(text, aspectRatio),
                                                        boxWidth = box.width() / variant.scale,
                                                        boxHeight = box.height() / variant.scale
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    continuation.resume(detections)
                }
                .addOnFailureListener {
                    continuation.resume(emptyList())
                }
        }

    private fun calculateConfidence(text: String, aspectRatio: Float): Float {
        // Simple confidence calculation based on:
        // 1. Text length (1-2 digits is ideal for jersey numbers)
        // 2. Bounding box aspect ratio
        
        var confidence = 0.5f
        
        // Prefer 1-2 digit numbers
        if (text.length in 1..2) {
            confidence += 0.3f
        }
        
        // Check bounding box aspect ratio (jerseys are usually taller than wide)
        if (aspectRatio in 1.0f..2.5f) {
            confidence += 0.2f
        }
        
        return confidence.coerceIn(0f, 1f)
    }

    suspend fun exportVideoWithBubbles(
        originalVideoUri: Uri,
        nameBubbles: List<NameBubble>,
        outputPath: String,
        onProgress: (Float) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // This would use FFmpeg or MediaMuxer to overlay the name bubbles
            // For now, we'll simulate the process and integrate with existing VideoRecordingManager
            
            onProgress(0.1f)
            
            // Step 1: Extract video frames
            onProgress(0.3f)
            
            // Step 2: Overlay name bubbles on each frame
            onProgress(0.6f)
            
            // Step 3: Reconstruct video with audio
            onProgress(0.9f)
            
            // Step 4: Save to output path
            onProgress(1.0f)
            
            return@withContext true
            
        } catch (e: Exception) {
            return@withContext false
        }
    }

    fun release() {
        textRecognizer.close()
        executor.shutdown()
    }
}

data class JerseyDetection(
    val number: String,
    val position: Offset,
    val confidence: Float,
    val boxWidth: Float,
    val boxHeight: Float
)

// Enhanced Player data class with position tracking
data class PlayerWithTracking(
    val player: Player,
    val positions: List<TimestampedPosition> = emptyList(),
    val isTracked: Boolean = false
)

data class TimestampedPosition(
    val timestamp: Long, // Video timestamp in milliseconds
    val position: Offset,
    val confidence: Float
)

data class PlayerTrackSample(
    val timestampMs: Long,
    val position: Offset,
    val confidence: Float
)

data class PlayerDetectionTrack(
    val playerId: String,
    val samples: List<PlayerTrackSample>
)

data class VideoPlayerDetectionResult(
    val bubbles: List<NameBubble>,
    val tracks: List<PlayerDetectionTrack>,
    val frameWidth: Int,
    val frameHeight: Int
)