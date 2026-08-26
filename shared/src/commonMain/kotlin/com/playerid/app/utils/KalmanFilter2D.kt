package com.playerid.app.utils

data class TrackingPoint(
    val x: Float,
    val y: Float
)

/** Simple constant-velocity 2D Kalman filter for player tracking. */
class KalmanFilter2D(
    initialX: Float,
    initialY: Float,
    private val processNoise: Float = 1f,
    private val measurementNoise: Float = 10f
) {
    private val state = floatArrayOf(initialX, initialY, 0f, 0f)
    private val covariance = Array(4) { FloatArray(4) }

    init {
        for (index in 0..3) covariance[index][index] = 1f
    }

    fun predict(): TrackingPoint {
        state[0] += state[2]
        state[1] += state[3]
        for (index in 0..3) covariance[index][index] += processNoise
        return currentState()
    }

    fun update(measuredX: Float, measuredY: Float) {
        val gain = measurementNoise / (measurementNoise + processNoise)
        state[0] += gain * (measuredX - state[0])
        state[1] += gain * (measuredY - state[1])
        state[2] = measuredX - state[0]
        state[3] = measuredY - state[1]
        for (index in 0..3) covariance[index][index] *= 1 - gain
    }

    fun currentState(): TrackingPoint = TrackingPoint(state[0], state[1])
}
