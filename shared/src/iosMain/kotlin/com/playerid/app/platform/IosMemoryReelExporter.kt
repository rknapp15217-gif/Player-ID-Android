@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.playerid.app.platform

import com.playerid.app.domain.camera.CameraCaptureException
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import platform.AVFoundation.*
import platform.CoreGraphics.*
import platform.CoreMedia.*
import platform.Foundation.*
import platform.QuartzCore.*
import platform.UIKit.*
import com.playerid.app.domain.memory.REEL_INTRO_MILLIS
import com.playerid.app.domain.memory.reelIntroTitleLines
import kotlin.math.min

class IosMemoryReelExporter(private val service: IosCameraService) {
    private var building = false
    private val canceled = MutableStateFlow(false)
    private var activeExport: AVAssetExportSession? = null

    fun cancel() {
        canceled.value = true
        activeExport?.cancelExport()
    }

    suspend fun build(sources: List<MediaReference>, title: String): MediaReference {
        if (building) throw CameraCaptureException("A reel is already being built.")
        if (sources.size < 2 || sources.any { it.kind != MediaKind.VIDEO } ||
            sources.map { it.identifier }.distinct().size != sources.size) {
            throw CameraCaptureException("Choose at least two different videos.")
        }
        building = true
        canceled.value = false
        try {
            val session = withContext(Dispatchers.Default) { compositionSession(sources, title) }
            if (canceled.value) throw CameraCaptureException("Reel export canceled. Your sources are unchanged.")
            activeExport = session
            return saveVideoExport(session, service, "Reel")
        } finally {
            activeExport = null
            building = false
        }
    }

    private fun compositionSession(sources: List<MediaReference>, title: String): AVAssetExportSession = memScoped {
        val urls = validatedMemoryShareUrls(sources)
        val composition = AVMutableComposition()
        val instructions = mutableListOf<AVMutableVideoCompositionInstruction>()
        // Android opens every reel with a title card; the clips start after it and the card is drawn over black.
        val introDuration = CMTimeMake(REEL_INTRO_MILLIS * 600 / 1000, 600)
        instructions += AVMutableVideoCompositionInstruction.videoCompositionInstruction().apply {
            setTimeRange(CMTimeRangeMake(CMTimeMake(0, 600), introDuration))
            setLayerInstructions(emptyList<Any>())
        }
        var cursor = introDuration
        val failure = alloc<ObjCObjectVar<NSError?>>()
        urls.forEachIndexed { index, url ->
            if (canceled.value) throw CameraCaptureException("Reel export canceled. Your sources are unchanged.")
            val asset = AVURLAsset(uRL = url, options = null)
            val video = asset.tracksWithMediaType(AVMediaTypeVideo).firstOrNull() as? AVAssetTrack
                ?: throw CameraCaptureException("Clip ${index + 1} has no playable video track. Nothing was saved.")
            val duration = video.timeRange.useContents { duration.readValue() }
            val seconds = CMTimeGetSeconds(duration)
            if (!seconds.isFinite() || seconds <= 0) {
                throw CameraCaptureException("Clip ${index + 1} has no usable duration.")
            }
            val sourceRange = video.timeRange
            val sourceStart = sourceRange.useContents { start.readValue() }
            val track = composition.addMutableTrackWithMediaType(AVMediaTypeVideo, kCMPersistentTrackID_Invalid)
                ?: throw CameraCaptureException("Cannot create a reel video track.")
            failure.value = null
            if (!track.insertTimeRange(sourceRange, ofTrack = video, atTime = cursor, error = failure.ptr)) {
                throw CameraCaptureException("Cannot add clip ${index + 1}: ${failure.value?.localizedDescription.orEmpty()}")
            }
            val layer = AVMutableVideoCompositionLayerInstruction.videoCompositionLayerInstructionWithAssetTrack(track)
            layer.setTransform(fitTransform(video), atTime = cursor)
            val instruction = AVMutableVideoCompositionInstruction.videoCompositionInstruction()
            instruction.setTimeRange(CMTimeRangeMake(cursor, duration))
            instruction.setLayerInstructions(listOf(layer))
            instructions += instruction
            val audio = asset.tracksWithMediaType(AVMediaTypeAudio).firstOrNull() as? AVAssetTrack
            if (audio != null) {
                val audioStart = audio.timeRange.useContents { start.readValue() }
                val audioEnd = CMTimeRangeGetEnd(audio.timeRange)
                val overlapStart = CMTimeMaximum(sourceStart, audioStart)
                val overlapEnd = CMTimeMinimum(CMTimeAdd(sourceStart, duration), audioEnd)
                if (CMTimeCompare(overlapEnd, overlapStart) > 0) {
                    val audioTrack = composition.addMutableTrackWithMediaType(AVMediaTypeAudio, kCMPersistentTrackID_Invalid)
                        ?: throw CameraCaptureException("Cannot create a reel audio track.")
                    failure.value = null
                    if (!audioTrack.insertTimeRange(CMTimeRangeMake(overlapStart, CMTimeSubtract(overlapEnd, overlapStart)),
                            ofTrack = audio, atTime = CMTimeAdd(cursor, CMTimeSubtract(overlapStart, sourceStart)), error = failure.ptr)) {
                        throw CameraCaptureException("Cannot add audio for clip ${index + 1}: ${failure.value?.localizedDescription.orEmpty()}")
                    }
                }
            }
            cursor = CMTimeAdd(cursor, duration)
        }
        val videoComposition = AVMutableVideoComposition().apply {
            setRenderSize(CGSizeMake(REEL_WIDTH, REEL_HEIGHT))
            setFrameDuration(CMTimeMake(1, 24))
            setInstructions(instructions)
            setAnimationTool(introAnimationTool(title, CMTimeGetSeconds(introDuration)))
        }
        val session = AVAssetExportSession.exportSessionWithAsset(composition, AVAssetExportPresetHighestQuality)
            ?: throw CameraCaptureException("This iPhone cannot export the selected reel.")
        session.videoComposition = videoComposition
        session
    }

    private fun introAnimationTool(title: String, introSeconds: Double): AVVideoCompositionCoreAnimationTool {
        val frame = CGRectMake(0.0, 0.0, REEL_WIDTH, REEL_HEIGHT)
        val parent = CALayer().apply { setFrame(frame) }
        val video = CALayer().apply { setFrame(frame) }
        val intro = CALayer().apply {
            setFrame(frame)
            setContents(introImage(title).CGImage?.let { interpretObjCPointer<Any>(it.rawValue) })
            setOpacity(0f)
        }
        intro.addAnimation(CAKeyframeAnimation.animationWithKeyPath("opacity").apply {
            setValues(listOf(1.0, 1.0, 0.0))
            setKeyTimes(listOf(0.0, 0.9, 1.0))
            setDuration(introSeconds)
            setBeginTime(AVCoreAnimationBeginTimeAtZero)
            setRemovedOnCompletion(false)
        }, forKey = "playeridReelIntro")
        parent.addSublayer(video)
        parent.addSublayer(intro)
        return AVVideoCompositionCoreAnimationTool
            .videoCompositionCoreAnimationToolWithPostProcessingAsVideoLayer(video, inLayer = parent)
    }

    /** Draws Android's intro card: navy background, blue bars, "HIGHLIGHT REEL", wrapped title, footer. */
    private fun introImage(title: String): UIImage {
        val format = UIGraphicsImageRendererFormat.defaultFormat().apply { setScale(1.0); setOpaque(true) }
        val renderer = UIGraphicsImageRenderer(size = CGSizeMake(REEL_WIDTH, REEL_HEIGHT), format = format)
        return renderer.imageWithActions { _ ->
            rgb(0x0A, 0x11, 0x18).setFill()
            UIRectFill(CGRectMake(0.0, 0.0, REEL_WIDTH, REEL_HEIGHT))
            rgb(0x1C, 0x8C, 0xFF).setFill()
            UIRectFill(CGRectMake(0.0, 0.0, REEL_WIDTH, 18.0))
            UIRectFill(CGRectMake(0.0, REEL_HEIGHT - 18.0, REEL_WIDTH, 18.0))
            val subtitleFont = UIFont.systemFontOfSize(30.0)
            val titleFont = UIFont.systemFontOfSize(56.0, weight = UIFontWeightMedium)
            val subtitleColor = rgb(0x9F, 0xC9, 0xFF)
            drawCentered("HIGHLIGHT REEL", REEL_HEIGHT / 2 - 56.0, subtitleFont, subtitleColor)
            reelIntroTitleLines(title).forEachIndexed { index, line ->
                drawCentered(line, REEL_HEIGHT / 2 + 10.0 + index * 56.0 * 1.2, titleFont, UIColor.whiteColor)
            }
            drawCentered("Created with PlayerID", REEL_HEIGHT - 84.0, subtitleFont, subtitleColor)
        }
    }

    /** Matches Android's Canvas.drawText: [baseline] is the text baseline, centered horizontally. */
    @Suppress("CAST_NEVER_SUCCEEDS")
    private fun drawCentered(text: String, baseline: Double, font: UIFont, color: UIColor) {
        val attributes = mapOf<Any?, Any?>(NSFontAttributeName to font, NSForegroundColorAttributeName to color)
        val string = text as NSString
        val width = string.sizeWithAttributes(attributes).useContents { width }
        string.drawAtPoint(CGPointMake((REEL_WIDTH - width) / 2, baseline - font.ascender), withAttributes = attributes)
    }

    private fun rgb(red: Int, green: Int, blue: Int) =
        UIColor.colorWithRed(red / 255.0, green = green / 255.0, blue = blue / 255.0, alpha = 1.0)

    private fun fitTransform(track: AVAssetTrack): CValue<CGAffineTransform> {
        val naturalSize = track.naturalSize
        val width = naturalSize.useContents { width }
        val height = naturalSize.useContents { height }
        val oriented = CGRectApplyAffineTransform(CGRectMake(0.0, 0.0, width, height), track.preferredTransform)
        return oriented.useContents {
            if (!size.width.isFinite() || !size.height.isFinite() || size.width <= 0 || size.height <= 0) {
                throw CameraCaptureException("A selected clip has invalid video dimensions.")
            }
            val scale = min(720.0 / size.width, 1280.0 / size.height)
            val offsetX = (720.0 - size.width * scale) / 2.0 - origin.x * scale
            val offsetY = (1280.0 - size.height * scale) / 2.0 - origin.y * scale
            track.preferredTransform.useContents {
                CGAffineTransformMake(a * scale, b * scale, c * scale, d * scale,
                    tx * scale + offsetX, ty * scale + offsetY)
            }
        }
    }
}

private const val REEL_WIDTH = 720.0
private const val REEL_HEIGHT = 1280.0
