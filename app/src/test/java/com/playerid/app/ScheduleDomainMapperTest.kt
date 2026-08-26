package com.playerid.app

import com.playerid.app.data.GameSchedule
import com.playerid.app.data.ChildProfile
import com.playerid.app.data.SportSeason
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
}