package com.playerid.app.domain.team

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleImportTest {
    @Test
    fun parsesCsvFieldsInRequestedTimeZone() {
        val entries = parseScheduleCsv(
            csvText = """
                date,startTime,endTime,opponent,location,latitude,longitude,gameLabel
                2026-09-12,7:00 PM,9:00 PM,Pine-Richland,Home Field,40.6,-80.0,Home Opener
            """.trimIndent(),
            timeZoneId = "UTC"
        )

        val entry = entries.single()
        assertEquals("Pine-Richland", entry.opponent)
        assertEquals("Home Field", entry.locationName)
        assertEquals(40.6, entry.latitude)
        assertEquals(-80.0, entry.longitude)
        assertEquals("Home Opener", entry.gameLabel)
        assertEquals(Instant.parse("2026-09-12T19:00:00Z").toEpochMilliseconds(), entry.startMs)
        assertEquals(Instant.parse("2026-09-12T21:00:00Z").toEpochMilliseconds(), entry.endMs)
    }

    @Test
    fun supportsQuotedCommasAndFallbackDuration() {
        val entry = parseScheduleCsv(
            csvText = """
                date,startTime,endTime,opponent,location
                2026-09-12,19:15,,"Pine-Richland, JV","Home, Field"
            """.trimIndent(),
            timeZoneId = "UTC"
        ).single()

        assertEquals("Pine-Richland, JV", entry.opponent)
        assertEquals("Home, Field", entry.locationName)
        assertEquals(2L * 60L * 60L * 1000L, entry.endMs - entry.startMs)
    }

    @Test
    fun skipsRowsWithInvalidDateOrStartTime() {
        val entries = parseScheduleCsv(
            csvText = """
                date,startTime,endTime,opponent,location
                invalid,7:00 PM,9:00 PM,Tigers,Home
                2026-02-30,7:00 PM,9:00 PM,Bears,Home
                2026-09-12,25:00,9:00 PM,Lions,Home
            """.trimIndent(),
            timeZoneId = "UTC"
        )

        assertTrue(entries.isEmpty())
    }
}