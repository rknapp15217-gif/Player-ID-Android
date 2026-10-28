@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.playerid.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.playerid.app.roster.RecognizedTextBox
import com.playerid.app.roster.ROSTER_TABLES_SCRIPT
import com.playerid.app.roster.RosterOcrResult
import com.playerid.app.roster.RosterTable
import com.playerid.app.roster.distinctRosterCandidates
import com.playerid.app.roster.parseRosterTablesJson
import com.playerid.app.roster.rosterTextFromBoxes
import com.playerid.app.ui.team.RosterImportAdapter
import com.playerid.app.ui.team.RosterWebBrowser
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGImageRef
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject

class RosterImportException(message: String) : Exception(message)

/** iPhone roster import: Photos picker + Vision text recognition, and a WKWebView for team websites. */
class IosRosterImportAdapter : RosterImportAdapter {
    private var pickerDelegate: PHPickerViewControllerDelegateProtocol? = null

    override suspend fun recognizeRosterImages(multiple: Boolean, onPicked: () -> Unit): RosterOcrResult? {
        if (pickerDelegate != null) return null
        var presenter = UIApplication.sharedApplication.keyWindow?.rootViewController
            ?: throw RosterImportException("Cannot open Photos from the app window. Try again.")
        while (true) presenter = presenter.presentedViewController ?: break
        val picked = CompletableDeferred<List<PHPickerResult>>()
        val configuration = PHPickerConfiguration().apply {
            selectionLimit = if (multiple) 0 else 1
            filter = PHPickerFilter.imagesFilter
        }
        val picker = PHPickerViewController(configuration)
        pickerDelegate = object : NSObject(), PHPickerViewControllerDelegateProtocol {
            override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
                picker.dismissViewControllerAnimated(true) {
                    picked.complete(didFinishPicking.filterIsInstance<PHPickerResult>())
                }
            }
        }
        picker.delegate = pickerDelegate
        val selections = try {
            presenter.presentViewController(picker, animated = true, completion = null)
            picked.await()
        } finally {
            pickerDelegate = null
        }
        if (selections.isEmpty()) return null
        onPicked()
        val results = selections.map { selection -> recognizeImage(loadImage(selection)) }
        return RosterOcrResult(
            candidates = distinctRosterCandidates(results.flatMap { it.candidates }),
            rawLines = results.flatMap { it.rawLines }
        )
    }

    @Composable
    override fun RosterWebView(
        initialUrl: String,
        onBrowserReady: (RosterWebBrowser) -> Unit,
        onUrlChanged: (String) -> Unit,
        modifier: Modifier
    ) {
        val urlChanged by rememberUpdatedState(onUrlChanged)
        val browser = remember { IosRosterWebBrowser { urlChanged(it) } }
        DisposableEffect(browser) {
            onBrowserReady(browser)
            browser.load(initialUrl)
            onDispose { browser.webView.stopLoading() }
        }
        UIKitView(factory = { browser.webView }, modifier = modifier)
    }

    private suspend fun loadImage(selection: PHPickerResult): CGImageRef {
        val provider = selection.itemProvider
        if (!provider.hasItemConformingToTypeIdentifier("public.image")) {
            throw RosterImportException("This Photos item is not an image.")
        }
        val data = CompletableDeferred<NSData>()
        provider.loadDataRepresentationForTypeIdentifier("public.image") { loaded, error ->
            if (loaded == null || error != null) {
                data.completeExceptionally(RosterImportException(
                    "Photos couldn't load this screenshot. ${error?.localizedDescription.orEmpty()}".trim()
                ))
            } else {
                data.complete(loaded)
            }
        }
        return UIImage(data = data.await()).CGImage
            ?: throw RosterImportException("This screenshot couldn't be read.")
    }
}

internal class IosRosterWebBrowser(private val onUrlChanged: (String) -> Unit) : RosterWebBrowser {
    private val navigationDelegate = object : NSObject(), WKNavigationDelegateProtocol {
        override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
            webView.URL?.absoluteString?.let(onUrlChanged)
        }
    }
    val webView = WKWebView(frame = CGRectZero.readValue(), configuration = WKWebViewConfiguration()).apply {
        navigationDelegate = this@IosRosterWebBrowser.navigationDelegate
        allowsBackForwardNavigationGestures = true
    }

    override fun load(url: String) {
        val target = NSURL.URLWithString(url) ?: return
        webView.loadRequest(NSURLRequest.requestWithURL(target))
    }

    override fun goBack() {
        if (webView.canGoBack) webView.goBack()
    }

    override fun goForward() {
        if (webView.canGoForward) webView.goForward()
    }

    override fun reload() {
        webView.reload()
    }

    override suspend fun rosterTables(): List<RosterTable> = parseRosterTablesJson(evaluateText(ROSTER_TABLES_SCRIPT))

    override suspend fun evaluateText(script: String): String {
        val result = CompletableDeferred<String>()
        webView.evaluateJavaScript(script) { value, _ -> result.complete(value as? String ?: "") }
        return result.await()
    }

    override suspend fun recognizePageText(): RosterOcrResult {
        val scrollView = webView.scrollView
        val originalOffset = scrollView.contentOffset.useContents { CGPointMake(x, y) }
        val viewportHeight = scrollView.bounds.useContents { size.height }
        val contentHeight = scrollView.contentSize.useContents { height }
        if (viewportHeight <= 0.0) throw RosterImportException("Web view not ready")
        val results = mutableListOf<RosterOcrResult>()
        var offset = 0.0
        var pages = 0
        try {
            do {
                scrollView.setContentOffset(CGPointMake(0.0, offset), animated = false)
                delay(350)
                snapshot()?.let { results += recognizeImage(it) }
                offset += viewportHeight * 0.9
                pages += 1
            } while (offset < contentHeight && pages < MAX_CAPTURE_PAGES)
        } finally {
            scrollView.setContentOffset(originalOffset, animated = false)
        }
        return RosterOcrResult(
            candidates = distinctRosterCandidates(results.flatMap { it.candidates }),
            rawLines = results.flatMap { it.rawLines }
        )
    }

    private suspend fun snapshot(): CGImageRef? {
        val image = CompletableDeferred<UIImage?>()
        webView.takeSnapshotWithConfiguration(null) { captured, _ -> image.complete(captured) }
        return image.await()?.CGImage
    }

    private companion object {
        const val MAX_CAPTURE_PAGES = 12
    }
}

internal suspend fun recognizeImage(image: CGImageRef): RosterOcrResult = withContext(Dispatchers.Default) {
    val request = VNRecognizeTextRequest().apply {
        recognitionLevel = VNRequestTextRecognitionLevelAccurate
        usesLanguageCorrection = false
    }
    val handler = VNImageRequestHandler(cGImage = image, options = emptyMap<Any?, Any?>())
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        if (!handler.performRequests(listOf(request), error.ptr)) {
            throw RosterImportException(error.value?.localizedDescription ?: "Text recognition failed.")
        }
    }
    val boxes = request.results.orEmpty().filterIsInstance<VNRecognizedTextObservation>().mapNotNull { observation ->
        val text = (observation.topCandidates(1u).firstOrNull() as? VNRecognizedText)?.string ?: return@mapNotNull null
        observation.boundingBox.useContents {
            // Vision uses a bottom-left origin; rows are grouped top-down.
            RecognizedTextBox(text, origin.x, 1.0 - origin.y - size.height, size.height)
        }
    }
    rosterTextFromBoxes(boxes)
}
