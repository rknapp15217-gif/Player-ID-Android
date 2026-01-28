package com.playerid.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TeamDao {
    
    @Query("SELECT * FROM teams WHERE isActive = 1 ORDER BY name ASC")
    fun getAllActiveTeams(): Flow<List<Team>>
    
    @Query("SELECT * FROM teams WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun getAllActiveTeamsByActivity(): Flow<List<Team>>
    
    @Query("SELECT * FROM teams WHERE name = :teamName AND isActive = 1")
    suspend fun getTeamByName(teamName: String): Team?
    
    @Query("SELECT * FROM teams WHERE id = :teamId")
    suspend fun getTeamById(teamId: String): Team?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeam(team: Team)
    
    @Update
    suspend fun updateTeam(team: Team)
    
    @Query("UPDATE teams SET isActive = 0, updatedAt = :timestamp WHERE name = :teamName")
    suspend fun deactivateTeam(teamName: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE teams SET name = :newName, updatedAt = :timestamp WHERE name = :oldName")
    suspend fun renameTeam(oldName: String, newName: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM teams WHERE isActive = 1")
    suspend fun getActiveTeamCount(): Int
    
    // Advanced queries for crowd-sourcing features
    @Query("""
        SELECT teams.id, teams.name, teams.description, teams.color, 
               teams.createdBy, teams.isActive, teams.isVerified, teams.isArchived,
               teams.createdAt, teams.updatedAt, teams.reportCount, teams.verificationCount,
               COUNT(players.id) as playerCount,
               MAX(players.updatedAt) as lastActivity
        FROM teams 
        LEFT JOIN players ON teams.name = players.team AND players.isActive = 1
        WHERE teams.isActive = 1 AND teams.isArchived = 0
        GROUP BY teams.id
        ORDER BY teams.isVerified DESC, playerCount DESC, teams.name ASC
    """)
    suspend fun getTeamsWithPlayerCounts(): List<TeamWithPlayerCount>
    
    @Query("""
        SELECT DISTINCT players.addedBy 
        FROM players 
        WHERE players.team = :teamName AND players.isActive = 1
        ORDER BY players.addedBy ASC
    """)
    suspend fun getTeamContributors(teamName: String): List<String>
    
    @Query("SELECT name FROM teams WHERE isActive = 1 AND isArchived = 0")
    suspend fun getAllActiveTeamNames(): List<String>
    
    @Query("UPDATE teams SET reportCount = reportCount + 1 WHERE name = :teamName")
    suspend fun reportTeam(teamName: String)
    
    @Query("UPDATE teams SET verificationCount = verificationCount + 1 WHERE name = :teamName")
    suspend fun verifyTeam(teamName: String)
    
    @Query("UPDATE teams SET isArchived = 1 WHERE name = :teamName")
    suspend fun archiveTeam(teamName: String)
    
    @Query("UPDATE teams SET lastActivityAt = :timestamp WHERE name = :teamName")
    suspend fun updateTeamActivity(teamName: String, timestamp: Long)
    
    @Query("""
        UPDATE teams SET isArchived = 1 
        WHERE lastActivityAt < :cutoffTimestamp 
        AND reportCount > verificationCount 
        AND isVerified = 0
    """)
    suspend fun archiveInactiveTeams(cutoffTimestamp: Long)
}

// Data class for team statistics
data class TeamWithPlayerCount(
    val id: String,
    val name: String,
    val description: String,
    val color: String,
    val createdBy: String,
    val isActive: Boolean,
    val isVerified: Boolean,
    val isArchived: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val reportCount: Int,
    val verificationCount: Int,
    val playerCount: Int,
    val lastActivity: Long?
)