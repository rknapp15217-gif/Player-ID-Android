package com.playerid.app.domain.recognition

import com.playerid.app.domain.team.testPlayer
import com.playerid.app.domain.team.testTeam
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JerseyRecognitionTest {
    private val team = testTeam().copy(homeJerseyColor = "#C8102E", awayJerseyColor = "#FFFFFF")
    private val roster = listOf(testPlayer("a", "7"), testPlayer("b", "10"), testPlayer("c", "12"))

    private fun read(number: String, color: String?, confidence: Float = 0.9f, at: Long = 0) =
        JerseyObservation(number, color, confidence, at)

    @Test
    fun numbersAreNormalized() {
        assertEquals("7", normalizeJerseyNumber("07"))
        assertEquals("10", normalizeJerseyNumber("1O"))
        assertEquals("0", normalizeJerseyNumber("00"))
        assertEquals(null, normalizeJerseyNumber("123"))
        assertEquals(null, normalizeJerseyNumber("AB"))
    }

    @Test
    fun colorDistanceSeparatesTeamsFromLookalikes() {
        assertEquals(0.0, jerseyColorDistance("#C8102E", "#C8102E"))
        assertTrue(jerseyColorDistance("#C8102E", "#B01030")!! < 110)
        assertTrue(jerseyColorDistance("#C8102E", "#1976D2")!! > 110)
        assertEquals(null, jerseyColorDistance("red", "#FFFFFF"))
    }

    @Test
    fun repeatedReadsInTeamColorMatchThePlayer() {
        val matches = matchJerseyPlayers(
            listOf(read("7", "#C41230", at = 0), read("7", "#BE1A33", at = 500), read("7", "#C8102E", at = 1000)),
            roster, team
        )
        val match = assertNotNull(matches.singleOrNull())
        assertEquals("a", match.player.id)
        assertTrue(match.colorVerified)
        assertEquals(3, match.frames)
    }

    @Test
    fun sameNumberOnTheOpponentColorIsNotCredited() {
        val matches = matchJerseyPlayers(
            listOf(read("7", "#1976D2"), read("7", "#1A73CC"), read("7", "#1B70C8")), roster, team
        )
        assertTrue(matches.isEmpty())
    }

    @Test
    fun singleOrLowConfidenceReadsAreIgnored() {
        assertTrue(matchJerseyPlayers(listOf(read("10", "#C8102E")), roster, team).isEmpty())
        assertTrue(matchJerseyPlayers(listOf(read("10", "#C8102E", 0.3f), read("10", "#C8102E", 0.4f)), roster, team).isEmpty())
    }

    @Test
    fun unknownColorStillCountsAndIsNotMarkedVerified() {
        val match = matchJerseyPlayers(listOf(read("12", null), read("12", null)), roster, team).single()
        assertEquals(false, match.colorVerified)
    }

    @Test
    fun duplicateRosterNumbersAreAmbiguousAndNotSuggested() {
        val twins = roster + testPlayer("d", "7")
        assertTrue(matchJerseyPlayers(listOf(read("7", null), read("7", null)), twins, team).isEmpty())
    }

    @Test
    fun strongestMatchComesFirst() {
        val matches = matchJerseyPlayers(
            listOf(read("7", null, 0.6f), read("7", null, 0.6f), read("10", null, 0.95f), read("10", null, 0.95f)),
            roster, team
        )
        assertEquals(listOf("b", "a"), matches.map { it.player.id })
    }
}
