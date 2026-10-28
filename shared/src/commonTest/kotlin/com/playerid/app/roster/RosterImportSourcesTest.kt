package com.playerid.app.roster

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RosterImportSourcesTest {
    @Test
    fun parsesTablesFromScriptOutputIncludingEncodedString() {
        val json = """[{"label":"Varsity","items":[
            {"number":"07","name":"Alex Smith","academicYear":"Sr.","position":"A"},
            {"number":"12","name":"Jordan Lee","academicYear":"fr","position":"FOGO"},
            {"number":"123","name":"Too Long","academicYear":"","position":""},
            {"number":"4","name":"R2D2","academicYear":"","position":""}]},
            {"label":"","items":[]}]"""
        val encoded = "\"" + json.replace("\n", "").replace("\"", "\\\"") + "\""
        listOf(json, encoded).forEach { raw ->
            val tables = parseRosterTablesJson(raw)
            assertEquals(1, tables.size)
            assertEquals("Varsity", tables[0].label)
            assertEquals(
                listOf(
                    RosterCandidate("Alex Smith", "7", "Attack", null, "Senior"),
                    RosterCandidate("Jordan Lee", "12", "FOGO", null, "Freshman")
                ),
                tables[0].candidates
            )
        }
        assertEquals(emptyList(), parseRosterTablesJson("not json"))
        assertEquals(emptyList(), parseRosterTablesJson(null))
    }

    @Test
    fun onlyTablesWithFivePlayersAreImportable() {
        val small = RosterTable("Coaches", List(4) { RosterCandidate("Coach $it", "$it", "", null, null) })
        val roster = RosterTable("Roster", List(5) { RosterCandidate("Player $it", "$it", "", null, null) })
        assertEquals(listOf(roster), importableRosterTables(listOf(small, roster)))
    }

    @Test
    fun dedupesByNumberAndNameAndFlagsDuplicateNumbers() {
        val players = listOf(
            RosterCandidate("Alex Smith", "7", "", null, null),
            RosterCandidate("alex smith", "7", "Attack", null, null),
            RosterCandidate("Sam Fox", "7", "", null, null)
        )
        val distinct = distinctRosterCandidates(players)
        assertEquals(2, distinct.size)
        assertEquals(setOf("7"), duplicateRosterNumbers(distinct))
    }

    @Test
    fun normalizesAddressBarInput() {
        assertEquals("https://example.com/roster", normalizeRosterWebUrl(" example.com/roster "))
        assertEquals("http://example.com", normalizeRosterWebUrl("http://example.com"))
        assertEquals("https://www.google.com/search?q=central+lacrosse+roster%26stats", normalizeRosterWebUrl("central lacrosse roster&stats"))
        assertEquals("example.com", displayRosterWebUrl("https://example.com"))
    }

    @Test
    fun groupsRecognizedTableCellsIntoRows() {
        val boxes = listOf(
            RecognizedTextBox("Alex Smith", 0.2, 0.101, 0.02),
            RecognizedTextBox("7", 0.05, 0.1, 0.02),
            RecognizedTextBox("12", 0.05, 0.2, 0.02),
            RecognizedTextBox("Jordan Lee", 0.2, 0.198, 0.02)
        )
        val result = rosterTextFromBoxes(boxes)
        assertEquals(listOf("7 Alex Smith", "12 Jordan Lee"), result.rawLines)
        assertTrue(result.candidates.any { it.number == "7" && it.name == "Alex Smith" }, result.candidates.toString())
        assertTrue(result.candidates.any { it.number == "12" && it.name == "Jordan Lee" }, result.candidates.toString())
    }
}
