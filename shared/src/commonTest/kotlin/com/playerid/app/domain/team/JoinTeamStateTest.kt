package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals

class JoinTeamStateTest {
    private val teams = listOf(
        JoinTeamItem("North Tigers", "#112233", 20),
        JoinTeamItem("South Bears", "#445566", 18),
        JoinTeamItem("West Tigers", "#778899", null)
    )

    @Test
    fun visibleTeamsExcludeSubscriptionsAndMatchSearchIgnoringCase() {
        val state = JoinTeamState(
            teams = teams,
            subscribedTeamNames = setOf("North Tigers"),
            searchQuery = "TIGERS"
        )

        assertEquals(listOf("West Tigers"), state.visibleTeams.map { it.name })
    }

    @Test
    fun reducerUpdatesStreamsWithoutLosingQuery() {
        val state = JoinTeamState(searchQuery = "Bears")
            .reduce(JoinTeamEvent.TeamsChanged(teams))
            .reduce(JoinTeamEvent.SubscriptionsChanged(setOf("North Tigers")))

        assertEquals("Bears", state.searchQuery)
        assertEquals(listOf("South Bears"), state.visibleTeams.map { it.name })
    }

    @Test
    fun blankSearchPreservesAvailableTeamOrder() {
        val state = JoinTeamState(
            teams = teams,
            subscribedTeamNames = setOf("South Bears")
        )

        assertEquals(listOf("North Tigers", "West Tigers"), state.visibleTeams.map { it.name })
    }
}