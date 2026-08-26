package com.playerid.app.domain.team

import com.playerid.app.platform.MediaReference
import kotlinx.coroutines.flow.Flow

data class ChildProfileRecord(
    val id: String,
    val displayName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = true
)

data class SportSeasonProfile(
    val id: String,
    val childId: String,
    val sportName: String,
    val seasonLabel: String,
    val teamName: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean = true
)

data class GameScheduleProfile(
    val id: String,
    val sportSeasonId: String,
    val opponentName: String,
    val gameLabel: String,
    val scheduledStartMs: Long,
    val scheduledEndMs: Long,
    val locationName: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val source: String = "manual",
    val createdAt: Long,
    val updatedAt: Long
)

data class MemoryItemProfile(
    val id: String,
    val media: MediaReference,
    val platformMediaId: Long,
    val displayName: String,
    val dateTakenMs: Long,
    val dateAddedMs: Long,
    val bucketName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val sportSeasonId: String? = null,
    val gameScheduleId: String? = null,
    val categorizationSource: String = "unassigned",
    val autoScore: Double = 0.0,
    val needsReview: Boolean = true,
    val reviewedAtMs: Long? = null,
    val createdAt: Long,
    val updatedAt: Long
)

interface ScheduleStorageRepository {
    fun observeActiveChildren(): Flow<List<ChildProfileRecord>>
    fun observeSeasonsForChild(childId: String): Flow<List<SportSeasonProfile>>
    fun observeGamesForSeason(seasonId: String): Flow<List<GameScheduleProfile>>
    fun observeGamesForTeam(teamName: String): Flow<List<GameScheduleProfile>>
    fun observeMemoriesForGame(gameId: String): Flow<List<MemoryItemProfile>>
}