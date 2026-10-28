package com.playerid.app.domain.team

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReelActionSearchTest {
    private val today = LocalDate(2026, 10, 26)
    private fun video(instant: String, suffix: String): MediaReference =
        MediaReference("file:///media/${Instant.parse(instant).toEpochMilliseconds()}-$suffix.mov", MediaKind.VIDEO)

    @Test
    fun createReelOfAllVideosFromTodaySelectsOnlyTodaysVideosIncludingUntagged() {
        val first = video("2026-10-26T00:00:00Z", "first")
        val second = video("2026-10-26T23:59:59Z", "second")
        val yesterday = video("2026-10-25T23:59:59Z", "yesterday")
        val tomorrow = video("2026-10-27T00:00:00Z", "tomorrow")
        val undated = MediaReference("file:///media/undated.mov", MediaKind.VIDEO)
        val command = parseMemoryReelCommand("create reel of all videos from today")!!
        assertEquals(listOf(first, second), searchCaptureLibrary(
            listOf(yesterday, first, second, tomorrow, undated), TeamWorkspaceSnapshot(),
            command.searchQuery, today, TimeZone.UTC
        ))
    }

    @Test
    fun todayUsesLocalMidnightRatherThanUtcOrTheLast24Hours() {
        val previousDay = video("2026-10-26T03:59:59Z", "previous")
        val start = video("2026-10-26T04:00:00Z", "start")
        val end = video("2026-10-27T03:59:59Z", "end")
        val nextDay = video("2026-10-27T04:00:00Z", "next")
        assertEquals(listOf(start, end), searchCaptureLibrary(
            listOf(previousDay, start, end, nextDay), TeamWorkspaceSnapshot(),
            "all videos recorded today", today, TimeZone.of("America/New_York")
        ))
    }

    @Test
    fun allPlaysAgainstAnOpponentIncludesUntaggedClipsAndExcludesOtherOpponents() {
        val first = video("2026-10-26T14:00:00Z", "first")
        val second = video("2026-10-26T15:00:00Z", "second")
        val other = video("2026-10-26T16:00:00Z", "other")
        val unassigned = video("2026-10-26T17:00:00Z", "unassigned")
        val snapshot = TeamWorkspaceSnapshot(
            captureOrganizations = mapOf(
                captureFileKey(first) to CaptureOrganization("Tigers", opponentName = "Pine-Richland"),
                captureFileKey(second) to CaptureOrganization("Tigers", opponentName = "Pine-Richland"),
                captureFileKey(other) to CaptureOrganization("Tigers", opponentName = "Lions")
            ),
            captureMomentTags = mapOf(captureFileKey(first) to "Goal")
        )
        for (name in listOf("Pine Richland", "Pine-Richland", "PR", "PineRichland")) {
            val command = parseMemoryReelCommand("Please create a reel of all plays against $name")!!
            assertEquals(listOf(first, second), searchCaptureLibrary(
                listOf(first, second, other, unassigned), snapshot, command.searchQuery, today, TimeZone.UTC
            ), name)
        }
    }

    @Test
    fun opponentNamesAreNotMistakenForMomentTagsAndDateStillIntersects() {
        val current = video("2026-10-26T12:00:00Z", "current")
        val previous = video("2026-10-25T12:00:00Z", "previous")
        val snapshot = TeamWorkspaceSnapshot(captureOrganizations = listOf(current, previous).associate {
            captureFileKey(it) to CaptureOrganization("Tigers", opponentName = "Ground Balls")
        })
        assertEquals(listOf(current), searchCaptureLibrary(
            listOf(previous, current), snapshot, "all plays against Ground Balls from today", today, TimeZone.UTC
        ))
    }

    @Test
    fun dateIntentSupportsYesterdayAcrossYearBoundariesWithoutRemovingOtherConstraints() {
        assertEquals(LocalDate(2025, 12, 31), memorySearchDate("videos from yesterday", LocalDate(2026, 1, 1)))
        assertEquals(today, memorySearchDate("videos from today", today))
        assertNull(memorySearchDate("all plays against Lions", today))
        assertEquals(listOf("lions"), parseMemorySearchQuery("create reel of all videos from today against Lions").terms)
    }
}
