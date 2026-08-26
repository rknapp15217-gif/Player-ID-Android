package com.playerid.app.data.repositories

import com.playerid.app.data.PlayerDao
import com.playerid.app.data.MemoryOrganizationDao
import com.playerid.app.data.TeamDao
import com.playerid.app.data.UserTeamSubscriptionDao
import com.playerid.app.domain.team.PlayerProfile
import com.playerid.app.domain.team.ChildProfileRecord
import com.playerid.app.domain.team.TeamProfile
import com.playerid.app.domain.team.TeamRosterRepository
import com.playerid.app.domain.team.TeamSubscription
import com.playerid.app.domain.team.TeamSubscriptionRepository
import com.playerid.app.domain.team.GameScheduleProfile
import com.playerid.app.domain.team.MemoryItemProfile
import com.playerid.app.domain.team.ScheduleStorageRepository
import com.playerid.app.domain.team.SportSeasonProfile
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

class RoomScheduleStorageRepository(
    private val memoryOrganizationDao: MemoryOrganizationDao
) : ScheduleStorageRepository {
    override fun observeActiveChildren() =
        memoryOrganizationDao.getActiveChildren().map { children ->
            children.map { it.toProfile() }
        }

    override fun observeSeasonsForChild(childId: String) =
        memoryOrganizationDao.getSeasonsForChild(childId).map { seasons ->
            seasons.map { it.toProfile() }
        }

    override fun observeGamesForSeason(seasonId: String) =
        memoryOrganizationDao.getGamesForSeason(seasonId).map { games ->
            games.map { it.toProfile() }
        }

    override fun observeGamesForTeam(teamName: String): Flow<List<GameScheduleProfile>> =
        memoryOrganizationDao.getGamesForTeam(teamName).map { games ->
            games.map { it.toProfile() }
        }

    override fun observeMemoriesForGame(gameId: String) =
        memoryOrganizationDao.getMemoryForGame(gameId).map { memories ->
            memories.map { it.toProfile() }
        }

    override suspend fun findActiveSeasonForTeam(teamName: String) =
        memoryOrganizationDao.getActiveSeasonForTeam(teamName)?.toProfile()

    override suspend fun findGamesSince(windowStartMs: Long) =
        memoryOrganizationDao.getGamesSince(windowStartMs).map { it.toProfile() }

    override suspend fun findMemoryByMediaIdentifier(identifier: String) =
        memoryOrganizationDao.findMemoryByUri(identifier)?.toProfile()

    override suspend fun findMemoryItems(ids: List<String>) =
        memoryOrganizationDao.getMemoryItemsByIds(ids).map { it.toProfile() }

    override suspend fun saveChild(child: ChildProfileRecord) {
        memoryOrganizationDao.upsertChildProfile(child.toEntity())
    }

    override suspend fun saveSeason(season: SportSeasonProfile) {
        memoryOrganizationDao.upsertSportSeason(season.toEntity())
    }

    override suspend fun saveGames(games: List<GameScheduleProfile>) {
        memoryOrganizationDao.upsertGameSchedules(games.map { it.toEntity() })
    }

    override suspend fun saveMemories(memories: List<MemoryItemProfile>) {
        memoryOrganizationDao.upsertMemoryItems(memories.map { it.toEntity() })
    }

    override suspend fun deleteMemories(ids: List<String>) {
        memoryOrganizationDao.deleteMemoryItemsByIds(ids)
    }
}