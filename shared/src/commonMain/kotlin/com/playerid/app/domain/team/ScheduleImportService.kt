package com.playerid.app.domain.team

private const val DEFAULT_CHILD_ID = "default-child"

class ScheduleImportService(
    private val repository: ScheduleStorageRepository
) {
    suspend fun import(
        teamName: String,
        sportName: String,
        entries: List<ScheduleImportEntry>,
        gameIds: List<String>,
        seasonYear: Int,
        timestamp: Long
    ): Int {
        if (entries.isEmpty()) return 0
        require(gameIds.size == entries.size) { "A game ID is required for each schedule entry" }

        repository.saveChild(
            ChildProfileRecord(
                id = DEFAULT_CHILD_ID,
                displayName = "My Child",
                createdAt = timestamp,
                updatedAt = timestamp
            )
        )

        val season = repository.findActiveSeasonForTeam(teamName) ?: SportSeasonProfile(
            id = "season-${teamName.lowercase().replace(" ", "-")}-$seasonYear",
            childId = DEFAULT_CHILD_ID,
            sportName = sportName,
            seasonLabel = "$seasonYear Season",
            teamName = teamName,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        repository.saveSeason(season)

        val games = entries.mapIndexed { index, entry ->
            GameScheduleProfile(
                id = gameIds[index],
                sportSeasonId = season.id,
                opponentName = entry.opponent,
                gameLabel = entry.gameLabel.ifBlank { "vs ${entry.opponent}" },
                scheduledStartMs = entry.startMs,
                scheduledEndMs = entry.endMs,
                locationName = entry.locationName,
                locationLat = entry.latitude,
                locationLng = entry.longitude,
                source = "uploaded",
                createdAt = timestamp,
                updatedAt = timestamp
            )
        }
        repository.saveGames(games)
        return games.size
    }
}