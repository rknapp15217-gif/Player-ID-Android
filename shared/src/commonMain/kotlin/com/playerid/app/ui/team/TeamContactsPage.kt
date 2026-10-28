package com.playerid.app.ui.team

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.playerid.app.domain.memory.GuardianShareContact
import com.playerid.app.domain.memory.favoriteShareContacts
import com.playerid.app.domain.memory.guardiansLinkedToPlayer
import com.playerid.app.domain.team.LocalTeamRepository
import com.playerid.app.domain.team.PlayerProfile
import com.playerid.app.domain.team.TeamStorageException
import kotlinx.coroutines.launch

private val FavoriteGold = Color(0xFFFFB300)

/**
 * Team > Contacts: each player's points of contact (parents/guardians) and the team's favorite share recipients.
 * Being a point of contact never stars a contact; favorites are chosen here and can come straight from the phone.
 */
@Composable
fun TeamContactsPage(
    teamName: String,
    players: List<PlayerProfile>,
    repository: LocalTeamRepository,
    pickContact: (suspend () -> GuardianShareContact?)?,
    onBack: () -> Unit
) {
    val snapshot by repository.snapshot.collectAsState()
    val favorites = favoriteShareContacts(snapshot, teamName)
        .sortedWith(compareBy({ lastNameSortKey(it.displayName) }, { it.displayName.lowercase() }))
    val roster = players.filter { it.isActive }.sortedWith(compareBy({ lastNameSortKey(it.name) }, { it.name.lowercase() }))
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var expandedPlayerIds by remember(teamName) { mutableStateOf(emptySet<String>()) }
    var error by remember { mutableStateOf<String?>(null) }

    fun run(action: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        scope.launch {
            try {
                action()
            } catch (failure: TeamStorageException) {
                error = failure.message ?: "PlayerID couldn't save this contact."
            } catch (failure: IllegalArgumentException) {
                error = failure.message ?: "Check the selected contact."
            } catch (failure: Exception) {
                error = failure.message ?: "Contacts couldn't be opened."
            } finally {
                busy = false
            }
        }
    }
    fun pickThen(onPicked: suspend (GuardianShareContact) -> Unit) = run {
        val picker = pickContact ?: throw IllegalArgumentException("Phone contacts aren't available on this device.")
        val picked = picker()
        if (picked != null) onPicked(picked)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    Spacer(Modifier.width(4.dp))
                    Text("Back")
                }
                Text("Contacts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 8.dp))
            }
        }
        error?.let { item { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) } }

        item { TeamOverviewSectionLabel("FAVORITES") }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                favorites.forEach { contact ->
                    ContactRow(
                        contact = contact,
                        detail = contactPlayersLabel(contact, roster),
                        enabled = !busy,
                        onToggleFavorite = { run { repository.setShareContactFavorite(teamName, contact.key, false) } }
                    )
                    Divider()
                }
                TextButton(
                    enabled = !busy && pickContact != null,
                    onClick = {
                        pickThen { picked ->
                            repository.addFavoriteShareContact(teamName, picked)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.ContactPhone, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add favorite from phone contacts")
                }
            }
        }

        item { TeamOverviewSectionLabel("PLAYERS") }
        if (roster.isEmpty()) item {
            Text("Add players to the roster first.", color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp))
        }
        items(roster, key = { it.id }) { player ->
            val contacts = guardiansLinkedToPlayer(snapshot, teamName, player.id)
                .sortedWith(compareBy({ lastNameSortKey(it.displayName) }, { it.displayName.lowercase() }))
            val expanded = player.id in expandedPlayerIds
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { expandedPlayerIds = if (expanded) expandedPlayerIds - player.id else expandedPlayerIds + player.id }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Full name always shows; long names wrap instead of truncating, with the number after them.
                    Text("${player.name}  #${player.number}", fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Hide contacts" else "Show contacts",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (expanded) {
                contacts.forEach { contact ->
                    ContactRow(
                        contact = contact,
                        detail = contact.phoneNumber ?: contact.email.orEmpty(),
                        enabled = !busy,
                        onToggleFavorite = { run { repository.setShareContactFavorite(teamName, contact.key, !contact.isFavorite) } },
                        onRemove = { run { repository.unlinkShareContact(teamName, player.id, contact.key) } },
                        removeDescription = "Remove ${contact.displayName} from ${player.name}"
                    )
                }
                TextButton(
                    enabled = !busy && pickContact != null,
                    onClick = { pickThen { picked -> repository.linkShareContact(teamName, player.id, picked) } },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Add contact")
                }
                }
            }
        }
    }
}

private val NameSuffixes = setOf("jr", "sr", "ii", "iii", "iv", "v")

/** Last name (ignoring Jr./III-style suffixes) for alphabetical roster order. */
private fun lastNameSortKey(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val trimmed = parts.dropLastWhile { parts.size > 1 && it.lowercase().trimEnd('.', ',') in NameSuffixes }
    return (trimmed.lastOrNull() ?: name).lowercase()
}

private fun contactPlayersLabel(contact: GuardianShareContact, roster: List<PlayerProfile>): String {
    val names = roster.filter { it.id in contact.linkedPlayerIds }.map { it.name.substringBefore(' ') }
    val reach = contact.phoneNumber ?: contact.email.orEmpty()
    return if (names.isEmpty()) reach else "$reach · ${names.joinToString()}"
}

@Composable
private fun ContactRow(
    contact: GuardianShareContact,
    detail: String,
    enabled: Boolean,
    onToggleFavorite: () -> Unit,
    onRemove: (() -> Unit)? = null,
    removeDescription: String = ""
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(36.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(contact.displayName.trim().take(1).uppercase(), fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Column(Modifier.weight(1f)) {
            Text(contact.displayName, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(enabled = enabled, onClick = onToggleFavorite) {
            if (contact.isFavorite) Icon(Icons.Default.Star, "Remove ${contact.displayName} from favorites", tint = FavoriteGold)
            else Icon(Icons.Outlined.StarOutline, "Add ${contact.displayName} to favorites",
                tint = MaterialTheme.colorScheme.outline)
        }
        if (onRemove != null) IconButton(enabled = enabled, onClick = onRemove) {
            Icon(Icons.Default.Close, removeDescription, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
