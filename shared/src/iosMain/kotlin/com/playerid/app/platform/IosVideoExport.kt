@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.playerid.app.platform

import com.playerid.app.domain.camera.CameraCaptureException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeGetSeconds
import platform.Foundation.*
import platform.UIKit.*

internal suspend fun saveVideoExport(
    session: AVAssetExportSession,
    service: IosCameraService,
    operation: String
): MediaReference {
    val type = when {
        AVFileTypeMPEG4 in session.supportedFileTypes -> AVFileTypeMPEG4
        AVFileTypeQuickTimeMovie in session.supportedFileTypes -> AVFileTypeQuickTimeMovie
        else -> throw CameraCaptureException("This video has no supported export format.")
    }
    val extension = if (type == AVFileTypeMPEG4) "mp4" else "mov"
    val temporary = NSURL.fileURLWithPath("${NSTemporaryDirectory()}playerid-export-${NSUUID().UUIDString}.$extension")
    session.outputURL = temporary
    session.outputFileType = type
    session.shouldOptimizeForNetworkUse = true
    val completed = CompletableDeferred<Unit>()
    val center = NSNotificationCenter.defaultCenter
    val background = center.addObserverForName(UIApplicationDidEnterBackgroundNotification, null, null) {
        session.cancelExport()
    }
    var started = false
    try {
        if (UIApplication.sharedApplication.applicationState != UIApplicationState.UIApplicationStateActive) {
            throw CameraCaptureException("Return PlayerID to the foreground, then retry $operation.")
        }
        session.exportAsynchronouslyWithCompletionHandler { completed.complete(Unit) }
        started = true
        completed.await()
        when (session.status) {
            AVAssetExportSessionStatusCompleted -> Unit
            AVAssetExportSessionStatusCancelled ->
                throw CameraCaptureException("$operation export canceled. No new copy was saved; your source memories are unchanged.")
            else -> throw CameraCaptureException("$operation export failed: ${session.error?.localizedDescription ?: "the video could not be encoded"}. Your source memories are unchanged.")
        }
        return withContext(Dispatchers.Default) {
            val output = AVURLAsset(uRL = temporary, options = null)
            val seconds = CMTimeGetSeconds(output.duration)
            if (output.tracksWithMediaType(AVMediaTypeVideo).isEmpty() || !seconds.isFinite() || seconds <= 0) {
                throw CameraCaptureException("The exported clip has no playable video. Your source memories are unchanged.")
            }
            service.importPhotoLibraryFile(temporary, video = true)
        }
    } finally {
        if (started && !completed.isCompleted) {
            session.cancelExport()
            withContext(NonCancellable) { completed.await() }
        }
        center.removeObserver(background)
        if (NSFileManager.defaultManager.fileExistsAtPath(temporary.path!!) &&
            !NSFileManager.defaultManager.removeItemAtURL(temporary, error = null)) {
            NSLog("PlayerID couldn't remove a video-export temporary file.")
        }
    }
}
