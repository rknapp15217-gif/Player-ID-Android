@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.playerid.app.platform

import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.recognition.JerseyObservation
import com.playerid.app.domain.recognition.normalizeJerseyNumber
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import platform.AVFoundation.*
import platform.CoreGraphics.*
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSURL
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Reads jersey numbers and the surrounding shirt color from sampled video frames with Apple Vision.
 * Matching the reads to a roster is shared logic in `matchJerseyPlayers`.
 */
class IosJerseyAnalyzer(
    private val frameIntervalMs: Long = 500,
    private val maxFrames: Int = 120
) {
    suspend fun analyze(media: MediaReference): List<JerseyObservation> = withContext(Dispatchers.Default) {
        val url = validatedMemoryShareUrls(listOf(media)).single()
        observe(url)
    }

    private suspend fun observe(url: NSURL): List<JerseyObservation> {
        val asset = AVURLAsset(uRL = url, options = null)
        val seconds = CMTimeGetSeconds(asset.duration)
        if (!seconds.isFinite() || seconds <= 0) throw CameraCaptureException("This video has no usable duration.")
        val generator = AVAssetImageGenerator(asset = asset).apply {
            appliesPreferredTrackTransform = true
            maximumSize = CGSizeMake(1280.0, 1280.0)
        }
        val durationMs = (seconds * 1000).toLong()
        val step = max(frameIntervalMs, durationMs / maxFrames)
        val reads = mutableListOf<JerseyObservation>()
        var timeMs = 0L
        while (timeMs < durationMs) {
            currentCoroutineContext().ensureActive()
            val frame = generator.copyCGImageAtTime(CMTimeMake(timeMs, 1000), actualTime = null, error = null)
            if (frame != null) {
                try {
                    reads += readFrame(frame, timeMs)
                } finally {
                    CGImageRelease(frame)
                }
            }
            timeMs += step
        }
        return reads
    }

    private fun readFrame(frame: CGImageRef, timeMs: Long): List<JerseyObservation> {
        val request = VNRecognizeTextRequest().apply {
            recognitionLevel = VNRequestTextRecognitionLevelAccurate
            usesLanguageCorrection = false
            minimumTextHeight = 0.02f
        }
        val handler = VNImageRequestHandler(cGImage = frame, options = emptyMap<Any?, Any?>())
        if (!handler.performRequests(listOf(request), error = null)) return emptyList()
        val found = mutableListOf<JerseyObservation>()
        (request.results ?: emptyList<Any?>()).filterIsInstance<VNRecognizedTextObservation>().forEach { text ->
            val candidate = (text.topCandidates(1u).firstOrNull() as? platform.Vision.VNRecognizedText) ?: return@forEach
            val number = normalizeJerseyNumber(candidate.string) ?: return@forEach
            val (x, y, w, h) = text.boundingBox.useContents { listOf(origin.x, origin.y, size.width, size.height) }
            found += JerseyObservation(number, shirtColor(frame, x, y, w, h), candidate.confidence, timeMs)
        }
        return found
    }

    /**
     * Average color of the shirt around a number: the number's box is grown on every side and only the surrounding
     * ring is averaged, so the digit's own color does not skew the result. Vision boxes are normalized, bottom-left.
     */
    private fun shirtColor(frame: CGImageRef, x: Double, y: Double, w: Double, h: Double): String? {
        val imageWidth = CGImageGetWidth(frame).toDouble()
        val imageHeight = CGImageGetHeight(frame).toDouble()
        val grow = 1.0
        val left = max(0.0, (x - w * grow) * imageWidth)
        val right = min(imageWidth, (x + w * (1 + grow)) * imageWidth)
        val top = max(0.0, (1 - (y + h * (1 + grow))) * imageHeight)
        val bottom = min(imageHeight, (1 - (y - h * grow)) * imageHeight)
        if (right - left < 4 || bottom - top < 4) return null
        val crop = CGImageCreateWithImageInRect(frame, CGRectMake(left, top, right - left, bottom - top)) ?: return null
        try {
            val side = 12
            val pixels = UByteArray(side * side * 4)
            val drawn = pixels.usePinned { pinned ->
                val space = CGColorSpaceCreateDeviceRGB()
                val context = CGBitmapContextCreate(
                    pinned.addressOf(0), side.convert(), side.convert(), 8u, (side * 4).convert(), space,
                    CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
                )
                CGColorSpaceRelease(space)
                if (context == null) false else {
                    CGContextDrawImage(context, CGRectMake(0.0, 0.0, side.toDouble(), side.toDouble()), crop)
                    CGContextRelease(context)
                    true
                }
            }
            if (!drawn) return null
            var r = 0L; var g = 0L; var b = 0L; var count = 0
            val inner = side / 3 until side - side / 3
            for (row in 0 until side) for (col in 0 until side) {
                if (row in inner && col in inner) continue
                val i = (row * side + col) * 4
                r += pixels[i].toInt(); g += pixels[i + 1].toInt(); b += pixels[i + 2].toInt(); count++
            }
            if (count == 0) return null
            fun hex(v: Long) = (v.toDouble() / count).roundToInt().coerceIn(0, 255).toString(16).padStart(2, '0')
            return "#${hex(r)}${hex(g)}${hex(b)}".uppercase()
        } finally {
            CGImageRelease(crop)
        }
    }
}
