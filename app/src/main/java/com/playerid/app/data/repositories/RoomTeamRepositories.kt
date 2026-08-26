package com.playerid.app.data.repositories

import com.playerid.app.data.PlayerDao
import com.playerid.app.data.TeamDao
import com.playerid.app.data.UserTeamSubscriptionDao
import com.playerid.app.domain.team.PlayerProfile
import com.playerid.app.domain.team.TeamProfile
import com.playerid.app.domain.team.TeamRosterRepository
import com.playerid.app.domain.team.TeamSubscription
import com.playerid.app.domain.team.TeamSubscriptionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTeamRosterRepository(
    private val teamDao: TeamDao,
    private val playerDao: PlayerDao
) : TeamRosterRepository {
    override fun observeActiveTeams(): Flow<List<TeamProfile>> =
        teamDao.getAllActiveTeams().map { teams -> teams.map { it.toProfile() } }

    override fun observePlayers(teamName: String): Flow<List<PlayerProfile>> =
        playerDao.getPlayersByTeam(teamName).map { players -> players.map { it.toProfile() } }

    override suspend fun findTeam(teamName: String): TeamProfile? =
        teamDao.getTeamByName(teamName)?.toProfile()

    override suspend fun findPlayer(teamName: String, number: String): PlayerProfile? =
        playerDao.getPlayerByNumber(number, teamName)?.toProfile()

    override suspend fun saveTeam(team: TeamProfile) {
        teamDao.insertTeam(team.toEntity())
    }

    override suspend fun savePlayer(player: PlayerProfile) {
        playerDao.insertPlayer(player.toEntity())
    }

    override suspend fun updatePlayer(player: PlayerProfile) {
        playerDao.updatePlayer(player.toEntity())
    }

    override suspend fun savePlayers(players: List<PlayerProfile>) {
        playerDao.insertPlayers(players.map { it.toEntity() })
    }

    override suspend fun deletePlayer(playerId: String) {
        playerDao.deletePlayerById(playerId)
    }
}

class RoomTeamSubscriptionRepository(
    private val subscriptionDao: UserTeamSubscriptionDao
) : TeamSubscriptionRepository {
    override fun observeSubscribedTeams(userId: String): Flow<List<TeamProfile>> =
        subscriptionDao.getUserSubscribedTeams(userId).map { teams ->
            teams.map { it.toProfile() }
        }

    override suspend fun subscribe(subscription: TeamSubscription) {
        subscriptionDao.subscribeToTeam(subscription.toEntity())
    }

    override suspend fun clear(userId: String) {
        subscriptionDao.clearUserSubscriptions(userId)
    }

    override suspend fun unsubscribe(userId: String, teamName: String) {
        subscriptionDao.unsubscribeFromTeam(userId, teamName)
    }
}