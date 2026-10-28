package com.playerid.app.roster

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A roster table found on a web page, labeled by its caption or nearest heading (Android parity). */
data class RosterTable(val label: String, val candidates: List<RosterCandidate>)

/** Text recognized in an image, positioned in normalized top-left coordinates (0..1). */
data class RecognizedTextBox(val text: String, val left: Double, val top: Double, val height: Double)

/** Same DOM table scan Android's website import runs in its WebView. Returns a JSON string. */
const val ROSTER_TABLES_SCRIPT: String = """
(function() {
  function findLabelForTable(table, index) {
    const caption = table.querySelector('caption');
    if (caption && caption.innerText.trim()) return caption.innerText.trim();
    let node = table;
    while (node) {
      let prev = node.previousElementSibling;
      while (prev) {
        if (/^H[1-4]$/.test(prev.tagName) && prev.innerText.trim()) {
          return prev.innerText.trim();
        }
        prev = prev.previousElementSibling;
      }
      node = node.parentElement;
    }
    return 'Roster Table ' + (index + 1);
  }

  const tables = Array.from(document.querySelectorAll('table'));
  const results = [];
  tables.forEach((table, index) => {
    const rows = Array.from(table.querySelectorAll('tr'));
    const items = [];
    rows.forEach(tr => {
      const cells = Array.from(tr.querySelectorAll('td,th')).map(c => c.innerText.trim());
      if (cells.length >= 2) {
        const num = cells[0].replace('#','').trim();
        const name = cells[1].trim();
        const year = cells[2] ? cells[2].trim() : '';
        const pos = cells[3] ? cells[3].trim() : '';
        if (/^\d{1,2}$/.test(num) && name) {
          items.push({ number: num, name: name, academicYear: year, position: pos });
        }
      }
    });
    if (items.length > 0) {
      results.push({ label: findLabelForTable(table, index), items: items });
    }
  });
  return JSON.stringify(results);
})();
"""

@Serializable
private data class RosterTableJson(val label: String = "", val items: List<RosterRowJson> = emptyList())

@Serializable
private data class RosterRowJson(
    val number: String = "",
    val name: String = "",
    val academicYear: String = "",
    val year: String = "",
    val position: String = "",
    val pos: String = ""
)

private val rosterJson = Json { ignoreUnknownKeys = true; isLenient = true }

/** Parses [ROSTER_TABLES_SCRIPT] output; accepts both raw JSON and a JSON-encoded string. */
fun parseRosterTablesJson(raw: String?): List<RosterTable> = runCatching {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isEmpty() || trimmed == "null") return emptyList()
    val text = if (trimmed.startsWith("\"")) rosterJson.decodeFromString<String>(trimmed) else trimmed
    rosterJson.decodeFromString<List<RosterTableJson>>(text).mapIndexedNotNull { index, table ->
        val candidates = table.items.mapNotNull { row ->
            val number = row.number.trim()
            val name = row.name.trim()
            if (number.length !in 1..2 || !number.all(Char::isDigit)) return@mapNotNull null
            if (name.length < 2 || name.any(Char::isDigit)) return@mapNotNull null
            RosterCandidate(
                name = name,
                number = number.toInt().toString(),
                position = normalizeTablePosition(row.position.ifBlank { row.pos }).orEmpty(),
                graduationYear = null,
                academicYear = normalizeTableAcademicYear(row.academicYear.ifBlank { row.year })
            )
        }
        if (candidates.isEmpty()) null
        else RosterTable(table.label.ifBlank { "Roster Table ${index + 1}" }, candidates)
    }
}.getOrDefault(emptyList())

/** Android only offers tables with at least five players; smaller ones are usually navigation or stats. */
fun importableRosterTables(tables: List<RosterTable>): List<RosterTable> = tables.filter { it.candidates.size >= 5 }

fun distinctRosterCandidates(candidates: List<RosterCandidate>): List<RosterCandidate> =
    candidates.distinctBy { it.number + "|" + it.name.lowercase() }

fun duplicateRosterNumbers(candidates: List<RosterCandidate>): Set<String> =
    candidates.groupBy { it.number }.filterValues { it.size > 1 }.keys

/** Address-bar input: URLs load directly, anything else becomes a Google search (Android parity). */
fun normalizeRosterWebUrl(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "https://"
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    val looksLikeUrl = trimmed.none(Char::isWhitespace) && '.' in trimmed && !trimmed.startsWith('.') && !trimmed.endsWith('.')
    return if (looksLikeUrl) "https://$trimmed" else "https://www.google.com/search?q=" + encodeQuery(trimmed)
}

fun displayRosterWebUrl(url: String): String = url.removePrefix("https://").removePrefix("http://")

/**
 * Groups recognized text into visual rows so "7 | Alex Smith | Sr | Attack" table cells become one
 * parseable line, and also returns the reading-order lines for card-style layouts.
 */
fun rosterTextFromBoxes(boxes: List<RecognizedTextBox>): RosterOcrResult {
    val ordered = boxes.filter { it.text.isNotBlank() }.sortedWith(compareBy({ it.top }, { it.left }))
    val rows = mutableListOf<MutableList<RecognizedTextBox>>()
    for (box in ordered) {
        val row = rows.lastOrNull()
        val rowCenter = row?.let { r -> r.sumOf { it.top + it.height / 2 } / r.size }
        val rowHeight = row?.maxOf { it.height } ?: 0.0
        if (row != null && rowCenter != null &&
            kotlin.math.abs(box.top + box.height / 2 - rowCenter) <= maxOf(rowHeight, box.height) * 0.5
        ) row.add(box) else rows.add(mutableListOf(box))
    }
    val rowLines = rows.map { row -> row.sortedBy { it.left }.joinToString(" ") { it.text.trim() } }
    val readingLines = ordered.map { it.text.trim() }
    return parseRosterText(lines = rowLines, blockLines = listOf(readingLines))
}

private fun normalizeTableAcademicYear(raw: String): String? = when (raw.trim().lowercase().removeSuffix(".")) {
    "freshman", "fr" -> "Freshman"
    "sophomore", "so" -> "Sophomore"
    "junior", "jr" -> "Junior"
    "senior", "sr" -> "Senior"
    else -> null
}

private fun normalizeTablePosition(raw: String): String? {
    val cleaned = raw.trim()
    if (cleaned.isBlank()) return null
    return when (cleaned.uppercase()) {
        "G", "GK", "GOALIE", "GOALKEEPER" -> "Goalie"
        "D", "DEF", "DEFENSE" -> "Defense"
        "M", "MID", "MIDFIELD" -> "Midfield"
        "A", "ATT", "ATTACK" -> "Attack"
        "LSM" -> "LSM"
        "FO", "FOGO", "M/FOGO" -> "FOGO"
        "D/LSM" -> "Defense"
        "A/M" -> "Attack"
        else -> cleaned
    }
}

private fun encodeQuery(value: String): String = buildString {
    value.encodeToByteArray().forEach { byte ->
        val code = byte.toInt() and 0xFF
        val char = code.toChar()
        when {
            char.isLetterOrDigit() && code < 0x80 || char in "-_.~" -> append(char)
            char == ' ' -> append('+')
            else -> {
                append('%')
                append("0123456789ABCDEF"[code shr 4])
                append("0123456789ABCDEF"[code and 0x0F])
            }
        }
    }
}
