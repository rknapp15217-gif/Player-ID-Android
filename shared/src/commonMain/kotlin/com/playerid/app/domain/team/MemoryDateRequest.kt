package com.playerid.app.domain.team

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

internal data class MemoryDateRequest(
    val start: String? = null,
    val end: String? = null,
    val remaining: String
)

internal fun parseMemoryDateRequest(query: String, today: LocalDate): MemoryDateRequest {
    val source = query.lowercase()
    fun result(match: MatchResult, start: LocalDate, end: LocalDate = start) =
        MemoryDateRequest(start.toString(), end.toString(), source.replaceRange(match.range, " "))
    val day = Regex("\\b(today|yesterday)(?:['’]s)?\\b").find(source)
    if (day != null) return result(day, if (day.groupValues[1] == "today") today else today.minus(DatePeriod(days = 1)))
    val weekend = Regex("\\b(last|previous|this)\\s+weekend(?:['’]s)?\\b").find(source)
    if (weekend != null) {
        val monday = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))
        val saturday = if (weekend.groupValues[1] == "this") monday.plus(DatePeriod(days = 5))
            else monday.minus(DatePeriod(days = 2))
        return result(weekend, saturday, saturday.plus(DatePeriod(days = 1)))
    }
    val summer = Regex("\\b(?:(last|previous|this)\\s+summer|summer(?:\\s+of)?\\s+((?:19|20)[0-9]{2}))(?:['’]s)?\\b").find(source)
    if (summer != null) {
        val year = summer.groupValues[2].toIntOrNull()
            ?: (today.year - if (summer.groupValues[1] in setOf("last", "previous") && today.monthNumber < 9) 1 else 0)
        return result(summer, LocalDate(year, 6, 1), LocalDate(year, 8, 31))
    }
    val relative = Regex("\\b(this|current|last|previous)\\s+(week|month|year)(?:['’]s)?\\b").find(source)
    if (relative != null) {
        val previous = relative.groupValues[1] in setOf("last", "previous")
        val (start, end) = when (relative.groupValues[2]) {
            "week" -> {
                val monday = today.minus(DatePeriod(days = today.dayOfWeek.ordinal + if (previous) 7 else 0))
                monday to monday.plus(DatePeriod(days = 6))
            }
            "month" -> {
                val first = LocalDate(today.year, today.monthNumber, 1)
                    .minus(DatePeriod(months = if (previous) 1 else 0))
                first to first.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
            }
            else -> {
                val year = today.year - if (previous) 1 else 0
                LocalDate(year, 1, 1) to LocalDate(year, 12, 31)
            }
        }
        return result(relative, start, end)
    }
    val days = Regex("\\b(?:last|past)\\s+([1-9][0-9]{0,2})\\s+days?\\b").find(source)
    if (days != null) return result(days, today.minus(DatePeriod(days = days.groupValues[1].toInt() - 1)), today)
    val iso = Regex("\\b([0-9]{4})-([0-9]{2})-([0-9]{2})\\b").find(source)
    if (iso != null) {
        val date = validSearchDate(iso.groupValues[1].toInt(), iso.groupValues[2].toInt(), iso.groupValues[3].toInt())
        if (date != null) return result(iso, date)
    }
    val months = listOf("january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december")
    val monthNames = months.flatMap { listOf(it, it.take(3)) }.distinct().joinToString("|")
    val namedDate = Regex("\\b($monthNames)\\s+([0-9]{1,2})(?:st|nd|rd|th)?(?:,?\\s+([0-9]{4}))?\\b").find(source)
    if (namedDate != null) {
        val month = months.indexOfFirst { it.startsWith(namedDate.groupValues[1]) } + 1
        val year = namedDate.groupValues[3].toIntOrNull() ?: today.year
        val date = validSearchDate(year, month, namedDate.groupValues[2].toInt())
        if (date != null) return result(namedDate, date)
    }
    // Do not extract the year from an invalid full date and accidentally broaden the search.
    if (iso == null && namedDate == null) {
        val year = Regex("\\b((?:19|20)[0-9]{2})\\b").find(source)
        if (year != null) {
            val value = year.groupValues[1].toInt()
            return result(year, LocalDate(value, 1, 1), LocalDate(value, 12, 31))
        }
    }
    return MemoryDateRequest(remaining = source)
}

private fun validSearchDate(year: Int, month: Int, day: Int): LocalDate? =
    try {
        LocalDate(year, month, day)
    } catch (_: IllegalArgumentException) {
        // Leave invalid dates as unmatched search terms rather than silently searching every clip.
        null
    }
