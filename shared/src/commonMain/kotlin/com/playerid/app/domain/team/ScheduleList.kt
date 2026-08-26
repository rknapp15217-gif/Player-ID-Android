package com.playerid.app.domain.team

data class ScheduleGameItem(
    val id: String,
    val opponentName: String,
    val gameLabel: String,
    val scheduledStartMs: Long,
    val dateLabel: String,
    val detailLabel: String
) {
    val title: String
        get() = gameLabel.ifBlank { "vs $opponentName" }
}

data class ScheduleListState(
    val games: List<ScheduleGameItem> = emptyList(),
    val searchQuery: String = ""
) {
    val visibleGames: List<ScheduleGameItem>
        get() {
            if (searchQuery.isBlank()) return games
            return games.filter { game ->
                game.opponentName.contains(searchQuery, ignoreCase = true) ||
                    game.gameLabel.contains(searchQuery, ignoreCase = true)
            }
        }

    fun upcomingGames(nowMs: Long): List<ScheduleGameItem> {
        return visibleGames.filter { it.scheduledStartMs >= nowMs }
    }

    fun pastGames(nowMs: Long): List<ScheduleGameItem> {
        return visibleGames.filter { it.scheduledStartMs < nowMs }.reversed()
    }

    fun reduce(event: ScheduleListEvent): ScheduleListState = when (event) {
        is ScheduleListEvent.GamesChanged -> copy(games = event.games)
        is ScheduleListEvent.SearchQueryChanged -> copy(searchQuery = event.query)
    }
}

sealed interface ScheduleListEvent {
    data class GamesChanged(val games: List<ScheduleGameItem>) : ScheduleListEvent
    data class SearchQueryChanged(val query: String) : ScheduleListEvent
}