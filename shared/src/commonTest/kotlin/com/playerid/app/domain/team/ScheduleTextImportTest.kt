package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleTextImportTest {
    @Test
    fun parsesOcrRowsAndColumnLayouts() {
        val rows = parseScheduleText(
            rawText = """
                AUG
                30
                7:00 PM
                vs Upper St. Clair
                SEP
                6
                6:30 PM
                @ Canon-McMillan
            """.trimIndent(),
            timeZoneId = "UTC",
            currentYear = 2026
        )
        val columns = parseScheduleText(
            rawText = """
                DATES
                May 10
                May 13
                OPPONENTS
                vs Pine-Richland
                @ Seneca Valley
                TIMES
                7:00 PM
                7:30 PM
            """.trimIndent(),
            timeZoneId = "UTC",
            currentYear = 2026
        )

        assertEquals(listOf("Upper St. Clair", "Canon-McMillan"), rows.map { it.opponent })
        assertEquals(listOf("Pine-Richland", "Seneca Valley"), columns.map { it.opponent })
    }

    @Test
    fun ignoresPageMenuDatesWithoutExplicitMatchups() {
        val entries = parseScheduleText(
            rawText = """
                NEWS
                August 24, 2026
                8:45 PM
                Tigers vs Lions
                Teams
                Roster
                Schedule
                May 10 vs Pine-Richland 7:00 PM
                May 13 @ Seneca Valley 7:30 PM
                Copyright August 20, 2026
            """.trimIndent(),
            timeZoneId = "UTC",
            currentYear = 2026
        )

        assertEquals(setOf("Pine-Richland", "Seneca Valley"), entries.map { it.opponent }.toSet())
    }

    @Test
    fun parsesNorthAlleghenyRowsAndPreservesScheduleLevels() {
        val entries = parseScheduleText(
            rawText = """
                SCHEDULE LEVEL: Varsity
                Sat, 8/22/2026
                10:00am
                MCDOWELL (Scrimmage) at Newman Stadium »
                SCHEDULE LEVEL: JV
                Sat, 8/22/2026
                10:00am
                MCDOWELL at Newman Stadium »
                Fri, 9/25/2026
                7:30pm
                @ Pine-Richland at Pine-Richland High School - Football Stadium »
            """.trimIndent(),
            timeZoneId = "America/New_York",
            currentYear = 2026
        )

        assertEquals(listOf("MCDOWELL", "MCDOWELL", "Pine-Richland"), entries.map { it.opponent })
        assertEquals(listOf("Varsity vs MCDOWELL", "JV vs MCDOWELL", "JV @ Pine-Richland"), entries.map { it.gameLabel })
        assertEquals("Newman Stadium", entries.first().locationName)
    }
}