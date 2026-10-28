@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.playerid.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import com.playerid.app.platform.memorySharePresenter
import kotlinx.cinterop.readValue
import kotlinx.coroutines.delay
import platform.AVFoundation.*
import platform.AVKit.AVPlayerViewController
import platform.CoreGraphics.CGRectZero
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.kCMTimeZero
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.QuartzCore.CATransaction
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UIKit.UIColor
import platform.UIKit.UIView

/** Share-screen preview: a still thumbnail that plays the video inline, like Android's Share preview. */
@Composable
internal fun ShareMemoryPreview(media: MediaReference, modifier: Modifier) {
    if (media.kind != MediaKind.VIDEO) {
        CaptureImage(media, modifier, thumbnail = true)
        return
    }
    var player by remember(media) { mutableStateOf<SharePreviewPlayer?>(null) }
    var playing by remember(media) { mutableStateOf(false) }
    var error by remember(media) { mutableStateOf<String?>(null) }
    val current = player
    DisposableEffect(current) {
        if (current == null) return@DisposableEffect onDispose { }
        val background = NSNotificationCenter.defaultCenter.addObserverForName(
            UIApplicationWillResignActiveNotification, null, null
        ) { current.pause() }
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(background)
            current.release()
        }
    }
    LaunchedEffect(current) {
        while (current != null) {
            if (current.item.status == AVPlayerItemStatusFailed) {
                error = "Cannot play this video: ${current.item.error?.localizedDescription ?: "the saved file could not be decoded"}"
                current.pause()
                break
            }
            // Messages, Mail and Contacts cover the Share screen; keep the clip from playing behind them.
            current.pauseIfCovered()
            playing = current.isPlaying
            delay(250)
        }
    }
    val start: () -> SharePreviewPlayer? = {
        player ?: try {
            SharePreviewPlayer(media).also { player = it }
        } catch (failure: CameraCaptureException) {
            error = failure.message
            null
        }
    }
    Box(modifier.background(Color.Black)) {
        if (current == null) {
            CaptureImage(media, Modifier.fillMaxSize(), thumbnail = true)
        } else {
            UIKitView(
                factory = { current.view },
                modifier = Modifier.fillMaxSize(),
                onResize = { view, rect -> view.setFrame(rect) }
            )
        }
        error?.let {
            Text(it, Modifier.align(Alignment.Center).padding(16.dp), color = MaterialTheme.colorScheme.error)
        }
        if (error == null) {
            IconButton(
                onClick = {
                    start()?.let { if (it.isPlaying) it.pause() else it.play() }
                    playing = player?.isPlaying == true
                },
                modifier = Modifier.align(Alignment.Center).size(52.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            ) {
                Icon(
                    if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (playing) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(
                onClick = {
                    try {
                        start()?.showFullScreen()
                    } catch (failure: CameraCaptureException) {
                        error = failure.message
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(36.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
            ) {
                Icon(Icons.Default.Fullscreen, contentDescription = "Full screen", tint = Color.White)
            }
        }
    }
}

private class SharePreviewPlayer(media: MediaReference) {
    val item = AVPlayerItem(uRL = NSURL.URLWithString(media.identifier)
        ?: throw CameraCaptureException("The saved video address is invalid."))
    private val player = AVPlayer(playerItem = item)
    private var fullScreen: AVPlayerViewController? = null
    val view = PlayerLayerView(player)

    val isPlaying: Boolean get() = player.rate != 0f

    fun play() {
        val duration = CMTimeGetSeconds(item.duration)
        if (duration > 0.0 && CMTimeGetSeconds(item.currentTime()) >= duration - 0.05) {
            player.seekToTime(kCMTimeZero.readValue())
        }
        player.play()
    }

    fun pause() = player.pause()

    fun showFullScreen() {
        val presenter = memorySharePresenter()
        val controller = AVPlayerViewController().apply {
            player = this@SharePreviewPlayer.player
            showsPlaybackControls = true
        }
        fullScreen = controller
        presenter.presentViewController(controller, animated = true) { play() }
    }

    fun pauseIfCovered() {
        val controller = fullScreen
        if (controller != null && controller.presentingViewController != null) return
        fullScreen = null
        if (isPlaying && view.window?.rootViewController?.presentedViewController != null) pause()
    }

    fun release() {
        player.pause()
        fullScreen?.dismissViewControllerAnimated(false, completion = null)
        fullScreen = null
        player.replaceCurrentItemWithPlayerItem(null)
    }
}

private class PlayerLayerView(player: AVPlayer) : UIView(frame = CGRectZero.readValue()) {
    private val playerLayer = AVPlayerLayer.playerLayerWithPlayer(player).apply {
        videoGravity = AVLayerVideoGravityResizeAspect
    }

    init {
        backgroundColor = UIColor.blackColor
        clipsToBounds = true
        userInteractionEnabled = false
        layer.addSublayer(playerLayer)
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        CATransaction.begin()
        CATransaction.setDisableActions(true)
        playerLayer.frame = bounds
        CATransaction.commit()
    }
}
