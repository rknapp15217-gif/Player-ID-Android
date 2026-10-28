package com.playerid.app.domain.team

import com.playerid.app.platform.MediaReference
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class CaptureLibraryGroup(
    val teamName: String?,
    val seasonId: String?,
    val opponent: String?,
    val captures: List<MediaReference>,
    val date: LocalDate? = null
)

/**
 * Groups memories like Android: one section per team, season, opponent and game date, so two games against the
 * same opponent get separate headers. Memories without an opponent are not split by date. Unassigned stays last.
 */
fun groupCaptureLibrary(
    captures: List<MediaReference>,
    snapshot: TeamWorkspaceSnapshot,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): List<CaptureLibraryGroup> = captures.groupBy { media ->
    val details = snapshot.captureOrganizations[captureFileKey(media)]
    val opponent = details?.let(snapshot::captureOpponentName)
    GroupKey(details?.teamName, details?.seasonId, opponent,
        if (opponent == null) null else captureDate(media, snapshot, timeZone))
}.map { (key, media) -> CaptureLibraryGroup(key.teamName, key.seasonId, key.opponent, media, key.date) }
    .sortedBy { it.teamName == null }

/** Reels: one section per year the reel was created (newest year first), not by team or opponent. */
data class ReelYearGroup(val year: Int?, val reels: List<MediaReference>)

fun groupReelsByYear(
    reels: List<MediaReference>,
    snapshot: TeamWorkspaceSnapshot,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): List<ReelYearGroup> = reels.groupBy { captureDate(it, snapshot, timeZone)?.year }
    .map { (year, items) -> ReelYearGroup(year, items) }
    .sortedWith(compareBy<ReelYearGroup> { it.year == null }.thenByDescending { it.year ?: 0 })

private data class GroupKey(val teamName: String?, val seasonId: String?, val opponent: String?, val date: LocalDate?)

/** When the memory was captured (the time encoded at the start of PlayerID filenames), else the linked game's date. */
fun captureDate(
    media: MediaReference,
    snapshot: TeamWorkspaceSnapshot,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDate? {
    val key = captureFileKey(media)
    val gameId = snapshot.captureOrganizations[key]?.gameId
    val millis = key.substringBefore('-').takeIf { it.length == 13 && it.all(Char::isDigit) }?.toLong()
        ?: snapshot.games.firstOrNull { it.id == gameId }?.scheduledStartMs
        ?: return null
    return Instant.fromEpochMilliseconds(millis).toLocalDateTime(timeZone).date
}

private val shortMonthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

/** Android-style "MMM d, yyyy" label for a group's capture date; empty when the group has no opponent or date. */
fun captureGroupDateLabel(group: CaptureLibraryGroup): String =
    group.date?.let { "${shortMonthNames[it.monthNumber - 1]} ${it.dayOfMonth}, ${it.year}" }.orEmpty()
