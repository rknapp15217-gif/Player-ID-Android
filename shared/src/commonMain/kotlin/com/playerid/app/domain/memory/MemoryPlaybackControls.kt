package com.playerid.app.domain.memory

interface MemoryPlayback {
    val durationMillis: Long
    val positionMillis: Long
    val isPlaying: Boolean
    fun play()
    fun pause()
    fun seek(positionMillis: Long, toleranceMillis: Long, completion: (Boolean) -> Unit)
}

class MemoryPlaybackControls(
    private val playback: MemoryPlayback,
    private val onError: (String) -> Unit
) {
    var isScrubbing = false
        private set
    var isSeeking = false
        private set
    var fraction = 0f
        private set
    private var resumeAfterSeek = false
    private var generation = 0
    private var pendingPosition: Long? = null

    fun refresh() {
        if (!isScrubbing && !isSeeking) {
            val duration = playback.durationMillis
            fraction = if (duration > 0) (playback.positionMillis.toDouble() / duration).toFloat().coerceIn(0f, 1f) else 0f
        }
    }

    fun togglePlayback() {
        if (isScrubbing || isSeeking) return
        if (playback.isPlaying) {
            playback.pause()
        } else if (playback.durationMillis > 0 && playback.positionMillis >= playback.durationMillis) {
            seek(0, resume = true)
        } else {
            playback.play()
        }
    }

    fun scrub(value: Float) {
        require(value.isFinite()) { "The playback seek position is invalid." }
        if (playback.durationMillis <= 0 || (isSeeking && !isScrubbing)) return
        if (!isScrubbing) {
            resumeAfterSeek = playback.isPlaying
            playback.pause()
            isScrubbing = true
        }
        fraction = value.coerceIn(0f, 1f)
        pendingPosition = (fraction * playback.durationMillis.toDouble()).toLong()
        if (!isSeeking) seekNextScrubPosition()
    }

    fun finishScrub() {
        if (!isScrubbing) return
        isScrubbing = false
        pendingPosition = (fraction * playback.durationMillis.toDouble()).toLong()
        if (!isSeeking) seekNextScrubPosition()
    }

    fun interrupt() {
        generation++
        isScrubbing = false
        isSeeking = false
        resumeAfterSeek = false
        pendingPosition = null
        playback.pause()
    }

    private fun seekNextScrubPosition() {
        val position = pendingPosition ?: return
        pendingPosition = null
        seek(position, resume = false, scrubbing = true)
    }

    private fun seek(position: Long, resume: Boolean, scrubbing: Boolean = false) {
        val request = ++generation
        isSeeking = true
        playback.seek(position, toleranceMillis = if (scrubbing && isScrubbing) 33 else 0) completion@ { completed ->
            if (request == generation) {
                isSeeking = false
                if (scrubbing && pendingPosition != null) {
                    seekNextScrubPosition()
                    return@completion
                }
                if (!completed) {
                    resumeAfterSeek = false
                    onError("Video seeking was interrupted. Try seeking again.")
                } else if (resume || (scrubbing && !isScrubbing && resumeAfterSeek)) playback.play()
                refresh()
            }
        }
    }
}
