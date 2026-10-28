package com.playerid.app.domain.camera

import com.playerid.app.platform.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RollingCaptureTest {
    private class Camera : CameraService, RollingCameraService {
        val calls = mutableListOf<String>()
        val video = MediaReference("rolling.mov", MediaKind.VIDEO, "video/quicktime")
        var failure: String? = null
        var rotationGate: CompletableDeferred<Unit>? = null
        override suspend fun start(configuration: CameraConfiguration) { calls += "start" }
        override suspend fun stop() { calls += "stop"; discardRollingBuffer() }
        override suspend fun capturePhoto(): MediaReference = error("Unexpected photo")
        override suspend fun startRecording() { error("Unexpected normal recording") }
        override suspend fun stopRecording(): MediaReference = error("Unexpected normal save")
        override suspend fun startRollingBuffer() { calls += "buffer" }
        override suspend fun rotateRollingBuffer() {
            calls += "rotate"
            rotationGate?.await()
            failure?.let { throw CameraCaptureException(it) }
        }
        override suspend fun saveRollingBuffer(): MediaReference {
            calls += "save rolling"
            failure?.let { throw CameraCaptureException(it) }
            return video
        }
        override suspend fun discardRollingBuffer() { calls += "discard" }
    }

    @Test
    fun savesExactlyLastThirtySecondsAcrossSegmentBoundary() {
        assertEquals(listOf(RollingCaptureRange(0, 7.0, 23.0), RollingCaptureRange(1, 0.0, 7.0)),
            rollingCaptureWindow(listOf(30.0, 7.0)))
        assertEquals(listOf(RollingCaptureRange(1, 0.0, 30.0)), rollingCaptureWindow(listOf(30.0, 30.0)))
        val ranges = rollingCaptureWindow(listOf(30.0, 30.0, 12.5))
        assertEquals(30.0, ranges.sumOf { it.durationSeconds })
        assertEquals(listOf(1, 2), ranges.map { it.segmentIndex })
    }

    @Test
    fun shortWarmupSavesOnlyAvailableFootageAndInvalidDurationsAreRejected() {
        assertEquals(listOf(RollingCaptureRange(0, 0.0, 4.25)), rollingCaptureWindow(listOf(4.25)))
        for (durations in listOf(emptyList(), listOf(0.0), listOf(-1.0), listOf(Double.NaN), listOf(Double.POSITIVE_INFINITY))) {
            assertFailsWith<IllegalArgumentException> { rollingCaptureWindow(durations) }
        }
    }

    @Test
    fun selectingRollingStartsBufferAndTapSavesWithMetadataAndReview() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        controller.changeMode(CaptureMode.ROLLING)
        assertEquals(CapturePhase.BUFFERING, controller.state.value.phase)
        assertTrue(controller.state.value.canCapture)
        assertTrue(controller.state.value.canChangeMode)
        controller.rotateRollingBuffer()
        var metadata: MediaReference? = null
        controller.capture { metadata = it }
        assertEquals(camera.video, metadata)
        assertEquals(camera.video, controller.state.value.pendingReview)
        assertEquals(1, controller.state.value.savedCount)
        val count = camera.calls.count { it == "buffer" }
        controller.resumeRollingBuffer()
        assertEquals(count, camera.calls.count { it == "buffer" })
        controller.dismissReview(camera.video)
        controller.resumeRollingBuffer()
        assertEquals(count + 1, camera.calls.count { it == "buffer" })
        assertEquals(CapturePhase.BUFFERING, controller.state.value.phase)
    }

    @Test
    fun switchingModeAndBackgroundingDiscardWithoutSaving() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        controller.changeMode(CaptureMode.ROLLING)
        controller.changeMode(CaptureMode.VIDEO)
        assertEquals(listOf("buffer", "discard"), camera.calls.takeLast(2))
        assertEquals(CaptureMode.VIDEO, controller.state.value.mode)
        controller.changeMode(CaptureMode.ROLLING)
        controller.close()
        assertEquals(CapturePhase.STOPPED, controller.state.value.phase)
        assertEquals(listOf("stop", "discard"), camera.calls.takeLast(2))
        assertNull(controller.state.value.lastSaved)
        assertNull(controller.state.value.pendingReview)
        assertEquals(0, controller.state.value.savedCount)
        controller.open()
        assertEquals(CapturePhase.BUFFERING, controller.state.value.phase)
    }

    @Test
    fun rotationOrExportFailuresAreVisibleAndNeverReportSavedClips() = runTest {
        for (rotate in listOf(false, true)) {
            val camera = Camera()
            val controller = CameraCaptureController(camera)
            controller.open()
            controller.changeMode(CaptureMode.ROLLING)
            camera.failure = "Storage full"
            if (rotate) controller.rotateRollingBuffer() else controller.capture()
            assertEquals(CapturePhase.FAILED, controller.state.value.phase)
            assertEquals("Storage full", controller.state.value.error)
            assertNull(controller.state.value.pendingReview)
            assertEquals(0, controller.state.value.savedCount)
        }
    }

    @Test
    fun rotationKeepsBufferingUiAndShutterTapWaitsThenSaves() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        controller.changeMode(CaptureMode.ROLLING)
        val before = controller.state.value
        val gate = CompletableDeferred<Unit>()
        camera.rotationGate = gate
        val rotation = launch { controller.rotateRollingBuffer() }
        runCurrent()
        assertEquals(before, controller.state.value)
        val shutter = launch { controller.capture() }
        runCurrent()
        assertEquals(before, controller.state.value)
        assertFalse("save rolling" in camera.calls)
        gate.complete(Unit)
        rotation.join()
        shutter.join()
        assertEquals(1, camera.calls.count { it == "save rolling" })
        assertEquals(camera.video, controller.state.value.pendingReview)
    }

    @Test
    fun rollingDefaultStartsBufferOnOpenAndResumesAfterDeletingReview() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera, initialMode = CaptureMode.ROLLING)
        assertEquals(CaptureMode.ROLLING, controller.state.value.mode)
        controller.open()
        assertEquals(CapturePhase.BUFFERING, controller.state.value.phase)
        controller.capture()
        assertEquals(camera.video, controller.state.value.pendingReview)
        controller.dismissReview(camera.video, deleted = true)
        assertNull(controller.state.value.pendingReview)
        assertNull(controller.state.value.lastSaved)
        assertEquals(0, controller.state.value.savedCount)
        controller.resumeRollingBuffer()
        assertEquals(CapturePhase.BUFFERING, controller.state.value.phase)
    }

    @Test
    fun deletingReviewedRollingClipClearsCameraReferenceAndRestartsBuffer() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera)
        controller.open()
        controller.changeMode(CaptureMode.ROLLING)
        controller.capture()
        controller.dismissReview(camera.video, deleted = true)
        assertNull(controller.state.value.pendingReview)
        assertNull(controller.state.value.lastSaved)
        assertEquals(0, controller.state.value.savedCount)
        controller.resumeRollingBuffer()
        assertEquals(CapturePhase.BUFFERING, controller.state.value.phase)
    }

    @Test
    fun reviewDismissalDoesNotWaitForDeletionAndCleanupIsIdempotent() = runTest {
        val camera = Camera()
        val controller = CameraCaptureController(camera, initialMode = CaptureMode.ROLLING)
        controller.open()
        controller.capture()
        val deleted = camera.video
        controller.dismissReview(deleted)
        assertNull(controller.state.value.pendingReview)
        assertEquals(deleted, controller.state.value.lastSaved)
        controller.resumeRollingBuffer()
        assertEquals(CapturePhase.BUFFERING, controller.state.value.phase)
        controller.forgetDeletedCapture(deleted)
        assertNull(controller.state.value.lastSaved)
        assertEquals(0, controller.state.value.savedCount)
        controller.forgetDeletedCapture(deleted)
        assertEquals(0, controller.state.value.savedCount)
        assertEquals(CapturePhase.BUFFERING, controller.state.value.phase)
    }

    @Test
    fun unexpectedStopIsVisibleButStaleSegmentCompletionIsIgnored() = runTest {
        val controller = CameraCaptureController(Camera())
        controller.open()
        controller.changeMode(CaptureMode.ROLLING)
        controller.recordingFinished { false }
        assertEquals(CapturePhase.BUFFERING, controller.state.value.phase)
        controller.recordingFinished { true }
        assertEquals(CapturePhase.FAILED, controller.state.value.phase)
        assertNotNull(controller.state.value.error)
    }
}
