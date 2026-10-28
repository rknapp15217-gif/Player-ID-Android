package com.playerid.app.domain.camera

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CameraZoomTest {
    @Test
    fun pinchChangesZoomMultiplicatively() {
        assertEquals(3.0, cameraZoomAfterGesture(2.0, 1.5, 1.0, 8.0))
        assertEquals(1.5, cameraZoomAfterGesture(3.0, 0.5, 1.0, 8.0))
        assertEquals(3.0, cameraZoomAfterGesture(3.0, 1.0, 1.0, 8.0))
    }

    @Test
    fun clampsToActualDeviceLimitsIncludingFixedZoom() {
        assertEquals(8.0, cameraZoomAfterGesture(7.0, 2.0, 1.0, 8.0))
        assertEquals(1.0, cameraZoomAfterGesture(2.0, 0.1, 1.0, 8.0))
        assertEquals(1.0, cameraZoomAfterGesture(1.0, 2.0, 1.0, 1.0))
        assertEquals(0.5, cameraZoomAfterGesture(1.0, 0.1, 0.5, 5.0))
        assertEquals(8.0, cameraZoomAfterGesture(Double.MAX_VALUE, 2.0, 1.0, 8.0))
    }

    @Test
    fun invalidGestureOrDeviceValuesAreRejected() {
        for (value in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, -1.0)) {
            assertFailsWith<IllegalArgumentException> { cameraZoomAfterGesture(1.0, value, 1.0, 8.0) }
            assertFailsWith<IllegalArgumentException> { cameraZoomAfterGesture(value, 1.1, 1.0, 8.0) }
            assertFailsWith<IllegalArgumentException> { cameraZoomAfterGesture(1.0, 1.1, value, 8.0) }
            assertFailsWith<IllegalArgumentException> { cameraZoomAfterGesture(1.0, 1.1, 1.0, value) }
        }
        assertFailsWith<IllegalArgumentException> { cameraZoomAfterGesture(1.0, 1.1, 4.0, 2.0) }
    }
}
