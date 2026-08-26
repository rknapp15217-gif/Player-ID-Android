package com.playerid.app.domain.team

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

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

private val ISO_DATE = Regex("^(\\d{4})-(\\d{2})-(\\d{2})$")
private val TIME = Regex("^(\\d{1,2})(?::(\\d{2}))?\\s*(AM|PM)?$", RegexOption.IGNORE_CASE)
private const val DEFAULT_GAME_DURATION_MS = 2L * 60L * 60L * 1000L