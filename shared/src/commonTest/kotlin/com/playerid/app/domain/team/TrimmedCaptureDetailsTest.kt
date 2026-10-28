package com.playerid.app.domain.team

import kotlinx.coroutines.test.runTest
import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.memory.deleteOriginalAfterTrim
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlin.test.*

class TrimmedCaptureDetailsTest {
    private val original = MediaReference("file:///media/original.mov", MediaKind.VIDEO)
    private val trimmed = MediaReference("file:///media/trimmed.mp4", MediaKind.VIDEO)

    @Test
    fun deletingOriginalKeepsDurableTrimmedDetailsAndUnrelatedMemories() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        val details = CaptureOrganization("Tigers", opponentName = "Lions")
        repository.organizeCapture("original.mov", details)
        repository.tagCapture("original.mov", "Goal")
        repository.tagCapture("other.mov", "Save")
        repository.copyCaptureDetails("original.mov", "trimmed.mp4")
        val deleted = mutableListOf<MediaReference>()
        deleteOriginalAfterTrim(original, trimmed, {
            assertEquals(details, repository.snapshot.value.captureOrganizations["trimmed.mp4"])
            assertEquals("Goal", repository.snapshot.value.captureMomentTags["trimmed.mp4"])
            deleted += it
        }, repository::forgetCapture)
        assertEquals(listOf(original), deleted)
        val restored = LocalTeamRepository.open(storage).snapshot.value
        assertNull(restored.captureOrganizations["original.mov"])
        assertNull(restored.captureMomentTags["original.mov"])
        assertEquals(details, restored.captureOrganizations["trimmed.mp4"])
        assertEquals("Goal", restored.captureMomentTags["trimmed.mp4"])
        assertEquals("Save", restored.captureMomentTags["other.mov"])
        assertEquals(1, restored.teams.size)
    }

    @Test
    fun deleteFailureLeavesOriginalDetailsAndSavedCopyUntouched() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.tagCapture("original.mov", "Goal")
        repository.copyCaptureDetails("original.mov", "trimmed.mp4")
        val before = storage.contents
        assertFailsWith<CameraCaptureException> {
            deleteOriginalAfterTrim(original, trimmed,
                { throw CameraCaptureException("Cannot delete file") }, repository::forgetCapture)
        }
        assertEquals(before, storage.contents)
        assertEquals("Goal", repository.snapshot.value.captureMomentTags["original.mov"])
        assertEquals("Goal", repository.snapshot.value.captureMomentTags["trimmed.mp4"])
    }

    @Test
    fun cleanupFailureCanRetryWithoutDeletingTrimmedCopyOrRepeatingExport() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.tagCapture("original.mov", "Goal")
        repository.copyCaptureDetails("original.mov", "trimmed.mp4")
        val deleted = mutableSetOf<MediaReference>()
        storage.failWrites = true
        assertFailsWith<TeamStorageException> {
            deleteOriginalAfterTrim(original, trimmed, { deleted += it }, repository::forgetCapture)
        }
        assertEquals(setOf(original), deleted)
        assertEquals("Goal", repository.snapshot.value.captureMomentTags["trimmed.mp4"])
        storage.failWrites = false
        deleteOriginalAfterTrim(original, trimmed, { deleted += it }, repository::forgetCapture)
        assertEquals(setOf(original), deleted)
        assertEquals(mapOf("trimmed.mp4" to "Goal"),
            LocalTeamRepository.open(storage).snapshot.value.captureMomentTags)
    }

    @Test
    fun sameFileCannotBeDeletedAsItsOwnTrimmedCopy() = runTest {
        var touched = false
        for (copy in listOf(original, original.copy(identifier = "file:///different-container/original.mov"))) {
            assertFailsWith<IllegalArgumentException> {
                deleteOriginalAfterTrim(original, copy, { touched = true }, { touched = true })
            }
        }
        assertFalse(touched)
    }

    @Test
    fun copiedDetailsAndTagAreDurableAndIndependentOfOriginal() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        repository.savePlayer(testPlayer())
        repository.saveChild(ChildProfileRecord("child", "My Child", 1, 1))
        repository.saveSeason(SportSeasonProfile("season", "child", "Soccer", "2026 Season", "Tigers", 1, 1))
        repository.saveGames(listOf(GameScheduleProfile("game", "season", "Lions", "vs Lions", 100, 200,
            createdAt = 1, updatedAt = 1)))
        val details = CaptureOrganization("Tigers", seasonId = "season", gameId = "game",
            playerId = "player", opponentName = "Lions")
        repository.organizeCapture("original.mov", details)
        repository.tagCapture("original.mov", "Goal")
        repository.copyCaptureDetails("original.mov", "trimmed.mp4")
        var restored = LocalTeamRepository.open(storage)
        assertEquals(details, restored.snapshot.value.captureOrganizations["original.mov"])
        assertEquals(details, restored.snapshot.value.captureOrganizations["trimmed.mp4"])
        assertEquals("Goal", restored.snapshot.value.captureMomentTags["trimmed.mp4"])
        restored.organizeCapture("trimmed.mp4", null)
        restored.tagCapture("trimmed.mp4", "Save")
        restored = LocalTeamRepository.open(storage)
        assertEquals(details, restored.snapshot.value.captureOrganizations["original.mov"])
        assertEquals("Goal", restored.snapshot.value.captureMomentTags["original.mov"])
        assertNull(restored.snapshot.value.captureOrganizations["trimmed.mp4"])
        assertEquals("Save", restored.snapshot.value.captureMomentTags["trimmed.mp4"])
    }

    @Test
    fun unassignedOriginalDoesNotInheritTheCameraSetup() = runTest {
        val repository = LocalTeamRepository.open(TestTeamStorage())
        repository.saveTeam(testTeam())
        repository.setCameraOrganization(CaptureOrganization("Tigers"))
        repository.copyCaptureDetails("import.mov", "trimmed.mp4")
        assertNull(repository.snapshot.value.captureOrganizations["trimmed.mp4"])
        assertNull(repository.snapshot.value.captureMomentTags["trimmed.mp4"])
        assertEquals(CaptureOrganization("Tigers"), repository.snapshot.value.cameraOrganization)
    }

    @Test
    fun failedMetadataCopyCanRetryWithoutChangingSourceOrOtherMemories() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.tagCapture("original.mov", "Goal")
        repository.tagCapture("other.mov", "Save")
        val before = storage.contents
        storage.failWrites = true
        assertFailsWith<TeamStorageException> { repository.copyCaptureDetails("original.mov", "trimmed.mp4") }
        assertEquals(before, storage.contents)
        assertNull(repository.snapshot.value.captureMomentTags["trimmed.mp4"])
        storage.failWrites = false
        repository.copyCaptureDetails("original.mov", "trimmed.mp4")
        repository.copyCaptureDetails("original.mov", "trimmed.mp4")
        assertEquals(mapOf("original.mov" to "Goal", "other.mov" to "Save", "trimmed.mp4" to "Goal"),
            LocalTeamRepository.open(storage).snapshot.value.captureMomentTags)
    }

    @Test
    fun invalidKeysAndOverwritingOriginalAreRejectedWithoutWriting() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        for ((source, destination) in listOf(
            "original.mov" to "original.mov", "../original.mov" to "trimmed.mp4",
            "original.mov" to "../trimmed.mp4", "original.mov" to "metadata.json"
        )) {
            assertFailsWith<IllegalArgumentException> { repository.copyCaptureDetails(source, destination) }
        }
        assertEquals(0, storage.writes)
    }
}
