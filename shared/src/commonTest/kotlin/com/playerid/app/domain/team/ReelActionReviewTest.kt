package com.playerid.app.domain.team

import com.playerid.app.domain.memory.temporaryMemoryPlaylist
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.*

class ReelActionReviewTest {
    private val today = LocalDate(2026, 10, 26)
    private val tyson = testPlayer("tyson", "28").copy(name = "Tyson Knapp")
    private val jake = testPlayer("jake", "12").copy(name = "Jake Knapp")
    private val early = MediaReference("file:///media/1792500000000-first.mov", MediaKind.VIDEO)
    private val late = MediaReference("file:///media/1792600000000-last.mov", MediaKind.VIDEO)
    private val other = MediaReference("file:///media/1792700000000-other.mov", MediaKind.VIDEO)
    private val photo = MediaReference("file:///media/1792800000000-photo.jpg", MediaKind.IMAGE)
    private val captures = listOf(other, photo, late, early)
    private val snapshot = TeamWorkspaceSnapshot(
        players = listOf(tyson, jake),
        captureOrganizations = captures.associate { media ->
            captureFileKey(media) to CaptureOrganization("Tigers",
                playerId = if (media == other) jake.id else tyson.id, opponentName = "Mt. Lebanon")
        },
        captureMomentTags = captures.associate { captureFileKey(it) to "Goal" }
    )

    @Test
    fun reelCommandAndSearchResultsUseTheSameAssignedPlayerMomentAndSeason() {
        val request = assertIs<LibraryActionRequest.Reel>(
            parseLibraryActionRequest("Create a reel of Tysons goals this season"))
        val query = interpret(request.command.searchQuery)
        assertEquals(listOf(early, late), sources(request.command.searchQuery, query))
        assertEquals(sources("Show Tyson goals this season", interpret("Show Tyson goals this season")),
            sources(request.command.searchQuery, query))
        assertEquals(emptyMap(), snapshot.captureReels)
        assertEquals(4, snapshot.captureOrganizations.size)
    }

    @Test
    fun namedSharingSeparatesTheRecipientFromTheReviewedSourceQuery() {
        val request = assertIs<LibraryActionRequest.Reel>(
            parseLibraryActionRequest("Share a reel of Tyson's goals against Mount Lebanon with Jenna"))
        assertTrue(request.share)
        assertEquals("Jenna", request.recipient)
        assertFalse(request.command.searchQuery.contains("Jenna"))
        assertEquals(listOf(early, late), sources(request.command.searchQuery, interpret(request.command.searchQuery)))
    }

    @Test
    fun ambiguousAndUnavailableCriteriaCannotBroadenTheDraft() {
        val ambiguous = interpretAskPlayloom("Tyson goals",
            listOf(tyson, jake.copy(name = "Tyson Smith")), listOf("Tigers"), emptyList(), today)
        assertNotNull(ambiguous.issue)
        assertEquals(emptyList(), sources("Tyson goals", ambiguous))
        val unsupported = interpret("shared goals")
        assertNotNull(unsupported.issue)
        assertEquals(emptyList(), sources("shared goals", unsupported))
        assertEquals(emptyList(), sources("Tyson saves", interpret("Tyson saves")))
    }

    @Test
    fun draftMembershipFollowsEditedChipsAndDoesNotModifySavedMetadata() {
        val query = interpret("Tyson goals this season")
        assertEquals(listOf(early, late), sources("", query))
        assertEquals(listOf(early, late, other), sources("", query.withoutChip("player:tyson")))
        val selection = sources("", query).toMutableList()
        selection.reverse()
        selection.removeLast()
        assertEquals(listOf(late), selection)
        assertEquals(listOf(early, late), sources("", query))
        assertEquals(emptyMap(), snapshot.captureReels)
    }

    private fun interpret(text: String) = interpretAskPlayloom(text, snapshot.players, listOf("Tigers"),
        listOf("Mt. Lebanon"), today)

    private fun sources(text: String, query: AskPlayloomQuery) = temporaryMemoryPlaylist(
        searchCaptureLibrary(captures, snapshot, text, today, TimeZone.UTC, query, reelsOnly = false), snapshot)
}
