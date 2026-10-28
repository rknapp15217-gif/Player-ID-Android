@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.playerid.app.platform

import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.memory.MemoryImportProgress
import com.playerid.app.domain.memory.MemoryImportResult
import com.playerid.app.domain.memory.importMemoriesSequentially
import kotlinx.coroutines.CompletableDeferred
import kotlinx.cinterop.autoreleasepool
import platform.darwin.NSObject
import platform.PhotosUI.*
import platform.UIKit.UIApplication

class IosPhotoLibraryImporter(private val camera: IosCameraService) {
    private var delegate: PHPickerViewControllerDelegateProtocol? = null

    suspend fun importMedia(onProgress: (MemoryImportProgress) -> Unit): MemoryImportResult {
        if (delegate != null) throw CameraCaptureException("A Photos import is already open.")
        var presenter = UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: throw CameraCaptureException("Cannot open Photos from the app window. Return to Memories and try again.")
        while (true) {
            val presented = presenter.presentedViewController ?: break
            presenter = presented
        }
        val result = CompletableDeferred<List<PHPickerResult>>()
        val configuration = PHPickerConfiguration().apply {
            selectionLimit = 0
            filter = PHPickerFilter.anyFilterMatchingSubfilters(
                listOf(PHPickerFilter.imagesFilter, PHPickerFilter.videosFilter)
            )
        }
        val picker = PHPickerViewController(configuration)
        delegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                picker.dismissViewControllerAnimated(true) {
                    if (didFinishPicking.any { it !is PHPickerResult }) {
                        result.completeExceptionally(CameraCaptureException("Photos returned an invalid selection. Nothing was imported."))
                    } else {
                        result.complete(didFinishPicking.filterIsInstance<PHPickerResult>())
                    }
                }
            }
        }
        picker.delegate = delegate
        try {
            presenter.presentViewController(picker, animated = true, completion = null)
            return importMemoriesSequentially(result.await(), onProgress, ::importItem)
        } finally {
            delegate = null
        }
    }

    private suspend fun importItem(selection: PHPickerResult): MediaReference {
        val provider = selection.itemProvider
        val video = provider.hasItemConformingToTypeIdentifier("public.movie")
        val type = if (video) "public.movie" else "public.image"
        if (!provider.hasItemConformingToTypeIdentifier(type)) {
            throw CameraCaptureException("This Photos item is not a supported photo or video.")
        }
        val result = CompletableDeferred<MediaReference>()
        provider.loadFileRepresentationForTypeIdentifier(type) { url, error ->
            if (url == null || error != null) {
                result.completeExceptionally(CameraCaptureException(
                    "Photos couldn't load this item. Check your connection for iCloud media and retry. ${error?.localizedDescription.orEmpty()}"
                ))
            } else {
                // Copy before this callback returns: Photos owns the temporary URL.
                autoreleasepool {
                    try {
                        result.complete(camera.importPhotoLibraryFile(url, video))
                    } catch (failure: CameraCaptureException) {
                        result.completeExceptionally(failure)
                    }
                }
            }
        }
        return result.await()
    }
}
