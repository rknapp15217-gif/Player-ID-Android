package com.playerid.app.ui.components

import com.playerid.app.domain.team.*
import kotlinx.datetime.LocalDate
import kotlin.test.*

class PlayloomGoPageTest {
    @Test
    fun allEightExamplesRouteToExistingSupportedWorkflows() {
        val requests = playloomGoExampleRequests(emptyList())
        assertEquals(8, requests.size)
        assertEquals(requests.size, requests.distinct().size)
        val today = LocalDate(2026, 10, 27)
        assertEquals("Score", parseMemorySearchQuery(requests[0], today).momentTag)
        assertEquals(MemorySeasonIntent.CURRENT, parseMemorySearchQuery(requests[0], today).seasonIntent)
        val reel = assertIs<LibraryActionRequest.Reel>(parseLibraryActionRequest(requests[1]))
        val reelQuery = parseMemorySearchQuery(reel.command.searchQuery, today)
        assertEquals("2026-10-24", reelQuery.dateFrom)
        assertEquals("2026-10-25", reelQuery.dateThrough)
        assertFalse(reel.share)
        val favorites = parseMemorySearchQuery(requests[2], today)
        assertTrue(favorites.favoritesOnly)
        assertEquals("Faceoff Win", favorites.momentTag)
        val share = assertIs<LibraryActionRequest.Reel>(parseLibraryActionRequest(requests[3]))
        assertTrue(share.share)
        assertNull(share.recipient)
        assertEquals("Score", parseMemorySearchQuery(share.command.searchQuery, today).momentTag)
        assertIs<LibraryActionRequest.Schedule>(parseLibraryActionRequest(requests[4]))
        assertIs<LibraryActionRequest.Count>(parseLibraryActionRequest(requests[5]))
        assertEquals(LibraryDestination.ROSTER, parseLibraryNavigation(requests[6])?.destination)
        assertEquals(LibraryDestination.TEAMS, parseLibraryNavigation(requests[7])?.destination)
    }

    @Test
    fun usesAvailableSavedPlayerHintWithoutInventingNames() {
        val request = "Find Alex Smith's goals this season"
        assertEquals(request, playloomGoExampleRequests(listOf("Open teams", request)).first())
        assertEquals("Find goals this season", playloomGoExampleRequests(emptyList()).first())
    }
}
