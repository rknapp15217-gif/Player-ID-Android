package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RosterListStateTest {
    private val players = listOf(
        player(id = "1", number = "12", name = "Alex Morgan"),
        player(id = "2", number = "7", name = "Jordan Lee")
    )

    @Test
    fun searchMatchesNameOrJerseyNumberWithoutChangingOrder() {
        val initialState = RosterListState(teamName = "Tigers", players = players)

        val nameMatches = initialState
            .reduce(RosterListEvent.SearchQueryChanged("jordan"))
            .visiblePlayers
        val numberMatches = initialState
            .reduce(RosterListEvent.SearchQueryChanged("1"))
            .visiblePlayers

        assertEquals(listOf("2"), nameMatches.map { it.id })
        assertEquals(listOf("1"), numberMatches.map { it.id })
        assertEquals(players, initialState.visiblePlayers)
    }

    @Test
    fun favoriteToggleAddsThenRemovesPlayerId() {
        val initialState = RosterListState(teamName = "Tigers", players = players)

        val favorited = initialState.reduce(RosterListEvent.FavoriteToggled("1"))
        val unfavorited = favorited.reduce(RosterListEvent.FavoriteToggled("1"))

        assertEquals(setOf("1"), favorited.favoritePlayerIds)
        assertTrue(unfavorited.favoritePlayerIds.isEmpty())
    }

    @Test
    fun playersChangedReplacesRosterWithoutResettingInteractions() {
        val initialState = RosterListState(
            teamName = "Tigers",
            searchQuery = "7",
            favoritePlayerIds = setOf("2")
        )

        val updated = initialState.reduce(RosterListEvent.PlayersChanged(players))

        assertEquals(players, updated.players)
        assertEquals("7", updated.searchQuery)
        assertEquals(setOf("2"), updated.favoritePlayerIds)
    }

    private fun player(id: String, number: String, name: String) = PlayerProfile(
        id = id,
        number = number,
        name = name,
        position = "Forward",
        teamName = "Tigers",
        academicYear = "",
        createdAt = 0,
        updatedAt = 0
    )
}