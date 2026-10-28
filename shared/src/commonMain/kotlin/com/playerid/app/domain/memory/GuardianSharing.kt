package com.playerid.app.domain.memory

import com.playerid.app.domain.team.CaptureOrganization
import com.playerid.app.domain.team.TeamWorkspaceSnapshot
import com.playerid.app.domain.team.captureFileKey
import com.playerid.app.domain.team.captureOpponentName
import com.playerid.app.platform.MediaReference
import kotlinx.serialization.Serializable

enum class GuardianSendResult { SENT, CANCELLED, DRAFT_SAVED }

@Serializable
data class GuardianShareContact(
    val displayName: String,
    val phoneNumber: String? = null,
    val email: String? = null,
    val linkedPlayerIds: Set<String> = emptySet(),
    /** Starred in Team > Contacts. Being a player's point of contact does not make a contact a favorite. */
    val isFavorite: Boolean = false
) {
    val key: String
        get() = phoneNumber?.filter(Char::isDigit)?.takeIf { it.isNotEmpty() }?.let { "phone:$it" }
            ?: "email:${email.orEmpty().trim().lowercase()}"
}

fun GuardianShareContact.validate() {
    require(displayName.isNotBlank()) { "Choose a contact with a name." }
    require(!phoneNumber.isNullOrBlank() || !email.isNullOrBlank()) { "Choose a phone number or email address." }
    require(phoneNumber == null || phoneNumber.any(Char::isDigit)) { "Choose a valid phone number." }
    require(email == null || ('@' in email && email.none(Char::isWhitespace))) { "Choose a valid email address." }
}

/**
 * Details used for sharing. A reel has no details of its own, so it uses its source clips'. That gives a
 * reel the same team, favorites and suggested players as the memories it was built from.
 */
private fun TeamWorkspaceSnapshot.shareOrganizations(media: MediaReference): List<CaptureOrganization> {
    val key = captureFileKey(media)
    captureOrganizations[key]?.let { return listOf(it) }
    return captureReels[key]?.sourceFileKeys.orEmpty().mapNotNull { captureOrganizations[it] }
}

fun memoryShareTeamNames(memories: List<MediaReference>, snapshot: TeamWorkspaceSnapshot): List<String> =
    memories.flatMap { snapshot.shareOrganizations(it) }.mapNotNull { it.teamName }.distinct()

fun guardianMemoryShareMessage(memories: List<MediaReference>, snapshot: TeamWorkspaceSnapshot): String {
    val opponents = memories.map { media ->
        snapshot.captureOrganizations[captureFileKey(media)]?.let(snapshot::captureOpponentName)
            ?.trim()?.takeIf { it.isNotEmpty() }
    }.distinct()
    val matchup = opponents.singleOrNull()?.let { " from the game against $it" }.orEmpty()
    val subject = if (memories.size == 1) "this memory" else "these memories"
    return "Hi! I caught $subject$matchup and thought your family might want " +
        (if (memories.size == 1) "it." else "them.") + "\n\nShared via PlayerID"
}

/** Players assigned in the memories' details, mirroring Android's detected-player suggestions. */
fun suggestedMemorySharePlayerIds(
    memories: List<MediaReference>,
    snapshot: TeamWorkspaceSnapshot,
    teamName: String?
): List<String> = memories.flatMap { snapshot.shareOrganizations(it) }
    .filter { it.teamName == teamName }
    .mapNotNull { it.playerId }
    .distinct()
    .filter { id -> snapshot.players.any { it.id == id && it.teamName == teamName && it.isActive } }

/** Contacts starred as favorites for a team, shown in the share sheet's Favorites section. */
fun favoriteShareContacts(snapshot: TeamWorkspaceSnapshot, teamName: String?): List<GuardianShareContact> =
    snapshot.teamShareContacts[teamName].orEmpty().filter { it.isFavorite }.sortedBy { it.displayName.lowercase() }

fun guardiansLinkedToPlayer(snapshot: TeamWorkspaceSnapshot, teamName: String?, playerId: String): List<GuardianShareContact> =
    snapshot.teamShareContacts[teamName].orEmpty().filter { playerId in it.linkedPlayerIds }

/** Roster players resolve to their saved guardians; favorites and phone contacts are added directly. */
fun memoryShareRecipients(
    snapshot: TeamWorkspaceSnapshot,
    teamName: String?,
    selectedPlayerIds: Set<String>,
    selectedFavoriteKeys: Set<String>,
    phoneContacts: List<GuardianShareContact>
): List<GuardianShareContact> {
    val saved = snapshot.teamShareContacts[teamName].orEmpty()
    return (saved.filter { contact -> contact.key in selectedFavoriteKeys || contact.linkedPlayerIds.any { it in selectedPlayerIds } } +
        phoneContacts).distinctBy { it.key }
}

data class GuardianShareRoutes(
    val messages: List<GuardianShareContact>,
    val mail: List<GuardianShareContact>
)

/** Messages reaches everyone with a phone number; Mail covers email-only recipients. */
fun guardianShareRoutes(recipients: List<GuardianShareContact>): GuardianShareRoutes {
    val (phone, other) = recipients.distinctBy { it.key }.partition { !it.phoneNumber.isNullOrBlank() }
    return GuardianShareRoutes(phone, other.filter { !it.email.isNullOrBlank() })
}
