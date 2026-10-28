package com.playerid.app.domain.memory

import kotlin.test.*

class VideoTrimmingTest {
    @Test
    fun fullRangeAndMiddleSelectionHaveExactMillisecondBoundaries() {
        assertEquals(VideoTrimRange(0, 10_000), videoTrimRange(0f, 1f, 10_000))
        val range = videoTrimRange(0.2f, 0.8f, 10_000)
        assertEquals(VideoTrimRange(2_000, 8_000), range)
        assertEquals(6_000, range.durationMillis)
        assertEquals(VideoTrimRange(0, 1), videoTrimRange(0f, 1f, 1))
    }

    @Test
    fun zeroReversedOutOfBoundsAndNonFiniteSelectionsAreRejected() {
        for ((start, end) in listOf(
            0f to 0f, 1f to 1f, 0.8f to 0.2f, -0.1f to 0.5f, 0.2f to 1.1f,
            Float.NaN to 1f, 0f to Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY to 1f
        )) {
            assertFailsWith<IllegalArgumentException> { videoTrimRange(start, end, 10_000) }
        }
        assertFailsWith<IllegalArgumentException> { videoTrimRange(0f, 1f, 0) }
        assertFailsWith<IllegalArgumentException> { videoTrimRange(0f, 1f, -1) }
        assertFailsWith<IllegalArgumentException> { videoTrimRange(0.5f, 0.50001f, 1) }
    }

    @Test
    fun nativeExportRangesMustFitTheSource() {
        VideoTrimRange(1, 2).validate(2)
        for (range in listOf(VideoTrimRange(-1, 2), VideoTrimRange(2, 2), VideoTrimRange(2, 1), VideoTrimRange(0, 3))) {
            assertFailsWith<IllegalArgumentException> { range.validate(2) }
        }
        assertFailsWith<IllegalArgumentException> { VideoTrimRange(0, 1).validate(0) }
    }

    @Test
    fun timestampsShowMinutesSecondsAndMillisecondsWithoutLosingPrecision() {
        assertEquals("0:00.000", videoTrimTimestamp(0))
        assertEquals("0:00.001", videoTrimTimestamp(1))
        assertEquals("1:01.234", videoTrimTimestamp(61_234))
        assertEquals("60:00.000", videoTrimTimestamp(3_600_000))
        assertFailsWith<IllegalArgumentException> { videoTrimTimestamp(-1) }
    }
}
