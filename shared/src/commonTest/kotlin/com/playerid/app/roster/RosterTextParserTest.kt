package com.playerid.app.roster

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RosterTextParserTest {
    @Test
    fun parsesNumberNameAndAcademicYear() {
        val result = parseRosterText(listOf("#12 Jordan Smith Jr"))

        assertEquals(1, result.candidates.size)
        assertEquals("12", result.candidates.single().number)
        assertEquals("Jordan Smith", result.candidates.single().name)
        assertEquals("Junior", result.candidates.single().academicYear)
    }

    @Test
    fun combinesAdjacentNameAndDetailLines() {
        val result = parseRosterText(
            lines = listOf("Taylor Morgan", "24 - GK"),
            blockLines = listOf(listOf("Taylor Morgan", "24 - GK"))
        )

        assertEquals(1, result.candidates.size)
        assertEquals("Goalie", result.candidates.single().position)
    }

    @Test
    fun rejectsNavigationAndUnnumberedNoise() {
        val result = parseRosterText(listOf("Team Settings", "Search Players", "Taylor Morgan"))

        assertTrue(result.candidates.isEmpty())
    }
}
