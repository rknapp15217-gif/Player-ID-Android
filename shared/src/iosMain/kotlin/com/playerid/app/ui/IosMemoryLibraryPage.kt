@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class,
    kotlin.experimental.ExperimentalNativeApi::class)

package com.playerid.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.team.LocalTeamRepository
import com.playerid.app.domain.team.captureFileKey
import com.playerid.app.domain.team.captureOpponentName
import com.playerid.app.ui.components.MemoryMetadataTicket
import com.playerid.app.domain.memory.memoryViewerZoomAfterGesture
import com.playerid.app.domain.memory.MemoryPlayback
import com.playerid.app.domain.memory.MemoryPlaybackControls
import com.playerid.app.domain.memory.VideoTrimRange
import com.playerid.app.platform.IosVideoTrimmer
import com.playerid.app.platform.IosMemoryReelExporter
import com.playerid.app.platform.IosCameraService
import com.playerid.app.platform.IosPhotoLibraryImporter
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import com.playerid.app.ui.screens.CameraCaptureLibraryPage
import com.playerid.app.ui.screens.VideoTrimPage
import com.playerid.app.ui.screens.MemoryReelBuilderPage
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.skia.Image
import platform.AVFoundation.*
import platform.AVKit.AVPlayerViewController
import platform.CoreGraphics.*
import platform.CoreGraphics.CGImageRelease
import platform.CoreMedia.CMTimeMake
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreImage.*
import platform.Foundation.*
import platform.QuartzCore.CACurrentMediaTime
import platform.UIKit.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.native.Platform

@Composable
fun IosMemoryLibraryPage(
    service: IosCameraService,
    repository: LocalTeamRepository,
    onNavigate: (com.playerid.app.domain.team.LibraryNavigationRequest) -> Unit,
    onOpenMore: () -> Unit,
    goMode: Boolean = false,
    goOpenGeneration: Int = 0
) {
    val importer = remember(service) { IosPhotoLibraryImporter(service) }
    val share = rememberGuardianMemorySharing(repository)
    val trimmer = remember(service) { IosVideoTrimmer(service) }
    val jerseyAnalyzer = remember { com.playerid.app.platform.IosJerseyAnalyzer() }
    val reelExporter = remember(service) { IosMemoryReelExporter(service) }
    val voiceSearch = remember { com.playerid.app.platform.IosVoiceSearch() }
    val snapshot by repository.snapshot.collectAsState()
    val goRecentRequests = androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(emptyList<String>())
    }
    androidx.compose.runtime.key(if (goMode) goOpenGeneration else 0) {
    CameraCaptureLibraryPage(
        onNavigate = onNavigate,
        onOpenMore = onOpenMore,
        goMode = goMode,
        goRecentRequests = goRecentRequests,
        playlistPlayback = { media, modifier, onEnded ->
            CaptureVideo(media, modifier, null, onEnded = onEnded)
        },
        onFullScreenViewerChange = com.playerid.app.platform.IosOrientationLock::setViewerOpen,
        repository = repository,
        loadCaptures = service::listSavedCaptures,
        importMedia = importer::importMedia,
        shareMedia = { share(it, null) },
        shareMediaWithRecipient = share,
        removeMedia = { media ->
            service.deleteSavedCapture(media)
            repository.forgetCapture(captureFileKey(media))
        },
        voiceSearch = voiceSearch,
        onLongPressHaptic = {
            UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy).apply { prepare(); impactOccurred() }
        },
        loadDuration = trimmer::duration,
        reelBuilder = { draft ->
            MemoryReelBuilderPage(
                initialSelection = draft.selection,
                initialTitle = draft.title,
                repository = repository,
                build = reelExporter::build,
                cancelBuild = reelExporter::cancel,
                thumbnail = { media, modifier -> CaptureImage(media, modifier, thumbnail = true) },
                preview = { media, modifier -> CaptureVideo(media, modifier, null) },
                onAddMemories = draft.onAddMemories,
                onBack = draft.onBack,
                onSaved = draft.onSaved,
                loadDuration = trimmer::duration
            )
        },
        videoEditor = { media, onBack, onSaved ->
            key(media.identifier) {
                VideoTrimPage(
                    media = media,
                    repository = repository,
                    loadDuration = trimmer::duration,
                    export = trimmer::export,
                    cancelExport = trimmer::cancel,
                    preview = { source, range, modifier -> CaptureVideo(source, modifier, null, range) },
                    loadFilmstrip = { source, count -> withContext(Dispatchers.Default) { loadFilmstripFrames(source, count) } },
                    readJerseys = jerseyAnalyzer::analyze,
                    onBack = onBack,
                    onSaved = onSaved
                )
            }
        },
        thumbnail = { media, modifier -> CaptureImage(media, modifier, thumbnail = true) },
        playback = { media, modifier ->
            key(media.identifier) {
                val opponent = snapshot.captureOrganizations[captureFileKey(media)]?.let(snapshot::captureOpponentName)
                val tag = snapshot.captureReels[captureFileKey(media)]?.name ?: snapshot.captureMomentTags[captureFileKey(media)]
                if (media.kind == MediaKind.VIDEO) {
                    CaptureVideo(media, modifier, listOfNotNull(tag, opponent?.let { "vs $it" })
                        .joinToString(" | ").takeIf { it.isNotEmpty() })
                } else {
                    Box(modifier) {
                        CaptureImage(media, Modifier.fillMaxSize(), thumbnail = false)
                        MemoryMetadataTicket(tag, opponent, null, Modifier.align(Alignment.TopStart).padding(14.dp))
                    }

                }
            }
        }
    )
    }
}

/** Evenly spaced small frames for the trim filmstrip; frames that can't be decoded are skipped. */
private fun loadFilmstripFrames(media: MediaReference, count: Int): List<ImageBitmap> {
    val url = NSURL.URLWithString(media.identifier)
        ?: throw CameraCaptureException("The saved media address is invalid.")
    val asset = AVURLAsset(uRL = url, options = null)
    val seconds = CMTimeGetSeconds(asset.duration)
    if (!seconds.isFinite() || seconds <= 0.0 || count <= 0) return emptyList()
    val generator = AVAssetImageGenerator(asset = asset).apply {
        appliesPreferredTrackTransform = true
        maximumSize = CGSizeMake(160.0, 160.0)
    }
    return (0 until count).mapNotNull { index ->
        val millis = ((index + 0.5) / count * seconds * 1000.0).toLong()
        memScoped {
            val frame = generator.copyCGImageAtTime(CMTimeMake(millis, 1000), actualTime = null, error = null)
                ?: return@mapNotNull null
            try {
                val data = UIImageJPEGRepresentation(UIImage.imageWithCGImage(frame), 0.7) ?: return@mapNotNull null
                val bytes = data.bytes?.readBytes(data.length.toInt()) ?: return@mapNotNull null
                Image.makeFromEncoded(bytes).toComposeImageBitmap()
            } finally {
                CGImageRelease(frame)
            }
        }
    }
}

@Composable
internal fun CaptureImage(media: MediaReference, modifier: Modifier, thumbnail: Boolean) {
    var zoom by remember(media) { mutableFloatStateOf(1f) }
    var bitmap by remember(media) { mutableStateOf<ImageBitmap?>(null) }
    var error by remember(media) { mutableStateOf<String?>(null) }
    LaunchedEffect(media, thumbnail) {
        try {
            bitmap = withContext(Dispatchers.Default) { loadCaptureImage(media, thumbnail) }
        } catch (failure: CameraCaptureException) {
            error = failure.message
        }
    }
    Box(modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val image = bitmap
        when {
            image != null -> Image(
                bitmap = image,
                contentDescription = if (media.kind == MediaKind.VIDEO) "Video thumbnail" else "Captured photo",
                modifier = Modifier.fillMaxSize().then(
                    if (thumbnail) Modifier else Modifier.pointerInput(media) {
                        detectTransformGestures { _, _, change, _ ->
                            zoom = memoryViewerZoomAfterGesture(zoom, change)
                        }
                    }.graphicsLayer { scaleX = zoom; scaleY = zoom }
                ),
                contentScale = if (thumbnail) ContentScale.Crop else ContentScale.Fit
            )
            error != null -> Text(
                error ?: "Image unavailable",
                Modifier.padding(8.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            else -> CircularProgressIndicator()
        }
    }
}

private fun loadCaptureImage(media: MediaReference, thumbnail: Boolean): ImageBitmap {
    val url = NSURL.URLWithString(media.identifier)
        ?: throw CameraCaptureException("The saved media address is invalid.")
    val image = if (media.kind == MediaKind.VIDEO) {
        val generator = AVAssetImageGenerator(asset = AVURLAsset(uRL = url, options = null)).apply {
            appliesPreferredTrackTransform = true
            maximumSize = CGSizeMake(480.0, 480.0)
        }
        memScoped {
            val failure = alloc<ObjCObjectVar<NSError?>>()
            failure.value = null
            val frame = generator.copyCGImageAtTime(CMTimeMake(0, 1), actualTime = null, error = failure.ptr)
                ?: throw CameraCaptureException("Video thumbnail unavailable: ${failure.value?.localizedDescription ?: "no decodable video frame"}")
            try {
                UIImage.imageWithCGImage(frame)
            } finally {
                CGImageRelease(frame)
            }
        }
    } else {
        val path = url.path ?: throw CameraCaptureException("The photo address has no file path.")
        UIImage.imageWithContentsOfFile(path)
            ?: throw CameraCaptureException("Cannot read this photo. Its saved file may be missing or damaged.")
    }
    val size = image.size.useContents { width to height }
    if (size.first <= 0.0 || size.second <= 0.0) throw CameraCaptureException("The image has invalid dimensions.")
    val scale = if (thumbnail) maxOf(360.0 / size.first, 360.0 / size.second)
        else minOf(1.0, 2048.0 / maxOf(size.first, size.second))
    val width = if (thumbnail) 360.0 else size.first * scale
    val height = if (thumbnail) 360.0 else size.second * scale
    // Drawing normalizes JPEG orientation and bounds decoded textures before Compose displays them.
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(width, height), true, 1.0)
    val rendered = try {
        image.drawInRect(CGRectMake(
            (width - size.first * scale) / 2.0, (height - size.second * scale) / 2.0,
            size.first * scale, size.second * scale
        ))
        UIGraphicsGetImageFromCurrentImageContext()
            ?: throw CameraCaptureException("Cannot render this image.")
    } finally {
        UIGraphicsEndImageContext()
    }
    val data = UIImagePNGRepresentation(rendered)
        ?: throw CameraCaptureException("Cannot decode this image.")
    val bytes = data.bytes?.readBytes(data.length.toInt())
        ?: throw CameraCaptureException("This image contains no data.")
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}

@Composable
private fun CaptureVideo(
    media: MediaReference,
    modifier: Modifier,
    metadataText: String?,
    trimRange: VideoTrimRange? = null,
    onEnded: (() -> Unit)? = null
) {
    val currentOnEnded by rememberUpdatedState(onEnded)
    val view = remember(media) { CapturePlayerView(media).apply { updateTrimRange(trimRange) } }
    var error by remember(media) { mutableStateOf<String?>(null) }
    DisposableEffect(view) {
        val center = NSNotificationCenter.defaultCenter
        val ended = center.addObserverForName(AVPlayerItemDidPlayToEndTimeNotification, view.item, null) {
            currentOnEnded?.invoke()
        }
        val background = center.addObserverForName(UIApplicationWillResignActiveNotification, null, null) {
            view.pause()
        }
        val failed = center.addObserverForName(AVPlayerItemFailedToPlayToEndTimeNotification, view.item, null) { notification ->
            val failure = notification?.userInfo?.get(AVPlayerItemFailedToPlayToEndTimeErrorKey) as? NSError
            error = "Video playback stopped: ${failure?.localizedDescription ?: "the saved file could not be played to the end"}"
            view.pause()
        }
        onDispose {
            center.removeObserver(background)
            center.removeObserver(ended)
            center.removeObserver(failed)
            view.release()
        }
    }
    LaunchedEffect(view) {
        while (true) {
            if (view.attachmentError != null || view.item.status == AVPlayerItemStatusFailed) {
                error = view.attachmentError ?: "Cannot play this video: ${view.item.error?.localizedDescription ?: "the saved file could not be decoded"}"
                view.pause()
                break
            }
            view.refreshControls()
            delay(250)
        }
    }
    Box(modifier) {
        if (error == null) {
            UIKitView(
                factory = { view },
                update = {
                    it.metadataText = metadataText
                    it.updateTrimRange(trimRange)
                },
                modifier = Modifier.fillMaxSize(),
                onResize = { nativeView, rect -> nativeView.setFrame(rect); nativeView.layoutPlayer() }
            )
        }
        error?.let {
            Text(it, Modifier.align(Alignment.Center).padding(24.dp), color = MaterialTheme.colorScheme.error)
        }
    }
}

private class CapturePlayerView(media: MediaReference) : UIView(frame = CGRectZero.readValue()),
    UIGestureRecognizerDelegateProtocol {
    val item = AVPlayerItem(uRL = NSURL.URLWithString(media.identifier)
        ?: throw CameraCaptureException("The saved video address is invalid."))
    private val player = AVPlayer(playerItem = item)
    private val controller = AVPlayerViewController().apply {
        player = this@CapturePlayerView.player
        showsPlaybackControls = false
    }
    private var attached = false
    private var trimRange: VideoTrimRange? = null
    private var scrubTiming: ScrubTiming? = null
    private val controls = MemoryPlaybackControls(object : MemoryPlayback {
        override val durationMillis: Long get() = trimRange?.durationMillis ?: milliseconds(item.duration)
        override val positionMillis: Long get() =
            (milliseconds(item.currentTime()) - (trimRange?.startMillis ?: 0)).coerceAtLeast(0)
        override val isPlaying: Boolean get() = player.rate != 0f
        override fun play() { player.play() }
        override fun pause() { player.pause() }
        override fun seek(positionMillis: Long, toleranceMillis: Long, completion: (Boolean) -> Unit) {
            val tolerance = CMTimeMake(toleranceMillis, 1000)
            val timing = scrubTiming
            val started = CACurrentMediaTime()
            player.seekToTime(CMTimeMake(positionMillis + (trimRange?.startMillis ?: 0), 1000), tolerance, tolerance) { finished ->
                val decoded = CACurrentMediaTime()
                dispatch_async(dispatch_get_main_queue()) {
                    timing?.recordSeek(decoded - started, CACurrentMediaTime() - decoded)
                    completion(finished)
                    reportCompletedScrub(timing)
                }
            }
        }
    }, onError = {
        seekErrorLabel.text = it
        seekErrorLabel.hidden = false
    })
    private val seekErrorLabel = UILabel().apply {
        textColor = UIColor.whiteColor
        backgroundColor = UIColor.blackColor.colorWithAlphaComponent(0.7)
        font = UIFont.systemFontOfSize(13.0)
        numberOfLines = 2
        textAlignment = NSTextAlignmentCenter
        userInteractionEnabled = false
        hidden = true
        accessibilityTraits = UIAccessibilityTraitStaticText
    }
    private val playImage = controlImage(play = true)
    private val pauseImage = controlImage(play = false)
    private val seekBar = MemorySeekSlider().apply {
        minimumValue = 0f
        maximumValue = 1f
        minimumTrackTintColor = UIColor.whiteColor
        maximumTrackTintColor = UIColor.whiteColor.colorWithAlphaComponent(0.35)
        setThumbImage(controlImage(play = null), forState = UIControlStateNormal)
        addTarget(this@CapturePlayerView, NSSelectorFromString("scrub:"), UIControlEventValueChanged)
        addTarget(this@CapturePlayerView, NSSelectorFromString("finishScrub:"), UIControlEventTouchUpInside)
        addTarget(this@CapturePlayerView, NSSelectorFromString("finishScrub:"), UIControlEventTouchUpOutside)
        addTarget(this@CapturePlayerView, NSSelectorFromString("cancelScrub:"), UIControlEventTouchCancel)
    }
    private val playButton = UIButton.buttonWithType(UIButtonTypeCustom).apply {
        accessibilityLabel = "Pause"
        addTarget(this@CapturePlayerView, NSSelectorFromString("togglePlayback:"), UIControlEventTouchUpInside)
    }
    var metadataText: String? = null
        set(value) {
            field = value
            ticketLabel.text = value
            ticket.hidden = value == null
            setNeedsLayout()
        }
    private val ticketLabel = UILabel().apply {
        textColor = UIColor.whiteColor
        font = UIFont.systemFontOfSize(14.0, weight = UIFontWeightSemibold)
        numberOfLines = 1
        lineBreakMode = NSLineBreakByTruncatingTail
    }
    private val ticket = UIView(frame = CGRectZero.readValue()).apply {
        backgroundColor = UIColor.blackColor.colorWithAlphaComponent(0.52)
        layer.cornerRadius = 8.0
        userInteractionEnabled = false
        hidden = true
        addSubview(ticketLabel)
    }
    private val zoom = MutableStateFlow(1f)
    private val pinch = UIPinchGestureRecognizer(target = this, action = NSSelectorFromString("handlePinch:"))
    var attachmentError: String? = null
        private set

    init {
        backgroundColor = UIColor.blackColor
        clipsToBounds = true
        multipleTouchEnabled = true
        pinch.cancelsTouchesInView = false
        pinch.delegate = this
        addGestureRecognizer(pinch)
    }

    fun updateTrimRange(range: VideoTrimRange?) {
        if (range == trimRange) return
        controls.interrupt()
        trimRange = range
        item.forwardPlaybackEndTime = CMTimeMake(range?.endMillis ?: 0, 1000)
        if (range != null) {
            controls.scrub(0f)
            controls.finishScrub()
        }
    }

    fun refreshControls() {
        controls.refresh()
        seekBar.hidden = trimRange != null || milliseconds(item.duration) <= 0
        seekBar.enabled = controls.isScrubbing || !controls.isSeeking
        if (!seekBar.tracking) seekBar.value = controls.fraction
        playButton.enabled = item.status == AVPlayerItemStatusReadyToPlay && !controls.isSeeking && !controls.isScrubbing
        playButton.accessibilityLabel = if (player.rate != 0f) "Pause" else "Play"
        playButton.setImage(if (player.rate == 0f) playImage else pauseImage, forState = UIControlStateNormal)
    }

    @ObjCAction
    fun togglePlayback(sender: UIButton) {
        seekErrorLabel.hidden = true
        controls.togglePlayback()
        refreshControls()
    }

    @ObjCAction
    fun scrub(sender: UISlider) {
        seekErrorLabel.hidden = true
        if (Platform.isDebugBinary && !controls.isScrubbing && !controls.isSeeking) {
            scrubTiming = ScrubTiming()
        }
        scrubTiming?.events = (scrubTiming?.events ?: 0) + 1
        controls.scrub(sender.value)
    }

    @ObjCAction
    fun finishScrub(sender: UISlider) {
        controls.finishScrub()
        refreshControls()
    }

    @ObjCAction
    fun cancelScrub(sender: UISlider) {
        scrubTiming?.let { finishScrubTiming(it, interrupted = true) }
        controls.interrupt()
        refreshControls()
    }

    private fun zoomComposition(): AVVideoComposition {
        val zoomState = zoom
        return AVVideoComposition.videoCompositionWithAsset(item.asset) { request ->
            val frame = request ?: return@videoCompositionWithAsset
            // AVFoundation exposes Core Image types as forward declarations in Kotlin 1.9.
            val source = interpretObjCPointer<CIImage>(frame.sourceImage.objcPtr())
            val scale = zoomState.value.toDouble()
            val center = source.extent.useContents {
                (origin.x + size.width / 2.0) to (origin.y + size.height / 2.0)
            }
            val transform = CGAffineTransformMake(
                scale, 0.0, 0.0, scale, center.first * (1.0 - scale), center.second * (1.0 - scale)
            )
            val rendered = source.imageByApplyingTransform(transform).imageByCroppingToRect(source.extent)
            frame.finishWithImage(interpretObjCPointer<objcnames.classes.CIImage>(rendered.objcPtr()), context = null)
        }
    }

    override fun gestureRecognizer(
        gestureRecognizer: UIGestureRecognizer,
        shouldRecognizeSimultaneouslyWithGestureRecognizer: UIGestureRecognizer
    ): Boolean = true

    override fun gestureRecognizerShouldBegin(gestureRecognizer: UIGestureRecognizer): Boolean =
        super<UIView>.gestureRecognizerShouldBegin(gestureRecognizer)

    override fun gestureRecognizer(gestureRecognizer: UIGestureRecognizer, shouldReceiveTouch: UITouch): Boolean {
        var touched = shouldReceiveTouch.view
        while (touched != null && touched != this) {
            if (touched is UIControl) return false
            touched = touched.superview
        }
        return true
    }

    @ObjCAction
    fun handlePinch(recognizer: UIPinchGestureRecognizer) {
        if (recognizer.state == UIGestureRecognizerStateBegan || recognizer.state == UIGestureRecognizerStateChanged) {
            zoom.value = memoryViewerZoomAfterGesture(zoom.value, recognizer.scale.toFloat())
            controller.videoGravity = AVLayerVideoGravityResizeAspect
            // Replacing the composition makes AVPlayer render a paused frame again.
            if (zoom.value == 1f) item.videoComposition = null
            else if (item.videoComposition == null || player.rate == 0f) item.videoComposition = zoomComposition()
        }
        recognizer.scale = 1.0
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()
        if (window != null && !attached) {
            var responder: UIResponder? = nextResponder
            while (responder != null && responder !is UIViewController) responder = responder.nextResponder
            val parent = responder as? UIViewController ?: window?.rootViewController
            if (parent != null) {
                parent.addChildViewController(controller)
                addSubview(controller.view)
                controller.didMoveToParentViewController(parent)
                addSubview(ticket)
                addSubview(seekBar)
                addSubview(playButton)
                addSubview(seekErrorLabel)
                attached = true
                layoutPlayer()
                if (trimRange == null) player.play()
                refreshControls()
            } else {
                attachmentError = "Cannot attach the video player to the app window. Return to Memories and reopen the video."
            }
        } else if (window == null) {
            pause()
        }
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        layoutPlayer()
    }

    override fun safeAreaInsetsDidChange() {
        super.safeAreaInsetsDidChange()
        layoutPlayer()
    }

    fun layoutPlayer() {
        controller.view.setFrame(bounds)
        val availableWidth = bounds.useContents { (size.width - 28.0).coerceIn(0.0, 360.0) }
        val textWidth = (availableWidth - 24.0).coerceAtLeast(0.0)
        val textSize = ticketLabel.sizeThatFits(CGSizeMake(textWidth, 17.0))
        val width = textSize.useContents { (width + 24.0).coerceAtMost(availableWidth) }
        ticket.setFrame(CGRectMake(14.0, 14.0, width, 37.0))
        ticketLabel.setFrame(CGRectMake(12.0, 10.0, (width - 24.0).coerceAtLeast(0.0), 17.0))
        val (insetLeft, insetRight, insetBottom) = safeAreaInsets.useContents { Triple(left, right, bottom) }
        bounds.useContents {
            if (size.width > size.height) {
                // Landscape: play button and progress slider share one row along the bottom edge, clear of the notch.
                val rowY = (size.height - insetBottom.coerceAtLeast(8.0) - 52.0).coerceAtLeast(0.0)
                val left = insetLeft + 12.0
                val right = size.width - insetRight - 16.0
                playButton.setFrame(CGRectMake(left, rowY, 48.0, 48.0))
                val sliderX = left + 48.0 + 10.0
                seekBar.setFrame(CGRectMake(sliderX, rowY + 4.0, (right - sliderX).coerceAtLeast(0.0), 40.0))
                seekErrorLabel.setFrame(CGRectMake(sliderX, (rowY - 44.0).coerceAtLeast(0.0), (right - sliderX).coerceAtLeast(0.0), 40.0))
            } else {
                playButton.setFrame(CGRectMake((size.width - 48.0) / 2.0, (size.height - 68.0).coerceAtLeast(0.0), 48.0, 48.0))
                seekBar.setFrame(CGRectMake(16.0, (size.height - 120.0).coerceAtLeast(0.0), (size.width - 32.0).coerceAtLeast(0.0), 40.0))
                seekErrorLabel.setFrame(CGRectMake(16.0, (size.height - 164.0).coerceAtLeast(0.0), (size.width - 32.0).coerceAtLeast(0.0), 40.0))
            }
        }
    }

    fun pause() {
        scrubTiming?.let { finishScrubTiming(it, interrupted = true) }
        controls.interrupt()
    }

    private fun finishScrubTiming(timing: ScrubTiming, interrupted: Boolean) {
        if (scrubTiming !== timing) return
        scrubTiming = null
        NSLog("PlayerID scrub: events=${timing.events}, seeks=${timing.seeks}, " +
            "averageSeekMs=${timing.averageSeekMillis}, maxSeekMs=${timing.maxSeekMillis}, " +
            "maxCallbackDelayMs=${timing.maxCallbackDelayMillis}, interrupted=$interrupted")
    }

    private fun reportCompletedScrub(timing: ScrubTiming?) {
        if (timing != null && !controls.isScrubbing && !controls.isSeeking) {
            finishScrubTiming(timing, interrupted = false)
        }
    }

    fun release() {
        pause()
        controller.willMoveToParentViewController(null)
        controller.view.removeFromSuperview()
        controller.removeFromParentViewController()
        controller.player = null
        ticket.removeFromSuperview()
        seekBar.removeFromSuperview()
        playButton.removeFromSuperview()
        seekErrorLabel.removeFromSuperview()
        attached = false
    }

    private class ScrubTiming {
        var events = 0
        var seeks = 0
            private set
        private var totalSeekSeconds = 0.0
        private var maxSeekSeconds = 0.0
        private var maxCallbackDelaySeconds = 0.0
        val averageSeekMillis: Int get() = if (seeks == 0) 0 else (totalSeekSeconds * 1000 / seeks).toInt()
        val maxSeekMillis: Int get() = (maxSeekSeconds * 1000).toInt()
        val maxCallbackDelayMillis: Int get() = (maxCallbackDelaySeconds * 1000).toInt()

        fun recordSeek(decodeSeconds: Double, deliverySeconds: Double) {
            seeks++
            totalSeekSeconds += decodeSeconds
            maxSeekSeconds = maxOf(maxSeekSeconds, decodeSeconds)
            maxCallbackDelaySeconds = maxOf(maxCallbackDelaySeconds, deliverySeconds)
        }
    }

    private class MemorySeekSlider : UISlider(frame = CGRectZero.readValue()) {
        init {
            accessibilityLabel = "Video position"
        }

        override fun trackRectForBounds(bounds: CValue<CGRect>): CValue<CGRect> = bounds.useContents {
            CGRectMake(0.0, (size.height - 4.0) / 2.0, size.width, 4.0)
        }

        override fun beginTrackingWithTouch(touch: UITouch, withEvent: UIEvent?): Boolean {
            updatePosition(touch)
            return true
        }

        override fun continueTrackingWithTouch(touch: UITouch, withEvent: UIEvent?): Boolean {
            updatePosition(touch)
            return true
        }

        @ObjCAction
        fun accessibilityIncrement() {
            adjustAccessiblePosition(0.05f)
        }

        @ObjCAction
        fun accessibilityDecrement() {
            adjustAccessiblePosition(-0.05f)
        }

        private fun adjustAccessiblePosition(change: Float) {
            value = (value + change).coerceIn(0f, 1f)
            sendActionsForControlEvents(UIControlEventValueChanged)
            sendActionsForControlEvents(UIControlEventTouchUpInside)
        }

        private fun updatePosition(touch: UITouch) {
            val width = bounds.useContents { size.width.coerceAtLeast(1.0) }
            value = touch.locationInView(this).useContents { (x / width).toFloat().coerceIn(0f, 1f) }
            sendActionsForControlEvents(UIControlEventValueChanged)
        }
    }

    private fun milliseconds(time: CValue<platform.CoreMedia.CMTime>): Long {
        val seconds = CMTimeGetSeconds(time)
        return if (seconds.isFinite() && seconds > 0.0) (seconds * 1000.0).toLong() else 0L
    }

    private fun controlImage(play: Boolean?): UIImage {
        val size = if (play == null) 20.0 else 36.0
        UIGraphicsBeginImageContextWithOptions(CGSizeMake(size, size), false, 0.0)
        return try {
            UIColor.whiteColor.setFill()
            val path = if (play == null) UIBezierPath.bezierPathWithOvalInRect(CGRectMake(0.0, 0.0, size, size))
            else UIBezierPath().apply {
                if (play) {
                    moveToPoint(CGPointMake(12.0, 7.5))
                    addLineToPoint(CGPointMake(28.5, 18.0))
                    addLineToPoint(CGPointMake(12.0, 28.5))
                    closePath()
                } else {
                    appendPath(UIBezierPath.bezierPathWithRect(CGRectMake(9.0, 7.5, 6.0, 21.0)))
                    appendPath(UIBezierPath.bezierPathWithRect(CGRectMake(21.0, 7.5, 6.0, 21.0)))
                }
            }
            path.fill()
            UIGraphicsGetImageFromCurrentImageContext()
                ?: throw CameraCaptureException("Cannot render video playback controls.")
        } finally {
            UIGraphicsEndImageContext()
        }
    }
}
