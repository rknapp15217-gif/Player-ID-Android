package com.playerid.app.domain.team

import com.playerid.app.domain.memory.chronologicalReelSources
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.*

class LibraryActionRequestTest {
    private val now = Instant.parse("2026-10-26T14:00:00Z").toEpochMilliseconds()
    private val games = listOf(
        ActionScheduleGame("past", "Tigers", "Lions", now - 1000),
        ActionScheduleGame("next", "Tigers", "Mt. Lebanon", now + 3600000, "North field"),
        ActionScheduleGame("later", "Tigers", "Penn-Trafford", now + 7200000),
        ActionScheduleGame("other", "Eagles", "Lions", now + 1800000, "South field")
    )

    @Test
    fun countQuestionsSeparateGrammarFromMemoryCriteria() {
        for (text in listOf(
            "How many faceoff wins did Tyson have yesterday?",
            "How many faceoff wins has Tyson had yesterday?",
            "Count faceoff wins Tyson yesterday",
            "Could you please tell me how many faceoff wins did Tyson have yesterday?"
        )) {
            val request = assertIs<LibraryActionRequest.Count>(parseLibraryActionRequest(text))
            val query = parseMemorySearchQuery(request.query, LocalDate(2026, 10, 26))
            assertEquals("Faceoff Win", query.momentTag, text)
            assertEquals("2026-10-25", query.dateFrom, text)
            assertEquals(listOf("tyson"), query.terms, text)
        }
        assertTrue(memoryCountAnswer(0, "Tyson's wins yesterday", false).contains("not complete game statistics"))
        assertTrue(memoryCountAnswer(2, "goals", true).contains("not individual plays"))
    }

    @Test
    fun sharingSeparatesRecipientFromSourceMatchingAndNeverSends() {
        for (verb in listOf("Share", "Send")) {
            val request = assertIs<LibraryActionRequest.Reel>(
                parseLibraryActionRequest("$verb a reel of tysons faceoff wins with Jenna")
            )
            assertTrue(request.share)
            assertEquals("Jenna", request.recipient)
            assertEquals("tysons faceoff wins", request.command.searchQuery)
            assertEquals("tyson faceoff wins",
                resolveActionMemoryQuery(request.command.searchQuery, listOf("Tyson Smith")).query)
        }
        val fullName = assertIs<LibraryActionRequest.Reel>(
            parseLibraryActionRequest("Please share a reel of Tyson's goals against Mount Lebanon to Jenna Smith")
        )
        assertEquals("Jenna Smith", fullName.recipient)
        assertTrue(fullName.command.searchQuery.contains("against Mount Lebanon"))
        val withSources = assertIs<LibraryActionRequest.Reel>(
            parseLibraryActionRequest("Share a reel with the latest 3 goals to Jenna"))
        assertEquals("Jenna", withSources.recipient)
        assertEquals("the latest 3 goals", withSources.command.searchQuery)
        assertIs<LibraryActionRequest.Unsupported>(parseLibraryActionRequest("Share a reel with Jenna"))
        assertFalse(assertIs<LibraryActionRequest.Reel>(
            parseLibraryActionRequest("Create a reel of goals")).share)
    }

    @Test
    fun ordinarySearchesAndUnknownQuestionsDoNotExecuteActions() {
        for (search in listOf("Tyson goals", "show reels yesterday", "find memories today", "Jenna")) {
            assertNull(parseLibraryActionRequest(search))
        }
        assertIs<LibraryActionRequest.Unsupported>(parseLibraryActionRequest("What was the final score?"))
    }

    @Test
    fun scheduleAnswersUseUpcomingGamesAndIncludeTeamDateTimeAndLocation() {
        assertEquals("No upcoming scheduled game against Penn Trafford.",
            answerScheduleQuestion(LibraryActionRequest.Schedule("When do we play Penn Trafford next?"),
                emptyList(), now, "UTC"))
        assertEquals("No upcoming scheduled game.",
            answerScheduleQuestion(LibraryActionRequest.Schedule("When is our next game?"),
                emptyList(), now, "UTC"))
        assertEquals("No scheduled game on 2026-10-28.", answer("Do we play on 2026-10-28?"))
        val next = answer("Who is our next opponent?")
        assertFalse(next.contains("From your saved schedule"))
        assertTrue(next.startsWith("Eagles vs Lions"))
        assertTrue(next.contains("Tigers vs Mt. Lebanon"))
        assertTrue(next.contains("Eagles vs Lions"))
        assertFalse(next.contains("Penn-Trafford"))
        assertTrue(next.contains("2026-10-26 at 3:00 PM"))
        assertTrue(next.contains("North field"))
        val named = answer("When do we play Mount Lebanon?")
        assertTrue(named.contains("Tigers vs Mt. Lebanon"))
        assertFalse(named.contains("Eagles"))
        assertTrue(answer("What time do Tigers play PT?").contains("Location not saved"))
        assertTrue(answer("Where is our next game?").contains("South field"))
        assertTrue(answer("When do we play Unknown?").startsWith("No upcoming"))
        assertTrue(answerScheduleQuestion(LibraryActionRequest.Schedule("Who is our next opponent?"),
            emptyList(), now, "UTC").startsWith("No upcoming"))
        val ambiguous = games + listOf(
            ActionScheduleGame("ambiguous", "Tigers", "Pine Township", now + 1000),
            ActionScheduleGame("substring", "Tigers", "Hampton", now + 2000)
        )
        assertTrue(answerScheduleQuestion(LibraryActionRequest.Schedule("When do we play PT?"),
            ambiguous, now, "UTC").startsWith("No upcoming"))
    }

    @Test
    fun ambiguousNamesAreNotGuessedAndRecipientsMatchWholeWords() {
        assertNotNull(resolveActionMemoryQuery("Tysons goals", listOf("Tyson Smith", "Tyson Jones")).error)
        assertNull(resolveActionMemoryQuery("Tyson Smith's goals", listOf("Tyson Smith", "Tyson Jones")).error)
        assertEquals("Tyson smith goals",
            resolveActionMemoryQuery("Tyson Smiths goals", listOf("Tyson Smith", "Tyson Jones")).query)
        assertTrue(actionRecipientMatches("Jenna", "Jenna Smith"))
        assertTrue(actionRecipientMatches("jenna smith", "Jenna Smith"))
        assertFalse(actionRecipientMatches("Ann", "Jenna Smith"))
        assertFalse(actionRecipientMatches("Jenna Jones", "Jenna Smith"))
        val request = assertIs<LibraryActionRequest.Reel>(
            parseLibraryActionRequest("Send a reel of Tyson goals to a contact in my phone Ryan Knapp")
        )
        assertEquals("Ryan Knapp", request.recipient)
        val share = assertIs<LibraryActionRequest.Share>(
            parseLibraryActionRequest("Send all memories against Mount Lebanon to Ryan Knapp"))
        assertEquals("against Mount Lebanon", share.query)
        val goals = assertIs<LibraryActionRequest.Share>(parseLibraryActionRequest("Send all Goals to Ryan Knapp"))
        assertEquals("Goals", goals.query)
        assertEquals("Ryan Knapp", goals.recipient)
        assertEquals("Tyson goals", assertIs<LibraryActionRequest.Share>(
            parseLibraryActionRequest("Share all of Tyson goals with Ryan Knapp")).query)
        assertEquals("Ryan Knapp", share.recipient)
        assertTrue(actionRecipientMatches("Ryan Knapp", "Ryan Knapp"))
    }

    @Test
    fun countingAndSharingUseSavedTagsDatesAndChronologicalSources() {
        val early = media("2026-10-25T12:00:00Z")
        val late = media("2026-10-25T13:00:00Z")
        val today = media("2026-10-26T12:00:00Z")
        val other = media("2026-10-25T14:00:00Z")
        val sources = listOf(late, today, other, early)
        val state = TeamWorkspaceSnapshot(
            players = listOf(testPlayer().copy(name = "Tyson Smith")),
            captureOrganizations = sources.associate {
                captureFileKey(it) to CaptureOrganization("Tigers", playerId = "player")
            },
            captureMomentTags = sources.associate {
                captureFileKey(it) to if (it == other) "Ground Ball" else "Faceoff Win"
            }
        )
        val request = assertIs<LibraryActionRequest.Count>(
            parseLibraryActionRequest("How many faceoff wins did Tyson have yesterday?")
        )
        val matches = searchCaptureLibrary(sources, state, request.query, LocalDate(2026, 10, 26), TimeZone.UTC)
        assertEquals(2, matches.size)
        assertEquals(listOf(early, late), chronologicalReelSources(matches, state))
    }

    @Test
    fun pastMeetingsUseMostRecentSavedGameAndOpponentAliases() {
        val schedule = listOf(
            ActionScheduleGame("old", "Tigers", "Mt. Lebanon", now - 86400000 * 10),
            ActionScheduleGame("recent", "Tigers", "Mt. Lebanon", now - 86400000 * 2, "Home field"),
            ActionScheduleGame("future", "Tigers", "Mt. Lebanon", now + 86400000),
            ActionScheduleGame("other", "Tigers", "Norwin", now - 1000)
        )
        for (text in listOf("When was the last time we played Mount Lebanon?",
            "When did we last play Mount Lebanon?", "Where was our last game against Mount Lebanon?")) {
            val request = assertIs<LibraryActionRequest.Schedule>(parseLibraryActionRequest(text), text)
            val answer = scheduleQuestionAnswer(request, schedule, now, "UTC")
            assertEquals(listOf("recent"), answer.games.map { it.id }, text)
            assertTrue(answer.text.contains("Home field"))
            assertFalse(answer.text.contains("2026-10-27"))
        }
        val request = assertIs<LibraryActionRequest.Schedule>(
            parseLibraryActionRequest("When was the last time we played Penn Trafford?"))
        assertEquals("No past scheduled game against Penn Trafford.",
            scheduleQuestionAnswer(request, schedule, now, "UTC").text)
        val dated = assertIs<LibraryActionRequest.Schedule>(parseLibraryActionRequest("What games did we play last week?"))
        assertEquals(listOf("recent"), scheduleQuestionAnswer(dated, schedule, now, "UTC").games.map { it.id })
        val future = assertIs<LibraryActionRequest.Schedule>(parseLibraryActionRequest("When do we play Mount Lebanon next?"))
        assertEquals(listOf("future"), scheduleQuestionAnswer(future, schedule, now, "UTC").games.map { it.id })
    }

    private fun media(instant: String) = MediaReference(
        "file:///media/${Instant.parse(instant).toEpochMilliseconds()}-clip.mov", MediaKind.VIDEO)

    private fun answer(text: String): String = answerScheduleQuestion(
        assertIs<LibraryActionRequest.Schedule>(parseLibraryActionRequest(text)), games, now, "UTC")

    @Test
    fun gameDayQuestionsIncludeEarlierGamesTodayButNotOtherDates() {
        for (text in listOf("Do we play today?", "What games do we have today?", "Do Tigers play today?")) {
            val result = answer(text)
            assertTrue(result.contains("Tigers vs Lions"), text)
            assertTrue(result.contains("Tigers vs Penn-Trafford"), text)
        }
        assertTrue(answer("Do we play today?").contains("Eagles vs Lions"))
        assertFalse(answer("Do Tigers play today?").contains("Eagles"))
        assertTrue(answer("Do we play on 2026-10-28?").startsWith("No scheduled"))
        assertTrue(answer("Do we play today against Unknown?").startsWith("No scheduled"))
        assertFalse(answer("When is our next game today?").contains("Tigers vs Lions"))
        val selected = scheduleQuestionAnswer(LibraryActionRequest.Schedule("When is our next game?"),
            games.filter { it.teamName == "Tigers" }, now, "UTC")
        assertEquals(listOf("next"), selected.games.map { it.id })
        assertFalse(selected.text.contains("Eagles"))
    }

    @Test
    fun weekendIncludesAllGamesAndHonorsLocalCalendarBoundaries() {
        val weekendGames = listOf(
            ActionScheduleGame("friday", "Tigers", "Friday", Instant.parse("2026-10-31T02:00:00Z").toEpochMilliseconds()),
            ActionScheduleGame("saturday", "Tigers", "Saturday", Instant.parse("2026-10-31T16:00:00Z").toEpochMilliseconds()),
            ActionScheduleGame("sunday", "Tigers", "Sunday", Instant.parse("2026-11-02T03:00:00Z").toEpochMilliseconds()),
            ActionScheduleGame("monday", "Tigers", "Monday", Instant.parse("2026-11-02T06:00:00Z").toEpochMilliseconds())
        )
        val request = assertIs<LibraryActionRequest.Schedule>(parseLibraryActionRequest("What games do we have this weekend?"))
        val result = scheduleQuestionAnswer(request, weekendGames, now, "America/New_York")
        assertEquals(listOf("saturday", "sunday"), result.games.map { it.id })
        assertFalse(result.text.contains("Friday"))
        assertFalse(result.text.contains("Monday"))
    }

    @Test
    fun againSelectsNextMeetingAndFollowUpsRequireOneUnchangedGame() {
        val request = assertIs<LibraryActionRequest.Schedule>(parseLibraryActionRequest("When do we play Mount Lebanon again?"))
        val later = games + games[1].copy(id = "rematch", startMs = now + 86400000)
        val result = scheduleQuestionAnswer(request, later, now, "UTC")
        assertEquals(listOf("next"), result.games.map { it.id })
        val followUp = assertIs<LibraryActionRequest.Schedule>(parseLibraryActionRequest("Where is it?"))
        assertTrue(scheduleQuestionAnswer(followUp, later, now, "UTC", result).text.contains("North field"))
        assertTrue(scheduleQuestionAnswer(followUp, later, now, "UTC").text.contains("Specify a team"))
        val allTeams = scheduleQuestionAnswer(LibraryActionRequest.Schedule("Who is our next opponent?"), games, now, "UTC")
        assertTrue(scheduleQuestionAnswer(followUp, games, now, "UTC", allTeams).text.contains("Specify a team"))
        assertTrue(scheduleQuestionAnswer(followUp, emptyList(), now, "UTC", result).text.contains("has changed"))
        val changed = later.map { if (it.id == "next") it.copy(location = "New field") else it }
        assertTrue(scheduleQuestionAnswer(followUp, changed, now, "UTC", result).text.contains("has changed"))
    }
}
