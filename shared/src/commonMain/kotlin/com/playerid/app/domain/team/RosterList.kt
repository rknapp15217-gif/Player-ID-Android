package com.playerid.app.domain.team

data class RosterListState(
    val teamName: String,
    val players: List<PlayerProfile> = emptyList(),
    val searchQuery: String = "",
    val favoritePlayerIds: Set<String> = emptySet()
) {
    val visiblePlayers: List<PlayerProfile>
        get() {
            if (searchQuery.isBlank()) return players
            return players.filter { player ->
                player.name.contains(searchQuery, ignoreCase = true) ||
                    player.number.contains(searchQuery, ignoreCase = true)
            }
        }

    fun reduce(event: RosterListEvent): RosterListState = when (event) {
        is RosterListEvent.PlayersChanged -> copy(players = event.players)
        is RosterListEvent.SearchQueryChanged -> copy(searchQuery = event.query)
        is RosterListEvent.FavoriteToggled -> copy(
            favoritePlayerIds = if (event.playerId in favoritePlayerIds) {
                favoritePlayerIds - event.playerId
            } else {
                favoritePlayerIds + event.playerId
            }
        )
    }
}

sealed interface RosterListEvent {
    data class PlayersChanged(val players: List<PlayerProfile>) : RosterListEvent
    data class SearchQueryChanged(val query: String) : RosterListEvent
    data class FavoriteToggled(val playerId: String) : RosterListEvent
}