package com.playerid.app.domain.team

import com.playerid.app.roster.RosterCandidate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TeamRosterServiceTest {
    @Test
    fun observeRosterDelegatesTeamScopedPlayers() = runTest {
        val repository = RecordingRosterRepository(
            players = mutableListOf(player(id = "1", number = "7"))
        )
        val service = TeamRosterService(repository)

        val roster = service.observeRoster("Tigers").first()

        assertEquals(listOf("1"), roster.map { it.id })
        assertEquals(listOf("observe:Tigers"), repository.operations)
    }

    @Test
    fun addPlayerAppliesIdentityContributorAndTimestamps() = runTest {
        val repository = RecordingRosterRepository()
        val service = TeamRosterService(repository)

        val saved = service.addPlayer(
            player = player(id = "temporary", number = "12"),
            playerId = "created-id",
            addedBy = "parent-1",
            timestamp = 42
        )

        assertEquals("created-id", saved.id)
        assertEquals("parent-1", saved.addedBy)
        assertEquals(42, saved.createdAt)
        assertEquals(42, saved.updatedAt)
        assertEquals(saved, repository.players.single())
    }

    @Test
    fun updatePlayerChangesOnlyUpdatedTimestamp() = runTest {
        val repository = RecordingRosterRepository()
        val service = TeamRosterService(repository)
        val existing = player(id = "1", number = "7").copy(
            addedBy = "coach",
            createdAt = 10,
            updatedAt = 10
        )

        val saved = service.updatePlayer(existing.copy(name = "Updated Name"), timestamp = 99)

        assertEquals("Updated Name", saved.name)
        assertEquals("coach", saved.addedBy)
        assertEquals(10, saved.createdAt)
        assertEquals(99, saved.updatedAt)
        assertEquals("update:1", repository.operations.last())
    }

    @Test
    fun deletePlayerDelegatesHardDeleteById() = runTest {
        val repository = RecordingRosterRepository(
            players = mutableListOf(player(id = "1", number = "7"))
        )
        val service = TeamRosterService(repository)

        service.deletePlayer("1")

        assertNull(repository.players.firstOrNull { it.id == "1" })
        assertEquals("delete:1", repository.operations.last())
    }

    @Test
    fun importRosterUpdatesCollisionsAndAddsNewPlayers() = runTest {
        val repository = RecordingRosterRepository(
            players = mutableListOf(
                player(id = "existing", number = "7").copy(
                    name = "Old Name",
                    position = "Goalie",
                    academicYear = "Senior",
                    createdAt = 5,
                    updatedAt = 5
                )
            )
        )
        val service = TeamRosterService(repository)
        val candidates = listOf(
            candidate(number = "7", name = "Updated Name", position = "", academicYear = null),
            candidate(number = "12", name = "New Player", position = "Forward", academicYear = "Junior")
        )

        val summary = service.importRoster(
            teamName = "Tigers",
            candidates = candidates,
            addedBy = "ocr_import",
            newPlayerIds = listOf("unused", "new-id"),
            timestamp = 100
        )

        val updated = repository.players.first { it.id == "existing" }
        val added = repository.players.first { it.id == "new-id" }
        assertEquals(RosterImportSummary(addedCount = 1, updatedCount = 1), summary)
        assertEquals("Updated Name", updated.name)
        assertEquals("Goalie", updated.position)
        assertEquals("Senior", updated.academicYear)
        assertEquals(5, updated.createdAt)
        assertEquals(100, updated.updatedAt)
        assertEquals("Forward", added.position)
        assertEquals("Junior", added.academicYear)
        assertEquals(100, added.createdAt)
    }

    private fun player(id: String, number: String) = PlayerProfile(
        id = id,
        number = number,
        name = "Player $number",
        position = "Forward",
        teamName = "Tigers",
        academicYear = "Junior",
        createdAt = 0,
        updatedAt = 0
    )

    private fun candidate(
        number: String,
        name: String,
        position: String,
        academicYear: String?
    ) = RosterCandidate(
        name = name,
        number = number,
        position = position,
        graduationYear = null,
        academicYear = academicYear
    )

    private class RecordingRosterRepository(
        val players: MutableList<PlayerProfile> = mutableListOf()
    ) : TeamRosterRepository {
        val operations = mutableListOf<String>()

        override fun observeActiveTeams(): Flow<List<TeamProfile>> = flowOf(emptyList())

        override fun observePlayers(teamName: String): Flow<List<PlayerProfile>> {
            operations += "observe:$teamName"
            return flowOf(players.filter { it.teamName == teamName })
        }

        override suspend fun findTeam(teamName: String): TeamProfile? = null

        override suspend fun findPlayer(teamName: String, number: String): PlayerProfile? =
            players.firstOrNull { it.teamName == teamName && it.number == number && it.isActive }

        override suspend fun saveTeam(team: TeamProfile) = Unit

        override suspend fun savePlayer(player: PlayerProfile) {
            operations += "save:${player.id}"
            players.removeAll { it.id == player.id }
            players += player
        }

        override suspend fun updatePlayer(player: PlayerProfile) {
            operations += "update:${player.id}"
            players.removeAll { it.id == player.id }
            players += player
        }

        override suspend fun savePlayers(players: List<PlayerProfile>) {
            players.forEach { savePlayer(it) }
        }

        override suspend fun deletePlayer(playerId: String) {
            operations += "delete:$playerId"
            players.removeAll { it.id == playerId }
        }
    }
}