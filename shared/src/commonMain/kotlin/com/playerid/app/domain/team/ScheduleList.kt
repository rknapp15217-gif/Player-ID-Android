package com.playerid.app.domain.team

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class ScheduleLabelPolicy(
    val monthLabels: List<String>,
    val amLabel: String,
    val pmLabel: String
) {
    init {
        require(monthLabels.size == 12) { "Schedule labels require all 12 months" }
    }

    companion object {
        val English = ScheduleLabelPolicy(
            monthLabels = listOf(
                "JAN", "FEB", "MAR", "APR", "MAY", "JUN",
                "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"
            ),
            amLabel = "AM",
            pmLabel = "PM"
        )
    }
}

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

fun scheduleGameItem(
    id: String,
    opponentName: String,
    gameLabel: String,
    scheduledStartMs: Long,
    locationName: String?,
    labelPolicy: ScheduleLabelPolicy = ScheduleLabelPolicy.English,
    utcOffsetMinutes: Int = 0
): ScheduleGameItem {
    val localStart = Instant.fromEpochMilliseconds(
        scheduledStartMs + utcOffsetMinutes * 60_000L
    ).toLocalDateTime(TimeZone.UTC)
    val monthLabel = labelPolicy.monthLabels[localStart.monthNumber - 1]
    val periodLabel = if (localStart.hour < 12) labelPolicy.amLabel else labelPolicy.pmLabel
    val twelveHour = when (val hour = localStart.hour % 12) {
        0 -> 12
        else -> hour
    }
    val timeLabel = "$twelveHour:${localStart.minute.toString().padStart(2, '0')} $periodLabel"

    return ScheduleGameItem(
        id = id,
        opponentName = opponentName,
        gameLabel = gameLabel,
        scheduledStartMs = scheduledStartMs,
        dateLabel = "$monthLabel\n${localStart.dayOfMonth.toString().padStart(2, '0')}",
        detailLabel = listOfNotNull(locationName, timeLabel).joinToString("  •  ")
    )
}
