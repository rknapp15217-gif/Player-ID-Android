@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.playerid.app.platform

import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.memory.MemoryShareResult
import kotlinx.coroutines.CompletableDeferred
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.UIKit.*
import kotlinx.cinterop.useContents

internal fun validatedMemoryShareUrls(memories: List<MediaReference>): List<NSURL> {
    if (memories.isEmpty()) throw CameraCaptureException("Select at least one memory to share.")
    return memories.distinctBy { it.identifier }.map { media ->
        val url = NSURL.URLWithString(media.identifier)
            ?: throw CameraCaptureException("A selected memory has an invalid file address.")
        val path = url.path
        if (!url.fileURL || path == null || !NSFileManager.defaultManager.isReadableFileAtPath(path)) {
            throw CameraCaptureException("A selected memory cannot be read. Nothing was shared. Return to Memories and retry.")
        }
        url
    }
}

internal fun memorySharePresenter(): UIViewController {
    if (UIApplication.sharedApplication.applicationState != UIApplicationState.UIApplicationStateActive) {
        throw CameraCaptureException("Return PlayerID to the foreground before opening sharing.")
    }
    var presenter = UIApplication.sharedApplication.keyWindow?.rootViewController
        ?: throw CameraCaptureException("Cannot open sharing from the app window. Return to Memories and retry.")
    while (true) {
        val presented = presenter.presentedViewController ?: break
        if (presented.isBeingDismissed()) throw CameraCaptureException("Wait for the current sheet to close, then retry.")
        presenter = presented
    }
    if (presenter.view.window == null) throw CameraCaptureException("The sharing screen is not attached. Return and retry.")
    return presenter
}

class IosMemorySharer {
    private var sharing = false

    suspend fun share(memories: List<MediaReference>): MemoryShareResult {
        if (sharing) throw CameraCaptureException("A share sheet is already open.")
        val urls = validatedMemoryShareUrls(memories)
        val presenter = memorySharePresenter()
        val result = CompletableDeferred<MemoryShareResult>()
        val controller = UIActivityViewController(activityItems = urls, applicationActivities = null)
        controller.completionWithItemsHandler = { _, completed, _, error ->
            if (error != null) {
                result.completeExceptionally(CameraCaptureException("Sharing failed: ${error.localizedDescription}"))
            } else {
                result.complete(if (completed) MemoryShareResult.COMPLETED else MemoryShareResult.CANCELLED)
            }
        }
        controller.popoverPresentationController?.let { popover ->
            popover.sourceView = presenter.view
            popover.sourceRect = presenter.view.bounds.useContents {
                CGRectMake(size.width / 2.0, size.height / 2.0, 1.0, 1.0)
            }
            popover.permittedArrowDirections = UIPopoverArrowDirectionUnknown
        }
        sharing = true
        try {
            presenter.presentViewController(controller, animated = true, completion = null)
            return result.await()
        } finally {
            sharing = false
        }
    }
}
