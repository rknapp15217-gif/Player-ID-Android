package com.playerid.app.domain.team

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.*

class ActionBarRequestsTest {
    private val today = LocalDate(2026, 10, 26)
    private fun video(date: String, suffix: String = "clip") = MediaReference(
        "file:///media/${Instant.parse("${date}T12:00:00Z").toEpochMilliseconds()}-$suffix.mov", MediaKind.VIDEO
    )
    private val latest = video("2026-10-26", "latest")
    private val yesterday = video("2026-10-25", "yesterday")
    private val previousWeek = video("2026-10-20", "week")
    private val previousMonth = video("2026-09-15", "month")
    private val previousYear = video("2025-10-20", "year")
    private val captures = listOf(previousMonth, latest, previousYear, yesterday, previousWeek)
    private val snapshot = TeamWorkspaceSnapshot(
        players = listOf(testPlayer().copy(name = "Tyson Smith")),
        captureOrganizations = captures.associate {
            captureFileKey(it) to CaptureOrganization("Tigers", playerId = "player", opponentName = "Penn-Trafford")
        },
        captureMomentTags = captures.associate { captureFileKey(it) to "Goal" },
        captureFavorites = setOf(captureFileKey(latest), captureFileKey(yesterday))
    )
    private fun search(query: String, state: TeamWorkspaceSnapshot = snapshot, items: List<MediaReference> = captures) =
        searchCaptureLibrary(items, state, query, today, TimeZone.UTC)

    @Test
    fun calendarDatePhrasesWorkInDifferentSentences() {
        val examples = mapOf(
            "Could you show me videos taken today please" to listOf(latest),
            "Find today's videos" to listOf(latest),
            "Show this week's goals" to listOf(latest),
            "Find every play from yesterday" to listOf(yesterday),
            "Show clips this week" to listOf(latest),
            "Show clips last week" to listOf(yesterday, previousWeek),
            "Show clips this month" to listOf(latest, yesterday, previousWeek),
            "Show clips previous month" to listOf(previousMonth),
            "Show clips this year" to listOf(previousMonth, latest, yesterday, previousWeek),
            "Find videos last year" to listOf(previousYear),
            "Find plays from the past 7 days" to listOf(latest, yesterday, previousWeek),
            "Show plays recorded on October 26, 2026" to listOf(latest),
            "Find videos from Oct 26th" to listOf(latest),
            "Find videos on 2026-10-26" to listOf(latest)
        )
        examples.forEach { (request, expected) -> assertEquals(expected, search(request), request) }
    }

    @Test
    fun dateRangesHandleMonthYearAndLeapDayBoundaries() {
        assertEquals("2025-12-01", parseMemorySearchQuery("last month", LocalDate(2026, 1, 5)).dateFrom)
        val leap = parseMemorySearchQuery("previous month", LocalDate(2024, 3, 5))
        assertEquals("2024-02-29", leap.dateThrough)
        val week = parseMemorySearchQuery("last week", LocalDate(2026, 1, 1))
        assertEquals("2025-12-22", week.dateFrom)
        assertEquals("2025-12-28", week.dateThrough)
        assertEquals("2026-10-26", parseMemorySearchQuery("last 1 day", today).dateFrom)
    }

    @Test
    fun favoritesPlayerActionOpponentAndDateCombine() {
        for (request in listOf(
            "Show me Tyson's favorite goals against Penn Trafford this week",
            "Please find Tyson Smith's starred scores vs PT from October 26",
            "Can you show Tyson's favourites against PennTrafford this week"
        )) {
            assertEquals(listOf(latest), search(request), request)
        }
        assertEquals(emptyList(), search("Tyson goals against Lions this week"))
        assertEquals(emptyList(), search("Jordan goals against Penn Trafford"))
    }

    @Test
    fun latestAndFirstApplyAfterOtherConstraintsAndSupportCounts() {
        assertEquals(listOf(latest), search("Find my latest goal"))
        assertEquals(listOf(previousYear), search("Show my first goal"))
        assertEquals(listOf(latest, yesterday), search("Find latest 2 saves",
            snapshot.copy(captureMomentTags = captures.associate { captureFileKey(it) to "Save" })))
        assertEquals(listOf(previousWeek, yesterday), search("first 2 goals this month"))
    }

    @Test
    fun photosVideosAndReelsAreExplicitTypes() {
        val photo = MediaReference(latest.identifier.replace(".mov", ".jpg"), MediaKind.IMAGE)
        val items = listOf(latest, photo)
        assertEquals(listOf(photo), search("show all pictures today", items = items))
        assertEquals(listOf(latest), search("all videos today", items = items))
        assertEquals(emptyList(), search("all reels today", items = items))
    }

    @Test
    fun reelRequestsReuseTheSameCombinationsWithoutChangingTheSelection() {
        for (command in listOf(
            "Could you create a reel of Tyson's favorite goals this month",
            "Please build me a reel with the latest 2 goals against Penn Trafford",
            "Generate a reel from all videos from last week"
        )) {
            val parsed = assertNotNull(parseMemoryReelCommand(command))
            val expected = when {
                "favorite" in command -> listOf(latest, yesterday)
                "latest" in command -> listOf(latest, yesterday)
                else -> listOf(yesterday, previousWeek)
            }
            assertEquals(expected, search(parsed.searchQuery), command)
        }
        assertNull(parseMemoryReelCommand("Show my reels from this year"))
    }

    @Test
    fun invalidDatesAndUnknownWordsDoNotTurnIntoUnfilteredResults() {
        assertEquals(emptyList(), search("videos on February 30, 2026"))
        assertEquals(emptyList(), search("all videos next millennium"))
        assertEquals(emptyList(), search("videos last 0 days"))
    }

    @Test
    fun mountAndMtMatchInBothDirectionsWithoutChangingSavedOpponents() {
        for (saved in listOf("Mt. Lebanon", "MT LEBANON", "Mount Lebanon")) {
            val other = video("2026-10-26", "other")
            val state = snapshot.copy(captureOrganizations = mapOf(
                captureFileKey(latest) to CaptureOrganization("Tigers", opponentName = saved),
                captureFileKey(yesterday) to CaptureOrganization("Tigers", opponentName = saved),
                captureFileKey(other) to CaptureOrganization("Tigers", opponentName = "Mount Pleasant")
            ))
            for (spoken in listOf("Mount Lebanon", "Mt. Lebanon", "Mt Lebanon")) {
                val command = assertNotNull(parseMemoryReelCommand("Make a reel of all plays against $spoken"))
                assertEquals(listOf(latest, yesterday), search(
                    command.searchQuery, state, listOf(latest, yesterday, other)
                ), "$saved / $spoken")
            }
            assertEquals(saved, state.captureOrganizations[captureFileKey(latest)]?.opponentName)
        }
        assertEquals("Smith", opponentSearchName("Smith", emptyList()))
    }

    @Test
    fun ambiguousOpponentInitialsAreNotGuessed() {
        assertEquals("Penn Trafford", opponentSearchName("Penn Trafford", listOf("Penn Trafford", "Pine Township")))
    }
}
