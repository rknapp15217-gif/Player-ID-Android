package com.playerid.app.domain.team

import com.playerid.app.roster.RosterCandidate
import kotlinx.coroutines.flow.Flow

class TeamRosterService(
    private val repository: TeamRosterRepository
) {
    fun observeRoster(teamName: String): Flow<List<PlayerProfile>> =
        repository.observePlayers(teamName)

    suspend fun addPlayer(
        player: PlayerProfile,
        playerId: String,
        addedBy: String,
        timestamp: Long
    ): PlayerProfile {
        val savedPlayer = player.copy(
            id = playerId,
            addedBy = addedBy,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        repository.savePlayer(savedPlayer)
        return savedPlayer
    }

    suspend fun updatePlayer(player: PlayerProfile, timestamp: Long): PlayerProfile {
        val savedPlayer = player.copy(updatedAt = timestamp)
        repository.updatePlayer(savedPlayer)
        return savedPlayer
    }

    suspend fun deletePlayer(playerId: String) {
        repository.deletePlayer(playerId)
    }

    suspend fun importRoster(
        teamName: String,
        candidates: List<RosterCandidate>,
        addedBy: String,
        newPlayerIds: List<String>,
        timestamp: Long
    ): RosterImportSummary {
        require(newPlayerIds.size >= candidates.size) {
            "A potential player ID is required for every roster candidate"
        }

        var addedCount = 0
        var updatedCount = 0
        candidates.forEachIndexed { index, candidate ->
            val existingPlayer = repository.findPlayer(teamName, candidate.number)
            val candidateYear = candidate.academicYear?.takeIf { it.isNotBlank() }
            val candidatePosition = candidate.position.takeIf { it.isNotBlank() }
            val savedPlayer = if (existingPlayer != null) {
                updatedCount += 1
                existingPlayer.copy(
                    name = candidate.name,
                    position = candidatePosition ?: existingPlayer.position,
                    academicYear = candidateYear ?: existingPlayer.academicYear,
                    addedBy = addedBy,
                    updatedAt = timestamp
                )
            } else {
                addedCount += 1
                PlayerProfile(
                    id = newPlayerIds[index],
                    number = candidate.number,
                    name = candidate.name,
                    position = candidatePosition.orEmpty(),
                    teamName = teamName,
                    academicYear = candidateYear ?: "Unknown",
                    addedBy = addedBy,
                    createdAt = timestamp,
                    updatedAt = timestamp
                )
            }
            if (existingPlayer != null) {
                repository.updatePlayer(savedPlayer)
            } else {
                repository.savePlayer(savedPlayer)
            }
        }

        return RosterImportSummary(addedCount = addedCount, updatedCount = updatedCount)
    }
}

data class RosterImportSummary(
    val addedCount: Int,
    val updatedCount: Int
)