package com.playerid.app.domain.team

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
}