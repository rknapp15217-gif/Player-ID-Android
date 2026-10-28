package com.playerid.app.ui.memory

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.memory.*
import com.playerid.app.domain.team.LocalTeamRepository
import com.playerid.app.domain.team.PlayerProfile
import com.playerid.app.domain.team.TeamStorageException
import com.playerid.app.domain.team.captureFileKey
import com.playerid.app.domain.team.captureOpponentName
import com.playerid.app.domain.team.actionRecipientMatches
import com.playerid.app.platform.MediaReference
import com.playerid.app.ui.screens.captureLibraryBadge
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val ShareAccent = Color(0xFF205C96)
private val ShareGreen = Color(0xFF159B62)

/** Mirrors Android's full-screen memory Share screen using iPhone guardian links and native composers. */
@Composable
fun GuardianMemoryShareDialog(
    memories: List<MediaReference>,
    repository: LocalTeamRepository,
    pickContact: suspend () -> GuardianShareContact?,
    sendToContacts: suspend (List<MediaReference>, List<GuardianShareContact>, String, Boolean) -> GuardianSendResult,
    preview: @Composable (MediaReference, Modifier) -> Unit,
    onComplete: (MemoryShareResult) -> Unit,
    requestedRecipient: String? = null,
    findPhoneContacts: (suspend (String) -> List<GuardianShareContact>)? = null
) {
    val snapshot by repository.snapshot.collectAsState()
    val suggestedTeams = memoryShareTeamNames(memories, snapshot)
    var teamName by remember { mutableStateOf(suggestedTeams.singleOrNull()) }
    val suggestedIds = suggestedMemorySharePlayerIds(memories, snapshot, teamName)
    var selectedPlayerIds by remember(teamName) { mutableStateOf(if (requestedRecipient == null) suggestedIds.toSet() else emptySet()) }
    var selectedFavoriteKeys by remember(teamName) { mutableStateOf(emptySet<String>()) }
    var phoneContacts by remember { mutableStateOf(emptyList<GuardianShareContact>()) }
    var showSuggested by remember { mutableStateOf(false) }
    var showRoster by remember { mutableStateOf(false) }
    var showFavorites by remember { mutableStateOf(false) }
    var showContactsPage by remember { mutableStateOf(false) }
    var messagesSent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val roster = snapshot.players.filter { it.teamName == teamName && it.isActive }.sortedBy { it.name.lowercase() }
    val favorites = favoriteShareContacts(snapshot, teamName)
    val recipients = memoryShareRecipients(snapshot, teamName, selectedPlayerIds, selectedFavoriteKeys, phoneContacts)
    var phoneMatches by remember { mutableStateOf(emptyList<GuardianShareContact>()) }
    var phoneSearchDone by remember { mutableStateOf(requestedRecipient == null || findPhoneContacts == null) }
    val savedMatches = snapshot.teamShareContacts.values.flatten()
        .filter { requestedRecipient != null && actionRecipientMatches(requestedRecipient, it.displayName) }
    // In-app contacts and phone contacts are both candidates; the same person (same number/email) appears once.
    val namedContacts = (savedMatches + phoneMatches).distinctBy { it.key }
    LaunchedEffect(requestedRecipient) {
        if (requestedRecipient != null && findPhoneContacts != null) {
            phoneMatches = try {
                findPhoneContacts(requestedRecipient).filter { actionRecipientMatches(requestedRecipient, it.displayName) }
            } catch (_: Exception) { emptyList() }
            phoneSearchDone = true
        }
    }
    LaunchedEffect(requestedRecipient, namedContacts.map { it.key }, phoneSearchDone) {
        if (requestedRecipient != null && phoneSearchDone && namedContacts.size == 1) phoneContacts = namedContacts
    }
    val togglePlayer: (String) -> Unit = { id ->
        selectedPlayerIds = if (id in selectedPlayerIds) selectedPlayerIds - id else selectedPlayerIds + id
    }
    val runAction: (suspend () -> Unit) -> Unit = { action ->
        if (!busy) {
            busy = true
            error = null
            message = null
            scope.launch {
                withContext(NonCancellable) {
                    try {
                        action()
                    } catch (failure: CameraCaptureException) {
                        error = failure.message ?: "PlayerID couldn't open sharing."
                    } catch (failure: TeamStorageException) {
                        error = failure.message ?: "PlayerID couldn't save this guardian."
                    } catch (failure: IllegalArgumentException) {
                        error = failure.message ?: "Check the selected people."
                    } finally {
                        busy = false
                    }
                }
            }
        }
    }
    val linkGuardian: (PlayerProfile) -> Unit = { player ->
        runAction {
            val team = requireNotNull(teamName) { "Choose a team." }
            val picked = pickContact() ?: run { message = "Contact selection canceled."; return@runAction }
            // Linking makes them the player's point of contact; it doesn't add them to Favorites.
            repository.linkShareContact(team, player.id, picked)
            selectedPlayerIds = selectedPlayerIds + player.id
            message = "${picked.displayName} is linked to ${player.name}."
        }
    }
    val share: () -> Unit = {
        runAction {
            val routes = guardianShareRoutes(recipients)
            val body = guardianMemoryShareMessage(memories, snapshot)
            if (routes.messages.isNotEmpty() && !messagesSent) {
                when (sendToContacts(memories, routes.messages, body, false)) {
                    GuardianSendResult.SENT -> messagesSent = true
                    else -> { message = "Sending canceled. Your memories are still saved."; return@runAction }
                }
            }
            if (routes.mail.isNotEmpty()) {
                when (sendToContacts(memories, routes.mail, body, true)) {
                    GuardianSendResult.SENT -> Unit
                    GuardianSendResult.CANCELLED -> {
                        message = if (messagesSent) "Sent in Messages. Mail was canceled; tap Share to retry Mail only."
                        else "Sending canceled. Your memories are still saved."
                        return@runAction
                    }
                    GuardianSendResult.DRAFT_SAVED -> {
                        message = "Mail draft saved, not sent. You can finish it in Mail."
                        return@runAction
                    }
                }
            }
            onComplete(MemoryShareResult.COMPLETED)
        }
    }
    val close: () -> Unit = { if (!busy) onComplete(MemoryShareResult.CANCELLED) }

    Dialog(
        onDismissRequest = { if (showContactsPage) showContactsPage = false else close() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val contactsTeam = teamName
        if (showContactsPage && contactsTeam != null) {
            // Same Team > Contacts screen, opened in place so the user returns straight to this share.
            Surface(Modifier.fillMaxSize(), shape = RectangleShape, color = MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                    com.playerid.app.ui.team.TeamContactsPage(
                        teamName = contactsTeam,
                        players = snapshot.players.filter { it.teamName == contactsTeam },
                        repository = repository,
                        pickContact = pickContact,
                        onBack = { showContactsPage = false }
                    )
                }
            }
            return@Dialog
        }
        Surface(Modifier.fillMaxSize(), shape = RectangleShape, color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp)) {
                    IconButton(onClick = close, enabled = !busy, modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                    Text("Share", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center))
                }
                Divider()
                SharePreview(memories, snapshot, preview)
                Divider()
                Column(
                    Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (requestedRecipient != null) {
                        when (namedContacts.size) {
                            0 -> Text(if (!phoneSearchDone) "Searching your contacts…"
                                else "No team or phone contact matched. Use Phone contacts below to choose $requestedRecipient.")
                            1 -> Unit
                            else -> {
                                Text("More than one contact matches. Select who to share with:")
                                namedContacts.forEach { contact ->
                                    val selected = phoneContacts.any { it.key == contact.key }
                                    Row(
                                        Modifier.fillMaxWidth().clickable(enabled = !busy) {
                                            phoneContacts = if (selected) phoneContacts.filterNot { it.key == contact.key }
                                            else phoneContacts + contact
                                        },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(selected, enabled = !busy, onCheckedChange = null)
                                        Spacer(Modifier.width(10.dp))
                                        Text("${contact.displayName} — ${contact.phoneNumber ?: contact.email.orEmpty()}")
                                    }
                                }
                            }
                        }
                    }
                    if (suggestedTeams.size != 1) {
                        CaptureChoiceMenu(
                            teamName ?: "Choose team", teamName != null, "Choose team",
                            snapshot.teams.filter { it.isActive && !it.isArchived }.map { it.name to it.name }, !busy
                        ) { teamName = it }
                    }
                    val suggested = suggestedIds.mapNotNull { id -> roster.firstOrNull { it.id == id } }
                    if (suggested.isNotEmpty()) {
                        ShareSectionHeader(Icons.Default.PersonSearch, "Suggested players",
                            "${suggested.count { it.id in selectedPlayerIds }} selected", showSuggested) { showSuggested = !showSuggested }
                    }
                    if (suggested.isNotEmpty() && showSuggested) {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            suggested.forEach { player ->
                                val selected = player.id in selectedPlayerIds
                                Column(
                                    Modifier.width(96.dp).clickable(enabled = !busy) { togglePlayer(player.id) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(contentAlignment = Alignment.TopEnd) {
                                        InitialsAvatar(player.name, 54.dp, Color(0xFFDDF3E8), Color(0xFF087A4B))
                                        if (selected) Icon(Icons.Default.CheckCircle, "Selected", tint = ShareGreen,
                                            modifier = Modifier.size(20.dp).background(Color.White, CircleShape))
                                    }
                                    Text(player.name, maxLines = 2, style = MaterialTheme.typography.labelMedium,
                                        textAlign = TextAlign.Center)
                                    Text("#${player.number}", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    ShareSectionHeader(Icons.Default.Group, "Roster",
                        "${roster.count { it.id in selectedPlayerIds }} selected", showRoster) { showRoster = !showRoster }
                    if (showRoster) {
                        if (roster.isEmpty()) ShareHint(if (teamName == null) "Choose a team to see the roster." else "No roster players")
                        else Column(Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                            roster.forEach { player ->
                                val selected = player.id in selectedPlayerIds
                                val guardians = guardiansLinkedToPlayer(snapshot, teamName, player.id)
                                Row(
                                    Modifier.fillMaxWidth().clickable(enabled = !busy) { togglePlayer(player.id) }.padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    InitialsAvatar(player.name, 38.dp, Color(0xFFDDF3E8), Color(0xFF087A4B))
                                    Column(Modifier.weight(1f)) {
                                        Text(player.name, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            "#${player.number} | " + when (guardians.size) {
                                                0 -> "No guardian linked"
                                                1 -> guardians.single().displayName
                                                else -> "${guardians.size} guardians"
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (guardians.isEmpty()) TextButton(enabled = !busy, onClick = { linkGuardian(player) }) { Text("Link") }
                                    Checkbox(selected, enabled = !busy, onCheckedChange = { togglePlayer(player.id) })
                                }
                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                            }
                        }
                    }

                    ShareSectionHeader(Icons.Default.Star, "Favorites",
                        "${favorites.count { it.key in selectedFavoriteKeys }} selected", showFavorites) { showFavorites = !showFavorites }
                    if (showFavorites) {
                        if (favorites.isEmpty()) Row(verticalAlignment = Alignment.CenterVertically) {
                            ShareHint("No favorites yet.")
                            TextButton(enabled = !busy, onClick = { showContactsPage = true }) { Text("Set favorites") }
                        }
                        else Column(Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState())) {
                            favorites.forEach { contact ->
                                val selected = contact.key in selectedFavoriteKeys
                                val toggle = {
                                    selectedFavoriteKeys = if (selected) selectedFavoriteKeys - contact.key else selectedFavoriteKeys + contact.key
                                }
                                Row(
                                    Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = toggle).padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    InitialsAvatar(contact.displayName, 40.dp, Color(0xFFE7F1FC), ShareAccent, single = true)
                                    Column(Modifier.weight(1f)) {
                                        Text(contact.displayName, fontWeight = FontWeight.SemiBold)
                                        Text(contact.phoneNumber ?: contact.email.orEmpty(), style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Checkbox(selected, enabled = !busy, onCheckedChange = { toggle() },
                                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF3676B8),
                                            uncheckedColor = Color(0xFF6C8FB3), checkmarkColor = Color.White))
                                    IconButton(enabled = !busy, onClick = {
                                        runAction {
                                            repository.setShareContactFavorite(requireNotNull(teamName), contact.key, false)
                                            selectedFavoriteKeys = selectedFavoriteKeys - contact.key
                                        }
                                    }) { Icon(Icons.Default.Close, "Remove ${contact.displayName} from favorites") }
                                }
                            }
                        }
                    }

                    ShareSectionHeader(Icons.Default.PersonAdd, "Phone contacts", "${phoneContacts.size} selected", null) {
                        runAction {
                            val picked = pickContact()
                            if (picked == null) message = "Contact selection canceled."
                            else phoneContacts = (phoneContacts + picked).distinctBy { it.key }
                        }
                    }
                    phoneContacts.forEach { contact ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            InitialsAvatar(contact.displayName, 40.dp, Color(0xFFE7F1FC), ShareAccent, single = true)
                            Column(Modifier.weight(1f)) {
                                Text(contact.displayName, fontWeight = FontWeight.SemiBold)
                                Text(contact.phoneNumber ?: contact.email.orEmpty(), style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(enabled = !busy, onClick = { phoneContacts = phoneContacts - contact }) {
                                Icon(Icons.Default.Close, "Remove ${contact.displayName}")
                            }
                        }
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                Button(
                    enabled = !busy && recipients.isNotEmpty(),
                    onClick = share,
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(54.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ShareGreen)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (recipients.isEmpty()) "Select people to share with"
                        else "Share with ${recipients.size} ${if (recipients.size == 1) "person" else "people"}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SharePreview(
    memories: List<MediaReference>,
    snapshot: com.playerid.app.domain.team.TeamWorkspaceSnapshot,
    preview: @Composable (MediaReference, Modifier) -> Unit
) {
    val first = memories.first()
    val key = captureFileKey(first)
    val organization = snapshot.captureOrganizations[key]
    val (tag, tagColor) = captureLibraryBadge(snapshot.captureReels[key]?.name, snapshot.captureMomentTags[key],
        MaterialTheme.colorScheme.onSurfaceVariant)
    val opponent = organization?.let(snapshot::captureOpponentName)
    val date = snapshot.games.firstOrNull { it.id == organization?.gameId }?.let {
        Instant.fromEpochMilliseconds(it.scheduledStartMs).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    }
    Box(
        Modifier.fillMaxWidth().requiredHeight(158.dp)
            .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)).background(Color.Black)
    ) {
        preview(first, Modifier.fillMaxSize())
        Row(
            Modifier.align(Alignment.TopStart).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PreviewChip(tag, tagColor)
            PreviewChip(opponent?.let { "vs $it" } ?: "vs Opponent", Color.Black.copy(alpha = 0.68f))
        }
        if (date != null) Text(
            date, style = MaterialTheme.typography.bodySmall, color = Color.White,
            maxLines = 1, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                .background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(5.dp))
                .padding(horizontal = 7.dp, vertical = 3.dp)
        )
        if (memories.size > 1) Text(
            "${memories.size} memories", color = Color.White, style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                .background(Color.Black.copy(alpha = 0.68f), RoundedCornerShape(5.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun PreviewChip(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(9.dp), color = color, contentColor = Color.White) {
        Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 150.dp).padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun ShareSectionHeader(icon: ImageVector, title: String, subtitle: String, expanded: Boolean?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = ShareAccent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                when (expanded) {
                    null -> Icons.Default.KeyboardArrowRight
                    true -> Icons.Default.KeyboardArrowUp
                    false -> Icons.Default.KeyboardArrowDown
                },
                contentDescription = when (expanded) {
                    null -> "Open $title"
                    true -> "Collapse $title"
                    false -> "Expand $title"
                }
            )
        }
    }
}

@Composable
private fun ShareHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun InitialsAvatar(name: String, size: Dp, background: Color, content: Color, single: Boolean = false) {
    Surface(Modifier.size(size), shape = CircleShape, color = background) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                if (single) name.trim().take(1).uppercase()
                else name.trim().split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString(""),
                fontWeight = FontWeight.Bold, color = content
            )
        }
    }
}
