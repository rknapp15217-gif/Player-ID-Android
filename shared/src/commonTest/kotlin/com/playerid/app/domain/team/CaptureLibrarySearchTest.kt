package com.playerid.app.domain.team

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CaptureLibrarySearchTest {
    private val photo = MediaReference("file:///media/photo.jpg", MediaKind.IMAGE)
    private val video = MediaReference("file:///media/video.mov", MediaKind.VIDEO)
    private val unassigned = MediaReference("file:///media/new.jpg", MediaKind.IMAGE)
    private val captures = listOf(video, photo, unassigned)
    private val snapshot = TeamWorkspaceSnapshot(
        players = listOf(testPlayer().copy(name = "Alex Smith", number = "17")),
        seasons = listOf(SportSeasonProfile("fall", "child", "Soccer", "Fall 2026", "Tigers", 1, 1)),
        games = listOf(GameScheduleProfile("game", "fall", "Lions", "Championship", 1, 2, createdAt = 1, updatedAt = 1)),
        captureOrganizations = mapOf(
            "photo.jpg" to CaptureOrganization("Tigers", "fall", "game", "player", "Old opponent"),
            "video.mov" to CaptureOrganization("Bears", opponentName = "Falcons")
        )
    )

    @Test
    fun matchesEachAvailableMetadataFieldWithoutCaseSensitivity() {
        for (query in listOf("tigers", "ALEX", "smith", "#17", "17", "fall", "2026 season", "lions", "championship", "tigers alex lions")) {
            assertEquals(listOf(photo), searchCaptureLibrary(captures, snapshot, query), query)
        }
        assertEquals(listOf(video), searchCaptureLibrary(captures, snapshot, "BEARS falcons video"))
        assertTrue(searchCaptureLibrary(captures, snapshot, "Old opponent").isEmpty())
        // The capture's linked game is in 1970; a 2026 season label is not a 2026 capture date.
        assertTrue(searchCaptureLibrary(captures, snapshot, "from 2026").isEmpty())
    }

    @Test
    fun blankSearchPreservesOrderAndUnassignedCaptures() {
        assertEquals(captures, searchCaptureLibrary(captures, snapshot, " \n\t "))
        assertEquals(listOf(unassigned), searchCaptureLibrary(captures, snapshot, "unassigned"))
        assertEquals(listOf(photo, unassigned), searchCaptureLibrary(captures, snapshot, "photo"))
        assertEquals(emptyList(), searchCaptureLibrary(emptyList(), snapshot, "alex"))
    }

    @Test
    fun allTermsMustMatchAndFiltersStillIntersect() {
        assertEquals(listOf(photo), searchCaptureLibrary(captures, snapshot, "  alex \t FALL\n17 "))
        assertTrue(searchCaptureLibrary(captures, snapshot, "alex falcons").isEmpty())
        val filtered = filterCaptures(captures, snapshot, CaptureLibraryFilter(teamName = "Bears"))
        assertTrue(searchCaptureLibrary(filtered, snapshot, "alex").isEmpty())
        assertEquals(listOf(video), searchCaptureLibrary(filtered, snapshot, "falcons"))
    }

    @Test
    fun searchUsesUpdatedDetailsAndDoesNotMutateAssignments() {
        val updated = snapshot.copy(players = listOf(testPlayer().copy(name = "Jordan")))
        assertTrue(searchCaptureLibrary(captures, updated, "alex").isEmpty())
        assertEquals(listOf(photo), searchCaptureLibrary(captures, updated, "jordan"))
        assertEquals("player", snapshot.captureOrganizations["photo.jpg"]?.playerId)
    }

    @Test
    fun missingReferencesUseRemainingMetadataWithoutSearchingFilePaths() {
        val missingReferences = snapshot.copy(players = emptyList(), seasons = emptyList(), games = emptyList())
        assertEquals(listOf(photo), searchCaptureLibrary(captures, missingReferences, "tigers old opponent photo"))
        assertTrue(searchCaptureLibrary(captures, missingReferences, "alex").isEmpty())
        assertTrue(searchCaptureLibrary(captures, snapshot, "file:///media").isEmpty())
        assertTrue(searchCaptureLibrary(captures, snapshot, "photo.jpg").isEmpty())
    }
}
