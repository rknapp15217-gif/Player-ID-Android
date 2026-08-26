package com.playerid.app.domain.team

data class JoinTeamItem(
    val name: String,
    val colorHex: String,
    val playerCount: Int? = null
)

data class JoinTeamState(
    val teams: List<JoinTeamItem> = emptyList(),
    val subscribedTeamNames: Set<String> = emptySet(),
    val searchQuery: String = ""
) {
    val visibleTeams: List<JoinTeamItem>
        get() = teams
            .filter { it.name !in subscribedTeamNames }
            .filter { it.name.contains(searchQuery, ignoreCase = true) }

    fun reduce(event: JoinTeamEvent): JoinTeamState = when (event) {
        is JoinTeamEvent.TeamsChanged -> copy(teams = event.teams)
        is JoinTeamEvent.SubscriptionsChanged -> copy(subscribedTeamNames = event.teamNames)
        is JoinTeamEvent.SearchQueryChanged -> copy(searchQuery = event.query)
    }
}

sealed interface JoinTeamEvent {
    data class TeamsChanged(val teams: List<JoinTeamItem>) : JoinTeamEvent
    data class SubscriptionsChanged(val teamNames: Set<String>) : JoinTeamEvent
    data class SearchQueryChanged(val query: String) : JoinTeamEvent
}