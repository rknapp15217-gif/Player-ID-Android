package com.playerid.app.domain.team

import com.playerid.app.platform.MediaReference
import com.playerid.app.platform.savedCaptureKind
import com.playerid.app.domain.camera.cameraJerseyOptions
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.playerid.app.domain.memory.validate

val defaultCaptureMomentTags = listOf("Goal", "Save", "Faceoff Win", "Assist", "Defensive Stop")

@Serializable
data class CaptureOrganization(
    val teamName: String,
    val seasonId: String? = null,
    val gameId: String? = null,
    val playerId: String? = null,
    val opponentName: String? = null
)

// iOS app-container paths can change after an update; capture filenames remain stable.
fun captureFileKey(media: MediaReference): String = media.identifier.substringAfterLast('/')

data class CaptureLibraryFilter(
    val teamName: String? = null,
    val seasonId: String? = null,
    val opponent: String? = null,
    val playerId: String? = null,
    val unassignedOnly: Boolean = false
)

fun normalizeCaptureFilter(filter: CaptureLibraryFilter, snapshot: TeamWorkspaceSnapshot): CaptureLibraryFilter {
    val team = filter.teamName?.takeIf { name -> snapshot.teams.any { it.name == name } }
    val seasons = snapshot.seasons.filter { team == null || it.teamName == team }
    val season = filter.seasonId?.takeIf { id -> seasons.any { it.id == id } }
    val seasonIds = seasons.filter { snapshot.matchesSeasonYear(it.id, season) }.map { it.id }.toSet()
    return filter.copy(
        teamName = team,
        seasonId = season,
        playerId = filter.playerId?.takeIf { id -> captureFilterPlayers(snapshot, team).any { it.id == id } },
        opponent = filter.opponent?.takeIf { name ->
            snapshot.games.any { it.sportSeasonId in seasonIds && it.opponentName.equals(name, ignoreCase = true) } ||
                snapshot.captureOrganizations.values.any {
                    (team == null || it.teamName == team) && snapshot.matchesSeasonYear(it.seasonId, season) &&
                        snapshot.captureOpponentName(it).equals(name, ignoreCase = true)
                }
        }
    )
}

fun filterCaptures(
    captures: List<MediaReference>,
    snapshot: TeamWorkspaceSnapshot,
    filter: CaptureLibraryFilter
): List<MediaReference> = captures.filter { media ->
    val key = captureFileKey(media)
    val keys = if (key in snapshot.captureOrganizations) listOf(key)
        else snapshot.captureReels[key]?.sourceFileKeys ?: listOf(key)
    keys.any { snapshot.captureMatchesFilter(it, filter) }
}

private fun TeamWorkspaceSnapshot.captureMatchesFilter(key: String, filter: CaptureLibraryFilter): Boolean {
    val assignment = captureOrganizations[key]
    return (!filter.unassignedOnly || assignment == null) &&
        (filter.teamName == null || assignment?.teamName == filter.teamName) &&
        (filter.seasonId == null || seasons.firstOrNull { it.id == filter.seasonId }?.let {
            captureSeasonYearForKey(key) == captureSeasonYear(it)
        } == true) &&
        (filter.playerId == null || matchesPlayerName(assignment?.playerId, filter.playerId)) &&
        (filter.opponent == null || assignment?.let(::captureOpponentName).equals(filter.opponent, ignoreCase = true))
}

private val seasonYearPattern = Regex("(^|[^0-9])((19|20)[0-9]{2})([^0-9]|$)")

/**
 * The year a season is shown as in the Memories filter: a year in its label ("2026 Season"),
 * otherwise the year of its first scheduled game, otherwise the year it was created.
 */
fun TeamWorkspaceSnapshot.captureSeasonYear(season: SportSeasonProfile): String {
    seasonYearPattern.find(season.seasonLabel)?.let { return it.groupValues[2] }
    val start = games.filter { it.sportSeasonId == season.id }.minOfOrNull { it.scheduledStartMs } ?: season.createdAt
    return Instant.fromEpochMilliseconds(start).toLocalDateTime(TimeZone.currentSystemDefault()).year.toString()
}

/** Explicit season assignments win; otherwise a known recording date supplies the default year. */
fun TeamWorkspaceSnapshot.captureSeasonYearForKey(
    key: String,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): String? {
    val organization = captureOrganizations[key]
    val seasonId = organization?.seasonId
        ?: games.firstOrNull { it.id == organization?.gameId }?.sportSeasonId
    if (seasonId != null) return seasons.firstOrNull { it.id == seasonId }?.let(::captureSeasonYear)
    return captureDate(MediaReference(key, savedCaptureKind(key) ?: return null), this, timeZone)?.year?.toString()
}

/** The season filter selects a year: any season with the same year as [filterSeasonId] matches. */
fun TeamWorkspaceSnapshot.matchesSeasonYear(seasonId: String?, filterSeasonId: String?): Boolean {
    if (filterSeasonId == null || seasonId == filterSeasonId) return true
    val wanted = seasons.firstOrNull { it.id == filterSeasonId } ?: return false
    val season = seasons.firstOrNull { it.id == seasonId } ?: return false
    return captureSeasonYear(season) == captureSeasonYear(wanted)
}

/** The player filter selects a name: the same child on several teams' rosters matches. */
fun TeamWorkspaceSnapshot.matchesPlayerName(playerId: String?, filterPlayerId: String?): Boolean {
    if (filterPlayerId == null || playerId == filterPlayerId) return true
    val wanted = players.firstOrNull { it.id == filterPlayerId }?.name?.trim() ?: return false
    val player = players.firstOrNull { it.id == playerId }?.name?.trim() ?: return false
    return player.equals(wanted, ignoreCase = true)
}

fun TeamWorkspaceSnapshot.captureOpponentName(organization: CaptureOrganization): String? =
    games.firstOrNull { it.id == organization.gameId }?.opponentName ?: organization.opponentName

internal fun TeamWorkspaceSnapshot.validateCaptureOrganization(organization: CaptureOrganization) {
    require(organization.opponentName == null || organization.opponentName.isNotBlank()) { "Enter an opponent or leave it empty." }
    require(teams.any { it.name == organization.teamName }) { "Choose an existing team." }
    require(organization.seasonId == null || seasons.any {
        it.id == organization.seasonId && it.teamName == organization.teamName
    }) { "Choose a season from this team." }
    require(organization.gameId == null || games.any {
        it.id == organization.gameId && it.sportSeasonId == organization.seasonId
    }) { "Choose a game from this season." }
    require(organization.playerId == null || players.any {
        it.id == organization.playerId && it.teamName == organization.teamName
    }) { "Choose a player from this team's roster." }
}
internal fun validateCaptureFileKey(fileKey: String) {
    require(fileKey.isNotBlank() && '/' !in fileKey && '\\' !in fileKey &&
        savedCaptureKind(fileKey) != null) {
        "This capture has an invalid file name."
    }

}

internal fun TeamWorkspaceSnapshot.validateCaptureOrganizations() {
    captureReels.forEach { (key, reel) ->
        validateCaptureFileKey(key)
        require(savedCaptureKind(key) == com.playerid.app.platform.MediaKind.VIDEO &&
            reel.name.isNotBlank() && reel.name == reel.name.trim() &&
            reel.sourceFileKeys.size >= 2 && reel.sourceFileKeys.distinct().size == reel.sourceFileKeys.size &&
            key !in reel.sourceFileKeys) { "A saved reel has invalid details." }
        reel.sourceFileKeys.forEach {
            validateCaptureFileKey(it)
            require(savedCaptureKind(it) == com.playerid.app.platform.MediaKind.VIDEO) { "Reels contain videos only." }
        }
    }
    teamShareContacts.forEach { (team, contacts) ->
        require(teams.any { it.name == team }) { "A guardian's team no longer exists." }
        require(contacts.map { it.key }.distinct().size == contacts.size) { "Duplicate guardian contacts." }
        contacts.forEach { contact ->
            contact.validate()
            require(contact.linkedPlayerIds.all { id -> players.any { it.id == id && it.teamName == team } }) {
                "A guardian's linked player no longer belongs to this team."
            }
        }
    }
    captureMomentTags.forEach { (key, tag) ->
        validateCaptureFileKey(key)
        require(tag.isNotBlank() && tag == tag.trim()) { "Enter a moment tag." }
    }
    require(customCaptureMomentTags.all { it.isNotBlank() && it == it.trim() } &&
        (defaultCaptureMomentTags + customCaptureMomentTags).map { it.lowercase() }.distinct().size ==
        defaultCaptureMomentTags.size + customCaptureMomentTags.size) { "Custom moment tags must have unique names." }
    cameraOrganization?.let { validateCaptureOrganization(it) }
    require(cameraJerseyLabel == null || (cameraOrganization != null &&
        cameraJerseyOptions(teams.firstOrNull { it.name == cameraOrganization.teamName }).any { it.first == cameraJerseyLabel })) {
        "Choose a jersey color from this team."
    }
    captureOrganizations.forEach { (key, organization) ->
        validateCaptureFileKey(key)
        validateCaptureOrganization(organization)
    }
}

/** Non-reel memories tagged to each player on [teamName], keyed by player id (Android roster badge parity). */
fun playerMemoryCounts(snapshot: TeamWorkspaceSnapshot, teamName: String): Map<String, Int> =
    snapshot.captureOrganizations
        .filter { (key, organization) ->
            key !in snapshot.captureReels && organization.teamName == teamName && organization.playerId != null
        }
        .values
        .groupingBy { it.playerId!! }
        .eachCount()

/** Opponents with at least one saved memory that matches the filter's team, season and player. */
fun captureFilterOpponents(
    snapshot: TeamWorkspaceSnapshot,
    filter: CaptureLibraryFilter,
    savedCaptureKeys: Set<String>
): List<String> = snapshot.captureOrganizations
    .filter { (key, organization) ->
        key in savedCaptureKeys &&
            (filter.teamName == null || organization.teamName == filter.teamName) &&
            snapshot.matchesSeasonYear(organization.seasonId, filter.seasonId) &&
            snapshot.matchesPlayerName(organization.playerId, filter.playerId)
    }
    .values
    .mapNotNull { snapshot.captureOpponentName(it)?.trim()?.takeIf(String::isNotEmpty) }
    .distinctBy { it.lowercase() }
    .sortedBy { it.lowercase() }

/**
 * Opponent choices for the memory details editor, like Android's opponent editor: opponents from
 * [teamName]'s schedule (limited to [seasonId] when set) plus opponents already saved on its memories.
 */
fun captureOpponentOptions(
    snapshot: TeamWorkspaceSnapshot,
    teamName: String,
    seasonId: String?
): List<String> {
    val seasonIds = snapshot.seasons
        .filter { it.teamName == teamName && (seasonId == null || it.id == seasonId) }
        .map { it.id }
        .toSet()
    val scheduled = snapshot.games.filter { it.sportSeasonId in seasonIds }.map { it.opponentName }
    val saved = snapshot.captureOrganizations.values
        .filter { it.teamName == teamName && (seasonId == null || it.seasonId == seasonId) }
        .mapNotNull(snapshot::captureOpponentName)
    return (scheduled + saved)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinctBy(::opponentKey)
        .sortedBy { it.lowercase() }
}

/**
 * Opponent suggestions for a focused opponent field. Until the user edits the text ([filtering] false) every
 * other known opponent is offered, so a prefilled current opponent doesn't hide the rest; once they type,
 * the list narrows to [matchingOpponentOptions].
 */
fun opponentSuggestions(options: List<String>, typed: String, filtering: Boolean, limit: Int = 8): List<String> =
    if (filtering && typed.isNotBlank()) matchingOpponentOptions(options, typed, limit)
    else options.filterNot { opponentKey(it) == opponentKey(typed) }.take(limit)

/** Up to [limit] [options] matching [typed]; names starting with it come first. Empty input lists the first options. */
fun matchingOpponentOptions(options: List<String>, typed: String, limit: Int = 5): List<String> {
    val query = typed.trim()
    if (query.isEmpty()) return options.take(limit)
    return options
        .filter { option ->
            val matches = option.contains(query, ignoreCase = true) ||
                opponentKey(option).contains(opponentKey(query)) ||
                (opponentKey(query).length >= 2 && opponentInitials(option).startsWith(opponentKey(query)))
            matches && opponentKey(option) != opponentKey(query)
        }
        .sortedBy { if (it.startsWith(query, ignoreCase = true)) 0 else 1 }
        .take(limit)
}

/**
 * Camera's capture details with the player linked to the selected team: that team's active "My Player",
 * or none (All) when the team has no My Player. Keeps the camera pill and new captures in sync with Teams.
 */
fun TeamWorkspaceSnapshot.linkedCameraOrganization(): CaptureOrganization? = cameraOrganization?.let { organization ->
    val myPlayer = assignedPlayerIds[organization.teamName]?.takeIf { id ->
        players.any { it.id == id && it.teamName == organization.teamName && it.isActive }
    }
    organization.copy(playerId = myPlayer)
}

/** Players chosen as "My Player" in Teams, limited to [teamName] when a team filter is set. */
fun captureFilterPlayers(snapshot: TeamWorkspaceSnapshot, teamName: String?): List<PlayerProfile> =
    snapshot.assignedPlayerIds
        .filterKeys { teamName == null || it == teamName }
        .mapNotNull { (team, id) -> snapshot.players.firstOrNull { it.id == id && it.teamName == team && it.isActive } }
        .sortedBy { it.name.lowercase() }
