package com.playerid.app.domain.voice

import com.playerid.app.domain.team.PlayerProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import com.playerid.app.domain.team.LibraryActionRequest
import com.playerid.app.domain.team.answerRosterQuestion
import com.playerid.app.domain.team.parseRosterQuestion

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

    @Test
    fun drawerQuestionsReuseAskRosterAnswersWithinTheCameraTeam() {
        val roster = listOf(
            players[0].copy(position = "GK", academicYear = "Freshman"),
            players[1].copy(position = "Midfield", academicYear = "Senior"),
            player("other", "1", "Other Goalie", "Bears").copy(position = "Goalie", academicYear = "Freshman"),
            player("inactive", "2", "Inactive Goalie", "Tigers").copy(position = "G", academicYear = "Freshman", isActive = false)
        )
        for (text in listOf("Who are our goalies?", "How many goalies do we have?",
            "Who is our freshman goalie?", "Who are the seniors?", "How many senior midfielders do we have?",
            "What number does Jake wear?", "Who is wearing jersey number seven?",
            "Who is number 1 on Bears?")) {
            val result = assertIs<VoiceCommandDecision.Answer>(
                processor.process(listOf("spotter $text"), listOf("Tigers", "Bears"), "Tigers", roster), text)
            assertEquals(answerRosterQuestion(LibraryActionRequest.Roster(
                requireNotNull(parseRosterQuestion(text))), roster, "Tigers",
                compact = true, includeCount = text.startsWith("How many")), result.message)
            assertFalse(result.message.contains("Other Goalie"), text)
            assertFalse(result.message.contains("Inactive Goalie"), text)
        }

        val goalie = assertIs<VoiceCommandDecision.Answer>(
            processor.process(listOf("Who is our freshman goalie?"), emptyList(), "Tigers", roster))
        assertTrue(goalie.message.contains("Jake Wilson"))
        assertFalse(goalie.message.contains("Alex Rodriguez"))
        assertFalse(goalie.message.contains("Tigers"))
        assertFalse(goalie.message.contains("saved roster match"))
        val count = assertIs<VoiceCommandDecision.Answer>(
            processor.process(listOf("How many goalies do we have?"), emptyList(), "Tigers", roster))
        assertTrue(count.message.startsWith("1 player\n\n"))
        assertFalse(count.message.contains("Tigers"))
        assertFalse(count.message.contains("saved roster match"))
        assertIs<VoiceCommandDecision.Error>(
            processor.process(listOf("Who are our goalies?"), emptyList(), null, roster))
    }

    @Test
    fun moreThanTwoPlayersAreDisplayedButNotReadAloud() {
        for (size in listOf(1, 2, 3, 10, 11, 14)) {
            val roster = (1..size).map {
                player("id$it", "$it", "Goalie $it", "Tigers").copy(position = "Goalie")
            }
            val answer = assertIs<VoiceCommandDecision.Answer>(
                processor.process(listOf("Who are our goalies?"), emptyList(), "Tigers", roster))
            roster.forEach { assertTrue(answer.message.contains("${it.name} #${it.number}"), "Missing ${it.name} in $size matches") }
            assertFalse(answer.message.contains("more matches"))
            if (size <= 2) {
                assertEquals(answer.message, answer.speech)
            } else {
                assertEquals("$size players. See the roster for the list.", answer.speech)
                assertFalse(answer.speech.contains("Goalie 1"))
            }

        }
    }

    @Test
    fun drawerListsEverySophomoreWithoutReadingTheirNames() {
        val roster = (1..14).map {
            player("id$it", "$it", "Sophomore $it", "Tigers").copy(academicYear = "Sophomore")
        }
        val answer = assertIs<VoiceCommandDecision.Answer>(
            processor.process(listOf("Who are our sophomores?"), emptyList(), "Tigers", roster))
        assertEquals(14, answer.message.split("\n\n").size)
        roster.forEach { assertTrue(answer.message.contains("${it.name} #${it.number}")) }
        assertFalse(answer.message.contains("more matches"))
        assertFalse(answer.message.contains("Tigers"))
        assertEquals("14 players. See the roster for the list.", answer.speech)
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