package com.playerid.app.ui.screens

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.playerid.app.ar.JerseyDetectionManager
import com.playerid.app.utils.RecordingManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

fun startCamera(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView, analyzer: JerseyDetectionManager, recordingManager: RecordingManager, onCameraReady: (Camera) -> Unit) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val recorder = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HIGHEST)).build()
        val videoCapture = VideoCapture.withOutput(recorder)
        recordingManager.setVideoCapture(videoCapture)
        val imageAnalyzer = ImageAnalysis.Builder().setTargetResolution(Size(1280, 720)).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
        imageAnalyzer.setAnalyzer(cameraExecutor, analyzer)
        try {
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, videoCapture, imageAnalyzer)
            onCameraReady(camera)
        } catch (e: Exception) { Log.e("CameraScreen", "Binding failed", e) }
    }, ContextCompat.getMainExecutor(context))
}
