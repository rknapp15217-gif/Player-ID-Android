package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TeamManagementStateTest {
    @Test
    fun requestingDialogReplacesPreviousDialog() {
        val state = TeamManagementState()
            .reduce(TeamManagementEvent.DialogRequested(TeamManagementDialog.InviteTeam))
            .reduce(TeamManagementEvent.DialogRequested(TeamManagementDialog.LeaveTeam))

        assertEquals(TeamManagementDialog.LeaveTeam, state.activeDialog)
    }

    @Test
    fun editPlayerRequestCarriesPlayerId() {
        val state = TeamManagementState()
            .reduce(TeamManagementEvent.EditPlayerRequested("player-7"))

        assertEquals(TeamManagementDialog.EditPlayer, state.activeDialog)
        assertEquals("player-7", state.editingPlayerId)
    }

    @Test
    fun dialogRequestAndDismissalClearEditPayload() {
        val editing = TeamManagementState()
            .reduce(TeamManagementEvent.EditPlayerRequested("player-7"))
        val replaced = editing.reduce(
            TeamManagementEvent.DialogRequested(TeamManagementDialog.TeamSettings)
        )
        val dismissed = replaced.reduce(TeamManagementEvent.DialogDismissed)

        assertNull(replaced.editingPlayerId)
        assertNull(dismissed.activeDialog)
        assertNull(dismissed.editingPlayerId)
    }
}