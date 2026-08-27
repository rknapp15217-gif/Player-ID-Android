package com.playerid.app.domain.team

import com.playerid.app.platform.MediaReference

data class MemoryIngestionCandidate(
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
    val longitude: Double? = null
)

data class MemoryIngestionGroup(
    val game: GameScheduleProfile,
    val memoryIds: List<String>
)

data class MemoryIngestionResult(
    val savedMemoryCount: Int,
    val maxDateAddedMs: Long,
    val groups: List<MemoryIngestionGroup>
)

class MemoryIngestionService(
    private val repository: ScheduleStorageRepository,
    private val matcher: MemoryGameMatcher = MemoryGameMatcher()
) {
    suspend fun ingest(
        candidates: List<MemoryIngestionCandidate>,
        candidateIds: List<String>,
        games: List<GameScheduleProfile>,
        previousMaxDateAddedMs: Long,
        timestamp: Long
    ): MemoryIngestionResult {
        require(candidateIds.size == candidates.size) { "A memory ID is required for each candidate" }

        val memories = mutableListOf<MemoryItemProfile>()
        val groupedIds = linkedMapOf<String, Pair<GameScheduleProfile, MutableList<String>>>()
        var maxDateAddedMs = previousMaxDateAddedMs

        candidates.forEachIndexed { index, candidate ->
            maxDateAddedMs = maxOf(maxDateAddedMs, candidate.dateAddedMs)
            if (repository.findMemoryByMediaIdentifier(candidate.media.identifier) != null) {
                return@forEachIndexed
            }

            val match = matcher.findBestMatch(
                dateTakenMs = candidate.dateTakenMs,
                latitude = candidate.latitude,
                longitude = candidate.longitude,
                games = games
            )
            val shouldPrompt = match != null && match.score >= 0.55
            val memoryId = candidateIds[index]
            memories += MemoryItemProfile(
                id = memoryId,
                media = candidate.media,
                platformMediaId = candidate.platformMediaId,
                displayName = candidate.displayName,
                dateTakenMs = candidate.dateTakenMs,
                dateAddedMs = candidate.dateAddedMs,
                bucketName = candidate.bucketName,
                width = candidate.width,
                height = candidate.height,
                durationMs = candidate.durationMs,
                latitude = candidate.latitude,
                longitude = candidate.longitude,
                sportSeasonId = if (shouldPrompt) match?.game?.sportSeasonId else null,
                gameScheduleId = if (shouldPrompt) match?.game?.id else null,
                categorizationSource = if (shouldPrompt) "auto_schedule_pending" else "unassigned",
                autoScore = match?.score ?: 0.0,
                needsReview = true,
                createdAt = timestamp,
                updatedAt = timestamp
            )

            if (shouldPrompt) {
                val game = requireNotNull(match).game
                groupedIds.getOrPut(game.id) { game to mutableListOf() }.second += memoryId
            }
        }

        if (memories.isNotEmpty()) repository.saveMemories(memories)
        return MemoryIngestionResult(
            savedMemoryCount = memories.size,
            maxDateAddedMs = maxDateAddedMs,
            groups = groupedIds.values
                .map { (game, ids) -> MemoryIngestionGroup(game, ids) }
                .sortedByDescending { it.memoryIds.size }
        )
    }
}