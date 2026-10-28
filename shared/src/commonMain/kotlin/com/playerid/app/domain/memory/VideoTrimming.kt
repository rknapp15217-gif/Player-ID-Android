package com.playerid.app.domain.memory

import kotlin.math.roundToLong
import com.playerid.app.platform.MediaReference
import com.playerid.app.domain.team.captureFileKey

internal suspend fun deleteOriginalAfterTrim(
    original: MediaReference,
    trimmed: MediaReference,
    deleteFile: suspend (MediaReference) -> Unit,
    forgetDetails: suspend (String) -> Unit
) {
    require(original.identifier != trimmed.identifier && captureFileKey(original) != captureFileKey(trimmed)) {
        "The trimmed clip must be a separate saved memory before deleting the original."
    }
    deleteFile(original)
    forgetDetails(captureFileKey(original))
}

data class VideoTrimRange(val startMillis: Long, val endMillis: Long) {
    val durationMillis: Long get() = endMillis - startMillis

    fun validate(sourceDurationMillis: Long) {
        require(sourceDurationMillis > 0) { "This video has no usable duration." }
        require(startMillis >= 0 && startMillis < endMillis && endMillis <= sourceDurationMillis) {
            "Choose an end time after the start time, within this video."
        }
    }
}

fun videoTrimRange(startFraction: Float, endFraction: Float, durationMillis: Long): VideoTrimRange {
    require(startFraction.isFinite() && endFraction.isFinite() &&
        startFraction >= 0f && endFraction <= 1f && startFraction < endFraction) {
        "Choose a valid start and end time."
    }
    return VideoTrimRange(
        (startFraction.toDouble() * durationMillis).roundToLong(),
        (endFraction.toDouble() * durationMillis).roundToLong()
    ).also { it.validate(durationMillis) }
}

fun videoTrimTimestamp(millis: Long): String {
    require(millis >= 0) { "Video time cannot be negative." }
    return "${millis / 60_000}:${(millis / 1000 % 60).toString().padStart(2, '0')}." +
        (millis % 1000).toString().padStart(3, '0')
}
