package com.playerid.app.domain.team

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

data class ScheduleImportEntry(
    val gameLabel: String,
    val opponent: String,
    val startMs: Long,
    val endMs: Long,
    val locationName: String?,
    val latitude: Double?,
    val longitude: Double?
)

fun parseScheduleCsv(
    csvText: String,
    timeZoneId: String = TimeZone.currentSystemDefault().id
): List<ScheduleImportEntry> {
    val lines = csvText
        .lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .toList()
    if (lines.isEmpty()) return emptyList()

    val timeZone = runCatching { TimeZone.of(timeZoneId) }.getOrElse { TimeZone.UTC }
    return lines.drop(1).mapNotNull { line ->
        val parts = splitScheduleCsvLine(line)
        if (parts.size < 5) return@mapNotNull null

        val date = parseIsoDate(parts[0]) ?: return@mapNotNull null
        val startTime = parseScheduleTime(parts[1]) ?: return@mapNotNull null
        val startMs = localEpochMilliseconds(date, startTime, timeZone) ?: return@mapNotNull null
        val endMs = parseScheduleTime(parts[2])?.let { endTime ->
            localEpochMilliseconds(date, endTime, timeZone)
        } ?: (startMs + DEFAULT_GAME_DURATION_MS)

        ScheduleImportEntry(
            gameLabel = parts.getOrNull(7)?.trim().orEmpty(),
            opponent = parts[3].trim().ifBlank { "Opponent" },
            startMs = startMs,
            endMs = endMs,
            locationName = parts[4].trim().ifBlank { null },
            latitude = parts.getOrNull(5)?.trim()?.toDoubleOrNull(),
            longitude = parts.getOrNull(6)?.trim()?.toDoubleOrNull()
        )
    }
}

fun parseScheduleText(
    rawText: String,
    timeZoneId: String = TimeZone.currentSystemDefault().id,
    currentYear: Int? = null
): List<ScheduleImportEntry> {
    val csvEntries = parseScheduleCsv(rawText, timeZoneId)
    if (csvEntries.isNotEmpty()) return csvEntries

    val timeZone = runCatching { TimeZone.of(timeZoneId) }.getOrElse { TimeZone.UTC }
    val resolvedYear = currentYear ?: Clock.System.now().toLocalDateTime(timeZone).year
    val lines = rawText.lineSequence()
        .map { it.replace("\u00A0", " ").replace(Regex("\\s+"), " ").trim() }
        .filter(String::isNotEmpty)
        .toList()
    val joined = lines.joinToString("\n")
    val dateMatches = FREE_FORM_DATE.findAll(joined).toList()

    fun scheduleLevelAt(position: Int): String? {
        return Regex("(?im)^SCHEDULE LEVEL:\\s*(.+)$")
            .findAll(joined.substring(0, position))
            .lastOrNull()
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }

    fun buildEntry(
        dateText: String,
        details: String,
        fallbackOpponent: String? = null,
        fallbackTime: String? = null,
        requireExplicitRowEvidence: Boolean = true
    ): ScheduleImportEntry? {
        val date = parseFlexibleScheduleDate(dateText, resolvedYear) ?: return null
        val detectedTime = FREE_FORM_TIME.find(details)?.value ?: fallbackTime
        val startTime = detectedTime?.let(::parseScheduleTime) ?: return null
        val explicitOpponent = OPPONENT.find(details)?.groupValues?.getOrNull(1)
            ?.replace(FREE_FORM_TIME, "")
            ?.replace(Regex("(?i)\\s+(?:home|away)\\s*$"), "")
            ?.trim(' ', '-', ':')
            ?.takeIf(String::isNotEmpty)
        if (requireExplicitRowEvidence && explicitOpponent == null) return null
        val opponent = explicitOpponent ?: fallbackOpponent?.trim()?.takeIf(String::isNotEmpty) ?: return null
        if (opponent.length > 60 || opponent.contains(NON_OPPONENT)) return null

        val startMs = localEpochMilliseconds(date, startTime, timeZone) ?: return null
        val isAway = details.contains(Regex("(?i)(?:^|\\s)@\\s*${Regex.escape(opponent)}"))
        return ScheduleImportEntry(
            gameLabel = if (isAway) "@ $opponent" else "vs $opponent",
            opponent = opponent,
            startMs = startMs,
            endMs = startMs + DEFAULT_GAME_DURATION_MS,
            locationName = null,
            latitude = null,
            longitude = null
        )
    }

    val segmented = dateMatches.mapIndexedNotNull { index, dateMatch ->
        val detailsStart = dateMatch.range.last + 1
        val detailsEnd = dateMatches.getOrNull(index + 1)?.range?.first ?: joined.length
        val nearbyDetails = joined.substring(detailsStart, detailsEnd)
            .lineSequence()
            .take(5)
            .joinToString("\n")
        buildEntry(dateMatch.value, nearbyDetails)
    }.distinctBy { it.startMs to it.opponent.lowercase() }

    val tableRows = dateMatches.mapIndexedNotNull { index, dateMatch ->
        if (!dateMatch.value.contains(Regex("[/-]\\d{2,4}"))) return@mapIndexedNotNull null
        val detailsStart = dateMatch.range.last + 1
        val detailsEnd = dateMatches.getOrNull(index + 1)?.range?.first ?: joined.length
        val details = joined.substring(detailsStart, detailsEnd)
            .lineSequence()
            .take(6)
            .joinToString("\n")
        val time = FREE_FORM_TIME.find(details)?.value ?: return@mapIndexedNotNull null
        val matchupLine = details.lineSequence().firstOrNull(VENUE_ROW::containsMatchIn)
            ?: return@mapIndexedNotNull null
        val venueMatch = VENUE_ROW.find(matchupLine) ?: return@mapIndexedNotNull null
        val rawOpponent = venueMatch.groupValues[1]
            .replace(Regex("\\s*».*$"), "")
            .replace(Regex("\\s*#\\s*$"), "")
            .replace(Regex("(?i)\\s*\\(scrimmage\\)\\s*$"), "")
            .trim()
        val isAway = rawOpponent.startsWith("@")
        val opponentWithoutMarker = rawOpponent.removePrefix("@").trim()
        val opponent = if (isAway) opponentWithoutMarker else extractUppercaseOpponent(opponentWithoutMarker)
        if (opponent.isBlank()) return@mapIndexedNotNull null
        val date = parseFlexibleScheduleDate(dateMatch.value, resolvedYear) ?: return@mapIndexedNotNull null
        val startTime = parseScheduleTime(time) ?: return@mapIndexedNotNull null
        val startMs = localEpochMilliseconds(date, startTime, timeZone) ?: return@mapIndexedNotNull null
        val matchupLabel = if (isAway) "@ $opponent" else "vs $opponent"
        val scheduleLevel = scheduleLevelAt(dateMatch.range.first)
        ScheduleImportEntry(
            gameLabel = scheduleLevel?.let { "$it $matchupLabel" } ?: matchupLabel,
            opponent = opponent,
            startMs = startMs,
            endMs = startMs + DEFAULT_GAME_DURATION_MS,
            locationName = venueMatch.groupValues[2].trim(),
            latitude = null,
            longitude = null
        )
    }

    if (tableRows.isNotEmpty()) {
        return tableRows
            .distinctBy { Triple(it.startMs, it.opponent.lowercase(), it.gameLabel.lowercase()) }
            .sortedBy(ScheduleImportEntry::startMs)
    }
    if (segmented.size == dateMatches.size || dateMatches.size <= 1) return segmented

    fun sectionLines(header: String, followingHeaders: Set<String>): List<String> {
        val start = lines.indexOfFirst { it.equals(header, ignoreCase = true) }
        if (start < 0) return emptyList()
        val end = ((start + 1) until lines.size).firstOrNull { index ->
            followingHeaders.any { lines[index].equals(it, ignoreCase = true) }
        } ?: lines.size
        return lines.subList(start + 1, end)
    }

    val dateSection = sectionLines("DATES", setOf("OPPONENTS", "TIMES"))
    val opponentSection = sectionLines("OPPONENTS", setOf("DATES", "TIMES"))
    val timeSection = sectionLines("TIMES", setOf("DATES", "OPPONENTS"))
    if (dateSection.isEmpty() || opponentSection.isEmpty() || timeSection.isEmpty()) return segmented

    val columnDates = FREE_FORM_DATE.findAll(dateSection.joinToString("\n")).toList()
    val opponents = OPPONENT.findAll(opponentSection.joinToString("\n")).mapNotNull { match ->
        match.groupValues.getOrNull(1)
            ?.replace(FREE_FORM_TIME, "")
            ?.replace(Regex("(?i)\\s+(?:home|away)\\s*$"), "")
            ?.trim(' ', '-', ':')
            ?.takeIf(String::isNotEmpty)
    }.toList()
    val times = FREE_FORM_TIME.findAll(timeSection.joinToString("\n")).map { it.value }.toList()
    val columnEntries = columnDates.mapIndexedNotNull { index, dateMatch ->
        buildEntry(
            dateText = dateMatch.value,
            details = "",
            fallbackOpponent = opponents.getOrNull(index),
            fallbackTime = times.getOrNull(index),
            requireExplicitRowEvidence = false
        )
    }
    val resolvedEntries = if (columnEntries.size == columnDates.size) columnEntries else segmented + columnEntries
    return resolvedEntries
        .distinctBy { it.startMs to it.opponent.lowercase() }
        .sortedBy(ScheduleImportEntry::startMs)
}

private data class ScheduleDate(val year: Int, val month: Int, val day: Int)
private data class ScheduleTime(val hour: Int, val minute: Int)

private fun parseIsoDate(raw: String): ScheduleDate? {
    val match = ISO_DATE.matchEntire(raw.trim()) ?: return null
    return ScheduleDate(
        year = match.groupValues[1].toInt(),
        month = match.groupValues[2].toInt(),
        day = match.groupValues[3].toInt()
    )
}

private fun parseFlexibleScheduleDate(raw: String, currentYear: Int): ScheduleDate? {
    val normalized = raw.trim().replace(Regex("\\s+"), " ")
    NUMERIC_DATE.matchEntire(normalized)?.let { match ->
        val rawYear = match.groupValues[3]
        val year = when (rawYear.length) {
            0 -> currentYear
            2 -> 2000 + rawYear.toInt()
            else -> rawYear.toInt()
        }
        return ScheduleDate(year, match.groupValues[1].toInt(), match.groupValues[2].toInt())
    }
    NAMED_DATE.matchEntire(normalized)?.let { match ->
        val month = MONTH_NUMBERS[match.groupValues[1].lowercase().take(3)] ?: return null
        val year = match.groupValues[3].ifBlank { currentYear.toString() }.toInt()
        return ScheduleDate(year, month, match.groupValues[2].toInt())
    }
    return null
}

private fun parseScheduleTime(raw: String): ScheduleTime? {
    val match = TIME.matchEntire(raw.trim()) ?: return null
    val sourceHour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].ifBlank { "0" }.toInt()
    val period = match.groupValues[3].uppercase()
    if (minute !in 0..59) return null

    val hour = if (period.isNotEmpty()) {
        if (sourceHour !in 1..12) return null
        sourceHour % 12 + if (period == "PM") 12 else 0
    } else {
        if (sourceHour !in 0..23) return null
        sourceHour
    }
    return ScheduleTime(hour, minute)
}

private fun localEpochMilliseconds(
    date: ScheduleDate,
    time: ScheduleTime,
    timeZone: TimeZone
): Long? = runCatching {
    LocalDateTime(date.year, date.month, date.day, time.hour, time.minute)
        .toInstant(timeZone)
        .toEpochMilliseconds()
}.getOrNull()

private fun splitScheduleCsvLine(line: String): List<String> {
    val values = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    line.forEach { char ->
        when {
            char == '"' -> inQuotes = !inQuotes
            char == ',' && !inQuotes -> {
                values += current.toString()
                current.clear()
            }
            else -> current.append(char)
        }
    }
    values += current.toString()
    return values
}

private fun extractUppercaseOpponent(raw: String): String {
    val cleaned = raw.replace(Regex("\\s*#\\s*$"), "").trim()
    return Regex("(?:[A-Z][A-Z.'-]*)(?:\\s+(?:[A-Z][A-Z.'-]*|W/)){0,5}")
        .findAll(cleaned)
        .map { it.value.trim() }
        .filter { it.length >= 3 }
        .toList()
        .lastOrNull()
        ?: cleaned
}

private val ISO_DATE = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$")
private val TIME = Regex("^(\\d{1,2})(?::(\\d{2}))?\\s*(AM|PM)?$", RegexOption.IGNORE_CASE)
private val NUMERIC_DATE = Regex("^(\\d{1,2})[/-](\\d{1,2})(?:[/-](\\d{2,4}))?$")
private val NAMED_DATE = Regex("^([A-Za-z]+)\\s+(\\d{1,2})(?:,?\\s+(\\d{4}))?$", RegexOption.IGNORE_CASE)
private val FREE_FORM_DATE = Regex(
    "(?i)\\b(?:\\d{1,2}[/-]\\d{1,2}(?:[/-]\\d{2,4})?|(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)[\\s\\n]+\\d{1,2}(?:,?[\\s\\n]+\\d{4})?)\\b"
)
private val FREE_FORM_TIME = Regex("(?i)\\b(?:1[0-2]|0?[1-9])(?::[0-5]\\d)?\\s*(?:AM|PM)\\b|\\b(?:[01]?\\d|2[0-3]):[0-5]\\d\\b")
private val OPPONENT = Regex("(?im)(?:^|\\n)\\s*(?:vs\\.?|@)\\s*([^|•,\\n]+)")
private val VENUE_ROW = Regex("(?im)^\\s*([^\\n|•]+?)\\s+at\\s+([^\\n|•»]+)")
private val NON_OPPONENT = Regex("(?i)\\b(menu|login|sign in|calendar|schedule|roster|navigation)\\b")
private val MONTH_NUMBERS = mapOf(
    "jan" to 1, "feb" to 2, "mar" to 3, "apr" to 4,
    "may" to 5, "jun" to 6, "jul" to 7, "aug" to 8,
    "sep" to 9, "oct" to 10, "nov" to 11, "dec" to 12
)
private const val DEFAULT_GAME_DURATION_MS = 2L * 60L * 60L * 1000L