package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TeamSelectionStateTest {
    @Test
    fun initialStateSupportsTeamAndCreateDialogDeepLinks() {
        val state = initialTeamSelectionState(
            initialTeamName = "Tigers",
            startCreateTeamInitially = true
        )

        assertEquals("Tigers", state.selectedTeamName)
        assertEquals(TeamSelectionDialog.CreateTeam, state.activeDialog)
    }

    @Test
    fun selectingAndClearingTeamPreservesOtherState() {
        val selected = TeamSelectionState()
            .reduce(TeamSelectionEvent.TeamSelected("Bears"))
        val cleared = selected.reduce(TeamSelectionEvent.TeamCleared)

        assertEquals("Bears", selected.selectedTeamName)
        assertNull(cleared.selectedTeamName)
    }

    @Test
    fun createdTeamDeepLinksToRosterOnlyWhenRequested() {
        val regular = TeamSelectionState().reduce(
            TeamSelectionEvent.TeamCreated("Tigers", openRosterAfterCreate = false)
        )
        val onboarding = TeamSelectionState().reduce(
            TeamSelectionEvent.TeamCreated("Tigers", openRosterAfterCreate = true)
        )

        assertNull(regular.selectedTeamName)
        assertFalse(regular.shouldOpenRoster(openRosterInitially = false))
        assertEquals("Tigers", onboarding.selectedTeamName)
        assertEquals("Tigers", onboarding.createdTeamName)
        assertTrue(onboarding.shouldOpenRoster(openRosterInitially = false))
    }

    @Test
    fun importedTeamBecomesSelectedAndDismissesDialog() {
        val state = TeamSelectionState(activeDialog = TeamSelectionDialog.TeamSnapImport)
            .reduce(TeamSelectionEvent.TeamImported("Imported Tigers"))

        assertEquals("Imported Tigers", state.selectedTeamName)
        assertNull(state.activeDialog)
    }
}