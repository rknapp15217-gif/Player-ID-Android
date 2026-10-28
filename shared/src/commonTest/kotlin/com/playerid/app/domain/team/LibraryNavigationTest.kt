package com.playerid.app.domain.team

import kotlin.test.*

class LibraryNavigationTest {
    @Test
    fun navigationGrammarDoesNotStealSearchesOrQuestions() {
        for (text in listOf("Open Tigers' roster", "Open Tigers’s roster")) {
            assertEquals("Tigers", parseLibraryNavigation(text)?.teamName)
        }
        assertEquals(LibraryDestination.ROSTER, parseLibraryNavigation("Open Tigers's roster")?.destination)
        assertEquals("Tigers", parseLibraryNavigation("Show the roster for Tigers")?.teamName)
        assertEquals("North Allegheny", parseLibraryNavigation("Open North Allegheny roster")?.teamName)
        assertEquals(LibraryDestination.SCHEDULE, parseLibraryNavigation("Show North Allegheny schedule")?.destination)
        assertEquals(LibraryDestination.SCHEDULE, parseLibraryNavigation("Show our schedule")?.destination)
        assertEquals(LibraryDestination.REELS, parseLibraryNavigation("Open reels")?.destination)
        assertEquals(LibraryDestination.CAMERA, parseLibraryNavigation("Take me to camera")?.destination)
        for (text in listOf("Show me Tyson's goals", "When is our next game?", "Who are our seniors?",
            "Create a reel of goals", "Show reels yesterday")) {
            assertNull(parseLibraryNavigation(text), text)
        }
    }

    @Test
    fun selectedNamedAndAmbiguousTeamsResolveWithoutGuessing() {
        val request = LibraryNavigationRequest(LibraryDestination.ROSTER)
        val teams = listOf("Tigers", "Bears")
        assertEquals("Tigers", resolveLibraryNavigation(request, teams, "Tigers").request?.teamName)
        assertNotNull(resolveLibraryNavigation(request, teams, null).error)
        assertNotNull(resolveLibraryNavigation(request, emptyList(), null).error)
        assertEquals("Bears", resolveLibraryNavigation(request.copy(teamName = "bears"), teams, "Tigers").request?.teamName)
        assertNotNull(resolveLibraryNavigation(request.copy(teamName = "Unknown"), teams, null).error)
        assertEquals("Tigers", resolveLibraryNavigation(request, listOf("Tigers"), null).request?.teamName)
        assertEquals(LibraryDestination.TEAMS, resolveLibraryNavigation(
            LibraryNavigationRequest(LibraryDestination.TEAMS), emptyList(), null).request?.destination)
    }
}
