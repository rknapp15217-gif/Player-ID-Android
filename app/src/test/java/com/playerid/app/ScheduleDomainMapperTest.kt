package com.playerid.app

import com.playerid.app.data.GameSchedule
import com.playerid.app.data.ChildProfile
import com.playerid.app.data.SportSeason
import com.playerid.app.data.MemoryItem
import com.playerid.app.data.repositories.toEntity
import com.playerid.app.data.repositories.toProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ScheduleDomainMapperTest {
    @Test
    fun entityProfileRoundTripPreservesAllFields() {
        val schedule = GameSchedule(
            id = "game-1",
            sportSeasonId = "season-1",
            opponentName = "North Hills",
            gameLabel = "Varsity vs North Hills",
            scheduledStartMs = 1_000L,
            scheduledEndMs = 2_000L,
            locationName = "Newman Stadium",
            locationLat = 40.6,
            locationLng = -80.0,
            source = "website",
            createdAt = 3_000L,
            updatedAt = 4_000L
        )

        assertEquals(schedule, schedule.toProfile().toEntity())
    }

    @Test
    fun hierarchyRoundTripsPreserveAllFields() {
        val child = ChildProfile("child-1", "Taylor", 1L, 2L, false)
        val season = SportSeason(
            id = "season-1",
            childId = child.id,
            sportName = "Football",
            seasonLabel = "2026",
            teamName = "Tigers",
            createdAt = 3L,
            updatedAt = 4L,
            isActive = false
        )

        assertEquals(child, child.toProfile().toEntity())
        assertEquals(season, season.toProfile().toEntity())
    }

    @Test
    fun memoryRoundTripPreservesMediaAndOrganizationFields() {
        val memory = MemoryItem(
            id = "memory-1",
            contentUri = "content://media/1",
            mediaStoreId = 7L,
            mimeType = "video/mp4",
            displayName = "touchdown.mp4",
            dateTakenMs = 10L,
            dateAddedMs = 11L,
            bucketName = "PlayerID",
            width = 1920,
            height = 1080,
            durationMs = 12L,
            latitude = 40.6,
            longitude = -80.0,
            sportSeasonId = "season-1",
            gameScheduleId = "game-1",
            categorizationSource = "automatic",
            autoScore = 0.9,
            needsReview = false,
            reviewedAtMs = 13L,
            createdAt = 14L,
            updatedAt = 15L
        )

        assertEquals(memory, memory.toProfile().toEntity())
    }
}