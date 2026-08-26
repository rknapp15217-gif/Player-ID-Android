package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TeamCreationFormStateTest {
    @Test
    fun reducerUpdatesAllCreationFields() {
        val state = TeamCreationFormState()
            .reduce(TeamCreationEvent.TeamNameChanged("Tigers"))
            .reduce(TeamCreationEvent.SportSelected("Lacrosse"))
            .reduce(TeamCreationEvent.HomeJerseyColorSelected("#111827"))
            .reduce(TeamCreationEvent.AwayJerseyColorSelected("#FFFFFF"))

        assertEquals("Tigers", state.teamName)
        assertEquals("Lacrosse", state.sport)
        assertEquals("#111827", state.homeJerseyColor)
        assertEquals("#FFFFFF", state.awayJerseyColor)
    }

    @Test
    fun submissionRejectsBlankNameAndTrimsValidName() {
        val blankState = TeamCreationFormState(teamName = "   ")
        val validState = TeamCreationFormState(teamName = "  Tigers  ")

        assertFalse(blankState.canSubmit)
        assertNull(blankState.submission())
        assertTrue(validState.canSubmit)
        assertEquals("Tigers", validState.submission()?.teamName)
    }

    @Test
    fun similarTeamsRemainAdvisoryForSubmission() {
        val state = TeamCreationFormState(teamName = "Tigers FC")

        assertEquals(listOf("Tigers Club"), state.similarTeams(listOf("Tigers Club")).map { it.name })
        assertTrue(state.canSubmit)
    }
}