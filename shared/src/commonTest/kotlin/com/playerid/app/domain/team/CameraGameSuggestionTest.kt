package com.playerid.app.domain.team

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CameraGameSuggestionTest {
    private val noon = Instant.parse("2026-10-19T12:00:00Z").toEpochMilliseconds()
    private val hour = 3_600_000L
    private val season = SportSeasonProfile("season", "child", "Soccer", "Fall", "Tigers", 1, 1)
    private val game = GameScheduleProfile("game", "season", "Lions", "vs Lions", noon, noon + hour, createdAt = 1, updatedAt = 1)
    private val snapshot = TeamWorkspaceSnapshot(seasons = listOf(season), games = listOf(game))

    @Test
    fun matchesAndroidTimeWindowIncludingExactBoundaries() {
        assertEquals(game, snapshot.suggestCameraGame("Tigers", noon - 3 * hour, "UTC"))
        assertEquals(game, snapshot.suggestCameraGame("Tigers", noon + 4 * hour, "UTC"))
        assertNull(snapshot.suggestCameraGame("Tigers", noon - 3 * hour - 1, "UTC"))
        assertNull(snapshot.suggestCameraGame("Tigers", noon + 4 * hour + 1, "UTC"))
    }

    @Test
    fun cameraSetupFollowsTheCurrentScheduledGame() {
        val manual = CaptureOrganization("Tigers", null, null, "player", "Hawks")
        val withSetup = snapshot.copy(cameraOrganization = manual)
        val applied = withSetup.cameraOrganizationForCurrentGame(noon, timeZoneId = "UTC")
        assertEquals(CaptureOrganization("Tigers", "season", "game", "player", "Lions"), applied)
        assertNull(withSetup.copy(cameraOrganization = applied).cameraOrganizationForCurrentGame(noon, timeZoneId = "UTC"))
        assertNull(withSetup.cameraOrganizationForCurrentGame(noon, setOf("game"), "UTC"))
        assertNull(withSetup.cameraOrganizationForCurrentGame(noon + 5 * hour, timeZoneId = "UTC"))
        assertNull(snapshot.cameraOrganizationForCurrentGame(noon, timeZoneId = "UTC"))
    }

    @Test
    fun onlyUsesSelectedTeamsActiveSeasons() {
        assertNull(snapshot.suggestCameraGame(null, noon, "UTC"))
        assertNull(snapshot.suggestCameraGame("Bears", noon, "UTC"))
        assertNull(snapshot.copy(seasons = listOf(season.copy(isActive = false)))
            .suggestCameraGame("Tigers", noon, "UTC"))
        assertNull(snapshot.copy(games = listOf(game.copy(sportSeasonId = "missing")))
            .suggestCameraGame("Tigers", noon, "UTC"))
    }

    @Test
    fun picksNearestStartRatherThanFirstImportedGame() {
        val later = game.copy(id = "later", scheduledStartMs = noon + hour, scheduledEndMs = noon + 2 * hour)
        val schedule = snapshot.copy(games = listOf(game, later))
        assertEquals(later, schedule.suggestCameraGame("Tigers", noon + hour, "UTC"))
    }

    @Test
    fun respectsLocalCalendarDateAtMidnight() {
        val start = Instant.parse("2026-10-20T00:30:00Z").toEpochMilliseconds()
        val schedule = snapshot.copy(games = listOf(game.copy(scheduledStartMs = start, scheduledEndMs = start + hour)))
        assertNull(schedule.suggestCameraGame("Tigers", start - hour, "UTC"))
        assertEquals("game", schedule.suggestCameraGame("Tigers", start - hour, "America/New_York")?.id)
    }

    @Test
    fun suggestionDoesNotModifySavedSetupOrExistingMemories() {
        val manual = CaptureOrganization("Tigers", opponentName = "Manual opponent")
        val state = snapshot.copy(cameraOrganization = manual, captureOrganizations = mapOf("photo.jpg" to manual))
        assertEquals(game, state.suggestCameraGame("Tigers", noon, "UTC"))
        assertEquals(manual, state.cameraOrganization)
        assertEquals(manual, state.captureOrganizations["photo.jpg"])
    }
}
