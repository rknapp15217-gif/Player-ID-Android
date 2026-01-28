package com.playerid.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserTeamSubscriptionDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun subscribeToTeam(subscription: UserTeamSubscription)
    
    @Query("DELETE FROM user_team_subscriptions WHERE userId = :userId AND teamName = :teamName")
    suspend fun unsubscribeFromTeam(userId: String, teamName: String)
    
    @Query("SELECT * FROM user_team_subscriptions WHERE userId = :userId AND isActive = 1 ORDER BY subscribedAt DESC")
    fun getUserSubscriptions(userId: String): Flow<List<UserTeamSubscription>>
    
    @Query("""
        SELECT teams.* FROM teams 
        INNER JOIN user_team_subscriptions ON teams.name = user_team_subscriptions.teamName 
        WHERE user_team_subscriptions.userId = :userId 
        AND user_team_subscriptions.isActive = 1 
        AND teams.isActive = 1 
        ORDER BY user_team_subscriptions.subscribedAt DESC
    """)
    fun getUserSubscribedTeams(userId: String): Flow<List<Team>>
    
    @Query("""
        SELECT teams.id, teams.name, teams.description, teams.color, 
               teams.createdBy, teams.isActive, teams.isVerified, teams.isArchived,
               teams.createdAt, teams.updatedAt, teams.reportCount, teams.verificationCount,
               COUNT(players.id) as playerCount,
               MAX(players.updatedAt) as lastActivity
        FROM teams 
        INNER JOIN user_team_subscriptions ON teams.name = user_team_subscriptions.teamName
        LEFT JOIN players ON teams.name = players.team AND players.isActive = 1
        WHERE user_team_subscriptions.userId = :userId 
        AND user_team_subscriptions.isActive = 1 
        AND teams.isActive = 1 AND teams.isArchived = 0
        GROUP BY teams.id
        ORDER BY user_team_subscriptions.subscribedAt DESC
    """)
    fun getUserSubscribedTeamsWithStats(userId: String): Flow<List<TeamWithPlayerCount>>
    
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM user_team_subscriptions 
            WHERE userId = :userId AND teamName = :teamName AND isActive = 1
        )
    """)
    suspend fun isUserSubscribedToTeam(userId: String, teamName: String): Boolean
    
    @Query("SELECT COUNT(*) FROM user_team_subscriptions WHERE userId = :userId AND isActive = 1")
    suspend fun getUserSubscriptionCount(userId: String): Int
    
    @Query("""
        SELECT teamName FROM user_team_subscriptions 
        WHERE userId = :userId AND isActive = 1 
        ORDER BY subscribedAt DESC
    """)
    suspend fun getUserSubscribedTeamNames(userId: String): List<String>
}