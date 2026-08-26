package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerFormStateTest {
    @Test
    fun submissionRequiresNameNumberAndPosition() {
        val incomplete = PlayerFormState.forNewPlayer("Tigers")
            .reduce(PlayerFormEvent.NameChanged("Alex"))
            .reduce(PlayerFormEvent.NumberChanged("7"))

        assertFalse(incomplete.canSubmit)
        assertNull(incomplete.submission())
        assertTrue(incomplete.reduce(PlayerFormEvent.PositionChanged("Forward")).canSubmit)
    }

    @Test
    fun reducerUpdatesAllFormFields() {
        val state = PlayerFormState()
            .reduce(PlayerFormEvent.NameChanged("Alex"))
            .reduce(PlayerFormEvent.NumberChanged("7"))
            .reduce(PlayerFormEvent.PositionChanged("Forward"))
            .reduce(PlayerFormEvent.TeamSelected("Tigers"))
            .reduce(PlayerFormEvent.AcademicYearSelected("Junior"))

        assertEquals("Alex", state.name)
        assertEquals("7", state.number)
        assertEquals("Forward", state.position)
        assertEquals("Tigers", state.teamName)
        assertEquals("Junior", state.academicYear)
    }

    @Test
    fun submissionTrimsFreeTextAndPreservesSelections() {
        val submission = PlayerFormState(
            name = "  Alex Morgan  ",
            number = "  12  ",
            position = "  Midfield  ",
            teamName = "Tigers",
            academicYear = "Senior"
        ).submission()

        assertEquals("Alex Morgan", submission?.name)
        assertEquals("12", submission?.number)
        assertEquals("Midfield", submission?.position)
        assertEquals("Tigers", submission?.teamName)
        assertEquals("Senior", submission?.academicYear)
    }

    @Test
    fun editingStateCopiesEditableProfileFields() {
        val profile = PlayerProfile(
            id = "player-1",
            number = "7",
            name = "Alex",
            position = "Forward",
            teamName = "Tigers",
            academicYear = "Junior",
            addedBy = "coach",
            createdAt = 10,
            updatedAt = 20
        )

        val state = PlayerFormState.forEditing(profile)

        assertEquals("Alex", state.name)
        assertEquals("7", state.number)
        assertEquals("Forward", state.position)
        assertEquals("Tigers", state.teamName)
        assertEquals("Junior", state.academicYear)
    }
}