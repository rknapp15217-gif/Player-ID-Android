package com.playerid.app.utils

import kotlin.test.Test
import kotlin.test.assertTrue

class KalmanFilter2DTest {
    @Test
    fun updateMovesStateTowardMeasurement() {
        val filter = KalmanFilter2D(initialX = 0f, initialY = 0f)

        filter.update(measuredX = 10f, measuredY = 20f)
        val state = filter.currentState()

        assertTrue(state.x in 0f..10f)
        assertTrue(state.y in 0f..20f)
    }

    @Test
    fun predictionAppliesLearnedVelocity() {
        val filter = KalmanFilter2D(initialX = 0f, initialY = 0f)
        filter.update(measuredX = 10f, measuredY = 0f)
        val beforePrediction = filter.currentState()
        val prediction = filter.predict()

        assertTrue(prediction.x > beforePrediction.x)
    }
}
