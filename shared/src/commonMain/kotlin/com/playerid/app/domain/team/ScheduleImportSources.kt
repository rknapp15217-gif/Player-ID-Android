package com.playerid.app.domain.team

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Same page scan Android's schedule website import runs: schedule table rows, else the page text. */
const val SCHEDULE_EXTRACTION_SCRIPT: String = """
(function() {
    const datePattern = /\b\d{1,2}[\/-]\d{1,2}[\/-]\d{2,4}\b/;
    const timePattern = /\b(?:1[0-2]|0?[1-9])(?::[0-5]\d)?\s*(?:am|pm)\b/i;
    const headings = Array.from(document.querySelectorAll('#schedule-results h2, h2'))
        .filter(heading => /Football Schedule$/i.test((heading.innerText || '').trim()));
    const sections = headings.map(heading => {
        let table = null;
        let node = heading.nextElementSibling;
        while (node && !table && !/^H[1-6]$/.test(node.tagName)) {
            if (node.matches && node.matches('table.standard-table')) table = node;
            node = node.nextElementSibling;
        }
        const headingText = (heading.innerText || '').trim();
        const levelText = headingText
            .replace(/^\d{4}-\d{4}\s+/i, '')
            .replace(/\s*Football Schedule$/i, '')
            .trim();
        return { level: levelText || 'Varsity', table: table };
    }).filter(section => section.table);
    const sourceSections = sections.length > 0
        ? sections
        : [{ level: 'Varsity', table: document.querySelector('#schedule-results table.standard-table, table.standard-table') }];
    const rows = sourceSections.flatMap(section => Array.from(section.table?.querySelectorAll('tbody tr') || []).map(row => {
        const date = (row.querySelector('.game-date')?.innerText || '').trim();
        const time = (row.querySelector('.game-time')?.innerText || '').trim();
        const opponent = (row.querySelector('.opp')?.innerText || '').trim();
        const venue = (row.querySelector('.venue')?.innerText || '').replace('»', '').trim();
        if (date && time && opponent) {
            return 'SCHEDULE LEVEL: ' + section.level + '\n' + date + '\n' + time + '\n' + opponent + (venue ? ' at ' + venue : '');
        }
        return Array.from(row.querySelectorAll('th,td'))
            .map(cell => (cell.innerText || '').trim())
            .filter(Boolean)
            .join('\n');
    })).filter(row => datePattern.test(row) && timePattern.test(row));
    return rows.length > 0 ? rows.join('\n') : (document.body.innerText || '');
})();
"""

const val SCHEDULE_IMPORT_START_URL = "https://www.google.com"

private val importMonthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** Android's review format, "MMM d, h:mm a" (for example "Oct 20, 6:00 PM"). */
fun formatScheduleImportStart(startMs: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val local = Instant.fromEpochMilliseconds(startMs).toLocalDateTime(timeZone)
    val hour = if (local.hour % 12 == 0) 12 else local.hour % 12
    val minute = local.minute.toString().padStart(2, '0')
    val period = if (local.hour < 12) "AM" else "PM"
    return "${importMonthNames[local.monthNumber - 1]} ${local.dayOfMonth}, $hour:$minute $period"
}

/** Games the shared workspace can save; others are shown but start unselected. */
fun ScheduleImportEntry.isImportable(): Boolean = opponent.isNotBlank() && endMs > startMs
