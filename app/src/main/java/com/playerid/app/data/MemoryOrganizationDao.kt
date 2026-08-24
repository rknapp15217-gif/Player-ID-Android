package com.playerid.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryOrganizationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChildProfile(child: ChildProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSportSeason(season: SportSeason)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGameSchedule(game: GameSchedule)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGameSchedules(games: List<GameSchedule>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemoryItem(item: MemoryItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemoryItems(items: List<MemoryItem>)

    @Update
    suspend fun updateMemoryItem(item: MemoryItem)

    @Query("SELECT * FROM child_profiles WHERE isActive = 1 ORDER BY displayName ASC")
    fun getActiveChildren(): Flow<List<ChildProfile>>

    @Query("SELECT * FROM sport_seasons WHERE childId = :childId AND isActive = 1 ORDER BY seasonLabel DESC, sportName ASC")
    fun getSeasonsForChild(childId: String): Flow<List<SportSeason>>

    @Query("SELECT * FROM game_schedules WHERE sportSeasonId = :sportSeasonId ORDER BY scheduledStartMs DESC")
    fun getGamesForSeason(sportSeasonId: String): Flow<List<GameSchedule>>

    @Query("""
        SELECT game_schedules.* FROM game_schedules
        INNER JOIN sport_seasons ON sport_seasons.id = game_schedules.sportSeasonId
        WHERE sport_seasons.teamName = :teamName AND sport_seasons.isActive = 1
        ORDER BY game_schedules.scheduledStartMs ASC
    """)
    fun getGamesForTeam(teamName: String): Flow<List<GameSchedule>>

    @Query("SELECT * FROM game_schedules WHERE sportSeasonId = :sportSeasonId ORDER BY scheduledStartMs DESC")
    suspend fun getGamesForSeasonSnapshot(sportSeasonId: String): List<GameSchedule>

    @Query("SELECT * FROM memory_items WHERE sportSeasonId = :sportSeasonId ORDER BY dateTakenMs DESC")
    fun getMemoryForSeason(sportSeasonId: String): Flow<List<MemoryItem>>

    @Query("SELECT * FROM memory_items WHERE gameScheduleId = :gameId ORDER BY dateTakenMs DESC")
    fun getMemoryForGame(gameId: String): Flow<List<MemoryItem>>

    @Query("SELECT * FROM memory_items WHERE needsReview = 1 ORDER BY autoScore ASC, dateTakenMs DESC")
    fun getNeedsReviewMemory(): Flow<List<MemoryItem>>

    @Query("SELECT * FROM memory_items WHERE contentUri = :contentUri LIMIT 1")
    suspend fun findMemoryByUri(contentUri: String): MemoryItem?

    @Query("SELECT * FROM memory_items WHERE id IN (:ids)")
    suspend fun getMemoryItemsByIds(ids: List<String>): List<MemoryItem>

    @Query("DELETE FROM memory_items WHERE id IN (:ids)")
    suspend fun deleteMemoryItemsByIds(ids: List<String>)

    @Query("SELECT * FROM sport_seasons WHERE teamName = :teamName AND isActive = 1 ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getActiveSeasonForTeam(teamName: String): SportSeason?

    @Query("SELECT * FROM sport_seasons WHERE id = :seasonId LIMIT 1")
    suspend fun getSeasonById(seasonId: String): SportSeason?

    @Query("SELECT * FROM game_schedules WHERE id = :gameId LIMIT 1")
    suspend fun getGameById(gameId: String): GameSchedule?

    @Query("SELECT * FROM game_schedules WHERE scheduledEndMs >= :windowStartMs ORDER BY scheduledStartMs ASC")
    suspend fun getGamesSince(windowStartMs: Long): List<GameSchedule>

    @Query("SELECT * FROM media_ingestion_state WHERE `key` = 'default' LIMIT 1")
    suspend fun getIngestionState(): MediaIngestionState?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertIngestionState(state: MediaIngestionState)
}
