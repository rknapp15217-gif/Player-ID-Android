@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.playerid.app.platform

import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.memory.VideoTrimRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeRangeMake
import platform.Foundation.*
import kotlin.math.roundToLong

class IosVideoTrimmer(private val service: IosCameraService) {
    private var activeExport: AVAssetExportSession? = null

    suspend fun duration(media: MediaReference): Long = withContext(Dispatchers.Default) {
        val asset = videoAsset(media)
        durationMillis(asset)
    }

    fun cancel() {
        activeExport?.cancelExport()
    }

    suspend fun export(media: MediaReference, range: VideoTrimRange): MediaReference {
        if (activeExport != null) throw CameraCaptureException("A trim export is already running.")
        val asset = withContext(Dispatchers.Default) { videoAsset(media) }
        range.validate(withContext(Dispatchers.Default) { durationMillis(asset) })
        val session = AVAssetExportSession.exportSessionWithAsset(asset, AVAssetExportPresetHighestQuality)
            ?: throw CameraCaptureException("This video cannot be exported on this iPhone.")
        session.timeRange = CMTimeRangeMake(CMTimeMake(range.startMillis, 1000), CMTimeMake(range.durationMillis, 1000))
        activeExport = session
        try {
            return saveVideoExport(session, service, "Trim")
        } finally {
            activeExport = null
        }
    }

    private fun videoAsset(media: MediaReference): AVURLAsset {
        if (media.kind != MediaKind.VIDEO) throw CameraCaptureException("Only videos can be trimmed.")
        val url = validatedMemoryShareUrls(listOf(media)).single()
        return AVURLAsset(uRL = url, options = null).also {
            if (it.tracksWithMediaType(AVMediaTypeVideo).isEmpty()) {
                throw CameraCaptureException("This file has no playable video track.")
            }
        }
    }

    private fun durationMillis(asset: AVAsset): Long {
        val seconds = CMTimeGetSeconds(asset.duration)
        if (!seconds.isFinite() || seconds <= 0 || seconds * 1000 > Long.MAX_VALUE.toDouble()) {
            throw CameraCaptureException("This video has no usable duration.")
        }
        return (seconds * 1000).roundToLong()
    }
}
