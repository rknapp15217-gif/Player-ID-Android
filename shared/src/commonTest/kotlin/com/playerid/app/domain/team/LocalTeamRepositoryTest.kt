package com.playerid.app.domain.team

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalTeamRepositoryTest {
    @Test
    fun newStorageIsEmptyWithoutWriting() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        assertEquals(TeamWorkspaceSnapshot(), repository.snapshot.value)
        assertEquals(0, storage.writes)
    }

    @Test
    fun roundTripPreservesProfilesSubscriptionsAndPlayerPreferences() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        val player = testPlayer().copy(teamSnapId = "external", syncStatus = "SYNCED")
        repository.savePlayer(player)
        repository.subscribe(TeamSubscription(LOCAL_TEAM_USER, "Tigers", 30))
        repository.toggleFavorite(player.id)
        repository.assignPlayer("Tigers", player.id)
        val restored = LocalTeamRepository.open(storage)
        assertEquals(repository.snapshot.value, restored.snapshot.value)
        assertEquals("external", restored.findPlayer("Tigers", "7")?.teamSnapId)
        assertEquals(listOf("Tigers"), restored.observeSubscribedTeams(LOCAL_TEAM_USER).first().map { it.name })
    }

    @Test
    fun failedWriteDoesNotPublishOrLoseExistingData() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        val savedText = storage.contents
        storage.failWrites = true
        assertFailsWith<TeamStorageException> { repository.savePlayer(testPlayer()) }
        assertEquals(savedText, storage.contents)
        assertTrue(repository.snapshot.value.players.isEmpty())
    }

    @Test
    fun malformedUnknownVersionAndMissingVersionAreNotOverwritten() = runTest {
        listOf("not json", """{"version":2}""", "{}", "").forEach { text ->
            val storage = TestTeamStorage(text)
            assertFailsWith<TeamStorageException> { LocalTeamRepository.open(storage) }
            assertEquals(text, storage.contents)
            assertEquals(0, storage.writes)
        }
    }

    @Test
    fun semanticallyInvalidSavedDataIsRejected() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        repository.savePlayer(testPlayer())
        storage.contents = storage.contents!!.replace("\"teamName\":\"Tigers\"", "\"teamName\":\"Missing\"")
        assertFailsWith<TeamStorageException> { LocalTeamRepository.open(storage) }
    }

    @Test
    fun transactionCommitsOnceAndRollsBackOnFailure() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.transaction {
            it.saveTeam(testTeam())
            it.savePlayer(testPlayer())
        }
        assertEquals(1, storage.writes)
        val before = repository.snapshot.value
        assertFailsWith<IllegalArgumentException> {
            repository.transaction {
                it.savePlayer(testPlayer(id = "second", number = "12"))
                it.savePlayer(testPlayer(id = "collision", number = "7"))
            }
        }
        assertEquals(before, repository.snapshot.value)
        assertEquals(1, storage.writes)
    }

    @Test
    fun failedTransactionCommitDoesNotPublishStagedChanges() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        storage.failWrites = true
        assertFailsWith<TeamStorageException> {
            repository.transaction {
                it.saveTeam(testTeam())
                it.subscribe(TeamSubscription(LOCAL_TEAM_USER, "Tigers", 0))
            }
        }
        assertTrue(repository.snapshot.value.teams.isEmpty())
        assertTrue(repository.snapshot.value.subscriptions.isEmpty())
        assertNull(storage.contents)
    }

    @Test
    fun concurrentMutationsDoNotLosePlayers() = runTest {
        val repository = LocalTeamRepository.open(TestTeamStorage())
        repository.saveTeam(testTeam())
        (1..20).map { index ->
            async { repository.savePlayer(testPlayer(id = "$index", number = "$index")) }
        }.awaitAll()
        assertEquals(20, repository.snapshot.value.players.size)
    }

    @Test
    fun cancellationDuringCommitKeepsPublishedAndDurableStateConsistent() = runTest {
        val writeStarted = CompletableDeferred<Unit>()
        val finishWrite = CompletableDeferred<Unit>()
        val storage = object : TeamSnapshotStorage {
            var contents: String? = null
            override suspend fun read(): String? = contents
            override suspend fun write(contents: String) {
                writeStarted.complete(Unit)
                finishWrite.await()
                this.contents = contents
            }
        }
        val repository = LocalTeamRepository.open(storage)
        val job = launch { repository.saveTeam(testTeam()) }
        writeStarted.await()
        job.cancel()
        finishWrite.complete(Unit)
        job.join()
        assertEquals(repository.snapshot.value, LocalTeamRepository.open(storage).snapshot.value)
        assertEquals(1, repository.snapshot.value.teams.size)
    }

    @Test
    fun cancelledTransactionDoesNotWritePartialChanges() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        assertFailsWith<kotlinx.coroutines.CancellationException> {
            repository.transaction {
                it.saveTeam(testTeam())
                throw kotlinx.coroutines.CancellationException("Cancelled before commit")
            }
        }
        assertTrue(repository.snapshot.value.teams.isEmpty())
        assertNull(storage.contents)
    }

    @Test
    fun editingDeletedPlayerDoesNotResurrectIt() = runTest {
        val repository = LocalTeamRepository.open(TestTeamStorage())
        repository.saveTeam(testTeam())
        repository.savePlayer(testPlayer())
        repository.deletePlayer("player")
        assertFailsWith<IllegalArgumentException> { repository.updatePlayer(testPlayer()) }
        assertTrue(repository.snapshot.value.players.isEmpty())
    }

    @Test
    fun rosterIsTeamScopedActiveAndNumericallySorted() = runTest {
        val repository = LocalTeamRepository.open(TestTeamStorage())
        repository.saveTeam(testTeam())
        repository.saveTeam(testTeam(id = "other", name = "Lions"))
        repository.savePlayers(listOf(
            testPlayer(id = "12", number = "12"),
            testPlayer(id = "2", number = "2"),
            testPlayer(id = "other", teamName = "Lions"),
            testPlayer(id = "inactive", number = "15").copy(isActive = false)
        ))
        assertEquals(listOf("2", "12"), repository.observePlayers("Tigers").first().map { it.number })
    }

    @Test
    fun duplicateJerseyIsRejectedWithinTeamButAllowedAcrossTeams() = runTest {
        val repository = LocalTeamRepository.open(TestTeamStorage())
        repository.saveTeam(testTeam())
        repository.saveTeam(testTeam(id = "other", name = "Lions"))
        repository.savePlayer(testPlayer())
        repository.savePlayer(testPlayer(id = "other", teamName = "Lions"))
        assertFailsWith<IllegalArgumentException> { repository.savePlayer(testPlayer(id = "collision")) }
        assertEquals(2, repository.snapshot.value.players.size)
    }

    @Test
    fun deletingPlayerClearsFavoritesAndAssignmentDurably() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        repository.savePlayer(testPlayer())
        repository.toggleFavorite("player")
        repository.assignPlayer("Tigers", "player")
        repository.deletePlayer("player")
        val restored = LocalTeamRepository.open(storage)
        assertTrue(restored.snapshot.value.players.isEmpty())
        assertTrue(restored.snapshot.value.favoritePlayerIds.isEmpty())
        assertTrue(restored.snapshot.value.assignedPlayerIds.isEmpty())
    }

    @Test
    fun leavingRetainsDataAndCanBeRejoinedWithoutChangingOtherSubscriptions() = runTest {
        val repository = LocalTeamRepository.open(TestTeamStorage())
        repository.saveTeam(testTeam())
        repository.savePlayer(testPlayer())
        repository.subscribe(TeamSubscription(LOCAL_TEAM_USER, "Tigers", 1))
        repository.subscribe(TeamSubscription("another-user", "Tigers", 2))
        repository.unsubscribe(LOCAL_TEAM_USER, "Tigers")
        assertTrue(repository.observeSubscribedTeams(LOCAL_TEAM_USER).first().isEmpty())
        assertEquals(1, repository.observeSubscribedTeams("another-user").first().size)
        assertEquals(1, repository.observePlayers("Tigers").first().size)
        repository.subscribe(TeamSubscription(LOCAL_TEAM_USER, "Tigers", 3))
        assertEquals(1, repository.observeSubscribedTeams(LOCAL_TEAM_USER).first().size)
    }

    @Test
    fun duplicateNamesAreRejectedAndExistingNamesCannotChange() = runTest {
        val repository = LocalTeamRepository.open(TestTeamStorage())
        repository.saveTeam(testTeam())
        assertFailsWith<IllegalArgumentException> { repository.saveTeam(testTeam(id = "other", name = "tigers")) }
        assertFailsWith<IllegalArgumentException> { repository.saveTeam(testTeam(name = "Renamed")) }
        assertFalse(repository.snapshot.value.teams.any { it.name == "Renamed" })
    }
}

internal class TestTeamStorage(var contents: String? = null) : TeamSnapshotStorage {
    var failWrites = false
    var writes = 0
    override suspend fun read(): String? = contents
    override suspend fun write(contents: String) {
        if (failWrites) throw TeamStorageException("Test storage is full")
        this.contents = contents
        writes += 1
    }
}

internal fun testTeam(id: String = "team", name: String = "Tigers") = TeamProfile(
    id = id, name = name, lastActivityAt = 1, createdAt = 1, updatedAt = 1
)

internal fun testPlayer(id: String = "player", number: String = "7", teamName: String = "Tigers") = PlayerProfile(
    id = id, number = number, name = "Alex Smith", position = "Goalie",
    teamName = teamName, academicYear = "Junior", createdAt = 1, updatedAt = 1
)
