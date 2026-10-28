@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.playerid.app.platform

import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.camera.rollingCaptureWindow
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.Foundation.*

internal suspend fun exportRollingVideo(urls: List<NSURL>, service: IosCameraService): MediaReference {
    val session = withContext(Dispatchers.Default) {
        memScoped {
            val assets = urls.map { AVURLAsset(uRL = it, options = null) }
            val videos = assets.map { asset ->
                asset.tracksWithMediaType(AVMediaTypeVideo).firstOrNull() as? AVAssetTrack
                    ?: throw CameraCaptureException("A Rolling segment has no playable video. Resume Rolling to retry.")
            }
            val durations = videos.map { CMTimeGetSeconds(it.timeRange.useContents { duration.readValue() }) }
            val ranges = try {
                rollingCaptureWindow(durations)
            } catch (failure: IllegalArgumentException) {
                throw CameraCaptureException(failure.message ?: "Rolling has no usable video yet.")
            }
            val composition = AVMutableComposition()
            val videoTrack = composition.addMutableTrackWithMediaType(AVMediaTypeVideo, kCMPersistentTrackID_Invalid)
                ?: throw CameraCaptureException("Cannot create the Rolling video track.")
            videoTrack.setPreferredTransform(videos.first().preferredTransform)
            val audioTrack = composition.addMutableTrackWithMediaType(AVMediaTypeAudio, kCMPersistentTrackID_Invalid)
                ?: throw CameraCaptureException("Cannot create the Rolling audio track.")
            val failure = alloc<ObjCObjectVar<NSError?>>()
            var cursor = CMTimeMake(0, 600)
            for (range in ranges) {
                val video = videos[range.segmentIndex]
                val sourceStart = CMTimeAdd(video.timeRange.useContents { start.readValue() },
                    CMTimeMakeWithSeconds(range.startSeconds, 600))
                val duration = CMTimeMakeWithSeconds(range.durationSeconds, 600)
                failure.value = null
                if (!videoTrack.insertTimeRange(CMTimeRangeMake(sourceStart, duration), video, cursor, failure.ptr)) {
                    throw CameraCaptureException("Cannot assemble Rolling video: ${failure.value?.localizedDescription.orEmpty()}")
                }
                val audio = assets[range.segmentIndex].tracksWithMediaType(AVMediaTypeAudio).firstOrNull() as? AVAssetTrack
                if (audio != null) {
                    val overlapStart = CMTimeMaximum(sourceStart, audio.timeRange.useContents { start.readValue() })
                    val overlapEnd = CMTimeMinimum(CMTimeAdd(sourceStart, duration), CMTimeRangeGetEnd(audio.timeRange))
                    if (CMTimeCompare(overlapEnd, overlapStart) > 0) {
                        failure.value = null
                        if (!audioTrack.insertTimeRange(
                                CMTimeRangeMake(overlapStart, CMTimeSubtract(overlapEnd, overlapStart)), audio,
                                CMTimeAdd(cursor, CMTimeSubtract(overlapStart, sourceStart)), failure.ptr)) {
                            throw CameraCaptureException("Cannot assemble Rolling audio: ${failure.value?.localizedDescription.orEmpty()}")
                        }
                    }
                }
                cursor = CMTimeAdd(cursor, duration)
            }
            AVAssetExportSession.exportSessionWithAsset(composition, AVAssetExportPresetPassthrough)
                ?: throw CameraCaptureException("This iPhone cannot export the Rolling clip.")
        }
    }
    return saveVideoExport(session, service, "Rolling")
}
