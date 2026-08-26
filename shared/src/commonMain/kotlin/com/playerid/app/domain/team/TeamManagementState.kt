package com.playerid.app.domain.team

enum class TeamManagementDialog {
    AddPlayer,
    EditPlayer,
    TeamSettings,
    LeaveTeam,
    InviteTeam,
    ImportRoster,
    ImportSchedule
}

data class TeamManagementState(
    val activeDialog: TeamManagementDialog? = null,
    val editingPlayerId: String? = null
) {
    fun reduce(event: TeamManagementEvent): TeamManagementState = when (event) {
        is TeamManagementEvent.DialogRequested -> copy(
            activeDialog = event.dialog,
            editingPlayerId = null
        )
        is TeamManagementEvent.EditPlayerRequested -> copy(
            activeDialog = TeamManagementDialog.EditPlayer,
            editingPlayerId = event.playerId
        )
        TeamManagementEvent.DialogDismissed -> TeamManagementState()
    }
}

sealed interface TeamManagementEvent {
    data class DialogRequested(val dialog: TeamManagementDialog) : TeamManagementEvent
    data class EditPlayerRequested(val playerId: String) : TeamManagementEvent
    data object DialogDismissed : TeamManagementEvent
}