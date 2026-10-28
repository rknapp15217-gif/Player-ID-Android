package com.playerid.app.domain.team

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScheduleImportSourcesTest {
    @Test
    fun formatsStartLikeAndroidReview() {
        val zone = TimeZone.UTC
        fun ms(hour: Int, minute: Int) = LocalDateTime(2026, 10, 20, hour, minute).toInstant(zone).toEpochMilliseconds()
        assertEquals("Oct 20, 6:05 PM", formatScheduleImportStart(ms(18, 5), zone))
        assertEquals("Oct 20, 12:00 AM", formatScheduleImportStart(ms(0, 0), zone))
        assertEquals("Oct 20, 12:30 PM", formatScheduleImportStart(ms(12, 30), zone))
    }

    @Test
    fun onlyGamesWithOpponentAndPositiveLengthAreImportable() {
        val game = ScheduleImportEntry("", "Tigers", 1_000, 2_000, null, null, null)
        assertTrue(game.isImportable())
        assertFalse(game.copy(endMs = 1_000).isImportable())
        assertFalse(game.copy(opponent = " ").isImportable())
    }

    @Test
    fun recognizedScheduleRowsParse() {
        val text = listOf("10/20/2026 6:00 PM vs Tigers at Main Field", "10/27/2026 7:30 PM at Lions").joinToString("\n")
        val entries = parseScheduleText(text, "UTC", 2026)
        assertEquals(2, entries.size, entries.toString())
    }
}
