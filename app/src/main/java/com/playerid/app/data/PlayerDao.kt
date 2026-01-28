package com.playerid.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE isActive = 1 ORDER BY number ASC")
    fun getAllActivePlayers(): Flow<List<Player>>

    @Query("SELECT COUNT(*) FROM players")
    suspend fun getPlayerCount(): Int
    
    @Query("SELECT * FROM players WHERE team = :teamName AND isActive = 1")
    fun getPlayersByTeam(teamName: String): Flow<List<Player>>
    
    @Query("SELECT * FROM players WHERE number = :number AND team = :team AND isActive = 1 LIMIT 1")
    suspend fun getPlayerByNumber(number: String, team: String): Player?
    
    @Query("SELECT * FROM players WHERE name LIKE '%' || :searchQuery || '%' OR number LIKE '%' || :searchQuery || '%'")
    fun searchPlayers(searchQuery: String): Flow<List<Player>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayer(player: Player)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlayers(players: List<Player>)
    
    @Update
    suspend fun updatePlayer(player: Player)
    
    @Delete
    suspend fun deletePlayer(player: Player)
    
    @Query("DELETE FROM players WHERE id = :playerId")
    suspend fun deletePlayerById(playerId: String)
    
    @Query("SELECT DISTINCT team FROM players WHERE isActive = 1")
    suspend fun getAllTeams(): List<String>
    
    @Query("SELECT COUNT(*) FROM players WHERE team = :teamName AND isActive = 1")
    suspend fun getTeamPlayerCount(teamName: String): Int

    @Query("UPDATE players SET team = :newName WHERE team = :oldName")
    suspend fun renameTeam(oldName: String, newName: String)

    @Query("DELETE FROM players WHERE team = :teamName")
    suspend fun deleteTeam(teamName: String)
}