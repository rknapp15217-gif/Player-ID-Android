package com.playerid.app.ui.components

import kotlin.test.*

class MemoryFilterScrollTest {
    @Test
    fun revealsOnlyAtFullPullThreshold() {
        val gestures = MemoryFilterScrollGestures(80f, 8f)
        assertNull(gestures.drag(-7f))
        assertEquals(false, gestures.drag(-1f))
        gestures.endGesture()
        assertNull(gestures.drag(20f))
        assertNull(gestures.drag(59f))
        assertEquals(true, gestures.drag(1f))
    }

    @Test
    fun separateSmallPullsDoNotAccumulate() {
        val gestures = MemoryFilterScrollGestures(80f, 8f)
        assertNull(gestures.drag(60f))
        gestures.endGesture()
        assertNull(gestures.drag(60f))
        assertEquals(true, gestures.drag(20f))
    }

    @Test
    fun reversingDirectionResetsDistance() {
        val gestures = MemoryFilterScrollGestures(80f, 8f)
        assertNull(gestures.drag(70f))
        assertEquals(false, gestures.drag(-10f))
        assertNull(gestures.drag(70f))
        assertEquals(true, gestures.drag(10f))
    }

    @Test
    fun thresholdsScaleWithScreenDensity() {
        val gestures = MemoryFilterScrollGestures(240f, 24f)
        assertNull(gestures.drag(239f))
        assertEquals(true, gestures.drag(1f))
        assertNull(gestures.drag(-23f))
        assertEquals(false, gestures.drag(-1f))
    }
}
