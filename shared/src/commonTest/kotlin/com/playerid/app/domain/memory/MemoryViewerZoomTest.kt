package com.playerid.app.domain.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MemoryViewerZoomTest {
    @Test
    fun changesZoomMultiplicatively() {
        assertEquals(3f, memoryViewerZoomAfterGesture(2f, 1.5f))
        assertEquals(1.5f, memoryViewerZoomAfterGesture(3f, 0.5f))
        assertEquals(2f, memoryViewerZoomAfterGesture(2f, 1f))
    }

    @Test
    fun clampsToAndroidViewerLimits() {
        assertEquals(4f, memoryViewerZoomAfterGesture(3f, 2f))
        assertEquals(1f, memoryViewerZoomAfterGesture(2f, 0.1f))
        assertEquals(4f, memoryViewerZoomAfterGesture(4f, Float.MAX_VALUE))
    }

    @Test
    fun rejectsInvalidStateAndGestureValues() {
        for (value in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0f, -1f)) {
            assertFailsWith<IllegalArgumentException> { memoryViewerZoomAfterGesture(1f, value) }
            assertFailsWith<IllegalArgumentException> { memoryViewerZoomAfterGesture(value, 1f) }
        }
        assertFailsWith<IllegalArgumentException> { memoryViewerZoomAfterGesture(4.1f, 1f) }
        assertFailsWith<IllegalArgumentException> { memoryViewerZoomAfterGesture(0.9f, 1f) }
    }
}
