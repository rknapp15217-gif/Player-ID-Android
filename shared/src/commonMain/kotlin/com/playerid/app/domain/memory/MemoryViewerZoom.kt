package com.playerid.app.domain.memory

fun memoryViewerZoomAfterGesture(current: Float, change: Float): Float {
    require(current.isFinite() && current in 1f..4f) { "The current viewer zoom is invalid." }
    require(change.isFinite() && change > 0f) { "The viewer zoom gesture is invalid." }
    return (current * change).coerceIn(1f, 4f)
}
