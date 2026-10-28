package com.playerid.app.domain.team

import kotlin.test.*

class RosterQuestionTest {
    @Test
    fun combinedClassAndPositionQuestionsRequireBothSavedDetails() {
        val players = listOf(
            testPlayer("fresh-goalie", "1").copy(name = "Fresh Goalie", teamName = "Tigers", position = "GK", academicYear = "Fr."),
            testPlayer("senior-goalie", "2").copy(name = "Senior Goalie", teamName = "Tigers", position = "Goalie", academicYear = "Senior"),
            testPlayer("fresh-mid", "3").copy(name = "Fresh Mid", teamName = "Tigers", position = "M", academicYear = "Freshman"),
            testPlayer("other", "4").copy(name = "Other Goalie", teamName = "Bears", position = "Goalkeeper", academicYear = "Freshman"),
            testPlayer("unknown", "5").copy(name = "Numeric Year", teamName = "Tigers", position = "G", academicYear = "2030"),
            testPlayer("inactive", "6").copy(name = "Inactive Goalie", teamName = "Tigers", position = "G", academicYear = "Freshman", isActive = false)
        )
        for (text in listOf("Who is our freshman goalie?", "Who are our freshmen goalkeepers?",
            "Show me our freshman goalies", "How many freshman goalies do we have?",
            "Count our freshmen goalkeepers", "Who is our goalie freshman?")) {
            assertNull(parseMemoryQuestion(text), text)
            val request = assertIs<LibraryActionRequest.Roster>(parseLibraryActionRequest(text), text)
            val answer = answerRosterQuestion(request, players, "Tigers")
            assertTrue(answer.contains("1 saved roster match"), "$text: $answer")
            assertTrue(answer.contains("Fresh Goalie #1"), answer)
            for (excluded in listOf("Senior Goalie", "Fresh Mid", "Other Goalie", "Numeric Year", "Inactive Goalie")) {
                assertFalse(answer.contains(excluded), "$text: $answer")
            }
            assertTrue(answerRosterQuestion(request, players, null).contains("2 saved roster matches"))
        }
        val named = assertIs<LibraryActionRequest.Roster>(
            parseLibraryActionRequest("Who is the freshman goalie on Bears?"))
        assertTrue(answerRosterQuestion(named, players, null).contains("Other Goalie"))
        assertFalse(answerRosterQuestion(named, players, "Tigers").contains("Other Goalie"))
        val absent = assertIs<LibraryActionRequest.Roster>(
            parseLibraryActionRequest("Who are our sophomore goalies?"))
        assertTrue(answerRosterQuestion(absent, players, "Tigers").contains("Missing details aren't inferred"))
        assertNull(parseRosterQuestion("Show me freshman goalie saves yesterday"))
        assertNotNull(parseMemoryQuestion("How many freshman goalie saves did I record?"))
    }

    private val tyson = testPlayer("tyson", "28").copy(
        name = "Tyson Knapp", teamName = "Tigers", position = "Midfield", academicYear = "2028")
    private val other = testPlayer("other", "28").copy(name = "Jake Smith", teamName = "Bears")
    private val roster = listOf(tyson, other, other.copy(id = "inactive", name = "Inactive Player", isActive = false))

    @Test
    fun rosterCountsBypassMemoryCountingAndRespectTeamScope() {
        val players = listOf(
            tyson.copy(position = "Goalie", academicYear = "Senior"),
            other.copy(position = "GK", academicYear = "Sr."),
            tyson.copy(id = "mid", name = "Mid Player", position = "M", academicYear = "Junior"),
            tyson.copy(id = "inactive", name = "Inactive Goalie", position = "G", isActive = false)
        )
        for (text in listOf(
            "How many goalies do we have?", "How many golies do we have?",
            "How many goalkeepers are there?", "Count our goalies",
            "What is the number of goalies we have?", "What's the number of goalies?",
            "Could you please tell me how many goalies do we have?",
            "How many goalies are on our roster?", "How many seniors do we have?"
        )) {
            assertNull(parseMemoryQuestion(text), text)
            val request = assertIs<LibraryActionRequest.Roster>(parseLibraryActionRequest(text), text)
            val selected = answerRosterQuestion(request, players, "Tigers")
            assertTrue(selected.contains("1 saved roster match"), "$text: $selected")
            assertFalse(selected.contains("Jake"))
            assertFalse(selected.contains("Inactive Goalie"))
            val all = answerRosterQuestion(request, players, null)
            assertTrue(all.contains("2 saved roster matches"), "$text: $all")
        }
        val named = assertIs<LibraryActionRequest.Roster>(parseLibraryActionRequest("How many goalies do we have on Bears?"))
        assertTrue(answerRosterQuestion(named, players, null).contains("Jake Smith"))
        assertFalse(answerRosterQuestion(named, players, "Tigers").contains("Jake Smith"))
        for (text in listOf("How many goals did Tyson have?", "How many saves did Tyson have?",
            "How many goalie videos did I record?", "How many seniors goals yesterday?")) {
            assertNotNull(parseMemoryQuestion(text), text)
            assertIs<LibraryActionRequest.Count>(parseLibraryActionRequest(text), text)
        }
    }

    @Test
    fun spokenAndTypedIdentificationQuestionsUseTheRosterIntent() {
        for (text in listOf("Who is number 28?", "Who's #28?", "Who is wearing jersey number twenty-eight?",
                "Can you tell me who is player number twenty eight", "Identify player 28",
                "Which player is #28?", "Who is Tyson?", "What number is Tyson?",
                "What number does Tyson wear?", "What is Tyson's jersey number?")) {
            val request = assertIs<LibraryActionRequest.Roster>(parseLibraryActionRequest(text), text)
            val answer = answerRosterQuestion(request, roster, "Tigers")
            assertTrue(answer.contains("Tyson Knapp #28"), "$text: $answer")
            assertTrue(answer.contains("Midfield"), answer)
            assertTrue(answer.contains("2028"), answer)
            assertFalse(answer.contains("Jake"), answer)
        }
    }

    @Test
    fun selectedTeamLimitsResultsAndAllTeamsLabelsDuplicateNumbers() {
        val request = LibraryActionRequest.Roster("28")
        val all = answerRosterQuestion(request, roster, null)
        assertTrue(all.contains("2 saved roster matches"))
        assertTrue(all.contains("Tigers"))
        assertTrue(all.contains("Bears"))
        assertFalse(all.contains("Inactive Player"))
        assertFalse(answerRosterQuestion(request, roster, "Bears").contains("Tyson"))
        val qualified = answerRosterQuestion(LibraryActionRequest.Roster("28 on Tigers"), roster, null)
        assertTrue(qualified.contains("Tyson"))
        assertFalse(qualified.contains("Jake"))
        assertFalse(answerRosterQuestion(LibraryActionRequest.Roster("28 on Bears"), roster, "Tigers").contains("Jake"))
    }

    @Test
    fun namesakesAreListedAndUnknownNamesAreNeverGuessed() {
        val namesakes = roster + tyson.copy(id = "namesake", number = "7", name = "Tyson Smith")
        val answer = answerRosterQuestion(LibraryActionRequest.Roster("Tyson"), namesakes, "Tigers")
        assertTrue(answer.contains("2 saved roster matches"))
        assertTrue(answer.contains("Tyson Smith #7"))
        val full = answerRosterQuestion(LibraryActionRequest.Roster("Tyson Knapp"), namesakes, "Tigers")
        assertFalse(full.contains("Tyson Smith"))
        for (query in listOf("99", "Tysan", "unknown", "#28 goals")) {
            assertTrue(answerRosterQuestion(LibraryActionRequest.Roster(query), roster, "Tigers")
                .startsWith("No saved roster match"), query)
        }
        assertTrue(answerRosterQuestion(LibraryActionRequest.Roster("28"), emptyList(), null)
            .contains("Add or import players"))
    }

    @Test
    fun jerseySpeechHandlesZeroAndNumberWordsWithoutExecutingCameraCommands() {
        val extra = roster + tyson.copy(id = "zero", number = "00", name = "Done Jones")
        for (query in listOf("double zero", "zero zero", "# 00")) {
            assertTrue(answerRosterQuestion(LibraryActionRequest.Roster(query), extra, "Tigers").contains("Done Jones #00"))
        }
        assertTrue(answerRosterQuestion(LibraryActionRequest.Roster("Done Jones"), extra, "Tigers").contains("Done Jones #00"))
    }

    @Test
    fun rosterRoutingPreservesMemoryScheduleAndSharingRequests() {
        assertIs<LibraryActionRequest.Schedule>(parseLibraryActionRequest("Who is our next opponent?"))
        assertIs<LibraryActionRequest.Schedule>(parseLibraryActionRequest("When do we play the Bears?"))
        assertIs<LibraryActionRequest.Count>(parseLibraryActionRequest("How many goals did Tyson have?"))
        assertIs<LibraryActionRequest.Reel>(parseLibraryActionRequest("Share a reel of Tyson's goals with Jenna"))
        assertNull(parseLibraryActionRequest("Find Tyson goals"))
        assertNull(parseRosterQuestion("Who scored the first goal yesterday?"))
        assertNull(parseRosterQuestion("What was Tyson's first goal?"))
    }

    @Test
    fun positionAndClassQuestionsMatchSavedDetailsAndCommonAliases() {
        val players = listOf(
            tyson.copy(position = "GK", academicYear = "Sr."),
            other.copy(position = "Goalkeeper", academicYear = "Senior"),
            tyson.copy(id = "mid", name = "Mid Player", position = "A/M", academicYear = "Junior"),
            tyson.copy(id = "unknown", name = "Unknown Player", position = "Unknown", academicYear = "2026"),
            tyson.copy(id = "inactive", name = "Inactive Goalie", position = "Goalie", isActive = false)
        )
        for (text in listOf("Who are our goalies?", "Who are our golies?", "Who are the seniors?",
                "Who are teh seniors?", "Which players are seniors?", "Show me our goalkeepers", "List seniors")) {
            val request = assertIs<LibraryActionRequest.Roster>(parseLibraryActionRequest(text), text)
            val answer = answerRosterQuestion(request, players, "Tigers")
            assertTrue(answer.contains("Tyson Knapp #28"), "$text: $answer")
            assertFalse(answer.contains("Jake Smith"), answer)
            assertFalse(answer.contains("Unknown Player"), answer)
            assertFalse(answer.contains("Inactive Goalie"), answer)
        }
        val mids = answerRosterQuestion(LibraryActionRequest.Roster("midfielders"), players, "Tigers")
        assertTrue(mids.contains("Mid Player"))
        assertTrue(answerRosterQuestion(LibraryActionRequest.Roster("juniors"), players, "Tigers").contains("Mid Player"))
        val all = answerRosterQuestion(LibraryActionRequest.Roster("seniors"), players, null)
        assertTrue(all.contains("2 saved roster matches"))
        assertTrue(all.contains("Bears"))
        val named = assertIs<LibraryActionRequest.Roster>(parseLibraryActionRequest("Who are the goalies on Bears?"))
        assertTrue(answerRosterQuestion(named, players, null).contains("Jake Smith"))
        assertFalse(answerRosterQuestion(named, players, "Tigers").contains("Jake Smith"))
        assertTrue(answerRosterQuestion(LibraryActionRequest.Roster("freshmen"), players, "Tigers")
            .contains("Missing details aren't inferred"))
    }

    @Test
    fun supportedPositionsAndClassesDoNotCaptureOrdinaryMemorySearches() {
        for ((group, saved) in listOf(
            "defenders" to "D", "attackers" to "Attack", "midfielders" to "Midfield",
            "long-stick midfielders" to "LSM", "faceoff specialists" to "FOGO",
            "freshmen" to "Fr.", "sophomores" to "So.", "juniors" to "Jr.", "seniors" to "Senior"
        )) {
            val request = assertIs<LibraryActionRequest.Roster>(parseLibraryActionRequest("Who are our $group?"))
            val player = tyson.copy(position = saved, academicYear = saved)
            assertTrue(answerRosterQuestion(request, listOf(player), "Tigers").contains("Tyson Knapp"))
        }
        assertNull(parseRosterQuestion("Show me Tyson's goals"))
        assertNull(parseRosterQuestion("Show me seniors goals yesterday"))
        assertNull(parseRosterQuestion("Who are our next opponents?"))
    }
}
