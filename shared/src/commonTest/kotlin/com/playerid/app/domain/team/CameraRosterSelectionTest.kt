package com.playerid.app.domain.team

import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CameraRosterSelectionTest {
    private val setup = CaptureOrganization("Tigers", opponentName = "Lions")

    private suspend fun repository(storage: TestTeamStorage = TestTeamStorage()): LocalTeamRepository {
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        repository.saveTeam(testTeam("bears", "Bears"))
        repository.savePlayer(testPlayer())
        repository.savePlayer(testPlayer("bear-player", "12", "Bears"))
        repository.setCameraOrganization(setup, "Away")
        return repository
    }

    @Test
    fun choosingAndClearingPlayerPreservesMatchupJerseyAndEarlierMemories() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.organizeCapture("earlier.jpg", CaptureOrganization("Bears"))
        repository.selectCameraPlayer("Tigers", "player")
        val selected = LocalTeamRepository.open(storage).snapshot.value
        assertEquals(setup.copy(playerId = "player"), selected.cameraOrganization)
        assertEquals("Away", selected.cameraJerseyLabel)
        assertEquals(CaptureOrganization("Bears"), selected.captureOrganizations["earlier.jpg"])
        repository.selectCameraPlayer("Tigers", null)
        assertEquals(setup, LocalTeamRepository.open(storage).snapshot.value.cameraOrganization)
        assertEquals("Away", repository.snapshot.value.cameraJerseyLabel)
    }

    @Test
    fun wrongTeamMissingAndInactivePlayersCannotBeSelected() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.savePlayer(testPlayer("inactive", "99").copy(isActive = false))
        val before = storage.contents
        for (id in listOf("bear-player", "missing", "inactive")) {
            assertFailsWith<IllegalArgumentException> { repository.selectCameraPlayer("Tigers", id) }
        }
        assertEquals(setup, repository.snapshot.value.cameraOrganization)
        assertEquals(before, storage.contents)
    }

    @Test
    fun staleDrawerCannotReplaceAChangedCameraTeam() = runTest {
        val repository = repository()
        val newSetup = CaptureOrganization("Bears", opponentName = "Falcons")
        repository.setCameraOrganization(newSetup, "Home")
        assertFailsWith<IllegalArgumentException> { repository.selectCameraPlayer("Tigers", "player") }
        assertFailsWith<IllegalArgumentException> { repository.selectCameraPlayer("Tigers", null) }
        assertEquals(newSetup, repository.snapshot.value.cameraOrganization)
        assertEquals("Home", repository.snapshot.value.cameraJerseyLabel)
    }

    @Test
    fun playerChoiceUsesLatestMatchupInsteadOfAnOldSnapshot() = runTest {
        val repository = repository()
        val updated = setup.copy(opponentName = "Falcons")
        repository.setCameraOrganization(updated, "Home")
        repository.selectCameraPlayer("Tigers", "player")
        assertEquals(updated.copy(playerId = "player"), repository.snapshot.value.cameraOrganization)
        assertEquals("Home", repository.snapshot.value.cameraJerseyLabel)
    }

    @Test
    fun failedWriteDoesNotPublishAPlayerChoice() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        val before = storage.contents
        storage.failWrites = true
        assertFailsWith<TeamStorageException> { repository.selectCameraPlayer("Tigers", "player") }
        assertEquals(setup, repository.snapshot.value.cameraOrganization)
        assertEquals(before, storage.contents)
    }

    @Test
    fun clearedSetupAndDeletedPlayerCannotBeSelectedFromOldDrawer() = runTest {
        val repository = repository()
        repository.deletePlayer("player")
        assertFailsWith<IllegalArgumentException> { repository.selectCameraPlayer("Tigers", "player") }
        repository.setCameraOrganization(null)
        assertFailsWith<IllegalArgumentException> { repository.selectCameraPlayer("Tigers", null) }
        assertNull(repository.snapshot.value.cameraOrganization)
    }
}
