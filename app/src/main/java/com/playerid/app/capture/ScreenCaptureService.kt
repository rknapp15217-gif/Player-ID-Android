package com.playerid.app.capture

import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat
import android.app.PendingIntent
import com.playerid.app.MainActivity
import com.playerid.app.R
import com.playerid.app.AppNavigationCallback
import com.playerid.app.roster.extractRosterCandidates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

class ScreenCaptureService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var isCapturing = false
    private var autoRemind = false
    private var reminderJob: kotlinx.coroutines.Job? = null
    private var autoCaptureJob: kotlinx.coroutines.Job? = null
    private var overlayContainer: LinearLayout? = null
    private var doneButton: Button? = null
    private var tutorialFingerView: View? = null
    private var tutorialAnimator: ValueAnimator? = null
    private var captureContent = CaptureContent.ROSTER

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val data = IntentCompat.getParcelableExtra(intent, EXTRA_DATA, Intent::class.java)
                autoRemind = intent.getBooleanExtra(EXTRA_AUTO_REMIND, false)
                captureContent = intent.getStringExtra(EXTRA_CAPTURE_CONTENT)
                    ?.let { runCatching { CaptureContent.valueOf(it) }.getOrNull() }
                    ?: CaptureContent.ROSTER
                if (resultCode != 0 && data != null) {
                    startForeground(NOTIFICATION_ID, buildNotification())
                    startCaptureSession(resultCode, data)
                    startReminderLoopIfNeeded()
                } else {
                    stopSelf()
                }
            }
            ACTION_HIDE_OVERLAY -> removeOverlay()
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        reminderJob?.cancel()
        reminderJob = null
        autoCaptureJob?.cancel()
        autoCaptureJob = null
        removeOverlay()
        releaseProjection()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startCaptureSession(resultCode: Int, data: Intent) {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        val metrics = resources.displayMetrics
        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "RosterCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        showOverlay()
    }

    private fun startReminderLoopIfNeeded() {
        if (!autoRemind || reminderJob != null) return
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        reminderJob = serviceScope.launch {
            while (true) {
                delay(12000)
                vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }

    private fun startAutoCapture() {
        if (autoCaptureJob != null) return
        autoCaptureJob = serviceScope.launch {
            while (true) {
                captureOnce()
                delay(3000) // Capture every 3 seconds
            }
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.VISIBLE
        }

        val captureButton = Button(this).apply {
            setText(R.string.start_capture)
            setOnClickListener {
                // Remove Capture button and show Done button
                (this.parent as? LinearLayout)?.removeView(this)
                if (doneButton?.parent == null) {
                    container.addView(doneButton)
                }
                // Start auto-capture every 3 seconds
                startAutoCapture()
            }
        }

        // Create Done button but don't add it yet - will be shown after Capture is pressed
        doneButton = Button(this).apply {
            setText(R.string.done)
            setOnClickListener {
                // Stop auto-capture
                autoCaptureJob?.cancel()
                autoCaptureJob = null
                
                val teamName = AppRosterCaptureRepository.activeTeamName.value
                val route = if (!teamName.isNullOrBlank()) {
                    if (captureContent == CaptureContent.SCHEDULE) {
                        "scheduleImport/${Uri.encode(teamName)}/app"
                    } else {
                        "appRosterImport/${Uri.encode(teamName)}"
                    }
                } else {
                    "team"
                }
                val launchIntent = Intent(this@ScreenCaptureService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(AppNavigationCallback.EXTRA_NAV_ROUTE, route)
                }
                startActivity(launchIntent)
                stopSelf()
            }
        }

        container.addView(captureButton)
        // Don't add Done button initially - only add after first capture

        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 120
        }

        windowManager?.addView(container, params)
        overlayView = container
        overlayContainer = container
        
        // Show tutorial screen immediately
        showTutorialScreen()
    }

    private fun removeOverlay() {
        autoCaptureJob?.cancel()
        autoCaptureJob = null
        removeTutorialScreen()
        overlayView?.let { view ->
            try {
                if (view.parent != null) {
                    windowManager?.removeView(view)
                }
            } catch (e: Exception) {
                // Ignore removal errors
            }
        }
        overlayView = null
        overlayContainer = null
        doneButton = null
    }
    
    private fun showTutorialScreen() {
        if (tutorialFingerView != null) return
        
        class TutorialOverlayView(context: Context) : FrameLayout(context) {
            init {
                setWillNotDraw(false) // Enable onDraw to be called
            }
            
            private val backgroundPaint = Paint().apply {
                color = Color.argb(230, 0, 0, 0) // Dark semi-transparent background
            }
            
            private val phonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            
            private val phoneBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.GRAY
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            
            private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(255, 25, 118, 210) // Blue
                style = Paint.Style.FILL
            }
            
            private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                textSize = 32f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            
            private val instructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                textSize = 72f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
            }
            
            private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                strokeWidth = 3f
            }
            
            private val fingerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(200, 100, 181, 246) // Semi-transparent blue
                style = Paint.Style.FILL
            }
            private val phoneRect = RectF()
            private val buttonRect = RectF()
            private val arrowPath = Path()
            
            var animationProgress = 0f
                set(value) {
                    field = value
                    invalidate()
                }
            
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                
                // Draw background
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
                
                // Phone dimensions and position (centered)
                val phoneWidth = width * 0.7f
                val phoneHeight = height * 0.75f
                val phoneLeft = (width - phoneWidth) / 2f
                val phoneTop = (height - phoneHeight) / 2f
                
                // Draw phone background
                phoneRect.set(phoneLeft, phoneTop, phoneLeft + phoneWidth, phoneTop + phoneHeight)
                canvas.drawRoundRect(phoneRect, 40f, 40f, phonePaint)
                canvas.drawRoundRect(phoneRect, 40f, 40f, phoneBorderPaint)
                
                val progress = animationProgress
                
                // Button positions
                val buttonWidth = phoneWidth * 0.4f
                val buttonHeight = 80f
                val buttonX = phoneLeft + phoneWidth - buttonWidth - 40f
                val buttonY = phoneTop + 60f
                
                when {
                    // Phase 1: Tap Capture (0-0.3)
                    progress < 0.3f -> {
                        val tapProgress = progress / 0.3f
                        
                        // Draw instruction text
                        canvas.drawText("Push Start Capture", phoneLeft + phoneWidth / 2f, phoneTop - 60f, instructionPaint)
                        
                        // Draw Capture button
                        buttonRect.set(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight)
                        canvas.drawRoundRect(buttonRect, 20f, 20f, buttonPaint)
                        canvas.drawText("Capture", buttonX + buttonWidth / 2f, buttonY + (buttonHeight / 2f) + 12f, textPaint)
                        
                        // Draw finger tapping
                        val fingerY = buttonY + (buttonHeight / 2f) - (tapProgress * 30f)
                        drawFinger(canvas, buttonX + buttonWidth / 2f, fingerY)
                    }
                    // Phase 2: Scroll (0.3-0.7)
                    progress < 0.7f -> {
                        val scrollProgress = (progress - 0.3f) / 0.4f
                        
                        // Draw instruction text
                        canvas.drawText("Scroll slowly", phoneLeft + phoneWidth / 2f, phoneTop - 60f, instructionPaint)
                        
                        // Draw scrolling roster lines
                        val contentTop = phoneTop + 150f
                        val contentBottom = phoneTop + phoneHeight - 150f
                        val lineSpacing = 80f
                        
                        for (i in 0..10) {
                            val lineY = contentTop + (i * lineSpacing) - (scrollProgress * lineSpacing * 3)
                            if (lineY > contentTop && lineY < contentBottom) {
                                // Draw player number
                                canvas.drawText("#${10 + i}", phoneLeft + 80f, lineY, textPaint)
                                // Draw player name line
                                canvas.drawLine(phoneLeft + 150f, lineY - 5f, phoneLeft + phoneWidth - 50f, lineY - 5f, linePaint)
                            }
                        }
                        
                        // Draw Done button at top right (same position as Capture)
                        buttonRect.set(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight)
                        canvas.drawRoundRect(buttonRect, 20f, 20f, buttonPaint)
                        canvas.drawText("Done", buttonX + buttonWidth / 2f, buttonY + (buttonHeight / 2f) + 12f, textPaint)
                        
                        // Draw finger scrolling
                        val fingerY = contentTop + 100f + (scrollProgress * 150f)
                        drawFinger(canvas, phoneLeft + phoneWidth / 2f, fingerY)
                        
                        // Draw down arrow next to finger
                        arrowPath.rewind()
                        arrowPath.moveTo(phoneLeft + phoneWidth / 2f + 60f, fingerY - 20f)
                        arrowPath.lineTo(phoneLeft + phoneWidth / 2f + 60f, fingerY + 40f)
                        arrowPath.lineTo(phoneLeft + phoneWidth / 2f + 50f, fingerY + 30f)
                        arrowPath.moveTo(phoneLeft + phoneWidth / 2f + 60f, fingerY + 40f)
                        arrowPath.lineTo(phoneLeft + phoneWidth / 2f + 70f, fingerY + 30f)
                        fingerPaint.style = Paint.Style.STROKE
                        fingerPaint.strokeWidth = 6f
                        canvas.drawPath(arrowPath, fingerPaint)
                        fingerPaint.style = Paint.Style.FILL
                    }
                    // Phase 3: Tap Done (0.7-1.0)
                    else -> {
                        val tapProgress = (progress - 0.7f) / 0.3f
                        
                        // Draw instruction text
                        canvas.drawText("Push Done", phoneLeft + phoneWidth / 2f, phoneTop - 60f, instructionPaint)
                        
                        // Draw Done button at top right (same position as Capture)
                        buttonRect.set(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight)
                        canvas.drawRoundRect(buttonRect, 20f, 20f, buttonPaint)
                        canvas.drawText("Done", buttonX + buttonWidth / 2f, buttonY + (buttonHeight / 2f) + 12f, textPaint)
                        
                        // Draw finger tapping Done
                        val fingerY = buttonY + (buttonHeight / 2f) - (tapProgress * 30f)
                        drawFinger(canvas, buttonX + buttonWidth / 2f, fingerY)
                    }
                }
            }
            
            private fun drawFinger(canvas: Canvas, x: Float, y: Float) {
                // Draw finger as circles
                canvas.drawCircle(x, y, 50f, fingerPaint)
                canvas.drawCircle(x, y - 40f, 45f, fingerPaint)
                canvas.drawCircle(x, y - 75f, 40f, fingerPaint)
            }
        }
        
        val tutorialView = TutorialOverlayView(this)
        
        // Add dismiss button
        val dismissButton = Button(this).apply {
            setText(R.string.got_it)
            textSize = 18f
            setOnClickListener {
                removeTutorialScreen()
            }
        }
        
        val dismissParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = 100
        }
        
        tutorialView.addView(dismissButton, dismissParams)
        
        val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        
        windowManager?.addView(tutorialView, params)
        tutorialFingerView = tutorialView
        
        // Animate continuously
        tutorialAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 6000 // 6 seconds for full animation
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                (tutorialView as? TutorialOverlayView)?.animationProgress = animation.animatedValue as Float
            }
            start()
        }
    }
    
    private fun removeTutorialScreen() {
        tutorialAnimator?.cancel()
        tutorialAnimator = null
        val fingerView = tutorialFingerView
        tutorialFingerView = null
        
        if (fingerView != null && fingerView != overlayView) {
            try {
                if (fingerView.parent != null) {
                    windowManager?.removeView(fingerView)
                }
            } catch (e: Exception) {
                // Ignore removal errors
            }
        }
    }

    private fun captureOnce() {
        if (isCapturing) return
        isCapturing = true
        
        // Capture image immediately, then release lock
        val image = imageReader?.acquireLatestImage()
        if (image == null) {
            isCapturing = false
            return
        }

        val bitmap = imageToBitmap(image)
        image.close()
        
        // Release lock immediately so next auto-capture can happen
        isCapturing = false
        
        // Process OCR in background
        serviceScope.launch {
            val scaled = scaleBitmap(bitmap, 2048)
            val result = extractRosterCandidates(scaled)
            if (captureContent == CaptureContent.SCHEDULE) {
                AppRosterCaptureRepository.addScheduleLines(result.rawLines)
            } else {
                AppRosterCaptureRepository.addCandidates(result.candidates)
            }
        }
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val width = image.width
        val height = image.height
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val maxSide = max(bitmap.width, bitmap.height)
        if (maxSide <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / maxSide.toFloat()
        val targetWidth = (bitmap.width * scale).roundToInt()
        val targetHeight = (bitmap.height * scale).roundToInt()

        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun releaseProjection() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun buildNotification(): Notification {
        val channelId = ensureNotificationChannel()
        val stopIntent = Intent(this, ScreenCaptureService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Roster capture active")
            .setContentText("Tap Capture for each screen, then Done.")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .addAction(0, "Stop", stopPendingIntent)
            .build()
    }

    private fun ensureNotificationChannel(): String {
        val channelId = "roster_capture"
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(channelId)
        if (existing == null) {
            val channel = NotificationChannel(
                channelId,
                "Roster Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
        return channelId
    }

    companion object {
        const val ACTION_START = "com.playerid.app.capture.START"
        const val ACTION_STOP = "com.playerid.app.capture.STOP"
        const val ACTION_HIDE_OVERLAY = "com.playerid.app.capture.HIDE_OVERLAY"
        const val ACTION_SHOW_OVERLAY = "com.playerid.app.capture.SHOW_OVERLAY"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_DATA = "data"
        const val EXTRA_AUTO_REMIND = "auto_remind"
        const val EXTRA_CAPTURE_CONTENT = "capture_content"
        private const val NOTIFICATION_ID = 4011
    }
}
