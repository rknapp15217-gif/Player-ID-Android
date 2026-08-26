package com.playerid.app.domain.team

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class MemoryReviewServiceTest {
    @Test
    fun acceptMarksExistingMemoriesReviewed() = runTest {
        val repository = RecordingScheduleStorageRepository(
            memories = mutableListOf(memory("one"), memory("two"))
        )

        val acceptedCount = MemoryReviewService(repository).accept(listOf("one", "missing"), 42L)

        assertEquals(1, acceptedCount)
        val accepted = repository.memories.single { it.id == "one" }
        assertFalse(accepted.needsReview)
        assertEquals("auto_schedule_accepted", accepted.categorizationSource)
        assertEquals(42L, accepted.reviewedAtMs)
        assertEquals(42L, accepted.updatedAt)
    }

    @Test
    fun skipDeletesOnlyRequestedMemories() = runTest {
        val repository = RecordingScheduleStorageRepository(
            memories = mutableListOf(memory("one"), memory("two"))
        )

        MemoryReviewService(repository).skip(listOf("one"))

        assertEquals(listOf("two"), repository.memories.map { it.id })
    }

    private fun memory(id: String) = MemoryItemProfile(
        id = id,
        media = MediaReference("content://media/$id", MediaKind.IMAGE, "image/jpeg"),
        platformMediaId = 1L,
        displayName = "$id.jpg",
        dateTakenMs = 1L,
        dateAddedMs = 1L,
        createdAt = 1L,
        updatedAt = 1L
    )

    private class RecordingScheduleStorageRepository(
        val memories: MutableList<MemoryItemProfile>
    ) : ScheduleStorageRepository {
        override fun observeActiveChildren(): Flow<List<ChildProfileRecord>> = flowOf(emptyList())
        override fun observeSeasonsForChild(childId: String): Flow<List<SportSeasonProfile>> = flowOf(emptyList())
        override fun observeGamesForSeason(seasonId: String): Flow<List<GameScheduleProfile>> = flowOf(emptyList())
        override fun observeGamesForTeam(teamName: String): Flow<List<GameScheduleProfile>> = flowOf(emptyList())
        override fun observeMemoriesForGame(gameId: String): Flow<List<MemoryItemProfile>> = flowOf(emptyList())
        override suspend fun findActiveSeasonForTeam(teamName: String): SportSeasonProfile? = null
        override suspend fun findGamesSince(windowStartMs: Long): List<GameScheduleProfile> = emptyList()
        override suspend fun findMemoryByMediaIdentifier(identifier: String): MemoryItemProfile? =
            memories.firstOrNull { it.media.identifier == identifier }

        override suspend fun findMemoryItems(ids: List<String>): List<MemoryItemProfile> =
            memories.filter { it.id in ids }

        override suspend fun saveChild(child: ChildProfileRecord) = Unit
        override suspend fun saveSeason(season: SportSeasonProfile) = Unit
        override suspend fun saveGames(games: List<GameScheduleProfile>) = Unit

        override suspend fun saveMemories(memories: List<MemoryItemProfile>) {
            memories.forEach { memory ->
                this.memories.removeAll { it.id == memory.id }
                this.memories += memory
            }
        }

        override suspend fun deleteMemories(ids: List<String>) {
            memories.removeAll { it.id in ids }
        }
    }
}