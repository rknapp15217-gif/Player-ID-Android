package com.playerid.app.domain.team

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlin.test.Test
import kotlin.test.assertEquals

class CaptureLibraryGroupsTest {
    private fun media(name: String) = MediaReference("file:///media/$name.jpg", MediaKind.IMAGE)

    @Test
    fun groupsByTeamSeasonAndOpponentWithoutDroppingUnassignedMedia() {
        val captures = listOf(media("first"), media("unassigned"), media("second"), media("other-team"), media("other-season"))
        val snapshot = TeamWorkspaceSnapshot(captureOrganizations = mapOf(
            "first.jpg" to CaptureOrganization("Tigers", "fall", opponentName = "Lions"),
            "second.jpg" to CaptureOrganization("Tigers", "fall", opponentName = "Lions"),
            "other-team.jpg" to CaptureOrganization("Bears", "fall", opponentName = "Lions"),
            "other-season.jpg" to CaptureOrganization("Tigers", "spring", opponentName = "Lions")
        ))
        val groups = groupCaptureLibrary(captures, snapshot)
        assertEquals(4, groups.size)
        assertEquals(listOf(captures[0], captures[2]), groups[0].captures)
        assertEquals("Bears", groups[1].teamName)
        assertEquals("spring", groups[2].seasonId)
        assertEquals(listOf(captures[1]), groups[3].captures)
        assertEquals(null, groups[3].teamName)
        assertEquals(captures.toSet(), groups.flatMap { it.captures }.toSet())
    }

    @Test
    fun usesScheduledOpponentAndKeepsInputOrderWithinGroups() {
        val captures = listOf(media("second"), media("first"))
        val game = GameScheduleProfile("game", "fall", "New opponent", "Game", 1, 2, createdAt = 1, updatedAt = 1)
        val snapshot = TeamWorkspaceSnapshot(
            games = listOf(game),
            captureOrganizations = captures.associate {
                captureFileKey(it) to CaptureOrganization("Tigers", "fall", "game", opponentName = "Old opponent")
            }
        )
        val groups = groupCaptureLibrary(captures, snapshot)
        assertEquals("New opponent", groups.single().opponent)
        assertEquals(captures, groups.single().captures)
        assertEquals(emptyList(), groupCaptureLibrary(emptyList(), snapshot))
    }
}
