package com.playerid.app.domain.team

import kotlinx.coroutines.flow.Flow

interface TeamRosterRepository {
    fun observeActiveTeams(): Flow<List<TeamProfile>>
    fun observePlayers(teamName: String): Flow<List<PlayerProfile>>
    suspend fun findTeam(teamName: String): TeamProfile?
    suspend fun findPlayer(teamName: String, number: String): PlayerProfile?
    suspend fun saveTeam(team: TeamProfile)
    suspend fun savePlayer(player: PlayerProfile)
    suspend fun updatePlayer(player: PlayerProfile)
    suspend fun savePlayers(players: List<PlayerProfile>)
    suspend fun deletePlayer(playerId: String)
}

interface TeamSubscriptionRepository {
    fun observeSubscribedTeams(userId: String): Flow<List<TeamProfile>>
    suspend fun subscribe(subscription: TeamSubscription)
    suspend fun clear(userId: String)
    suspend fun unsubscribe(userId: String, teamName: String)
}

class TeamSubscriptionService(
    private val repository: TeamSubscriptionRepository
) {
    suspend fun replaceWithTeam(userId: String, teamName: String, subscribedAt: Long): Boolean {
        val normalizedTeamName = teamName.trim()
        if (normalizedTeamName.isEmpty()) return false

        repository.clear(userId)
        repository.subscribe(
            TeamSubscription(
                userId = userId,
                teamName = normalizedTeamName,
                subscribedAt = subscribedAt
            )
        )
        return true
    }
}