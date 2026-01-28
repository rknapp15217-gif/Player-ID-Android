package com.playerid.app.utils

import android.graphics.PointF

/**
 * Simple 2D Kalman filter for player tracking.
 */
class KalmanFilter2D(
    initialX: Float,
    initialY: Float,
    private val processNoise: Float = 1f,
    private val measurementNoise: Float = 10f
) {
    private var state = floatArrayOf(initialX, initialY, 0f, 0f) // x, y, vx, vy
    private var covariance = Array(4) { FloatArray(4) { 0f } }

    init {
        for (i in 0..3) covariance[i][i] = 1f
    }

    fun predict(): PointF {
        // Simple constant velocity model
        state[0] += state[2]
        state[1] += state[3]
        // Increase uncertainty
        for (i in 0..3) covariance[i][i] += processNoise
        return PointF(state[0], state[1])
    }

    fun update(measuredX: Float, measuredY: Float) {
        // Kalman gain (simplified for demonstration)
        val k = measurementNoise / (measurementNoise + processNoise)
        state[0] += k * (measuredX - state[0])
        state[1] += k * (measuredY - state[1])
        // Update velocity
        state[2] = measuredX - state[0]
        state[3] = measuredY - state[1]
        // Reduce uncertainty
        for (i in 0..3) covariance[i][i] *= (1 - k)
    }

    fun getState(): PointF = PointF(state[0], state[1])
}
