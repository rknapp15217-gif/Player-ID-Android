package com.playerid.app.domain.team

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CaptureFavoritesAndNotesTest {
    private val video = MediaReference("file:///media/video.mov", MediaKind.VIDEO)
    private val other = MediaReference("file:///media/other.mov", MediaKind.VIDEO)

    @Test
    fun favoritesAndNotesSurviveRelaunchAndAreClearedWhenMemoryIsRemoved() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.setCaptureFavorite("video.mov", true)
        repository.setCaptureNote("video.mov", "  Top-corner rocket  ")
        val restored = LocalTeamRepository.open(storage)
        assertEquals(setOf("video.mov"), restored.snapshot.value.captureFavorites)
        assertEquals("Top-corner rocket", restored.snapshot.value.captureNotes["video.mov"])

        restored.setCaptureNote("video.mov", "   ")
        assertNull(restored.snapshot.value.captureNotes["video.mov"])
        restored.setCaptureNote("video.mov", "Again")
        restored.forgetCapture("video.mov")
        assertTrue(restored.snapshot.value.captureFavorites.isEmpty())
        assertTrue(restored.snapshot.value.captureNotes.isEmpty())
        assertFailsWith<IllegalArgumentException> { restored.setCaptureNote("video.mov", "x".repeat(MAX_CAPTURE_NOTE_LENGTH + 1)) }
    }

    @Test
    fun trimmedCopyKeepsFavoriteAndNote() = runTest {
        val repository = LocalTeamRepository.open(TestTeamStorage())
        repository.setCaptureFavorite("video.mov", true)
        repository.setCaptureNote("video.mov", "Breakaway")
        repository.copyCaptureDetails("video.mov", "trimmed.mp4")
        assertTrue("trimmed.mp4" in repository.snapshot.value.captureFavorites)
        assertEquals("Breakaway", repository.snapshot.value.captureNotes["trimmed.mp4"])
    }

    @Test
    fun sentenceSearchesMatchTagsNotesAndFavorites() = runTest {
        val repository = LocalTeamRepository.open(TestTeamStorage())
        repository.tagCapture("video.mov", "Goal")
        repository.tagCapture("other.mov", "Save")
        repository.setCaptureFavorite("other.mov", true)
        repository.setCaptureNote("video.mov", "Rocket from the wing")
        val snapshot = repository.snapshot.value
        val all = listOf(video, other)
        assertEquals(listOf(video), searchCaptureLibrary(all, snapshot, "Find all goals this season"))
        assertEquals(listOf(other), searchCaptureLibrary(all, snapshot, "Find the last save"))
        assertEquals(listOf(other), searchCaptureLibrary(all, snapshot, "favorite plays"))
        assertEquals(listOf(video), searchCaptureLibrary(all, snapshot, "rocket"))
        assertEquals(listOf("tyson", "goal"), captureSearchTerms("Find Tyson's goal"))
    }

    @Test
    fun spokenFaceOffPhrasingsMatchFaceoffWinTag() = runTest {
        val repository = LocalTeamRepository.open(TestTeamStorage())
        repository.tagCapture("video.mov", "Faceoff Win")
        repository.tagCapture("other.mov", "Save")
        val snapshot = repository.snapshot.value
        val all = listOf(video, other)
        listOf(
            "Show Face Off Wins", "show face-off wins.", "Show faceoff wins", "Show me face off wins!",
            "face off", "Faceoffs"
        ).forEach { query ->
            assertEquals(listOf(video), searchCaptureLibrary(all, snapshot, query), query)
        }
        assertEquals(listOf(other), searchCaptureLibrary(all, snapshot, "Show saves."))
    }

    @Test
    fun searchIdeasUseTeamPlayersAndOpponents() {
        val ideas = captureLibrarySearchIdeas(TeamWorkspaceSnapshot())
        assertTrue(ideas.isNotEmpty())
        assertTrue(ideas.none { "Penn Trafford" in it || "Tyson" in it })
        assertTrue(ideas.all { it.startsWith("Find") || parseLibraryActionRequest(it) != null })
        assertTrue(ideas.any { parseMemoryReelCommand(it) != null })
    }

    @Test
    fun searchIdeasNameOnlyMyPlayer() {
        val sam = testPlayer("sam", "1").copy(name = "Sam Jones")
        val tyson = testPlayer("tyson", "2").copy(name = "Tyson Lee")
        val withoutMyPlayer = TeamWorkspaceSnapshot(players = listOf(sam, tyson))
        assertTrue(captureLibrarySearchIdeas(withoutMyPlayer).none { "Sam" in it || "Tyson" in it })

        val ideas = captureLibrarySearchIdeas(withoutMyPlayer.copy(assignedPlayerIds = mapOf("Tigers" to "tyson")))
        assertTrue("Make a reel of Tyson Lee's saves" in ideas)
        assertTrue(ideas.none { "Sam" in it })
    }

    @Test
    fun unassignedGroupIsListedLast() {
        val organized = MediaReference("file:///media/organized.mov", MediaKind.VIDEO)
        val snapshot = TeamWorkspaceSnapshot(
            captureOrganizations = mapOf("organized.mov" to CaptureOrganization(teamName = "Lions"))
        )
        val groups = groupCaptureLibrary(listOf(video, organized, other), snapshot)
        assertEquals(listOf("Lions", null), groups.map { it.teamName })
        assertEquals(listOf(video, other), groups.last().captures)
    }

    @Test
    fun sameOpponentOnDifferentDatesGetsSeparateGroups() {
        val utc = kotlinx.datetime.TimeZone.UTC
        // Oct 21 2026 twice (morning and evening), then Nov 12 2026.
        val first = MediaReference("file:///media/1792540800000-a.mov", MediaKind.VIDEO)
        val sameDay = MediaReference("file:///media/1792580400000-b.mov", MediaKind.VIDEO)
        val later = MediaReference("file:///media/1794441600000-c.mov", MediaKind.VIDEO)
        val loose = MediaReference("file:///media/1794441600000-d.mov", MediaKind.VIDEO)
        val pr = CaptureOrganization(teamName = "Lions", opponentName = "PR")
        val snapshot = TeamWorkspaceSnapshot(captureOrganizations = mapOf(
            "1792540800000-a.mov" to pr, "1792580400000-b.mov" to pr, "1794441600000-c.mov" to pr,
            "1794441600000-d.mov" to CaptureOrganization(teamName = "Lions")
        ))
        val groups = groupCaptureLibrary(listOf(later, first, sameDay, loose), snapshot, utc)
        assertEquals(listOf("Nov 12, 2026", "Oct 21, 2026", ""), groups.map(::captureGroupDateLabel))
        assertEquals(listOf(listOf(later), listOf(first, sameDay), listOf(loose)), groups.map { it.captures })
        assertEquals(listOf("PR", "PR", null), groups.map { it.opponent })
    }

    @Test
    fun groupDateUsesLinkedGameOrCaptureTimestamp() {
        val stamped = MediaReference("file:///media/1792540800000-abc.mov", MediaKind.VIDEO)
        val snapshot = TeamWorkspaceSnapshot(
            captureOrganizations = mapOf("1792540800000-abc.mov" to CaptureOrganization(teamName = "Lions", opponentName = "Bears"))
        )
        val utc = kotlinx.datetime.TimeZone.UTC
        val group = groupCaptureLibrary(listOf(stamped), snapshot, utc).single()
        assertEquals("Oct 21, 2026", captureGroupDateLabel(group))
        assertEquals("", captureGroupDateLabel(CaptureLibraryGroup(null, null, null, listOf(stamped))))
    }

    @Test
    fun groupDateIsTheCaptureDateEvenWhenLinkedToAGameOnAnotherDay() {
        val utc = kotlinx.datetime.TimeZone.UTC
        val stamped = MediaReference("file:///media/1792540800000-abc.mov", MediaKind.VIDEO)
        val unstamped = MediaReference("file:///media/imported.mov", MediaKind.VIDEO)
        // Game scheduled Nov 12 2026; the stamped clip was captured Oct 21 2026.
        val game = GameScheduleProfile("game", "season", "Bears", "vs Bears", 1794441600000, 1794445200000, createdAt = 1, updatedAt = 1)
        val details = CaptureOrganization(teamName = "Lions", seasonId = "season", gameId = "game")
        val snapshot = TeamWorkspaceSnapshot(games = listOf(game), captureOrganizations = mapOf(
            "1792540800000-abc.mov" to details, "imported.mov" to details
        ))
        assertEquals(kotlinx.datetime.LocalDate(2026, 10, 21), captureDate(stamped, snapshot, utc))
        assertEquals(kotlinx.datetime.LocalDate(2026, 11, 12), captureDate(unstamped, snapshot, utc))
    }
}
