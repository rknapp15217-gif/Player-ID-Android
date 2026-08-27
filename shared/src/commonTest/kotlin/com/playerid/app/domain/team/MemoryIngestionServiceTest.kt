package com.playerid.app.domain.team

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemoryIngestionServiceTest {
    @Test
    fun savesMatchedMemoryAndBuildsReviewGroup() = runTest {
        val repository = RecordingRepository()
        val game = game("game-1")

        val result = MemoryIngestionService(repository).ingest(
            candidates = listOf(candidate("one", dateAddedMs = 9L)),
            candidateIds = listOf("memory-1"),
            games = listOf(game),
            previousMaxDateAddedMs = 5L,
            timestamp = 42L
        )

        val saved = repository.savedMemories.single()
        assertEquals(1, result.savedMemoryCount)
        assertEquals(9L, result.maxDateAddedMs)
        assertEquals("game-1", saved.gameScheduleId)
        assertEquals("season-1", saved.sportSeasonId)
        assertEquals("auto_schedule_pending", saved.categorizationSource)
        assertEquals(listOf("memory-1"), result.groups.single().memoryIds)
    }

    @Test
    fun advancesWatermarkButSkipsExistingMedia() = runTest {
        val existing = candidate("existing", dateAddedMs = 20L)
        val repository = RecordingRepository(existingIdentifiers = setOf(existing.media.identifier))

        val result = MemoryIngestionService(repository).ingest(
            candidates = listOf(existing),
            candidateIds = listOf("unused-id"),
            games = listOf(game("game-1")),
            previousMaxDateAddedMs = 5L,
            timestamp = 42L
        )

        assertEquals(20L, result.maxDateAddedMs)
        assertTrue(repository.savedMemories.isEmpty())
        assertTrue(result.groups.isEmpty())
    }

    @Test
    fun lowScoreMemoryRemainsUnassignedAndNeedsReview() = runTest {
        val repository = RecordingRepository()

        val result = MemoryIngestionService(repository).ingest(
            candidates = listOf(candidate("late", dateTakenMs = 86_400_000L)),
            candidateIds = listOf("memory-2"),
            games = listOf(game("game-1")),
            previousMaxDateAddedMs = 0L,
            timestamp = 42L
        )

        val saved = repository.savedMemories.single()
        assertEquals(null, saved.gameScheduleId)
        assertEquals("unassigned", saved.categorizationSource)
        assertTrue(saved.needsReview)
        assertTrue(result.groups.isEmpty())
    }

    private fun candidate(
        name: String,
        dateTakenMs: Long = 5_400_000L,
        dateAddedMs: Long = 5_400_000L
    ) = MemoryIngestionCandidate(
        media = MediaReference("content://media/$name", MediaKind.IMAGE, "image/jpeg"),
        platformMediaId = 1L,
        displayName = "$name.jpg",
        dateTakenMs = dateTakenMs,
        dateAddedMs = dateAddedMs
    )

    private fun game(id: String) = GameScheduleProfile(
        id = id,
        sportSeasonId = "season-1",
        opponentName = "Tigers",
        gameLabel = "vs Tigers",
        scheduledStartMs = 3_600_000L,
        scheduledEndMs = 7_200_000L,
        createdAt = 0L,
        updatedAt = 0L
    )

    private class RecordingRepository(
        private val existingIdentifiers: Set<String> = emptySet()
    ) : ScheduleStorageRepository {
        val savedMemories = mutableListOf<MemoryItemProfile>()

        override fun observeActiveChildren(): Flow<List<ChildProfileRecord>> = flowOf(emptyList())
        override fun observeSeasonsForChild(childId: String): Flow<List<SportSeasonProfile>> = flowOf(emptyList())
        override fun observeGamesForSeason(seasonId: String): Flow<List<GameScheduleProfile>> = flowOf(emptyList())
        override fun observeGamesForTeam(teamName: String): Flow<List<GameScheduleProfile>> = flowOf(emptyList())
        override fun observeMemoriesForGame(gameId: String): Flow<List<MemoryItemProfile>> = flowOf(emptyList())
        override suspend fun findActiveSeasonForTeam(teamName: String): SportSeasonProfile? = null
        override suspend fun findGamesSince(windowStartMs: Long): List<GameScheduleProfile> = emptyList()
        override suspend fun findMemoryByMediaIdentifier(identifier: String): MemoryItemProfile? =
            if (identifier in existingIdentifiers) savedMemory(identifier) else null

        override suspend fun findMemoryItems(ids: List<String>): List<MemoryItemProfile> = emptyList()
        override suspend fun saveChild(child: ChildProfileRecord) = Unit
        override suspend fun saveSeason(season: SportSeasonProfile) = Unit
        override suspend fun saveGames(games: List<GameScheduleProfile>) = Unit

        override suspend fun saveMemories(memories: List<MemoryItemProfile>) {
            savedMemories += memories
        }

        override suspend fun deleteMemories(ids: List<String>) = Unit

        private fun savedMemory(identifier: String) = MemoryItemProfile(
            id = "existing",
            media = MediaReference(identifier, MediaKind.IMAGE, "image/jpeg"),
            platformMediaId = 1L,
            displayName = "existing.jpg",
            dateTakenMs = 1L,
            dateAddedMs = 1L,
            createdAt = 1L,
            updatedAt = 1L
        )
    }
}