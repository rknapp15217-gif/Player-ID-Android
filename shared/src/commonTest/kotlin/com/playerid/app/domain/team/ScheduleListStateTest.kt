package com.playerid.app.domain.team

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleListStateTest {
    private val games = listOf(
        game("oldest", "Tigers", "", 100L),
        game("recent", "Bears", "Senior Night", 200L),
        game("boundary", "Lions", "Home Opener", 300L),
        game("future", "Eagles", "", 400L)
    )

    @Test
    fun searchMatchesOpponentOrGameLabel() {
        val byOpponent = ScheduleListState(games, "bears")
        val byLabel = ScheduleListState(games, "opener")

        assertEquals(listOf("recent"), byOpponent.visibleGames.map { it.id })
        assertEquals(listOf("boundary"), byLabel.visibleGames.map { it.id })
    }

    @Test
    fun partitionPreservesUpcomingOrderAndReversesPastOrder() {
        val state = ScheduleListState(games)

        assertEquals(listOf("boundary", "future"), state.upcomingGames(nowMs = 300L).map { it.id })
        assertEquals(listOf("recent", "oldest"), state.pastGames(nowMs = 300L).map { it.id })
    }

    @Test
    fun reducerUpdatesSearchWithoutReplacingGames() {
        val state = ScheduleListState(games)
            .reduce(ScheduleListEvent.SearchQueryChanged("Tigers"))

        assertEquals(games, state.games)
        assertEquals("Tigers", state.searchQuery)
    }

    @Test
    fun blankGameLabelUsesOpponentTitle() {
        assertEquals("vs Tigers", games.first().title)
        assertEquals("Senior Night", games[1].title)
    }

    @Test
    fun formatterHandlesMidnightNoonAndLocationInUtc() {
        val midnight = formatted("2026-08-26T00:05:00Z", location = "Home Field")
        val noon = formatted("2026-08-26T12:30:00Z")

        assertEquals("AUG\n26", midnight.dateLabel)
        assertEquals("Home Field  •  12:05 AM", midnight.detailLabel)
        assertEquals("12:30 PM", noon.detailLabel)
    }

    @Test
    fun formatterAppliesTimeZoneBeforeBuildingDateLabel() {
        val item = formatted(
            instant = "2026-08-26T02:15:00Z",
            utcOffsetMinutes = -4 * 60
        )

        assertEquals("AUG\n25", item.dateLabel)
        assertEquals("10:15 PM", item.detailLabel)
    }

    @Test
    fun formatterUsesInjectedLocaleLabels() {
        val policy = ScheduleLabelPolicy(
            monthLabels = (1..12).map { "M$it" },
            amLabel = "a",
            pmLabel = "p"
        )

        val item = formatted("2026-08-26T17:04:00Z", labelPolicy = policy)

        assertEquals("M8\n26", item.dateLabel)
        assertEquals("5:04 p", item.detailLabel)
    }

    private fun game(
        id: String,
        opponentName: String,
        gameLabel: String,
        scheduledStartMs: Long
    ) = ScheduleGameItem(
        id = id,
        opponentName = opponentName,
        gameLabel = gameLabel,
        scheduledStartMs = scheduledStartMs,
        dateLabel = "DATE",
        detailLabel = "DETAIL"
    )

    private fun formatted(
        instant: String,
        location: String? = null,
        labelPolicy: ScheduleLabelPolicy = ScheduleLabelPolicy.English,
        utcOffsetMinutes: Int = 0
    ) = scheduleGameItem(
        id = "game",
        opponentName = "Tigers",
        gameLabel = "",
        scheduledStartMs = Instant.parse(instant).toEpochMilliseconds(),
        locationName = location,
        labelPolicy = labelPolicy,
        utcOffsetMinutes = utcOffsetMinutes
    )
}
