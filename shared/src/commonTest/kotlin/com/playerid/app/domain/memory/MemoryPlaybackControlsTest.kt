package com.playerid.app.domain.memory

import kotlin.test.*

class MemoryPlaybackControlsTest {
    private val playback = TestPlayback()
    private val errors = mutableListOf<String>()
    private val controls = MemoryPlaybackControls(playback, errors::add)

    @Test
    fun togglesPlaybackAndRefreshesProgress() {
        controls.togglePlayback()
        assertTrue(playback.isPlaying)
        controls.togglePlayback()
        assertFalse(playback.isPlaying)
        playback.positionMillis = 2500
        controls.refresh()
        assertEquals(0.25f, controls.fraction)
    }

    @Test
    fun playingScrubPausesAndResumesOnlyAfterSeekCompletes() {
        playback.play()
        controls.scrub(0.5f)
        controls.scrub(0.75f)
        assertFalse(playback.isPlaying)
        controls.refresh()
        assertEquals(0.75f, controls.fraction)
        controls.finishScrub()
        assertEquals(5000, playback.seekPosition)
        playback.completeSeek(true)
        assertEquals(7500, playback.seekPosition)
        assertTrue(controls.isSeeking)
        controls.togglePlayback()
        assertFalse(playback.isPlaying)
        playback.completeSeek(true)
        assertTrue(playback.isPlaying)
        assertFalse(controls.isSeeking)
    }

    @Test
    fun pausedScrubRemainsPausedAndClampsToEndpoints() {
        controls.scrub(-2f)
        assertEquals(0f, controls.fraction)
        controls.scrub(2f)
        controls.finishScrub()
        playback.completeSeek(true)
        assertEquals(10000, playback.seekPosition)
        playback.completeSeek(true)
        assertFalse(playback.isPlaying)
    }

    @Test
    fun interruptionPreventsPendingSeekFromRestartingAudio() {
        playback.play()
        controls.scrub(0.5f)
        controls.finishScrub()
        controls.interrupt()
        playback.completeSeek(true)
        assertFalse(playback.isPlaying)
        assertFalse(controls.isSeeking)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun interruptedDragCannotResumeOnLateTouchUp() {
        playback.play()
        controls.scrub(0.5f)
        controls.interrupt()
        controls.finishScrub()
        assertEquals(5000, playback.seekPosition)
        playback.completeSeek(true)
        assertFalse(playback.isPlaying)
    }

    @Test
    fun failedSeekReportsErrorAndDoesNotResume() {
        playback.play()
        controls.scrub(0.5f)
        controls.finishScrub()
        playback.completeSeek(false)
        playback.completeSeek(false)
        assertEquals(1, errors.size)
        assertFalse(playback.isPlaying)
        assertFalse(controls.isSeeking)
    }

    @Test
    fun playAtEndSeeksToStartBeforePlaying() {
        playback.positionMillis = playback.durationMillis
        controls.togglePlayback()
        assertEquals(0, playback.seekPosition)
        assertFalse(playback.isPlaying)
        playback.completeSeek(true)
        assertTrue(playback.isPlaying)
    }

    @Test
    fun mainPlaybackSeeksDuringDragAndCoalescesToLatestForwardOrBackwardPosition() {
        controls.scrub(0.1f)
        assertEquals(33, playback.seekTolerance)
        assertEquals(1000, playback.seekPosition)
        controls.scrub(0.4f)
        controls.scrub(0.9f)
        controls.scrub(0.3f)
        assertEquals(1, playback.seekCount)
        playback.completeSeek(true)
        assertEquals(3000, playback.seekPosition)
        assertEquals(2, playback.seekCount)
        playback.completeSeek(true)
        assertEquals(3000, playback.positionMillis)
        assertTrue(controls.isScrubbing)
        assertFalse(controls.isSeeking)
        controls.finishScrub()
        assertEquals(0, playback.seekTolerance)
        playback.completeSeek(true)
        assertFalse(playback.isPlaying)
    }

    @Test
    fun releasingDuringSeekAlwaysFinishesWithExactTarget() {
        playback.play()
        controls.scrub(0.5f)
        assertEquals(33, playback.seekTolerance)
        controls.finishScrub()
        playback.completeSeek(true)
        assertEquals(5000, playback.seekPosition)
        assertEquals(0, playback.seekTolerance)
        assertFalse(playback.isPlaying)
        controls.interrupt()
        playback.completeSeek(true)
        assertFalse(playback.isPlaying)
    }

    @Test
    fun releaseAfterLiveSeekCompletesResumesOriginalPlayback() {
        playback.play()
        controls.scrub(0.5f)
        playback.completeSeek(true)
        assertFalse(playback.isPlaying)
        controls.finishScrub()
        assertFalse(playback.isPlaying)
        assertEquals(0, playback.seekTolerance)
        playback.completeSeek(true)
        assertTrue(playback.isPlaying)
        assertEquals(2, playback.seekCount)
    }

    @Test
    fun interruptionDropsQueuedDragPosition() {
        playback.play()
        controls.scrub(0.25f)
        controls.scrub(0.75f)
        controls.interrupt()
        playback.completeSeek(true)
        assertEquals(1, playback.seekCount)
        assertFalse(playback.isPlaying)
        assertFalse(controls.isSeeking)
    }

    @Test
    fun failedIntermediateSeekStillChasesLatestPosition() {
        playback.play()
        controls.scrub(0.25f)
        controls.scrub(0.75f)
        controls.finishScrub()
        playback.completeSeek(false)
        assertEquals(7500, playback.seekPosition)
        playback.completeSeek(true)
        assertTrue(playback.isPlaying)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun failedLiveSeekDoesNotResumeWhenFingerIsReleased() {
        playback.play()
        controls.scrub(0.5f)
        playback.completeSeek(false)
        controls.finishScrub()
        assertFalse(playback.isPlaying)
        assertEquals(1, errors.size)
    }

    @Test
    fun unknownDurationDisablesSeekingAndInvalidInputIsRejected() {
        playback.durationMillis = 0
        controls.scrub(0.5f)
        assertFalse(controls.isScrubbing)
        controls.refresh()
        assertEquals(0f, controls.fraction)
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY)) {
            assertFailsWith<IllegalArgumentException> { controls.scrub(value) }
        }
    }
}

private class TestPlayback : MemoryPlayback {
    override var durationMillis = 10000L
    override var positionMillis = 0L
    override var isPlaying = false
    var seekPosition: Long? = null
    var seekCount = 0
    var seekTolerance: Long? = null
    private var completion: ((Boolean) -> Unit)? = null
    override fun play() { isPlaying = true }
    override fun pause() { isPlaying = false }
    override fun seek(positionMillis: Long, toleranceMillis: Long, completion: (Boolean) -> Unit) {
        seekCount++
        seekTolerance = toleranceMillis
        seekPosition = positionMillis
        this.completion = completion
    }
    fun completeSeek(success: Boolean) {
        if (success) positionMillis = checkNotNull(seekPosition)
        val callback = checkNotNull(completion)
        completion = null
        callback(success)
    }
}
