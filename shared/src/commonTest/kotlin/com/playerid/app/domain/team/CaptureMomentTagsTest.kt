package com.playerid.app.domain.team

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CaptureMomentTagsTest {
    @Test
    fun tagsAndCustomButtonsSurviveRelaunchAndContainerRelocation() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.tagCapture("video.mov", "  Winning play  ", rememberCustom = true)
        repository.tagCapture("other.mov", "winning PLAY", rememberCustom = true)
        repository.tagCapture("goal.mov", "Goal", rememberCustom = true)
        val restored = LocalTeamRepository.open(storage).snapshot.value
        assertEquals("Winning play", restored.captureMomentTags["video.mov"])
        assertEquals(listOf("Winning play"), restored.customCaptureMomentTags)
        val media = MediaReference("file:///new-container/video.mov", MediaKind.VIDEO)
        assertEquals(listOf(media), searchCaptureLibrary(listOf(media), restored, "winning video"))
        assertTrue(searchCaptureLibrary(listOf(media), restored, "goal").isEmpty())
    }

    @Test
    fun customButtonChangesDoNotRetagExistingMemories() = runTest {
        val repository = LocalTeamRepository.open(TestTeamStorage())
        repository.tagCapture("video.mov", "Great play", rememberCustom = true)
        repository.renameCustomCaptureTag("Great play", "Great save")
        assertEquals(listOf("Great save"), repository.snapshot.value.customCaptureMomentTags)
        assertEquals("Great play", repository.snapshot.value.captureMomentTags["video.mov"])
        repository.removeCustomCaptureTag("Great save")
        assertTrue(repository.snapshot.value.customCaptureMomentTags.isEmpty())
        assertEquals("Great play", repository.snapshot.value.captureMomentTags["video.mov"])
    }

    @Test
    fun deletingCaptureDetailsKeepsOtherMemoriesTeamsAndCustomButtons() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        repository.organizeCapture("video.mov", CaptureOrganization("Tigers"))
        repository.tagCapture("video.mov", "Great play", rememberCustom = true)
        repository.tagCapture("other.mov", "Save")
        repository.forgetCapture("video.mov")
        repository.forgetCapture("video.mov")
        val restored = LocalTeamRepository.open(storage).snapshot.value
        assertNull(restored.captureOrganizations["video.mov"])
        assertNull(restored.captureMomentTags["video.mov"])
        assertEquals("Save", restored.captureMomentTags["other.mov"])
        assertEquals(listOf("Great play"), restored.customCaptureMomentTags)
        assertEquals(1, restored.teams.size)
    }

    @Test
    fun invalidTagsAndFailedWritesDoNotPublishChanges() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.tagCapture("video.mov", "Great play", rememberCustom = true)
        val before = repository.snapshot.value
        val contents = storage.contents
        assertFailsWith<IllegalArgumentException> { repository.tagCapture("video.mov", "  ") }
        assertFailsWith<IllegalArgumentException> { repository.tagCapture("../video.mov", "Goal") }
        assertFailsWith<IllegalArgumentException> { repository.renameCustomCaptureTag("Great play", "goal") }
        assertEquals(before, repository.snapshot.value)
        assertEquals(contents, storage.contents)
        storage.failWrites = true
        assertFailsWith<TeamStorageException> { repository.tagCapture("video.mov", "Save") }
        assertFailsWith<TeamStorageException> { repository.forgetCapture("video.mov") }
        assertEquals(before, repository.snapshot.value)
        assertEquals(contents, storage.contents)
    }

    @Test
    fun existingSnapshotsDefaultToNoTagsWithoutBeingRewritten() = runTest {
        val storage = TestTeamStorage("""{"version":1,"onboardingCompleted":true}""")
        val snapshot = LocalTeamRepository.open(storage).snapshot.value
        assertTrue(snapshot.captureMomentTags.isEmpty())
        assertTrue(snapshot.customCaptureMomentTags.isEmpty())
        assertEquals(0, storage.writes)
    }
}
