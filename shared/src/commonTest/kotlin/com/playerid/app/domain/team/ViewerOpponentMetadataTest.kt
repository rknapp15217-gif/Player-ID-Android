package com.playerid.app.domain.team

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ViewerOpponentMetadataTest {
    private val details = CaptureOrganization("Tigers", gameId = "game", opponentName = "Manual opponent")
    private val game = GameScheduleProfile(
        "game", "season", "Lions", "Final", 1, 2, createdAt = 1, updatedAt = 1
    )

    @Test
    fun scheduledOpponentOverridesManualAndReflectsRenames() {
        val snapshot = TeamWorkspaceSnapshot(games = listOf(game))
        assertEquals("Lions", snapshot.captureOpponentName(details))
        assertEquals("Bears", snapshot.copy(games = listOf(game.copy(opponentName = "Bears"))).captureOpponentName(details))
    }

    @Test
    fun manualOpponentWorksWithoutScheduledGame() {
        assertEquals("Manual opponent", TeamWorkspaceSnapshot().captureOpponentName(details))
    }

    @Test
    fun teamOnlyAssignmentHasNoInventedOpponent() {
        assertNull(TeamWorkspaceSnapshot().captureOpponentName(CaptureOrganization("Tigers")))
    }
}
