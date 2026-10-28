package com.playerid.app.domain.team

import kotlin.test.*

class ScheduleRelatedMemoriesTest {
    private val games = listOf(ActionScheduleGame("next", "Tigers", "Mt. Lebanon", 1000))

    @Test
    fun matchesOpponentAliasesOnlyWithinTheAnsweredTeam() {
        assertTrue(matchesScheduleOpponent(games, "Tigers", "Mount Lebanon", listOf("Mt. Lebanon")))
        assertTrue(matchesScheduleOpponent(games, "tigers", "MT LEBANON", listOf("Mt. Lebanon")))
        assertFalse(matchesScheduleOpponent(games, "Eagles", "Mt. Lebanon", listOf("Mt. Lebanon")))
        assertFalse(matchesScheduleOpponent(games, "Tigers", "Fox Chapel", listOf("Mt. Lebanon", "Fox Chapel")))
    }

    @Test
    fun noGameOrMissingAssignmentDoesNotShowUnrelatedMemories() {
        assertFalse(matchesScheduleOpponent(emptyList(), "Tigers", "Mt. Lebanon", emptyList()))
        assertFalse(matchesScheduleOpponent(games, null, "Mt. Lebanon", emptyList()))
        assertFalse(matchesScheduleOpponent(games, "Tigers", null, emptyList()))
    }

    @Test
    fun supportsPunctuationAndUnambiguousSavedAbbreviations() {
        val answer = listOf(ActionScheduleGame("next", "Tigers", "Pine-Richland", 1000))
        assertTrue(matchesScheduleOpponent(answer, "Tigers", "Pine Richland", listOf("Pine-Richland")))
        assertTrue(matchesScheduleOpponent(answer, "Tigers", "PR", listOf("Pine-Richland")))
        assertFalse(matchesScheduleOpponent(answer, "Tigers", "PR", listOf("Pine-Richland", "Penn Ridge")))
    }
}
