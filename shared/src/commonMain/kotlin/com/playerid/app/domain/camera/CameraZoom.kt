package com.playerid.app.domain.camera

fun cameraZoomAfterGesture(current: Double, change: Double, minimum: Double, maximum: Double): Double {
    require(current.isFinite() && current > 0.0) { "The current camera zoom is invalid." }
    require(change.isFinite() && change > 0.0) { "The camera zoom gesture is invalid." }
    require(minimum.isFinite() && maximum.isFinite() && minimum > 0.0 && maximum >= minimum) {
        "The camera reported an invalid zoom range."
    }
    return (current * change).coerceIn(minimum, maximum)
}
