package com.playerid.app.domain.voice

import com.playerid.app.domain.team.PlayerProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class VoiceCommandProcessorTest {
    private val processor = VoiceCommandProcessor()
    private val players = listOf(
        player("one", "7", "Jake Wilson", "Tigers"),
        player("two", "23", "Alex Rodriguez", "Tigers")
    )

    @Test
    fun captureHasPriorityWithoutWakeWord() {
        assertIs<VoiceCommandDecision.Capture>(processor.process(listOf("finish"), emptyList(), null, emptyList()))
    }

    @Test
    fun switchesToNamedTeam() {
        val decision = processor.process(listOf("spotter switch team Tigers"), listOf("Tigers"), null, players)
        assertEquals("Tigers", assertIs<VoiceCommandDecision.SwitchTeam>(decision).teamName)
    }

    @Test
    fun alternateHypothesisCanMatchJerseyNumber() {
        val decision = processor.process(listOf("spotter player", "number seven"), listOf("Tigers"), "Tigers", players)
        assertEquals("one", assertIs<VoiceCommandDecision.Match>(decision).players.single().id)
    }

    @Test
    fun fuzzyNameMatchesRosterPlayer() {
        val decision = processor.process(listOf("spotter jake wilson"), listOf("Tigers"), "Tigers", players)
        assertEquals("one", assertIs<VoiceCommandDecision.Match>(decision).players.single().id)
    }

    @Test
    fun missingTeamReturnsExistingError() {
        val decision = processor.process(listOf("spotter seven"), listOf("Tigers"), null, players)
        assertEquals("Please select a team first", assertIs<VoiceCommandDecision.Error>(decision).message)
    }

    private fun player(id: String, number: String, name: String, team: String) = PlayerProfile(
        id = id,
        number = number,
        name = name,
        position = "",
        teamName = team,
        academicYear = "",
        createdAt = 0L,
        updatedAt = 0L
    )
}