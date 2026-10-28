package com.playerid.app.domain.team

import com.playerid.app.domain.memory.GuardianShareContact
import com.playerid.app.domain.memory.validate
import com.playerid.app.domain.memory.MemoryReel
import com.playerid.app.domain.memory.validateReelSelection
import com.playerid.app.platform.MediaReference
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.Required
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val CAPTURE_SEASON_CHILD_ID = "default-child"

class TeamStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)

interface TeamSnapshotStorage {
    suspend fun read(): String?
    suspend fun write(contents: String)
}

const val MAX_CAPTURE_NOTE_LENGTH = 280

@Serializable
data class TeamWorkspaceSnapshot(
    @Required val version: Int = 1,
    val teams: List<TeamProfile> = emptyList(),
    val players: List<PlayerProfile> = emptyList(),
    val subscriptions: List<TeamSubscription> = emptyList(),
    val children: List<ChildProfileRecord> = emptyList(),
    val seasons: List<SportSeasonProfile> = emptyList(),
    val games: List<GameScheduleProfile> = emptyList(),
    val memories: List<MemoryItemProfile> = emptyList(),
    val favoritePlayerIds: Set<String> = emptySet(),
    val assignedPlayerIds: Map<String, String> = emptyMap(),
    val onboardingCompleted: Boolean = false,
    val captureOrganizations: Map<String, CaptureOrganization> = emptyMap(),
    val cameraOrganization: CaptureOrganization? = null,
    val cameraJerseyLabel: String? = null,
    val captureMomentTags: Map<String, String> = emptyMap(),
    val customCaptureMomentTags: List<String> = emptyList(),
    val teamShareContacts: Map<String, List<GuardianShareContact>> = emptyMap(),
    val captureReels: Map<String, MemoryReel> = emptyMap(),
    val captureFavorites: Set<String> = emptySet(),
    val captureNotes: Map<String, String> = emptyMap()
) {
    val requiresOnboarding: Boolean
        get() = !onboardingCompleted && teams.isEmpty()
}

/**
 * File-backed repository for the local-only host. Android continues to use Room.
 * Publish mutations only after durable storage succeeds.
 */
class LocalTeamRepository private constructor(
    private val storage: TeamSnapshotStorage,
    snapshot: TeamWorkspaceSnapshot
) : TeamRosterRepository, TeamSubscriptionRepository, ScheduleStorageRepository {
    private val mutex = Mutex()
    private val mutableSnapshot = MutableStateFlow(snapshot)
    val snapshot: StateFlow<TeamWorkspaceSnapshot> = mutableSnapshot.asStateFlow()

    private suspend fun mutate(change: (TeamWorkspaceSnapshot) -> TeamWorkspaceSnapshot) {
        mutex.withLock {
            val next = change(snapshot.value)
            commit(next)
        }
    }

    // Stage multi-step shared services so partial imports are never persisted.
    suspend fun <T> transaction(block: suspend (LocalTeamRepository) -> T): T = mutex.withLock {
        val staged = LocalTeamRepository(object : TeamSnapshotStorage {
            override suspend fun read(): String? = null
            override suspend fun write(contents: String) = Unit
        }, snapshot.value)
        val result = block(staged)
        val next = staged.snapshot.value
        commit(next)
        result
    }

    private suspend fun commit(proposed: TeamWorkspaceSnapshot) = withContext(NonCancellable) {
        // Every save stores one spelling per opponent, so variants typed anywhere can't create duplicates.
        val next = proposed.withCanonicalOpponents()
        next.validateCaptureOrganizations()
        storage.write(json.encodeToString(next))
        mutableSnapshot.value = next
    }

    override fun observeActiveTeams(): Flow<List<TeamProfile>> = snapshot.map { state ->
        state.teams.filter { it.isActive && !it.isArchived }.sortedBy { it.name.lowercase() }
    }.distinctUntilChanged()

    override fun observePlayers(teamName: String): Flow<List<PlayerProfile>> = snapshot.map { state ->
        state.players.filter { it.teamName == teamName && it.isActive }
            .sortedWith(compareBy({ it.number.toIntOrNull() ?: Int.MAX_VALUE }, { it.number }, { it.name }))
    }.distinctUntilChanged()

    override suspend fun findTeam(teamName: String): TeamProfile? =
        snapshot.value.teams.firstOrNull { it.name == teamName }

    override suspend fun findPlayer(teamName: String, number: String): PlayerProfile? =
        snapshot.value.players.firstOrNull { it.teamName == teamName && it.number == number }

    override suspend fun saveTeam(team: TeamProfile) = mutate { state ->
        require(team.id.isNotBlank() && team.name.isNotBlank()) { "A team needs an ID and name." }
        require(state.teams.none { it.id != team.id && it.name.equals(team.name, ignoreCase = true) }) {
            "A team with this name already exists on this iPhone."
        }
        val existing = state.teams.firstOrNull { it.id == team.id }
        require(existing == null || existing.name == team.name) { "Renaming an existing team is not supported." }
        state.copy(teams = state.teams.filterNot { it.id == team.id } + team)
    }

    suspend fun renameTeam(teamId: String, newName: String) = mutate { state -> state.renamingTeam(teamId, newName) }

    override suspend fun savePlayer(player: PlayerProfile) = savePlayers(listOf(player))

    override suspend fun updatePlayer(player: PlayerProfile) = mutate { state ->
        require(state.players.any { it.id == player.id }) { "This player no longer exists." }
        state.withPlayers(listOf(player))
    }

    override suspend fun savePlayers(players: List<PlayerProfile>) = mutate { state ->
        state.withPlayers(players)
    }

    private fun TeamWorkspaceSnapshot.withPlayers(players: List<PlayerProfile>): TeamWorkspaceSnapshot {
        var updated = this.players
        players.forEach { player ->
            require(player.id.isNotBlank()) { "A player needs an ID." }
            require(teams.any { it.name == player.teamName }) { "The player's team no longer exists." }
            require(updated.none { it.id == player.id && it.teamName != player.teamName }) {
                "Moving an existing player to another team is not supported."
            }
            require(updated.none {
                it.id != player.id && it.teamName == player.teamName && it.number == player.number
            }) { "Jersey number ${player.number} is already used on this team." }
            updated = updated.filterNot { it.id == player.id } + player
        }
        return copy(players = updated)
    }

    override suspend fun deletePlayer(playerId: String) = mutate { state ->
        state.copy(
            players = state.players.filterNot { it.id == playerId },
            favoritePlayerIds = state.favoritePlayerIds - playerId,
            assignedPlayerIds = state.assignedPlayerIds.filterValues { it != playerId },
            captureOrganizations = state.captureOrganizations.mapValues { (_, organization) ->
                if (organization.playerId == playerId) organization.copy(playerId = null) else organization
            },
            cameraOrganization = state.cameraOrganization?.let {
                if (it.playerId == playerId) it.copy(playerId = null) else it
            },
            teamShareContacts = state.teamShareContacts.mapValues { (_, contacts) ->
                contacts.map { it.copy(linkedPlayerIds = it.linkedPlayerIds - playerId) }
                    .filter { it.isFavorite || it.linkedPlayerIds.isNotEmpty() }
            }
        )
    }

    suspend fun toggleFavorite(playerId: String) = mutate { state ->
        require(state.players.any { it.id == playerId }) { "This player no longer exists." }
        state.copy(favoritePlayerIds = if (playerId in state.favoritePlayerIds) {
            state.favoritePlayerIds - playerId
        } else {
            state.favoritePlayerIds + playerId
        })
    }

    suspend fun assignPlayer(teamName: String, playerId: String?) = mutate { state ->
        require(playerId == null || state.players.any { it.id == playerId && it.teamName == teamName }) {
            "Choose a player from this team's roster."
        }
        state.copy(assignedPlayerIds = if (playerId == null) {
            state.assignedPlayerIds - teamName
        } else {
            state.assignedPlayerIds + (teamName to playerId)
        })
    }

    suspend fun completeOnboarding() = mutate { it.copy(onboardingCompleted = true) }

    suspend fun setCameraOrganization(organization: CaptureOrganization?, jerseyLabel: String? = null) = mutate { state ->
        organization?.let { state.validateCaptureOrganization(it) }
        state.copy(cameraOrganization = organization, cameraJerseyLabel = jerseyLabel)
    }

    suspend fun selectCameraPlayer(teamName: String, playerId: String?) = mutate { state ->
        val organization = state.cameraOrganization
        require(organization != null && organization.teamName == teamName) {
            "Camera team changed. Reopen its roster and select the player again."
        }
        require(playerId == null || state.players.any {
            it.id == playerId && it.teamName == teamName && it.isActive
        }) { "Choose an active player from this team's roster." }
        state.copy(cameraOrganization = organization.copy(playerId = playerId))
    }
    /**
     * Returns [teamName]'s season for [year], creating an inactive "<year> Season" when the team has none,
     * so memories can be filed under seasons that have no imported schedule. Inactive keeps it from
     * replacing the team's current season for schedule import and Camera.
     */
    suspend fun ensureCaptureSeason(teamName: String, year: Int, nowMs: Long): String {
        require(year in 1900..9999) { "Enter a four-digit season year." }
        var seasonId = ""
        mutate { state ->
            require(state.teams.any { it.name == teamName }) { "Choose an existing team." }
            val existing = state.seasons.firstOrNull { it.teamName == teamName && state.captureSeasonYear(it) == year.toString() }
            if (existing != null) {
                seasonId = existing.id
                return@mutate state
            }
            val child = state.children.firstOrNull()
                ?: ChildProfileRecord(CAPTURE_SEASON_CHILD_ID, "My Child", nowMs, nowMs)
            val base = "season-${teamName.lowercase().replace(" ", "-")}-$year-memories"
            seasonId = generateSequence(1) { it + 1 }.map { if (it == 1) base else "$base-$it" }
                .first { id -> state.seasons.none { it.id == id } }
            state.copy(
                children = if (state.children.any { it.id == child.id }) state.children else state.children + child,
                seasons = state.seasons + SportSeasonProfile(
                    id = seasonId,
                    childId = child.id,
                    sportName = state.teams.first { it.name == teamName }.sport,
                    seasonLabel = "$year Season",
                    teamName = teamName,
                    createdAt = nowMs,
                    updatedAt = nowMs,
                    isActive = false
                )
            )
        }
        return seasonId
    }

    suspend fun organizeCapture(fileKey: String, organization: CaptureOrganization?) = mutate { state ->
        validateCaptureFileKey(fileKey)
        if (organization != null) state.validateCaptureOrganization(organization)
        state.copy(captureOrganizations = if (organization == null) {
            state.captureOrganizations - fileKey
        } else {
            state.captureOrganizations + (fileKey to organization)
        })
    }

    suspend fun tagCapture(fileKey: String, label: String, rememberCustom: Boolean = false) = mutate { state ->
        validateCaptureFileKey(fileKey)
        val tag = label.trim()
        require(tag.isNotEmpty()) { "Enter a moment tag." }
        state.copy(
            captureMomentTags = state.captureMomentTags + (fileKey to tag),
            customCaptureMomentTags = if (rememberCustom &&
                (defaultCaptureMomentTags + state.customCaptureMomentTags).none { it.equals(tag, ignoreCase = true) }
            ) state.customCaptureMomentTags + tag else state.customCaptureMomentTags
        )
    }

    suspend fun clearCaptureTag(fileKey: String) = mutate { state ->
        validateCaptureFileKey(fileKey)
        state.copy(captureMomentTags = state.captureMomentTags - fileKey)
    }

    suspend fun renameCustomCaptureTag(previous: String, label: String) = mutate { state ->
        val tag = label.trim()
        require(previous in state.customCaptureMomentTags) { "This custom tag no longer exists." }
        require(tag.isNotEmpty()) { "Enter a moment tag." }
        require((defaultCaptureMomentTags + (state.customCaptureMomentTags - previous))
            .none { it.equals(tag, ignoreCase = true) }) { "A tag with this name already exists." }
        state.copy(customCaptureMomentTags = state.customCaptureMomentTags.map { if (it == previous) tag else it })
    }

    suspend fun removeCustomCaptureTag(label: String) = mutate { state ->
        state.copy(customCaptureMomentTags = state.customCaptureMomentTags - label)
    }

    suspend fun forgetCapture(fileKey: String) = mutate { state ->
        validateCaptureFileKey(fileKey)
        state.copy(
            captureOrganizations = state.captureOrganizations - fileKey,
            captureMomentTags = state.captureMomentTags - fileKey,
            captureReels = state.captureReels - fileKey,
            captureFavorites = state.captureFavorites - fileKey,
            captureNotes = state.captureNotes - fileKey
        )
    }

    suspend fun setCaptureFavorite(fileKey: String, favorite: Boolean) = mutate { state ->
        validateCaptureFileKey(fileKey)
        state.copy(captureFavorites = if (favorite) state.captureFavorites + fileKey else state.captureFavorites - fileKey)
    }

    /** Saves the memory's "What happened" note; a blank note removes it. */
    suspend fun setCaptureNote(fileKey: String, note: String) = mutate { state ->
        validateCaptureFileKey(fileKey)
        val trimmed = note.trim()
        require(trimmed.length <= MAX_CAPTURE_NOTE_LENGTH) { "Keep the note under $MAX_CAPTURE_NOTE_LENGTH characters." }
        state.copy(captureNotes = if (trimmed.isEmpty()) state.captureNotes - fileKey
            else state.captureNotes + (fileKey to trimmed))
    }

    suspend fun copyCaptureDetails(sourceKey: String, destinationKey: String) = mutate { state ->
        validateCaptureFileKey(sourceKey)
        validateCaptureFileKey(destinationKey)
        require(sourceKey != destinationKey) { "Save the trimmed clip as a new memory." }
        val organization = state.captureOrganizations[sourceKey]
        val tag = state.captureMomentTags[sourceKey]
        state.copy(
            captureOrganizations = (state.captureOrganizations - destinationKey).let {
                if (organization == null) it else it + (destinationKey to organization)
            },
            captureMomentTags = (state.captureMomentTags - destinationKey).let {
                if (tag == null) it else it + (destinationKey to tag)
            },
            captureReels = (state.captureReels - destinationKey).let {
                val reel = state.captureReels[sourceKey]
                if (reel == null) it else it + (destinationKey to reel)
            },
            captureFavorites = if (sourceKey in state.captureFavorites) state.captureFavorites + destinationKey
                else state.captureFavorites - destinationKey,
            captureNotes = (state.captureNotes - destinationKey).let {
                val note = state.captureNotes[sourceKey]
                if (note == null) it else it + (destinationKey to note)
            }
        )
    }

    suspend fun saveCaptureReel(fileKey: String, name: String, sources: List<MediaReference>) = mutate { state ->
        validateCaptureFileKey(fileKey)
        require(com.playerid.app.platform.savedCaptureKind(fileKey) == com.playerid.app.platform.MediaKind.VIDEO) {
            "A reel must be saved as a video."
        }
        validateReelSelection(name, sources)
        val keys = sources.map(::captureFileKey)
        require(fileKey !in keys && keys.none { it in state.captureReels }) { "Choose source memories, not existing reels." }
        keys.forEach(::validateCaptureFileKey)
        state.copy(captureReels = state.captureReels + (fileKey to MemoryReel(name.trim(), keys)))
    }

    suspend fun saveShareContact(teamName: String, contact: GuardianShareContact) = mutate { state ->
        val normalized = contact.copy(displayName = contact.displayName.trim(),
            phoneNumber = contact.phoneNumber?.trim(), email = contact.email?.trim())
        normalized.validate()
        require(state.teams.any { it.name == teamName }) { "Choose an existing team." }
        require((contact.isFavorite || contact.linkedPlayerIds.isNotEmpty()) && contact.linkedPlayerIds.all { id ->
            state.players.any { it.id == id && it.teamName == teamName && it.isActive }
        }) { "Link this guardian to at least one active player on this team." }
        val existing = state.teamShareContacts[teamName].orEmpty()
        state.copy(teamShareContacts = state.teamShareContacts + (teamName to
            (existing.filterNot { it.key == normalized.key } + normalized)))
    }

    /** Adds [contact] as a point of contact for [playerId], keeping any players and favorite flag it already has. */
    suspend fun linkShareContact(teamName: String, playerId: String, contact: GuardianShareContact) {
        val existing = snapshot.value.teamShareContacts[teamName].orEmpty().firstOrNull { it.key == contact.key }
        saveShareContact(teamName, contact.copy(
            linkedPlayerIds = existing?.linkedPlayerIds.orEmpty() + contact.linkedPlayerIds + playerId,
            isFavorite = existing?.isFavorite == true
        ))
    }

    /** Stars [contact] for sharing, keeping the players it is already linked to. */
    suspend fun addFavoriteShareContact(teamName: String, contact: GuardianShareContact) {
        val existing = snapshot.value.teamShareContacts[teamName].orEmpty().firstOrNull { it.key == contact.key }
        saveShareContact(teamName, contact.copy(
            linkedPlayerIds = existing?.linkedPlayerIds.orEmpty() + contact.linkedPlayerIds,
            isFavorite = true
        ))
    }

    /** Removes [playerId]'s link. A contact left with no players and no star is deleted. */
    suspend fun unlinkShareContact(teamName: String, playerId: String, key: String) = mutate { state ->
        state.copy(teamShareContacts = state.teamShareContacts + (teamName to
            state.teamShareContacts[teamName].orEmpty().mapNotNull { contact ->
                if (contact.key != key) contact
                else contact.copy(linkedPlayerIds = contact.linkedPlayerIds - playerId).takeIf { it.isFavorite || it.linkedPlayerIds.isNotEmpty() }
            }))
    }

    /** Stars or unstars a saved contact. Unstarring a contact that isn't anyone's point of contact deletes it. */
    suspend fun setShareContactFavorite(teamName: String, key: String, favorite: Boolean) = mutate { state ->
        val contacts = state.teamShareContacts[teamName].orEmpty()
        require(contacts.any { it.key == key }) { "This contact was removed. Refresh and try again." }
        state.copy(teamShareContacts = state.teamShareContacts + (teamName to contacts.mapNotNull { contact ->
            if (contact.key != key) contact
            else contact.copy(isFavorite = favorite).takeIf { it.isFavorite || it.linkedPlayerIds.isNotEmpty() }
        }))
    }

    suspend fun removeShareContact(teamName: String, key: String) = mutate { state ->
        state.copy(teamShareContacts = state.teamShareContacts + (teamName to
            state.teamShareContacts[teamName].orEmpty().filterNot { it.key == key }))
    }

    override fun observeSubscribedTeams(userId: String): Flow<List<TeamProfile>> = snapshot.map { state ->
        val names = state.subscriptions.filter { it.userId == userId && it.isActive }.map { it.teamName }.toSet()
        state.teams.filter { it.name in names && it.isActive && !it.isArchived }.sortedBy { it.name.lowercase() }
    }.distinctUntilChanged()

    override suspend fun subscribe(subscription: TeamSubscription) = mutate { state ->
        require(state.teams.any { it.name == subscription.teamName }) { "This team no longer exists." }
        state.copy(subscriptions = state.subscriptions.filterNot {
            it.userId == subscription.userId && it.teamName == subscription.teamName
        } + subscription)
    }

    override suspend fun clear(userId: String) = mutate { state ->
        state.copy(subscriptions = state.subscriptions.filterNot { it.userId == userId })
    }

    override suspend fun unsubscribe(userId: String, teamName: String) = mutate { state ->
        state.copy(subscriptions = state.subscriptions.filterNot {
            it.userId == userId && it.teamName == teamName
        })
    }

    override fun observeActiveChildren(): Flow<List<ChildProfileRecord>> =
        snapshot.map { it.children.filter { child -> child.isActive } }.distinctUntilChanged()

    override fun observeSeasonsForChild(childId: String): Flow<List<SportSeasonProfile>> =
        snapshot.map { it.seasons.filter { season -> season.childId == childId && season.isActive } }
            .distinctUntilChanged()

    override fun observeGamesForSeason(seasonId: String): Flow<List<GameScheduleProfile>> =
        snapshot.map { it.games.filter { game -> game.sportSeasonId == seasonId }.sortedBy { game -> game.scheduledStartMs } }
            .distinctUntilChanged()

    override fun observeGamesForTeam(teamName: String): Flow<List<GameScheduleProfile>> = snapshot.map { state ->
        val seasons = state.seasons.filter { it.teamName == teamName && it.isActive }.map { it.id }.toSet()
        state.games.filter { it.sportSeasonId in seasons }.sortedBy { it.scheduledStartMs }
    }.distinctUntilChanged()

    override fun observeMemoriesForGame(gameId: String): Flow<List<MemoryItemProfile>> =
        snapshot.map { it.memories.filter { memory -> memory.gameScheduleId == gameId } }.distinctUntilChanged()

    override suspend fun findActiveSeasonForTeam(teamName: String): SportSeasonProfile? =
        snapshot.value.seasons.firstOrNull { it.teamName == teamName && it.isActive }

    override suspend fun findGamesSince(windowStartMs: Long): List<GameScheduleProfile> =
        snapshot.value.games.filter { it.scheduledStartMs >= windowStartMs }

    override suspend fun findMemoryByMediaIdentifier(identifier: String): MemoryItemProfile? =
        snapshot.value.memories.firstOrNull { it.media.identifier == identifier }

    override suspend fun findMemoryItems(ids: List<String>): List<MemoryItemProfile> =
        snapshot.value.memories.filter { it.id in ids }

    override suspend fun saveChild(child: ChildProfileRecord) = mutate { state ->
        state.copy(children = state.children.filterNot { it.id == child.id } + child)
    }

    override suspend fun saveSeason(season: SportSeasonProfile) = mutate { state ->
        state.copy(seasons = state.seasons.filterNot { it.id == season.id } + season)
    }

    override suspend fun saveGames(games: List<GameScheduleProfile>) = mutate { state ->
        require(games.all { game ->
            game.id.isNotBlank() && game.opponentName.isNotBlank() &&
                game.scheduledEndMs > game.scheduledStartMs &&
                state.seasons.any { it.id == game.sportSeasonId }
        }) { "Every game needs a season, opponent, and valid start and end times." }
        val ids = games.map { it.id }.toSet()
        state.copy(games = state.games.filterNot { it.id in ids } + games)
    }

    suspend fun deleteGame(gameId: String) = mutate { state ->
        state.copy(
            games = state.games.filterNot { it.id == gameId },
            memories = state.memories.map {
                if (it.gameScheduleId == gameId) it.copy(gameScheduleId = null) else it
            },
            captureOrganizations = state.captureOrganizations.mapValues { (_, organization) ->
                if (organization.gameId == gameId) organization.copy(gameId = null) else organization
            },
            cameraOrganization = state.cameraOrganization?.let {
                if (it.gameId == gameId) it.copy(gameId = null) else it
            }
        )
    }

    override suspend fun saveMemories(memories: List<MemoryItemProfile>) = mutate { state ->
        val ids = memories.map { it.id }.toSet()
        state.copy(memories = state.memories.filterNot { it.id in ids } + memories)
    }

    override suspend fun deleteMemories(ids: List<String>) = mutate { state ->
        state.copy(memories = state.memories.filterNot { it.id in ids })
    }

    companion object {
        private val json = Json { encodeDefaults = true }

        suspend fun open(storage: TeamSnapshotStorage): LocalTeamRepository {
            val contents = storage.read()
            val snapshot = if (contents == null) TeamWorkspaceSnapshot() else try {
                json.decodeFromString<TeamWorkspaceSnapshot>(contents)
            } catch (error: SerializationException) {
                throw TeamStorageException("PlayerID data could not be read. The existing file has not been changed.", error)
            }
            if (snapshot.version != 1) {
                throw TeamStorageException("This data requires another version of PlayerID. The existing file has not been changed.")
            }
            try {
                require(snapshot.teams.map { it.id }.distinct().size == snapshot.teams.size)
                require(snapshot.teams.map { it.name.lowercase() }.distinct().size == snapshot.teams.size)
                require(snapshot.teams.all { it.id.isNotBlank() && it.name.isNotBlank() })
                require(snapshot.players.map { it.id }.distinct().size == snapshot.players.size)
                require(snapshot.players.map { it.teamName to it.number }.distinct().size == snapshot.players.size)
                require(snapshot.players.all { player ->
                    player.id.isNotBlank() && snapshot.teams.any { it.name == player.teamName }
                })
                require(snapshot.games.map { it.id }.distinct().size == snapshot.games.size)
                require(snapshot.games.all { game ->
                    game.id.isNotBlank() && game.scheduledEndMs > game.scheduledStartMs &&
                        snapshot.seasons.any { it.id == game.sportSeasonId }
                })
                require(snapshot.seasons.all { season ->
                    snapshot.teams.any { it.name == season.teamName } &&
                        snapshot.children.any { it.id == season.childId }
                })
                snapshot.validateCaptureOrganizations()
            } catch (error: IllegalArgumentException) {
                throw TeamStorageException("PlayerID's saved data is inconsistent. The existing file has not been changed.", error)
            }
            // Older data may hold several spellings of one opponent; they are merged in memory and saved with the next change.
            return LocalTeamRepository(storage, snapshot.withCanonicalOpponents())
        }
    }
}

/** Team names are the link to players, seasons, and memories, so a rename rewrites every reference at once. */
internal fun TeamWorkspaceSnapshot.renamingTeam(teamId: String, newName: String): TeamWorkspaceSnapshot {
    val name = newName.trim()
    require(name.isNotBlank()) { "Enter a team name." }
    val team = teams.firstOrNull { it.id == teamId } ?: throw IllegalArgumentException("This team no longer exists.")
    val oldName = team.name
    if (oldName == name) return this
    require(teams.none { it.id != teamId && it.name.equals(name, ignoreCase = true) }) {
        "A team with this name already exists on this iPhone."
    }
    fun String.renamed() = if (this == oldName) name else this
    fun CaptureOrganization.renamed() = if (teamName == oldName) copy(teamName = name) else this
    return copy(
        teams = teams.map { if (it.id == teamId) it.copy(name = name) else it },
        players = players.map { if (it.teamName == oldName) it.copy(teamName = name) else it },
        subscriptions = subscriptions.map { if (it.teamName == oldName) it.copy(teamName = name) else it },
        seasons = seasons.map { if (it.teamName == oldName) it.copy(teamName = name) else it },
        assignedPlayerIds = assignedPlayerIds.mapKeys { it.key.renamed() },
        captureOrganizations = captureOrganizations.mapValues { it.value.renamed() },
        cameraOrganization = cameraOrganization?.renamed(),
        teamShareContacts = teamShareContacts.mapKeys { it.key.renamed() }
    )
}
