package com.playerid.app.domain.team

import kotlinx.coroutines.flow.Flow

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

interface ScheduleStorageRepository {
    fun observeGamesForTeam(teamName: String): Flow<List<GameScheduleProfile>>
}