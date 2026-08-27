package com.playerid.app.domain.team

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryBrowsingStateHolderTest {
    @Test
    fun selectionLoadsEachLevelAndBackClearsDescendants() = runTest {
        val repository = BrowsingRepository()
        val holder = MemoryBrowsingStateHolder(repository, backgroundScope)
        val child = child("child-1")
        val season = season("season-1", child.id)
        val game = game("game-1", season.id)

        repository.children.value = listOf(child)
        holder.selectChild(child)
        runCurrent()
        repository.seasons.getValue(child.id).value = listOf(season)
        holder.selectSeason(season)
        runCurrent()
        repository.games.getValue(season.id).value = listOf(game)
        holder.selectGame(game)
        runCurrent()

        assertEquals(game, holder.state.value.selectedGame)
        holder.goBackToSeasons()
        assertEquals(child, holder.state.value.selectedChild)
        assertNull(holder.state.value.selectedSeason)
        assertEquals(emptyList(), holder.state.value.games)
        assertEquals(emptyList(), holder.state.value.memories)
    }

    @Test
    fun selectingAnotherChildCancelsPreviousSeasonCollection() = runTest {
        val repository = BrowsingRepository()
        val holder = MemoryBrowsingStateHolder(repository, backgroundScope)
        val first = child("first")
        val second = child("second")

        holder.selectChild(first)
        runCurrent()
        holder.selectChild(second)
        runCurrent()
        repository.seasons.getValue(first.id).value = listOf(season("stale", first.id))
        repository.seasons.getValue(second.id).value = listOf(season("current", second.id))
        runCurrent()

        assertEquals(second, holder.state.value.selectedChild)
        assertEquals(listOf("current"), holder.state.value.seasons.map { it.id })
    }

    private fun child(id: String) = ChildProfileRecord(id, id, 0L, 0L)

    private fun season(id: String, childId: String) = SportSeasonProfile(
        id = id,
        childId = childId,
        sportName = "Football",
        seasonLabel = "2026",
        teamName = "Tigers",
        createdAt = 0L,
        updatedAt = 0L
    )

    private fun game(id: String, seasonId: String) = GameScheduleProfile(
        id = id,
        sportSeasonId = seasonId,
        opponentName = "Lions",
        gameLabel = "vs Lions",
        scheduledStartMs = 1L,
        scheduledEndMs = 2L,
        createdAt = 0L,
        updatedAt = 0L
    )

    private class BrowsingRepository : ScheduleStorageRepository {
        val children = MutableStateFlow<List<ChildProfileRecord>>(emptyList())
        val seasons = mutableMapOf<String, MutableStateFlow<List<SportSeasonProfile>>>()
        val games = mutableMapOf<String, MutableStateFlow<List<GameScheduleProfile>>>()
        val memories = mutableMapOf<String, MutableStateFlow<List<MemoryItemProfile>>>()

        override fun observeActiveChildren(): Flow<List<ChildProfileRecord>> = children
        override fun observeSeasonsForChild(childId: String) =
            seasons.getOrPut(childId) { MutableStateFlow(emptyList()) }

        override fun observeGamesForSeason(seasonId: String) =
            games.getOrPut(seasonId) { MutableStateFlow(emptyList()) }

        override fun observeGamesForTeam(teamName: String): Flow<List<GameScheduleProfile>> =
            MutableStateFlow(emptyList())

        override fun observeMemoriesForGame(gameId: String) =
            memories.getOrPut(gameId) { MutableStateFlow(emptyList()) }

        override suspend fun findActiveSeasonForTeam(teamName: String): SportSeasonProfile? = null
        override suspend fun findGamesSince(windowStartMs: Long): List<GameScheduleProfile> = emptyList()
        override suspend fun findMemoryByMediaIdentifier(identifier: String): MemoryItemProfile? = null
        override suspend fun findMemoryItems(ids: List<String>): List<MemoryItemProfile> = emptyList()
        override suspend fun saveChild(child: ChildProfileRecord) = Unit
        override suspend fun saveSeason(season: SportSeasonProfile) = Unit
        override suspend fun saveGames(games: List<GameScheduleProfile>) = Unit
        override suspend fun saveMemories(memories: List<MemoryItemProfile>) = Unit
        override suspend fun deleteMemories(ids: List<String>) = Unit
    }
}