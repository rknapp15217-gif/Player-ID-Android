package com.playerid.app.domain.team

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.*

class AskPlayloomQueryTest {
    private val today = LocalDate(2026, 10, 26)
    private val tyson = testPlayer("tyson", "28").copy(name = "Tyson Smith", teamName = "Tigers")
    private val jake = testPlayer("jake", "12").copy(name = "Jake Smith", teamName = "Tigers")
    private val players = listOf(tyson, jake)
    private val opponents = listOf("Mt. Lebanon", "Norwin", "Penn-Trafford")
    private fun interpret(text: String, roster: List<PlayerProfile> = players) =
        interpretAskPlayloom(text, roster, listOf("Tigers", "Eagles"), opponents, today)
    private val early = video("2025-07-01", "early")
    private val late = video("2025-07-02", "late")
    private val other = video("2025-07-03", "other")
    private val untagged = video("2025-07-04", "untagged")
    private val current = video("2026-10-25", "current")
    private val season = SportSeasonProfile("season", "family", "Lacrosse", "2025 Season", "Tigers", 0, 0)
    private val state = TeamWorkspaceSnapshot(
        players = players,
        seasons = listOf(season),
        captureOrganizations = mapOf(
            captureFileKey(early) to CaptureOrganization("Tigers", season.id, playerId = tyson.id, opponentName = "Mt. Lebanon"),
            captureFileKey(late) to CaptureOrganization("Tigers", season.id, playerId = tyson.id, opponentName = "Norwin"),
            captureFileKey(other) to CaptureOrganization("Tigers", season.id, playerId = jake.id, opponentName = "Norwin"),
            captureFileKey(untagged) to CaptureOrganization("Tigers", season.id, playerId = tyson.id, opponentName = "Norwin"),
            captureFileKey(current) to CaptureOrganization("Tigers", playerId = tyson.id, opponentName = "Norwin")
        ),
        captureMomentTags = listOf(early, late, other, current).associate { captureFileKey(it) to "Goal" },
        captureFavorites = setOf(captureFileKey(early), captureFileKey(other)),
        captureNotes = mapOf(captureFileKey(untagged) to "Tyson goal at the tournament")
    )
    private val videos = listOf(current, untagged, other, late, early)

    @Test
    fun firstGoalOfCalendarYearDoesNotLetAnEarlierReelHideTheMemory() {
        val first = video("2026-01-02", "first-goal")
        val later = video("2026-03-05", "later-goal")
        val reel = video("2026-01-01", "reel")
        val snapshot = state.copy(
            captureOrganizations = state.captureOrganizations + mapOf(
                captureFileKey(first) to CaptureOrganization("Tigers", playerId = tyson.id),
                captureFileKey(later) to CaptureOrganization("Tigers", playerId = tyson.id)
            ),
            captureMomentTags = state.captureMomentTags + mapOf(
                captureFileKey(first) to "Goal", captureFileKey(later) to "Goal"
            ),
            captureReels = mapOf(captureFileKey(reel) to com.playerid.app.domain.memory.MemoryReel(
                "Tyson goals", listOf(captureFileKey(first), captureFileKey(later))))
        )
        for (text in listOf("find tyson's first goal of 2026", "Find Tyson’s first goal in 2026",
                "find tysons earliest goal from 2026")) {
            val parsed = interpret(text)
            assertEquals(listOf(tyson), parsed.players, text)
            assertEquals("Score", parsed.filters.momentTag, text)
            assertEquals("2026-01-01", parsed.filters.dateFrom, text)
            assertEquals("2026-12-31", parsed.filters.dateThrough, text)
            assertEquals(MemoryResultSelection.FIRST, parsed.filters.resultSelection, text)
            assertEquals(emptyList(), parsed.filters.terms, text)
            val matches = searchCaptureLibrary(listOf(later, reel, first), snapshot, text,
                today, TimeZone.UTC, parsed, reelsOnly = false)
            assertEquals(listOf(first), matches, text)
            assertEquals(listOf(reel), searchCaptureLibrary(listOf(later, reel, first), snapshot, text,
                today, TimeZone.UTC, parsed, reelsOnly = true), text)
            assertEquals(listOf(later), searchCaptureLibrary(listOf(later, reel, first), snapshot,
                "Tyson latest goal of 2026", today, TimeZone.UTC,
                interpret("Tyson latest goal of 2026"), reelsOnly = false), text)
        }
    }

    @Test
    fun exactPartialAndPossessiveNamesResolveToPlayerIds() {
        for (text in listOf("Show Tyson Smith's goals", "Find Tyson's goals", "show Tys goals", "Find Tysons goals")) {
            val parsed = interpret(text)
            assertNull(parsed.issue, text)
            assertEquals(listOf(tyson.id), parsed.players.map { it.id }, text)
            assertEquals(emptyList(), parsed.filters.terms, text)
            assertEquals(listOf(current, late, early), search(text), text)
        }
    }

    @Test
    fun siblingsRemainSeparateAndSimilarNamesRequireClarification() {
        assertEquals(listOf(other), search("Find Jake's goals"))
        val tyler = testPlayer("tyler", "3").copy(name = "Tyler James", teamName = "Tigers")
        assertNotNull(interpret("Find Ty goals", players + tyler).issue)
        val namesake = tyson.copy(id = "eagle-tyson", teamName = "Eagles")
        val ambiguous = interpret("Find Tyson Smith goals", players + namesake)
        assertNotNull(ambiguous.issue)
        val scoped = interpret("Find Tyson Smith goals for Tigers", players + namesake)
        assertNull(scoped.issue)
        assertEquals(listOf(tyson), scoped.players)
    }

    @Test
    fun knownNamesCannotMatchNotesOrUnassignedBackgroundPlayers() {
        val parsed = interpret("Find Jake goals")
        assertFalse(parsed.matchesAssignment(tyson.id, tyson.name, "Tigers", "Norwin"))
        assertFalse(parsed.matchesAssignment(null, null, "Tigers", "Norwin"))
        assertEquals(listOf(other), search("Find Jake goals"))
        assertFalse(search("Tyson goals").contains(untagged))
        assertEquals(listOf(untagged), search("Tyson tournament"))
        assertNotNull(interpret("Show clips where Jake was detected").issue)
        assertEquals(emptyList(), search("Show clips where Jake was detected"))
    }

    @Test
    fun favoritesYearSeasonOpponentAndMediaConstraintsCombine() {
        assertEquals(listOf(early), search("Show Tyson's favorite goals from 2025"))
        assertEquals(listOf(late, early), search("Find Tyson goals last season"))
        assertEquals(listOf(late, early), search("Find Tyson goals 2025 season"))
        assertEquals(listOf(early), search("Tyson goals against Mount Lebanon"))
        assertEquals(listOf(early), search("Tyson goals against ML"))
        assertEquals(emptyList(), search("Tyson goals against unknown"))
        assertEquals(listOf(current), search("Tyson goals this season"))
        assertEquals(emptyList(), search("Tyson photos from 2025"))
        assertEquals(listOf(early), search("Find Tyson's first goal"))
        assertEquals(listOf(early), search("Find Tyson's first goal last season"))
    }

    @Test
    fun localCalendarWindowsCoverWeekendSummerAndYearBoundaries() {
        assertEquals("2026-10-24", interpret("last weekend's goals").filters.dateFrom)
        assertEquals("2026-10-25", interpret("last weekend's goals").filters.dateThrough)
        assertEquals(listOf(current), search("Tyson goals last weekend"))
        assertEquals(listOf(late, early), search("Tyson goals summer 2025"))
        assertEquals("2025-06-01", interpret("summer 2025").filters.dateFrom)
        assertEquals("2026-08-31", interpret("last summer").filters.dateThrough)
        assertEquals("2025-06-01", interpretAskPlayloom("last summer", players, emptyList(), emptyList(),
            LocalDate(2026, 2, 1)).filters.dateFrom)
        assertEquals("2025-12-31", interpret("goals from 2025").filters.dateThrough)
        assertEquals(emptyList(), search("goals on February 30, 2025"))
        assertEquals(emptyList(), search("goals on 2025-02-30"))
        val midnight = MediaReference("file:///media/${Instant.parse("2026-10-26T02:00:00Z").toEpochMilliseconds()}-night.mov", MediaKind.VIDEO)
        val midnightState = state.copy(
            captureOrganizations = mapOf(captureFileKey(midnight) to CaptureOrganization("Tigers", playerId = tyson.id)),
            captureMomentTags = mapOf(captureFileKey(midnight) to "Goal")
        )
        assertEquals(listOf(midnight), searchCaptureLibrary(listOf(midnight), midnightState, "Tyson goals yesterday",
            today, TimeZone.of("America/New_York"), interpret("Tyson goals yesterday")))
    }

    @Test
    fun editableChipsChangeExecutionWithoutChangingSavedData() {
        val original = interpret("Tyson favorite goals from 2025")
        assertTrue(original.chips().any { it.label.contains("Tyson") })
        assertTrue(original.chips().any { it.id == "favorite" })
        val changed = original.withoutChip("favorite").withoutChip("player:tyson")
        assertEquals(listOf(other, late, early), searchCaptureLibrary(videos, state, "", today, TimeZone.UTC, changed))
        assertEquals(listOf(tyson), original.players)
        assertTrue(original.filters.favoritesOnly)
        val cleared = original.chips().fold(original) { query, chip -> query.withoutChip(chip.id) }
        assertEquals(emptyList(), cleared.chips())
    }

    @Test
    fun unsupportedOwnershipAndUnknownConstraintsNeverBroadenResults() {
        assertNotNull(interpret("Show memories shared with me").issue)
        assertEquals(emptyList(), search("Show memories shared with me"))
        assertEquals(emptyList(), search("Tyson imaginary championship"))
        assertEquals(emptyList(), search("Tyson goals", emptyList()))
        assertEquals(videos, search(""))
    }

    @Test
    fun legacyFirstNameAssignmentsRequireAUniqueRosterMatch() {
        assertEquals(tyson, resolveMemoryPlayerAssignment("Tyson", "Tigers", players))
        val namesake = tyson.copy(id = "other-tyson", name = "Tyson Jones")
        assertNull(resolveMemoryPlayerAssignment("Tyson", "Tigers", players + namesake))
        assertEquals(tyson, resolveMemoryPlayerAssignment("Tyson Smith", "Tigers", players + namesake))
        assertNull(resolveMemoryPlayerAssignment("Tyson", "Eagles", players))
    }

    @Test
    fun reelSearchKeepsPlayerAndMomentOnTheSameSourceAndNeverSwitchesTabs() {
        val reel = video("2026-10-26", "reel")
        val snapshot = state.copy(
            captureReels = mapOf(captureFileKey(reel) to com.playerid.app.domain.memory.MemoryReel(
                "Team highlights", listOf(captureFileKey(untagged), captureFileKey(other))))
        )
        assertEquals(emptyList(), searchCaptureLibrary(listOf(reel), snapshot, "Tyson goals", today, TimeZone.UTC,
            interpret("Tyson goals")))
        assertEquals(listOf(reel), searchCaptureLibrary(listOf(reel), snapshot, "Jake goals", today, TimeZone.UTC,
            interpret("Jake goals")))
        assertEquals(emptyList(), searchCaptureLibrary(listOf(other), snapshot, "Tyson goals", today, TimeZone.UTC,
            interpret("Tyson goals")))
    }

    private fun search(text: String, source: List<MediaReference> = videos) =
        searchCaptureLibrary(source, state, text, today, TimeZone.UTC, interpret(text))

    private fun video(date: String, suffix: String) = MediaReference(
        "file:///media/${Instant.parse("${date}T12:00:00Z").toEpochMilliseconds()}-$suffix.mov", MediaKind.VIDEO)
}
