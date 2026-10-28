package com.playerid.app.domain.camera

import com.playerid.app.platform.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CameraCaptureControllerTest {
    private class Camera : CameraService {
        val calls = mutableListOf<String>()
        var failure: String? = null
        var photoGate: CompletableDeferred<Unit>? = null
        val photo = MediaReference("photo.jpg", MediaKind.IMAGE, "image/jpeg")
        val video = MediaReference("video.mov", MediaKind.VIDEO, "video/quicktime")

        override suspend fun start(configuration: CameraConfiguration) {
            calls += "start"
            assertTrue(configuration.includeAudio)
            failure?.let { throw CameraCaptureException(it) }
        }
        override suspend fun stop() { calls += "stop" }
        override suspend fun capturePhoto(): MediaReference {
            calls += "photo"
            photoGate?.await()
            failure?.let { throw CameraCaptureException(it) }
            return photo
        }
        override suspend fun startRecording() { calls += "record" }
        override suspend fun stopRecording(): MediaReference {
            calls += "save"
            failure?.let { throw CameraCaptureException(it) }
            return video
        }
    }

    @Test
    fun onlySuccessfullySavedVideosOpenReviewAndDismissalDoesNotRepeatIt() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        controller.capture()
        assertNull(controller.state.value.pendingReview)
        controller.selectMode(CaptureMode.VIDEO)
        controller.capture()
        assertNull(controller.state.value.pendingReview)
        controller.capture()
        assertEquals(camera.video, controller.state.value.pendingReview)
        controller.dismissReview(camera.photo)
        assertEquals(camera.video, controller.state.value.pendingReview)
        controller.dismissReview(camera.video)
        controller.close()
        controller.open()
        assertNull(controller.state.value.pendingReview)
        assertEquals(camera.video, controller.state.value.lastSaved)
        assertEquals(2, controller.state.value.savedCount)
    }

    @Test
    fun automaticAndBackgroundFinalizationKeepTheVideoAvailableForReview() = runTest {
        for (background in listOf(false, true)) {
            val camera = Camera()
            val controller = CameraCaptureController(camera)
            controller.open()
            controller.selectMode(CaptureMode.VIDEO)
            controller.capture()
            if (background) controller.close() else controller.recordingFinished()
            assertEquals(camera.video, controller.state.value.pendingReview)
            controller.open()
            assertEquals(camera.video, controller.state.value.pendingReview)
            controller.dismissReview(camera.video, deleted = true)
            assertNull(controller.state.value.pendingReview)
            assertNull(controller.state.value.lastSaved)
            assertEquals(0, controller.state.value.savedCount)
        }
    }

    @Test
    fun failedVideoMetadataSaveDoesNotOpenReview() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        controller.selectMode(CaptureMode.VIDEO)
        controller.capture { throw CameraCaptureException("Metadata save failed") }
        controller.capture()
        assertEquals(CapturePhase.FAILED, controller.state.value.phase)
        assertNull(controller.state.value.pendingReview)
        assertEquals("Metadata save failed", controller.state.value.error)
    }

    @Test
    fun photoBecomesSavedOnlyAfterServiceCompletes() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        assertEquals(CapturePhase.READY, controller.state.value.phase)
        controller.capture()
        assertEquals(camera.photo, controller.state.value.lastSaved)
        assertEquals(1, controller.state.value.savedCount)
        assertEquals(listOf("stop", "start", "photo"), camera.calls)
    }

    @Test
    fun deniedPermissionIsVisibleAndCanBeRetried() = runTest {
        val camera = Camera().apply { failure = "Camera access denied" }
        val controller = CameraCaptureController(camera)
        controller.open()
        assertEquals(CapturePhase.FAILED, controller.state.value.phase)
        assertEquals("Camera access denied", controller.state.value.error)
        assertFalse(controller.state.value.canCapture)
        controller.capture()
        assertFalse("photo" in camera.calls)
        camera.failure = null
        controller.open()
        assertEquals(CapturePhase.READY, controller.state.value.phase)
        assertNull(controller.state.value.error)
    }

    @Test
    fun modeIsLockedWhileRecordingAndStopSavesVideo() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        controller.selectMode(CaptureMode.VIDEO)
        controller.capture()
        assertEquals(CapturePhase.RECORDING, controller.state.value.phase)
        controller.selectMode(CaptureMode.PHOTO)
        assertEquals(CaptureMode.VIDEO, controller.state.value.mode)
        controller.capture()
        assertEquals(camera.video, controller.state.value.lastSaved)
        assertEquals(1, controller.state.value.savedCount)
        assertEquals(CapturePhase.READY, controller.state.value.phase)
    }

    @Test
    fun leavingCameraFinishesRecordingBeforeStoppingSession() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        controller.selectMode(CaptureMode.VIDEO)
        controller.capture()
        controller.close()
        assertEquals(listOf("stop", "start", "record", "save", "stop"), camera.calls)
        assertEquals(camera.video, controller.state.value.lastSaved)
        assertEquals(CapturePhase.STOPPED, controller.state.value.phase)
    }

    @Test
    fun storageFailureDoesNotReportSuccessAndStillReleasesCamera() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        controller.selectMode(CaptureMode.VIDEO)
        controller.capture()
        camera.failure = "Storage is full"
        controller.close()
        assertNull(controller.state.value.lastSaved)
        assertEquals("Storage is full", controller.state.value.error)
        assertEquals(CapturePhase.FAILED, controller.state.value.phase)
        assertEquals("stop", camera.calls.last())
    }

    @Test
    fun concurrentShutterPressDoesNotStartSecondCapture() = runTest {
        val gate = CompletableDeferred<Unit>()
        val camera = Camera().apply { photoGate = gate }
        val controller = CameraCaptureController(camera)
        controller.open()
        val first = launch { controller.capture() }
        runCurrent()
        assertEquals(CapturePhase.CAPTURING, controller.state.value.phase)
        assertNull(controller.state.value.lastSaved)
        controller.selectMode(CaptureMode.VIDEO)
        controller.capture()
        assertEquals(CaptureMode.PHOTO, controller.state.value.mode)
        gate.complete(Unit)
        first.join()
        assertEquals(1, camera.calls.count { it == "photo" })
    }

    @Test
    fun cancellationAndTabExitWaitForPhotoSave() = runTest {
        val gate = CompletableDeferred<Unit>()
        val camera = Camera().apply { photoGate = gate }
        val controller = CameraCaptureController(camera)
        controller.open()
        val capture = launch { controller.capture() }
        runCurrent()
        capture.cancel()
        val close = launch { controller.close() }
        runCurrent()
        assertEquals(CapturePhase.CAPTURING, controller.state.value.phase)
        gate.complete(Unit)
        capture.join()
        close.join()
        assertEquals(camera.photo, controller.state.value.lastSaved)
        assertEquals(CapturePhase.STOPPED, controller.state.value.phase)
    }

    @Test
    fun interruptionsRemainVisibleAndPreviouslySavedMediaIsRetained() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        controller.capture()
        controller.close("Camera interrupted")
        assertEquals("Camera interrupted", controller.state.value.error)
        controller.open()
        assertEquals(camera.photo, controller.state.value.lastSaved)
        assertNull(controller.state.value.error)
    }

    @Test
    fun nativeAutomaticStopUpdatesRecordingStateAndConsumesSavedMediaOnce() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        controller.selectMode(CaptureMode.VIDEO)
        controller.capture()
        controller.recordingFinished()
        controller.recordingFinished()
        assertEquals(CapturePhase.READY, controller.state.value.phase)
        assertEquals(camera.video, controller.state.value.lastSaved)
        assertEquals(1, camera.calls.count { it == "save" })
    }

    @Test
    fun nativeRecordingFailureStopsTimerAndShowsError() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        controller.selectMode(CaptureMode.VIDEO)
        controller.capture()
        camera.failure = "Recording interrupted"
        controller.recordingFinished()
        assertEquals(CapturePhase.FAILED, controller.state.value.phase)
        assertEquals("Recording interrupted", controller.state.value.error)
        assertNull(controller.state.value.lastSaved)
    }

    @Test
    fun staleNativeCompletionCannotStopANewerRecording() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        controller.selectMode(CaptureMode.VIDEO)
        controller.capture()
        controller.recordingFinished { false }
        assertEquals(CapturePhase.RECORDING, controller.state.value.phase)
        assertFalse("save" in camera.calls)
    }

    @Test
    fun photoMetadataFinishesBeforeCaptureBecomesReady() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        val metadataGate = CompletableDeferred<Unit>()
        controller.open()
        val capture = launch {
            controller.capture {
                assertEquals(camera.photo, it)
                metadataGate.await()
            }
        }
        runCurrent()
        assertEquals(CapturePhase.SAVING, controller.state.value.phase)
        assertFalse(controller.state.value.canCapture)
        controller.selectMode(CaptureMode.VIDEO)
        assertEquals(CaptureMode.PHOTO, controller.state.value.mode)
        metadataGate.complete(Unit)
        capture.join()
        assertEquals(CapturePhase.READY, controller.state.value.phase)
    }

    @Test
    fun recordingKeepsStartDetailsInsteadOfStopDetails() = runTest {
        val controller = CameraCaptureController(Camera())
        val assignments = mutableListOf<String>()
        controller.open()
        controller.selectMode(CaptureMode.VIDEO)
        controller.capture { assignments += "starting team" }
        controller.capture { assignments += "different team at stop" }
        assertEquals(listOf("starting team"), assignments)
    }

    @Test
    fun automaticAndTabExitFinalizationSaveStartDetailsOnce() = runTest {
        for (automatic in listOf(true, false)) {
            val controller = CameraCaptureController(Camera())
            var writes = 0
            controller.open()
            controller.selectMode(CaptureMode.VIDEO)
            controller.capture { writes++ }
            if (automatic) controller.recordingFinished() else controller.close()
            controller.recordingFinished()
            controller.close()
            assertEquals(1, writes)
        }
    }

    @Test
    fun metadataFailureRetainsMediaAndDoesNotContaminateNextCapture() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        controller.capture { throw CameraCaptureException("Capture saved but game details failed") }
        assertEquals(CapturePhase.FAILED, controller.state.value.phase)
        assertEquals(camera.photo, controller.state.value.lastSaved)
        assertEquals(1, controller.state.value.savedCount)
        assertEquals("Capture saved but game details failed", controller.state.value.error)
        controller.open()
        controller.capture()
        assertEquals(CapturePhase.READY, controller.state.value.phase)
        assertEquals(2, controller.state.value.savedCount)
    }

    @Test
    fun cancellationWaitsForCaptureMetadataBeforeClose() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        val gate = CompletableDeferred<Unit>()
        var persisted = false
        controller.open()
        val capture = launch {
            controller.capture { gate.await(); persisted = true }
        }
        runCurrent()
        capture.cancel()
        val close = launch { controller.close() }
        runCurrent()
        assertFalse(persisted)
        assertFalse(close.isCompleted)
        gate.complete(Unit)
        capture.join()
        close.join()
        assertTrue(persisted)
        assertEquals(CapturePhase.STOPPED, controller.state.value.phase)
    }

    @Test
    fun cancellingAutomaticFinalizationCannotDropGameDetails() = runTest {
        val controller = CameraCaptureController(Camera())
        val gate = CompletableDeferred<Unit>()
        var persisted = false
        controller.open()
        controller.selectMode(CaptureMode.VIDEO)
        controller.capture { gate.await(); persisted = true }
        val automatic = launch { controller.recordingFinished() }
        runCurrent()
        automatic.cancel()
        val close = launch { controller.close() }
        runCurrent()
        assertFalse(close.isCompleted)
        gate.complete(Unit)
        automatic.join()
        close.join()
        assertTrue(persisted)
        assertEquals(CapturePhase.STOPPED, controller.state.value.phase)
    }
}
