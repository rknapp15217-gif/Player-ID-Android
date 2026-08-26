package com.playerid.app

import com.playerid.app.domain.team.parseScheduleText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleImportParserTest {
    @Test
    fun parsesCsvSchedule() {
        val entries = parseScheduleText(
            """
            date,startTime,endTime,opponent,location,latitude,longitude,gameLabel
            2026-09-12,7:00 PM,9:00 PM,Pine-Richland,Home Field,,,vs Pine-Richland
            """.trimIndent()
        )

        assertEquals(1, entries.size)
        assertEquals("Pine-Richland", entries.single().opponent)
        assertEquals("Home Field", entries.single().locationName)
    }

    @Test
    fun parsesOcrOrWebsiteScheduleText() {
        val entries = parseScheduleText(
            """
            UPCOMING
            May 10 vs Pine-Richland 7:00 PM
            May 13 @ Seneca Valley 7:00 PM
            """.trimIndent()
        )

        assertEquals(2, entries.size)
        assertTrue(entries.any { it.opponent == "Pine-Richland" })
        assertTrue(entries.any { it.opponent == "Seneca Valley" })
    }

    @Test
    fun parsesWebsiteScheduleRenderedAsColumns() {
        val entries = parseScheduleText(
            """
            DATES
            May 10
            May 13
            May 17
            OPPONENTS
            vs Pine-Richland
            @ Seneca Valley
            vs Wexford
            TIMES
            7:00 PM
            7:30 PM
            6:00 PM
            """.trimIndent()
        )

        assertEquals(3, entries.size)
        assertEquals(listOf("Pine-Richland", "Seneca Valley", "Wexford"), entries.map { it.opponent })
    }

    @Test
    fun parsesScreenshotWithMonthAndDayOnSeparateLines() {
        val entries = parseScheduleText(
            """
            AUG
            30
            7:00 PM
            vs Upper St. Clair
            SEP
            6
            6:30 PM
            @ Canon-McMillan
            """.trimIndent()
        )

        assertEquals(2, entries.size)
        assertTrue(entries.any { it.opponent == "Upper St. Clair" })
        assertTrue(entries.any { it.opponent == "Canon-McMillan" })
    }

    @Test
    fun ignoresDatesTimesAndMatchupHeadlinesInPageMenus() {
        val entries = parseScheduleText(
            """
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
            """.trimIndent()
        )

        assertEquals(2, entries.size)
        assertEquals(setOf("Pine-Richland", "Seneca Valley"), entries.map { it.opponent }.toSet())
    }

    @Test
    fun parsesNorthAlleghenyScheduleTableRows() {
        val entries = parseScheduleText(
            """
            NEXT BROADCAST - FRI, AUG 28 @ 7:30PM
            Football: NA vs. North Hills
            2026-2027 Football Schedule
            Sat, 8/22/2026
            10:00am
            MCDOWELL (Scrimmage) at Newman Stadium »
            -
            Fri, 8/28/2026
            7:30pm
            NORTH HILLS at Newman Stadium »
            Fri, 9/11/2026
            7:30pm
            Military/First Responders Night WOODLAND HILLS at Newman Stadium »
            Fri, 9/25/2026
            7:30pm
            @ Pine-Richland at Pine-Richland High School - Football Stadium »
            Copyright 2013 North Allegheny School District
            """.trimIndent()
        )

        assertEquals(4, entries.size)
        assertEquals(
            listOf("MCDOWELL", "NORTH HILLS", "WOODLAND HILLS", "Pine-Richland"),
            entries.map { it.opponent }
        )
        assertEquals("Newman Stadium", entries.first().locationName)
    }

    @Test
    fun preservesVarsityAndJvGamesAtTheSameTime() {
        val entries = parseScheduleText(
            """
            SCHEDULE LEVEL: Varsity
            Sat, 8/22/2026
            10:00am
            MCDOWELL at Newman Stadium
            SCHEDULE LEVEL: JV
            Sat, 8/22/2026
            10:00am
            MCDOWELL at Newman Stadium
            SCHEDULE LEVEL: JV-B
            Sat, 8/22/2026
            12:00pm
            MCDOWELL at Newman Stadium
            """.trimIndent()
        )

        assertEquals(3, entries.size)
        assertEquals(
            setOf("Varsity vs MCDOWELL", "JV vs MCDOWELL", "JV-B vs MCDOWELL"),
            entries.map { it.gameLabel }.toSet()
        )
    }
}