package com.playerid.app.domain.team

import com.playerid.app.domain.memory.*
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class GuardianSharingTest {
    private val guardian = GuardianShareContact("Parent", phoneNumber = "+1 (555) 123-4567",
        linkedPlayerIds = setOf("player"))
    private val video = MediaReference("file:///container/video.mov", MediaKind.VIDEO)
    private val photo = MediaReference("file:///container/photo.jpg", MediaKind.IMAGE)

    private suspend fun repository(storage: TestTeamStorage = TestTeamStorage()): LocalTeamRepository {
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        repository.saveTeam(testTeam("bears", "Bears"))
        repository.savePlayer(testPlayer())
        repository.savePlayer(testPlayer("second", "8"))
        repository.savePlayer(testPlayer("bear", "4", "Bears"))
        return repository
    }

    @Test
    fun guardianLinksSurviveRelaunchWithoutChangingMemoryOrganization() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        val organization = CaptureOrganization("Tigers", playerId = "second", opponentName = "Lions")
        repository.organizeCapture("video.mov", organization)
        repository.saveShareContact("Tigers", guardian)
        val restored = LocalTeamRepository.open(storage).snapshot.value
        assertEquals(listOf(guardian), restored.teamShareContacts["Tigers"])
        assertEquals(organization, restored.captureOrganizations["video.mov"])
        assertEquals(3, restored.players.size)
    }

    @Test
    fun normalizedPhoneAndEmailKeysUpdateRatherThanDuplicateRecipients() = runTest {
        val repository = repository()
        repository.saveShareContact("Tigers", guardian)
        val updated = guardian.copy(displayName = "  Parent Updated ", phoneNumber = "+15551234567",
            linkedPlayerIds = setOf("player", "second"))
        repository.saveShareContact("Tigers", updated)
        assertEquals(listOf(updated.copy(displayName = "Parent Updated")),
            repository.snapshot.value.teamShareContacts["Tigers"])
        val email = GuardianShareContact("Parent", email = "Parent@Example.com", linkedPlayerIds = setOf("player"))
        repository.saveShareContact("Tigers", email)
        repository.saveShareContact("Tigers", email.copy(email = " parent@example.com ", linkedPlayerIds = setOf("second")))
        val contacts = repository.snapshot.value.teamShareContacts["Tigers"].orEmpty()
        assertEquals(2, contacts.size)
        assertEquals(setOf("second"), contacts.single { it.email != null }.linkedPlayerIds)
    }

    @Test
    fun contactsAreScopedToTheirTeamAndLinksCanBeExplicitlyReplaced() = runTest {
        val repository = repository()
        repository.saveShareContact("Tigers", guardian)
        repository.saveShareContact("Bears", guardian.copy(linkedPlayerIds = setOf("bear")))
        repository.saveShareContact("Tigers", guardian.copy(linkedPlayerIds = setOf("second")))
        assertEquals(setOf("second"), repository.snapshot.value.teamShareContacts["Tigers"]!!.single().linkedPlayerIds)
        assertEquals(setOf("bear"), repository.snapshot.value.teamShareContacts["Bears"]!!.single().linkedPlayerIds)
    }

    @Test
    fun blankInvalidAndCrossTeamChoicesDoNotWrite() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        val before = storage.contents
        for (invalid in listOf(
            guardian.copy(displayName = " "),
            guardian.copy(phoneNumber = null),
            guardian.copy(phoneNumber = "abc"),
            guardian.copy(phoneNumber = null, email = "not-an-email"),
            guardian.copy(linkedPlayerIds = emptySet()),
            guardian.copy(linkedPlayerIds = setOf("bear")),
            guardian.copy(linkedPlayerIds = setOf("missing"))
        )) {
            assertFailsWith<IllegalArgumentException> { repository.saveShareContact("Tigers", invalid) }
        }
        assertFailsWith<IllegalArgumentException> { repository.saveShareContact("Missing", guardian) }
        assertEquals(before, storage.contents)
        assertTrue(repository.snapshot.value.teamShareContacts.isEmpty())
    }

    @Test
    fun inactivePlayersCannotBeNewlyLinked() = runTest {
        val repository = repository()
        repository.updatePlayer(testPlayer().copy(isActive = false))
        assertFailsWith<IllegalArgumentException> { repository.saveShareContact("Tigers", guardian) }
    }

    @Test
    fun deletingPlayerPrunesOnlyItsGuardianLinksAndLeavesContactsUsableAfterRelaunch() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.saveShareContact("Tigers", guardian.copy(linkedPlayerIds = setOf("player", "second")))
        repository.deletePlayer("player")
        val restored = LocalTeamRepository.open(storage)
        assertEquals(setOf("second"), restored.snapshot.value.teamShareContacts["Tigers"]!!.single().linkedPlayerIds)
        restored.deletePlayer("second")
        assertTrue(LocalTeamRepository.open(storage).snapshot.value.teamShareContacts["Tigers"].orEmpty().isEmpty())
    }

    @Test
    fun removingGuardianKeepsPlayersAndOtherTeamsAndMemories() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.saveShareContact("Tigers", guardian)
        repository.saveShareContact("Bears", guardian.copy(linkedPlayerIds = setOf("bear")))
        repository.tagCapture("video.mov", "Goal")
        repository.removeShareContact("Tigers", guardian.key)
        val restored = LocalTeamRepository.open(storage).snapshot.value
        assertTrue(restored.teamShareContacts["Tigers"].orEmpty().isEmpty())
        assertEquals(1, restored.teamShareContacts["Bears"]!!.size)
        assertEquals(3, restored.players.size)
        assertEquals("Goal", restored.captureMomentTags["video.mov"])
    }

    @Test
    fun failedContactWritesDoNotPublishOrDestroyExistingLinks() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.saveShareContact("Tigers", guardian)
        val before = storage.contents
        val state = repository.snapshot.value
        storage.failWrites = true
        assertFailsWith<TeamStorageException> {
            repository.saveShareContact("Tigers", guardian.copy(linkedPlayerIds = setOf("second")))
        }
        assertFailsWith<TeamStorageException> { repository.removeShareContact("Tigers", guardian.key) }
        assertEquals(before, storage.contents)
        assertEquals(state, repository.snapshot.value)
    }

    @Test
    fun oldSnapshotsLoadWithoutContactsOrWrites() = runTest {
        val storage = TestTeamStorage("""{"version":1,"onboardingCompleted":true}""")
        assertTrue(LocalTeamRepository.open(storage).snapshot.value.teamShareContacts.isEmpty())
        assertEquals(0, storage.writes)
    }

    @Test
    fun mixedTeamsAndUnassignedMediaRemainInTheOutgoingSelection() = runTest {
        val repository = repository()
        repository.organizeCapture("video.mov", CaptureOrganization("Tigers", opponentName = "Lions"))
        assertEquals(listOf("Tigers"), memoryShareTeamNames(listOf(video, photo), repository.snapshot.value))
        assertFalse(guardianMemoryShareMessage(listOf(video, photo), repository.snapshot.value).contains("Lions"))
        repository.organizeCapture("photo.jpg", CaptureOrganization("Bears", opponentName = "Wolves"))
        val memories = listOf(photo, video)
        assertEquals(listOf("Bears", "Tigers"), memoryShareTeamNames(memories, repository.snapshot.value))
        assertEquals(memories, selectedMemoriesForSharing(memories, memories.map { it.identifier }.toSet()))
        val message = guardianMemoryShareMessage(memories, repository.snapshot.value)
        assertTrue(message.contains("these memories"))
        assertFalse(message.contains("Lions"))
        assertFalse(message.contains("Wolves"))
        assertTrue(guardianMemoryShareMessage(listOf(video), repository.snapshot.value).contains("against Lions"))
    }

    @Test
    fun linkingAPlayerContactDoesNotMakeItAFavorite() = runTest {
        val repository = repository()
        val picked = guardian.copy(linkedPlayerIds = emptySet())
        repository.linkShareContact("Tigers", "player", picked)
        repository.linkShareContact("Tigers", "second", picked.copy(displayName = "Parent Updated"))
        val saved = repository.snapshot.value.teamShareContacts["Tigers"]!!.single()
        assertEquals(setOf("player", "second"), saved.linkedPlayerIds)
        assertEquals("Parent Updated", saved.displayName)
        assertFalse(saved.isFavorite)
        assertTrue(favoriteShareContacts(repository.snapshot.value, "Tigers").isEmpty())
    }

    @Test
    fun favoritesCanComeFromPhoneContactsWithoutAPlayerAndSurviveRelaunch() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        val coach = GuardianShareContact("Coach", email = "Coach@Example.com")
        repository.addFavoriteShareContact("Tigers", coach)
        val restored = LocalTeamRepository.open(storage).snapshot.value
        val favorite = favoriteShareContacts(restored, "Tigers").single()
        assertEquals("Coach", favorite.displayName)
        assertTrue(favorite.linkedPlayerIds.isEmpty())
        val recipients = memoryShareRecipients(restored, "Tigers", emptySet(), setOf(favorite.key), emptyList())
        assertEquals(listOf(favorite.key), recipients.map { it.key })
    }

    @Test
    fun starringAPointOfContactKeepsItsPlayersAndUnstarringKeepsThePointOfContact() = runTest {
        val repository = repository()
        repository.linkShareContact("Tigers", "player", guardian)
        repository.setShareContactFavorite("Tigers", guardian.key, true)
        repository.linkShareContact("Tigers", "second", guardian)
        val starred = repository.snapshot.value.teamShareContacts["Tigers"]!!.single()
        assertTrue(starred.isFavorite)
        assertEquals(setOf("player", "second"), starred.linkedPlayerIds)
        repository.setShareContactFavorite("Tigers", guardian.key, false)
        val plain = repository.snapshot.value.teamShareContacts["Tigers"]!!.single()
        assertFalse(plain.isFavorite)
        assertEquals(setOf("player", "second"), plain.linkedPlayerIds)
    }

    @Test
    fun removingTheLastLinkOrStarDeletesTheContactButFavoritesSurviveUnlinking() = runTest {
        val repository = repository()
        repository.linkShareContact("Tigers", "player", guardian)
        repository.unlinkShareContact("Tigers", "player", guardian.key)
        assertTrue(repository.snapshot.value.teamShareContacts["Tigers"].orEmpty().isEmpty())

        repository.linkShareContact("Tigers", "player", guardian)
        repository.setShareContactFavorite("Tigers", guardian.key, true)
        repository.unlinkShareContact("Tigers", "player", guardian.key)
        val favorite = repository.snapshot.value.teamShareContacts["Tigers"]!!.single()
        assertTrue(favorite.isFavorite && favorite.linkedPlayerIds.isEmpty())
        repository.setShareContactFavorite("Tigers", guardian.key, false)
        assertTrue(repository.snapshot.value.teamShareContacts["Tigers"].orEmpty().isEmpty())
    }

    @Test
    fun contactsWithoutPlayersMustBeFavorites() = runTest {
        val repository = repository()
        assertFailsWith<IllegalArgumentException> {
            repository.saveShareContact("Tigers", guardian.copy(linkedPlayerIds = emptySet()))
        }
    }

    @Test
    fun reelsUseTheirSourceClipsTeamSoMemoryFavoritesAreReelFavorites() = runTest {
        val repository = repository()
        val clips = listOf(MediaReference("file:///container/a.mov", MediaKind.VIDEO),
            MediaReference("file:///container/b.mov", MediaKind.VIDEO))
        repository.organizeCapture("a.mov", CaptureOrganization("Tigers", playerId = "player"))
        repository.organizeCapture("b.mov", CaptureOrganization("Tigers", playerId = "second"))
        repository.saveCaptureReel("reel.mp4", "Goals", clips)
        repository.addFavoriteShareContact("Tigers", GuardianShareContact("Coach", email = "coach@example.com"))
        val reel = listOf(MediaReference("file:///container/reel.mp4", MediaKind.VIDEO))
        val snapshot = repository.snapshot.value
        assertEquals(listOf("Tigers"), memoryShareTeamNames(reel, snapshot))
        assertEquals(favoriteShareContacts(snapshot, "Tigers"), favoriteShareContacts(snapshot, memoryShareTeamNames(reel, snapshot).single()))
        assertEquals(setOf("player", "second"), suggestedMemorySharePlayerIds(reel, snapshot, "Tigers").toSet())
    }
}
