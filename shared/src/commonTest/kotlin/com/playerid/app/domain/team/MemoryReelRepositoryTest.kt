package com.playerid.app.domain.team

import com.playerid.app.domain.memory.MemoryReel
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MemoryReelRepositoryTest {
    private val first = MediaReference("file:///old-container/first.mov", MediaKind.VIDEO)
    private val second = MediaReference("file:///old-container/second.mp4", MediaKind.VIDEO)
    private val reel = MediaReference("file:///new-container/reel.mp4", MediaKind.VIDEO)
    private val sources = listOf(first, second)

    @Test
    fun storesTrimmedNameAndOrderedFilenameKeysAcrossContainerRelocation() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        repository.organizeCapture("first.mov", CaptureOrganization("Tigers"))
        repository.tagCapture("first.mov", "Goal")
        val before = repository.snapshot.value
        repository.saveCaptureReel("reel.mp4", "  Season Highlights  ", sources.reversed())
        val restored = LocalTeamRepository.open(storage).snapshot.value
        assertEquals(MemoryReel("Season Highlights", listOf("second.mp4", "first.mov")),
            restored.captureReels[captureFileKey(reel)])
        assertEquals(before.captureOrganizations, restored.captureOrganizations)
        assertEquals(before.captureMomentTags, restored.captureMomentTags)
        assertEquals(before.teams, restored.teams)
        assertNull(restored.captureOrganizations["reel.mp4"])
        assertFalse(storage.contents!!.contains("old-container"))
    }

    @Test
    fun olderSnapshotsDefaultToNoReelsWithoutWriting() = runTest {
        val original = """{"version":1,"captureMomentTags":{"first.mov":"Goal"}}"""
        val storage = TestTeamStorage(original)
        val snapshot = LocalTeamRepository.open(storage).snapshot.value
        assertTrue(snapshot.captureReels.isEmpty())
        assertEquals("Goal", snapshot.captureMomentTags["first.mov"])
        assertEquals(original, storage.contents)
        assertEquals(0, storage.writes)
    }

    @Test
    fun metadataFailureIsAtomicAndRetryDoesNotAddDuplicateReels() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.tagCapture("first.mov", "Goal")
        val before = repository.snapshot.value
        val contents = storage.contents
        storage.failWrites = true
        assertFailsWith<TeamStorageException> { repository.saveCaptureReel("reel.mp4", "Goals", sources) }
        assertEquals(before, repository.snapshot.value)
        assertEquals(contents, storage.contents)
        storage.failWrites = false
        repeat(2) { repository.saveCaptureReel("reel.mp4", "Goals", sources) }
        assertEquals(mapOf("reel.mp4" to MemoryReel("Goals", listOf("first.mov", "second.mp4"))),
            LocalTeamRepository.open(storage).snapshot.value.captureReels)
        assertEquals(before.captureMomentTags, repository.snapshot.value.captureMomentTags)
    }

    @Test
    fun rejectsInvalidSourceOutputAndNestedReelsWithoutWriting() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        for ((key, videos) in listOf(
            "reel.jpg" to sources, "../reel.mp4" to sources,
            "first.mov" to sources, "reel.mp4" to listOf(first, first),
            "reel.mp4" to listOf(first, second.copy(identifier = "file:///media/photo.jpg")),
            "reel.mp4" to listOf(first)
        )) {
            assertFailsWith<IllegalArgumentException> { repository.saveCaptureReel(key, "Goals", videos) }
        }
        assertFailsWith<IllegalArgumentException> { repository.saveCaptureReel("reel.mp4", " ", sources) }
        assertEquals(0, storage.writes)
        repository.saveCaptureReel("reel.mp4", "Goals", sources)
        val before = storage.contents
        assertFailsWith<IllegalArgumentException> {
            repository.saveCaptureReel("another.mp4", "Nested", listOf(first, reel))
        }
        assertEquals(before, storage.contents)
    }

    @Test
    fun deletingSourceMetadataDoesNotDeleteFinishedReel() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.tagCapture("first.mov", "Goal")
        repository.saveCaptureReel("reel.mp4", "Goals", sources)
        repository.forgetCapture("first.mov")
        val restored = LocalTeamRepository.open(storage)
        assertEquals(listOf("first.mov", "second.mp4"), restored.snapshot.value.captureReels["reel.mp4"]?.sourceFileKeys)
        assertEquals(listOf(reel), searchCaptureLibrary(listOf(reel), restored.snapshot.value, "Goals reel"))
        restored.forgetCapture("reel.mp4")
        assertTrue(LocalTeamRepository.open(storage).snapshot.value.captureReels.isEmpty())
    }

    @Test
    fun trimmingReelKeepsItsNameAndSourceReferencesIndependently() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveCaptureReel("reel.mp4", "Goals", sources)
        repository.copyCaptureDetails("reel.mp4", "trimmed.mov")
        assertEquals(repository.snapshot.value.captureReels["reel.mp4"], repository.snapshot.value.captureReels["trimmed.mov"])
        repository.forgetCapture("reel.mp4")
        assertEquals(MemoryReel("Goals", listOf("first.mov", "second.mp4")),
            LocalTeamRepository.open(storage).snapshot.value.captureReels["trimmed.mov"])
    }

    @Test
    fun reelFilterRequiresOneSourceToMatchAllFacets() {
        val snapshot = TeamWorkspaceSnapshot(
            captureReels = mapOf("reel.mp4" to MemoryReel("Goals", listOf("first.mov", "second.mp4"))),
            captureOrganizations = mapOf(
                "first.mov" to CaptureOrganization("Tigers", playerId = "alex"),
                "second.mp4" to CaptureOrganization("Bears", playerId = "sam")
            )
        )
        assertEquals(listOf(reel), filterCaptures(listOf(reel), snapshot, CaptureLibraryFilter(teamName = "Tigers", playerId = "alex")))
        assertTrue(filterCaptures(listOf(reel), snapshot, CaptureLibraryFilter(teamName = "Tigers", playerId = "sam")).isEmpty())
        assertTrue(filterCaptures(listOf(reel), snapshot, CaptureLibraryFilter(unassignedOnly = true)).isEmpty())
        val unassignedSource = snapshot.copy(captureOrganizations = snapshot.captureOrganizations - "second.mp4")
        assertEquals(listOf(reel), filterCaptures(listOf(reel), unassignedSource, CaptureLibraryFilter(unassignedOnly = true)))
    }

    @Test
    fun explicitReelAssignmentOverridesInheritedSourceFacetsAndSearch() {
        val snapshot = TeamWorkspaceSnapshot(
            captureReels = mapOf("reel.mp4" to MemoryReel("Playoffs", listOf("first.mov", "second.mp4"))),
            captureMomentTags = mapOf("first.mov" to "Goal"),
            captureOrganizations = mapOf("first.mov" to CaptureOrganization("Tigers", opponentName = "Lions"),
                "second.mp4" to CaptureOrganization("Bears", opponentName = "Falcons"))
        )
        assertEquals(listOf(reel), searchCaptureLibrary(listOf(reel), snapshot, "PLAYOFFS reel tigers goal falcons video"))
        assertTrue(searchCaptureLibrary(listOf(reel), snapshot, "unassigned").isEmpty())
        assertTrue(searchCaptureLibrary(listOf(reel), snapshot, "unknown").isEmpty())
        val assigned = snapshot.copy(captureOrganizations = snapshot.captureOrganizations + ("reel.mp4" to CaptureOrganization("Wolves")))
        assertTrue(filterCaptures(listOf(reel), assigned, CaptureLibraryFilter(teamName = "Tigers")).isEmpty())
        assertEquals(listOf(reel), filterCaptures(listOf(reel), assigned, CaptureLibraryFilter(teamName = "Wolves")))
        assertTrue(searchCaptureLibrary(listOf(reel), assigned, "tigers").isEmpty())
        assertEquals(listOf(reel), searchCaptureLibrary(listOf(reel), assigned, "Playoffs wolves reel"))
    }

    @Test
    fun reelsAreGroupedByCreationYearNewestFirstIgnoringTeamAndOpponent() {
        val utc = kotlinx.datetime.TimeZone.UTC
        // 2025-06-01, 2026-03-10 and 2026-10-01 (UTC) as PlayerID filename timestamps.
        val reel2025 = MediaReference("file:///c/1748736000000-a.mov", MediaKind.VIDEO)
        val reelMarch2026 = MediaReference("file:///c/1773100800000-b.mov", MediaKind.VIDEO)
        val reelOct2026 = MediaReference("file:///c/1790812800000-c.mov", MediaKind.VIDEO)
        val undated = MediaReference("file:///c/reel.mov", MediaKind.VIDEO)
        val snapshot = TeamWorkspaceSnapshot(captureOrganizations = mapOf(
            "1748736000000-a.mov" to CaptureOrganization("Tigers", opponentName = "Lions"),
            "1773100800000-b.mov" to CaptureOrganization("Bears", opponentName = "Wolves")
        ))
        val groups = groupReelsByYear(listOf(reelOct2026, undated, reel2025, reelMarch2026), snapshot, utc)
        assertEquals(listOf(2026, 2025, null), groups.map { it.year })
        assertEquals(listOf(reelOct2026, reelMarch2026), groups.first().reels)
    }

    @Test
    fun searchingAYearFindsReelsCreatedThatYear() {
        val reel2025 = MediaReference("file:///c/1748736000000-a.mov", MediaKind.VIDEO)
        val reel2026 = MediaReference("file:///c/1790812800000-c.mov", MediaKind.VIDEO)
        val snapshot = TeamWorkspaceSnapshot(captureReels = mapOf(
            "1748736000000-a.mov" to MemoryReel("Old", listOf("x.mov", "y.mov")),
            "1790812800000-c.mov" to MemoryReel("New", listOf("x.mov", "y.mov"))
        ))
        assertEquals(listOf(reel2026),
            searchCaptureLibrary(listOf(reel2025, reel2026), snapshot, "show me all the reels for 2026"))
    }
}
