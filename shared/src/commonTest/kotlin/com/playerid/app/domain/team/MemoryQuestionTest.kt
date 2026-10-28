package com.playerid.app.domain.team

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.*

class MemoryQuestionTest {
    private val today = LocalDate(2026, 10, 26)
    private val tyson = testPlayer("tyson", "28").copy(name = "Tyson Smith")
    private val jake = testPlayer("jake", "12").copy(name = "Jake Smith")
    private val first = video("2026-10-25T12:00:00Z")
    private val later = video("2026-10-25T14:00:00Z")
    private val other = video("2026-10-25T15:00:00Z")
    private val current = video("2026-10-26T12:00:00Z")
    private val items = listOf(current, later, other, first)
    private val snapshot = TeamWorkspaceSnapshot(
        players = listOf(tyson, jake),
        captureOrganizations = items.associate { media ->
            captureFileKey(media) to CaptureOrganization("Tigers",
                playerId = if (media == other) jake.id else tyson.id,
                opponentName = if (media == first) "Norwin" else "Mt. Lebanon")
        },
        captureMomentTags = items.associate { captureFileKey(it) to "Faceoff Win" }
    )

    @Test
    fun countQuestionUsesTheSameStructuredFiltersAndSupportingClips() {
        for (text in listOf("How many faceoff wins did Tyson have yesterday?",
                "Could you tell me how many faceoff wins did Tyson have yesterday?",
                "Count Tysons faceoff wins from yesterday")) {
            val query = interpret(text)
            val matches = search(text, query)
            assertEquals(listOf(later, first), matches, text)
            assertEquals("Faceoff Win", query.filters.momentTag)
            assertEquals("2026-10-25", query.filters.dateFrom)
            val answer = assertNotNull(answerMemoryQuestion(query, matches.size, false))
            assertEquals("2 recorded faceoff wins", answer.title)
            assertTrue(answer.detail.contains("not complete game statistics"))
            assertEquals(matches.size, 2)
        }
    }

    @Test
    fun opponentSummaryUsesRecordedEventsRatherThanTheSchedule() {
        val text = "Which teams did Tyson win faceoffs against?"
        val query = interpret(text)
        assertEquals(MemoryQuestionKind.OPPONENTS, query.question?.kind)
        val matches = search(text, query)
        assertEquals(listOf(current, later, first), matches)
        val names = matches.mapNotNull { snapshot.captureOrganizations[captureFileKey(it)]?.opponentName }
        val answer = assertNotNull(answerMemoryQuestion(query, matches.size, false, names))
        assertEquals("2 recorded opponents", answer.title)
        assertTrue(answer.detail.contains("Norwin"))
        assertTrue(answer.detail.contains("Mt. Lebanon"))
        assertFalse(answer.detail.contains("Based on saved"))
        assertTrue(answer.detail.contains("Not complete game statistics"))
        assertEquals("Opponent not saved", answerMemoryQuestion(query, 1, false)?.title)
    }

    @Test
    fun firstRecordedQuestionRanksSupportingMemoryAndDisplaysItsDate() {
        val text = "What was Tyson's first recorded faceoff win of 2026?"
        val query = interpret(text)
        assertEquals(MemoryQuestionKind.FIRST, query.question?.kind)
        assertEquals(listOf(first), search(text, query))
        val answer = assertNotNull(answerMemoryQuestion(query, 1, false, firstDate = "2026-10-25"))
        assertEquals("First recorded faceoff win", answer.title)
        assertTrue(answer.detail.contains("2026-10-25"))
        val goal = interpret("What was Tyson's first recorded goal?")
        assertEquals("Score", goal.filters.momentTag)
        assertEquals(MemoryResultSelection.FIRST, goal.filters.resultSelection)
        val changed = query.withoutChip("sort")
        assertEquals(3, search(text, changed).size)
        assertEquals("3 recorded faceoff wins", answerMemoryQuestion(changed, 3, false)?.title)
        assertEquals("Recorded faceoff win", answerMemoryQuestion(query, 1, false)?.title)
        val memory = interpret("What was Tyson's first recorded memory?")
        assertEquals("First recorded memory",
            answerMemoryQuestion(memory, 1, false, firstDate = "2026-10-25")?.title)
    }

    @Test
    fun editedChipsRecomputeAnswersWithoutKeepingTheOldCount() {
        val text = "How many faceoff wins did Tyson have yesterday?"
        val query = interpret(text)
        val changed = query.withoutChip("player:tyson").withoutChip("date")
        assertEquals(4, search(text, changed).size)
        assertEquals("4 recorded faceoff wins", answerMemoryQuestion(changed, 4, false)?.title)
        assertEquals(2, search(text, query).size)
    }

    @Test
    fun emptyAmbiguousAndUnavailableMetadataDoNotInventAnswers() {
        val empty = interpret("How many goals did Tyson record yesterday?")
        assertEquals(emptyList(), search("", empty))
        assertEquals("0 recorded goals", answerMemoryQuestion(empty, 0, false)?.title)
        assertEquals("", answerMemoryQuestion(empty, 0, false)?.detail)
        val received = interpret("How many memories did we receive from other parents?")
        assertNotNull(received.issue)
        assertEquals(emptyList(), search("", received))
        assertNull(answerMemoryQuestion(received.copy(issue = "Unavailable source metadata"), 0, false))
        val ambiguous = interpretAskPlayloom("How many goals did Ty record?", listOf(tyson,
            jake.copy(name = "Tyler Smith")), emptyList(), emptyList(), today)
        assertNotNull(ambiguous.issue)
        assertNull(answerMemoryQuestion(ambiguous, 0, false))
        assertNull(parseMemoryQuestion("Who is our next opponent?"))
        assertNull(parseMemoryQuestion("Find Tyson goals"))
    }

    @Test
    fun reelAnswersCannotClaimIndividualEventCounts() {
        val query = interpret("How many faceoff wins did Tyson have yesterday?")
        val answer = assertNotNull(answerMemoryQuestion(query, 2, true))
        assertEquals("2 saved reels", answer.title)
        assertTrue(answer.detail.contains("not a count of individual plays"))
    }

    @Test
    fun questionsCombineExistingFavoritesDatesAndSeasonConstraints() {
        val favorites = interpret("How many favorite memories do I have from 2026?")
        assertTrue(favorites.filters.favoritesOnly)
        assertEquals("2026-01-01", favorites.filters.dateFrom)
        assertEquals(emptyList(), favorites.filters.terms)
        val season = interpret("How many goals did I record this season?")
        assertEquals("Score", season.filters.momentTag)
        assertEquals(MemorySeasonIntent.CURRENT, season.filters.seasonIntent)
        assertEquals(emptyList(), season.filters.terms)
        val weekend = interpret("How many goals did I capture last weekend?")
        assertEquals("2026-10-24", weekend.filters.dateFrom)
        assertEquals("2026-10-25", weekend.filters.dateThrough)
        assertEquals(emptyList(), weekend.filters.terms)
        val opponents = interpret("Which teams did Tyson score against?")
        assertEquals(MemoryQuestionKind.OPPONENTS, opponents.question?.kind)
        assertEquals("Score", opponents.filters.momentTag)
        assertEquals(listOf(tyson), opponents.players)
        assertEquals(emptyList(), opponents.filters.terms)
    }

    @Test
    fun opponentQuestionsKeepDatesAfterAgainstAndStayOutOfActionDialogs() {
        for (suffix in listOf("yesterday", "yesterday?", "this season", "last weekend")) {
            val text = "Which teams did Tyson score against $suffix"
            val query = interpret(text)
            assertEquals(MemoryQuestionKind.OPPONENTS, query.question?.kind, text)
            assertEquals("Score", query.filters.momentTag, text)
            assertEquals(emptyList(), query.filters.terms, text)
        }
        val goals = snapshot.copy(captureMomentTags = items.associate { captureFileKey(it) to "Goal" })
        val text = "Which teams did Tyson score against yesterday?"
        val query = interpret(text)
        val matches = searchCaptureLibrary(items, goals, text, today, TimeZone.UTC, query, reelsOnly = false)
        assertEquals(listOf(later, first), matches)
        assertEquals("2026-10-25", query.filters.dateFrom)
        assertEquals("2 recorded opponents", answerMemoryQuestion(query, matches.size, false,
            matches.mapNotNull { goals.captureOrganizations[captureFileKey(it)]?.opponentName })?.title)
    }

    @Test
    fun firstGoalThisSeasonUsesCalendarYearOrTheSelectedSavedSeason() {
        val old = video("2025-03-01T12:00:00Z")
        val spring = video("2026-03-01T12:00:00Z")
        val next = video("2026-03-02T12:00:00Z")
        val unknown = video("2026-03-03T12:00:00Z")
        val captures = listOf(next, unknown, old, spring)
        val seasons = listOf(2025, 2026).map { year ->
            SportSeasonProfile("$year", "child", "Lacrosse", "$year Season", "Tigers", 1, 1)
        }
        val saved = snapshot.copy(
            seasons = seasons,
            captureOrganizations = captures.associate {
                captureFileKey(it) to CaptureOrganization("Tigers",
                    seasonId = when (it) { old -> "2025"; unknown -> null; else -> "2026" },
                    playerId = tyson.id)
            },
            captureMomentTags = captures.associate { captureFileKey(it) to "Goal" }
        )
        val text = "What was Tyson's first goal this season"
        for (day in listOf(LocalDate(2026, 3, 24), today)) {
            val query = interpretAskPlayloom(text, saved.players, listOf("Tigers"), emptyList(), day)
            assertEquals("2026", query.filters.seasonYear)
            assertEquals(listOf(spring), searchCaptureLibrary(captures, saved, text, day, TimeZone.UTC, query, false))
            val selected = query.withSelectedSeasonYear("2025")
            assertEquals(listOf(old), searchCaptureLibrary(captures, saved, text, day, TimeZone.UTC, selected, false))
            assertEquals(listOf("This season (2025)"), selected.chips().filter { it.id == "season" }.map { it.label })
            val cleared = selected.withoutChip("season")
            assertNull(cleared.filters.seasonYear)
            assertNull(cleared.filters.seasonIntent)
            assertEquals(listOf(old), searchCaptureLibrary(captures, saved, text, day, TimeZone.UTC, cleared, false))
            val previous = interpretAskPlayloom("Find Tyson's first goal last season",
                saved.players, listOf("Tigers"), emptyList(), day).withSelectedSeasonYear("2026")
            assertEquals("2025", previous.filters.seasonYear)
            assertEquals(listOf(old), searchCaptureLibrary(captures, saved, "", day, TimeZone.UTC, previous, false))
        }
    }

    @Test
    fun missingSeasonDefaultsToRecordingYearButNeverOverridesAnExplicitSeason() {
        val text = "Show me Tyson's first goal this season"
        val query = interpret(text)
        val explicit = SportSeasonProfile("old", "child", "Lacrosse", "2025 Season", "Tigers", 1, 1)
        val goals = snapshot.copy(
            seasons = listOf(explicit),
            captureMomentTags = items.associate { captureFileKey(it) to "Goal" },
            captureOrganizations = snapshot.captureOrganizations +
                (captureFileKey(first) to snapshot.captureOrganizations.getValue(captureFileKey(first)).copy(seasonId = "old"))
        )
        assertEquals(listOf(later), searchCaptureLibrary(items, goals, text, today, TimeZone.UTC, query, false))
        assertEquals("2026", goals.captureSeasonYearForKey(captureFileKey(later), TimeZone.UTC))
        assertEquals("2025", goals.captureSeasonYearForKey(captureFileKey(first), TimeZone.UTC))
        assertNull(goals.captureSeasonYearForKey("undated.mov", TimeZone.UTC))
        val filter = CaptureLibraryFilter(seasonId = "old")
        assertEquals(listOf(first), filterCaptures(items, goals, filter))
    }

    @Test
    fun firstGoalOpponentQuestionUsesEarliestMatchingMemoryNotSchedule() {
        val goals = snapshot.copy(captureMomentTags = items.associate { captureFileKey(it) to "Goal" })
        for (text in listOf(
            "Who did Tyson have his first goal against this season?",
            "Who did Tyson score his first goal against this season?",
            "Who did Tyson have his first goal against in 2026?"
        )) {
            val query = interpret(text)
            assertEquals(MemoryQuestionKind.OPPONENTS, query.question?.kind, text)
            assertEquals(MemoryResultSelection.FIRST, query.filters.resultSelection, text)
            assertEquals("Score", query.filters.momentTag, text)
            assertEquals(listOf(tyson), query.players, text)
            assertEquals(emptyList(), query.filters.terms, text)
            val matches = searchCaptureLibrary(items, goals, text, today, TimeZone.UTC, query, false)
            assertEquals(listOf(first), matches, text)
            val names = matches.mapNotNull { goals.captureOrganizations[captureFileKey(it)]?.opponentName }
            val answer = assertNotNull(answerMemoryQuestion(query, matches.size, false, names, "2026-10-25"))
            assertEquals("Norwin", answer.title)
            assertEquals("First recorded goal · 2026-10-25", answer.detail)
            assertEquals("Opponent not saved", answerMemoryQuestion(query, 1, false,
                firstDate = "2026-10-25")?.title)
            assertEquals("First recorded goal not determined", answerMemoryQuestion(query, 1, false, names)?.title)
        }
    }

    @Test
    fun firstMomentWhereAndWhenQuestionsResolveDateAndSavedLocation() {
        val goals = snapshot.copy(captureMomentTags = items.associate { captureFileKey(it) to "Goal" })
        for (prefix in listOf("Where and when was", "When and where was", "Where was", "When was")) {
            val text = "$prefix Tyson's first goal this season?"
            val query = interpret(text)
            assertEquals(MemoryQuestionKind.FIRST, query.question?.kind, text)
            assertEquals(prefix.contains("Where", true), query.question?.asksLocation, text)
            assertEquals(emptyList(), query.filters.terms, text)
            assertEquals(listOf(first), searchCaptureLibrary(items, goals, text, today, TimeZone.UTC, query, false))
            val answer = assertNotNull(answerMemoryQuestion(query, 1, false,
                firstDate = "2026-10-25", firstLocation = "North field"))
            assertTrue(answer.detail.contains("2026-10-25"))
            if (prefix.contains("Where", true)) {
                assertEquals("2026-10-25 · North field", answer.detail)
                assertEquals("2026-10-25 · Location not saved",
                    answerMemoryQuestion(query, 1, false, firstDate = "2026-10-25")?.detail)
            }
        }
    }

    private fun interpret(text: String) = interpretAskPlayloom(text, snapshot.players,
        listOf("Tigers"), listOf("Norwin", "Mt. Lebanon"), today)
    private fun search(text: String, query: AskPlayloomQuery) =
        searchCaptureLibrary(items, snapshot, text, today, TimeZone.UTC, query, reelsOnly = false)
    private fun video(date: String) = MediaReference(
        "file:///media/${Instant.parse(date).toEpochMilliseconds()}-clip.mov", MediaKind.VIDEO)
}
