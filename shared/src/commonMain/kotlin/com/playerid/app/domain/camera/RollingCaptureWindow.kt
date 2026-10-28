package com.playerid.app.domain.camera

data class RollingCaptureRange(val segmentIndex: Int, val startSeconds: Double, val durationSeconds: Double)

fun rollingCaptureWindow(durations: List<Double>): List<RollingCaptureRange> {
    require(durations.isNotEmpty() && durations.all { it.isFinite() && it > 0 }) {
        "Rolling segments must contain playable video."
    }
    var remaining = 30.0
    return durations.indices.reversed().mapNotNull { index ->
        val duration = minOf(durations[index], remaining)
        remaining -= duration
        if (duration > 0) RollingCaptureRange(index, durations[index] - duration, duration) else null
    }.reversed()
}
