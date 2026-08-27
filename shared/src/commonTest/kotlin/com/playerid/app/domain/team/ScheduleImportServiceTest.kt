package com.playerid.app.domain.team

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ScheduleImportServiceTest {
    @Test
    fun createsDefaultHierarchyAndUploadedGames() = runTest {
        val repository = RecordingRepository()

        val count = ScheduleImportService(repository).import(
            teamName = "North Allegheny",
            sportName = "Football",
            entries = listOf(entry("Tigers", "")),
            gameIds = listOf("game-1"),
            seasonYear = 2026,
            timestamp = 42L
        )

        assertEquals(1, count)
        assertEquals("default-child", repository.savedChild?.id)
        assertEquals("season-north-allegheny-2026", repository.savedSeason?.id)
        assertEquals("Football", repository.savedSeason?.sportName)
        assertEquals("game-1", repository.savedGames.single().id)
        assertEquals("vs Tigers", repository.savedGames.single().gameLabel)
        assertEquals("uploaded", repository.savedGames.single().source)
        assertEquals(42L, repository.savedGames.single().createdAt)
    }

    @Test
    fun reusesExistingSeasonWithoutReplacingItsFields() = runTest {
        val existing = SportSeasonProfile(
            id = "existing-season",
            childId = "child-2",
            sportName = "Soccer",
            seasonLabel = "Fall",
            teamName = "Tigers",
            createdAt = 1L,
            updatedAt = 2L
        )
        val repository = RecordingRepository(activeSeason = existing)

        ScheduleImportService(repository).import(
            teamName = "Tigers",
            sportName = "Football",
            entries = listOf(entry("Lions", "Varsity @ Lions")),
            gameIds = listOf("game-2"),
            seasonYear = 2026,
            timestamp = 42L
        )

        assertEquals(existing, repository.savedSeason)
        assertEquals("existing-season", repository.savedGames.single().sportSeasonId)
        assertEquals("Varsity @ Lions", repository.savedGames.single().gameLabel)
    }

    @Test
    fun emptyImportDoesNotWriteStorage() = runTest {
        val repository = RecordingRepository()

        val count = ScheduleImportService(repository).import(
            teamName = "Tigers",
            sportName = "Football",
            entries = emptyList(),
            gameIds = emptyList(),
            seasonYear = 2026,
            timestamp = 42L
        )

        assertEquals(0, count)
        assertEquals(null, repository.savedChild)
        assertEquals(emptyList(), repository.savedGames)
    }

    private fun entry(opponent: String, label: String) = ScheduleImportEntry(
        gameLabel = label,
        opponent = opponent,
        startMs = 1_000L,
        endMs = 2_000L,
        locationName = "Stadium",
        latitude = 40.0,
        longitude = -80.0
    )

    private class RecordingRepository(
        private val activeSeason: SportSeasonProfile? = null
    ) : ScheduleStorageRepository {
        var savedChild: ChildProfileRecord? = null
        var savedSeason: SportSeasonProfile? = null
        var savedGames = emptyList<GameScheduleProfile>()

        override fun observeActiveChildren(): Flow<List<ChildProfileRecord>> = flowOf(emptyList())
        override fun observeSeasonsForChild(childId: String): Flow<List<SportSeasonProfile>> = flowOf(emptyList())
        override fun observeGamesForSeason(seasonId: String): Flow<List<GameScheduleProfile>> = flowOf(emptyList())
        override fun observeGamesForTeam(teamName: String): Flow<List<GameScheduleProfile>> = flowOf(emptyList())
        override fun observeMemoriesForGame(gameId: String): Flow<List<MemoryItemProfile>> = flowOf(emptyList())
        override suspend fun findActiveSeasonForTeam(teamName: String) = activeSeason
        override suspend fun findGamesSince(windowStartMs: Long): List<GameScheduleProfile> = emptyList()
        override suspend fun findMemoryByMediaIdentifier(identifier: String): MemoryItemProfile? = null
        override suspend fun findMemoryItems(ids: List<String>): List<MemoryItemProfile> = emptyList()

        override suspend fun saveChild(child: ChildProfileRecord) {
            savedChild = child
        }

        override suspend fun saveSeason(season: SportSeasonProfile) {
            savedSeason = season
        }

        override suspend fun saveGames(games: List<GameScheduleProfile>) {
            savedGames = games
        }

        override suspend fun saveMemories(memories: List<MemoryItemProfile>) = Unit
        override suspend fun deleteMemories(ids: List<String>) = Unit
    }
}