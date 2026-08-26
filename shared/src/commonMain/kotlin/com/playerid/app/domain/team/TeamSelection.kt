package com.playerid.app.domain.team

data class TeamSelectionItem(
    val name: String,
    val homeColorHex: String,
    val awayColorHex: String,
    val playerCount: Int? = null
)

enum class TeamSelectionDialog {
    CreateTeam,
    JoinTeam,
    TeamSnapImport
}

data class TeamSelectionState(
    val selectedTeamName: String? = null,
    val createdTeamName: String? = null,
    val activeDialog: TeamSelectionDialog? = null
) {
    fun shouldOpenRoster(openRosterInitially: Boolean): Boolean {
        return openRosterInitially || (
            createdTeamName != null && createdTeamName == selectedTeamName
        )
    }

    fun reduce(event: TeamSelectionEvent): TeamSelectionState = when (event) {
        is TeamSelectionEvent.TeamSelected -> copy(selectedTeamName = event.teamName)
        TeamSelectionEvent.TeamCleared -> copy(selectedTeamName = null)
        is TeamSelectionEvent.DialogRequested -> copy(activeDialog = event.dialog)
        TeamSelectionEvent.DialogDismissed -> copy(activeDialog = null)
        is TeamSelectionEvent.TeamCreated -> copy(
            selectedTeamName = if (event.openRosterAfterCreate) event.teamName else selectedTeamName,
            createdTeamName = if (event.openRosterAfterCreate) event.teamName else createdTeamName,
            activeDialog = null
        )
        is TeamSelectionEvent.TeamImported -> copy(
            selectedTeamName = event.teamName,
            activeDialog = null
        )
    }
}

fun initialTeamSelectionState(
    initialTeamName: String?,
    startCreateTeamInitially: Boolean
): TeamSelectionState = TeamSelectionState(
    selectedTeamName = initialTeamName,
    activeDialog = if (startCreateTeamInitially) TeamSelectionDialog.CreateTeam else null
)

sealed interface TeamSelectionEvent {
    data class TeamSelected(val teamName: String) : TeamSelectionEvent
    data object TeamCleared : TeamSelectionEvent
    data class DialogRequested(val dialog: TeamSelectionDialog) : TeamSelectionEvent
    data object DialogDismissed : TeamSelectionEvent
    data class TeamCreated(
        val teamName: String,
        val openRosterAfterCreate: Boolean
    ) : TeamSelectionEvent
    data class TeamImported(val teamName: String) : TeamSelectionEvent
}