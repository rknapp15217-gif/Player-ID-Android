package com.playerid.app.domain.team

import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import com.playerid.app.domain.camera.CameraCaptureException
import com.playerid.app.domain.camera.organizeSavedCapture
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CaptureOrganizationTest {
    private val season = SportSeasonProfile("season", "child", "Soccer", "2026 Season", "Tigers", 1, 1)
    private val game = GameScheduleProfile("game", "season", "Lions", "vs Lions", 100, 200, createdAt = 1, updatedAt = 1)
    private val photo = MediaReference("file:///old-container/media/photo.jpg", MediaKind.IMAGE)
    private val video = MediaReference("file:///old-container/media/video.mov", MediaKind.VIDEO)
    private val organization = CaptureOrganization("Tigers", "season", "game", "player")

    private suspend fun repository(storage: TestTeamStorage = TestTeamStorage()): LocalTeamRepository {
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        repository.saveTeam(testTeam("second", "Bears"))
        repository.savePlayer(testPlayer())
        repository.savePlayer(testPlayer("bear-player", "12", "Bears"))
        repository.saveChild(ChildProfileRecord("child", "My Child", 1, 1))
        repository.saveSeason(season)
        repository.saveSeason(season.copy(id = "bear-season", teamName = "Bears"))
        repository.saveGames(listOf(game, game.copy(id = "bear-game", sportSeasonId = "bear-season")))
        return repository
    }

    @Test
    fun newRecordingsPersistTheirRecordingYearUnlessASeasonWasChosen() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        val recorded = MediaReference("file:///media/1792584000000-clip.mov", MediaKind.VIDEO)
        repository.organizeSavedCapture(recorded, CaptureOrganization("Tigers", playerId = "player"))
        val restored = LocalTeamRepository.open(storage).snapshot.value
        val assignment = restored.captureOrganizations.getValue(captureFileKey(recorded))
        assertNotNull(assignment.seasonId)
        assertEquals("2026", restored.captureSeasonYearForKey(captureFileKey(recorded)))
        assertEquals("player", assignment.playerId)
        val older = season.copy(id = "older", seasonLabel = "2025 Season")
        repository.saveSeason(older)
        repository.organizeSavedCapture(recorded, assignment.copy(seasonId = older.id))
        assertEquals("2025", repository.snapshot.value.captureSeasonYearForKey(captureFileKey(recorded)))
    }

    @Test
    fun assignmentsSurviveRelaunchAndContainerPathChanges() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.organizeCapture(captureFileKey(photo), organization)
        val restored = LocalTeamRepository.open(storage)
        val relocated = photo.copy(identifier = "file:///new-container/media/photo.jpg")
        assertEquals(organization, restored.snapshot.value.captureOrganizations[captureFileKey(relocated)])
        assertEquals(listOf(relocated), filterCaptures(listOf(relocated), restored.snapshot.value, CaptureLibraryFilter(teamName = "Tigers")))
    }

    @Test
    fun importedVideoAssignmentsSurviveRelaunchWithoutChangingExistingCaptures() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.organizeCapture("photo.jpg", organization)
        for (extension in listOf("mp4", "m4v")) {
            repository.organizeCapture("import.$extension", organization)
        }
        val restored = LocalTeamRepository.open(storage)
        assertEquals(organization, restored.snapshot.value.captureOrganizations["photo.jpg"])
        for (extension in listOf("mp4", "m4v")) {
            val imported = MediaReference("file:///new-container/media/import.$extension", MediaKind.VIDEO)
            assertEquals(organization, restored.snapshot.value.captureOrganizations[captureFileKey(imported)])
            assertEquals(listOf(imported), searchCaptureLibrary(listOf(imported), restored.snapshot.value, "Tigers video"))
        }
    }

    @Test
    fun oldVersionOneSnapshotsLoadWithoutAssignments() = runTest {
        val storage = TestTeamStorage("""{"version":1,"onboardingCompleted":true}""")
        val repository = LocalTeamRepository.open(storage)
        assertTrue(repository.snapshot.value.captureOrganizations.isEmpty())
        assertNull(repository.snapshot.value.cameraOrganization)
        assertTrue(repository.snapshot.value.onboardingCompleted)
        assertEquals(0, storage.writes)
    }

    @Test
    fun invalidCrossTeamAndCrossSeasonChoicesAreRejected() = runTest {
        val repository = repository()
        listOf(
            organization.copy(teamName = "Missing"),
            organization.copy(seasonId = "bear-season"),
            organization.copy(gameId = "bear-game"),
            organization.copy(seasonId = null),
            organization.copy(playerId = "bear-player")
        ).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> { repository.organizeCapture("photo.jpg", invalid) }
        }
        assertTrue(repository.snapshot.value.captureOrganizations.isEmpty())
    }

    @Test
    fun invalidFileKeysAreRejectedWithoutChangingStorage() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        val before = storage.contents
        listOf("", "path/photo.jpg", "..\\photo.jpg", "metadata.json").forEach { invalid ->
            assertFailsWith<IllegalArgumentException> { repository.organizeCapture(invalid, organization) }
        }
        assertEquals(before, storage.contents)
    }

    @Test
    fun failedSaveDoesNotPublishAssignment() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.organizeCapture("photo.jpg", organization)
        val before = storage.contents
        storage.failWrites = true
        assertFailsWith<TeamStorageException> {
            repository.organizeCapture("photo.jpg", CaptureOrganization("Bears"))
        }
        assertEquals(organization, repository.snapshot.value.captureOrganizations["photo.jpg"])
        assertEquals(before, storage.contents)
    }

    @Test
    fun clearingDetailsDoesNotDeleteOtherAssignmentsOrTeamData() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.organizeCapture("photo.jpg", organization)
        repository.organizeCapture("video.mov", CaptureOrganization("Bears"))
        repository.organizeCapture("photo.jpg", null)
        val restored = LocalTeamRepository.open(storage)
        assertNull(restored.snapshot.value.captureOrganizations["photo.jpg"])
        assertEquals(CaptureOrganization("Bears"), restored.snapshot.value.captureOrganizations["video.mov"])
        assertEquals(2, restored.snapshot.value.teams.size)
        assertEquals(2, restored.snapshot.value.games.size)
    }

    @Test
    fun filtersIntersectAndUnassignedCapturesAreNeverLost() = runTest {
        val repository = repository()
        repository.organizeCapture("photo.jpg", organization)
        val snapshot = repository.snapshot.value
        val captures = listOf(photo, video)
        assertEquals(captures, filterCaptures(captures, snapshot, CaptureLibraryFilter()))
        assertEquals(listOf(video), filterCaptures(captures, snapshot, CaptureLibraryFilter(unassignedOnly = true)))
        listOf(
            CaptureLibraryFilter(teamName = "Tigers"),
            CaptureLibraryFilter(seasonId = "season"),
            CaptureLibraryFilter(opponent = "lions"),
            CaptureLibraryFilter(playerId = "player"),
            CaptureLibraryFilter("Tigers", "season", "Lions", "player")
        ).forEach { filter ->
            assertEquals(listOf(photo), filterCaptures(captures, snapshot, filter))
        }
        assertTrue(filterCaptures(captures, snapshot, CaptureLibraryFilter(teamName = "Bears")).isEmpty())
        assertTrue(filterCaptures(captures, snapshot, CaptureLibraryFilter("Tigers", unassignedOnly = true)).isEmpty())
    }

    @Test
    fun teamOnlyAssignmentsNeedNoScheduleOrPlayer() = runTest {
        val repository = repository()
        repository.organizeCapture("photo.jpg", CaptureOrganization("Tigers"))
        assertEquals(listOf(photo), filterCaptures(listOf(photo), repository.snapshot.value, CaptureLibraryFilter(teamName = "Tigers")))
        assertTrue(filterCaptures(listOf(photo), repository.snapshot.value, CaptureLibraryFilter(seasonId = "season")).isEmpty())
    }

    @Test
    fun deletingGameOrPlayerClearsOnlyThatAssignmentReference() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.organizeCapture("photo.jpg", organization)
        repository.deleteGame("game")
        repository.deletePlayer("player")
        assertEquals(organization.copy(gameId = null, playerId = null), LocalTeamRepository.open(storage).snapshot.value.captureOrganizations["photo.jpg"])
    }

    @Test
    fun corruptAssignmentDataIsNotSilentlyReplaced() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.organizeCapture("photo.jpg", organization)
        storage.contents = storage.contents!!.replace("\"gameId\":\"game\"", "\"gameId\":\"missing\"")
        val writes = storage.writes
        assertFailsWith<TeamStorageException> { LocalTeamRepository.open(storage) }
        assertEquals(writes, storage.writes)
    }

    @Test
    fun opponentFilterListsOnlyOpponentsWithSavedMemoriesForTheSelection() = runTest {
        val repository = repository()
        repository.saveGames(listOf(game.copy(id = "game-2", opponentName = "Hawks", scheduledStartMs = 300, scheduledEndMs = 400)))
        repository.organizeCapture("photo.jpg", organization)
        repository.organizeCapture("video.mov", CaptureOrganization("Tigers", "season", opponentName = "Eagles"))
        repository.organizeCapture("deleted.jpg", CaptureOrganization("Tigers", opponentName = "Ghosts"))
        repository.organizeCapture("bear.jpg", CaptureOrganization("Bears", "bear-season", "bear-game", "bear-player"))
        val snapshot = repository.snapshot.value
        val saved = setOf("photo.jpg", "video.mov", "bear.jpg")

        assertEquals(listOf("Eagles", "Lions"), captureFilterOpponents(snapshot, CaptureLibraryFilter("Tigers"), saved))
        assertEquals(listOf("Lions"), captureFilterOpponents(snapshot, CaptureLibraryFilter("Tigers", playerId = "player"), saved))
        assertEquals(listOf("Lions"), captureFilterOpponents(snapshot, CaptureLibraryFilter("Bears"), saved))
        assertEquals(listOf("Eagles", "Lions"), captureFilterOpponents(snapshot, CaptureLibraryFilter(), saved))
    }

    @Test
    fun playerFilterListsOnlyMyPlayers() = runTest {
        val repository = repository()
        repository.savePlayer(testPlayer("teammate", "9"))
        assertEquals(emptyList(), captureFilterPlayers(repository.snapshot.value, null))
        repository.assignPlayer("Tigers", "player")
        repository.assignPlayer("Bears", "bear-player")
        val snapshot = repository.snapshot.value
        assertEquals(setOf("player", "bear-player"), captureFilterPlayers(snapshot, null).map { it.id }.toSet())
        assertEquals(listOf("player"), captureFilterPlayers(snapshot, "Tigers").map { it.id })
        assertNull(normalizeCaptureFilter(CaptureLibraryFilter(playerId = "teammate"), snapshot).playerId)
        assertEquals("player", normalizeCaptureFilter(CaptureLibraryFilter(playerId = "player"), snapshot).playerId)
    }

    @Test
    fun opponentSpellingsAreNormalized() {
        assertEquals("Pine-Richland", displayOpponentName("PINE-RICHLAND"))
        assertEquals("Mt. Lebanon", displayOpponentName("MT.  LEBANON "))
        assertEquals("St. Edward (OH)", displayOpponentName("St. Edward (OH)"))
        assertEquals("CAN", displayOpponentName("CAN"))
        val known = listOf("Pine-Richland", "Upper St. Clair", "University School")
        assertEquals("Pine-Richland", canonicalOpponentName("pine richland", known))
        assertEquals("Pine-Richland", canonicalOpponentName("PR", known))
        assertEquals("Upper St. Clair", canonicalOpponentName("usc", known))
        assertEquals("Hawks", canonicalOpponentName(" Hawks ", known))
        // Ambiguous initials stay as typed.
        assertEquals("PSC", canonicalOpponentName("PSC", listOf("Peters State College", "Pine Sun City")))
        assertEquals(listOf("Pine-Richland"), matchingOpponentOptions(known, "pr"))
    }

    @Test
    fun storedOpponentVariantsMergeIntoTheScheduleSpelling() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.saveGames(listOf(game.copy(id = "game-pr", opponentName = "PINE-RICHLAND", scheduledStartMs = 300, scheduledEndMs = 400)))
        repository.organizeCapture("a.mov", CaptureOrganization("Tigers", opponentName = "Pine Richland"))
        repository.organizeCapture("b.mov", CaptureOrganization("Tigers", opponentName = "PR"))
        repository.organizeCapture("c.mov", CaptureOrganization("Bears", opponentName = "pine richland"))
        repository.organizeCapture("d.mov", CaptureOrganization("Bears", opponentName = "Pine Richland"))
        val snapshot = LocalTeamRepository.open(storage).snapshot.value
        assertEquals("Pine-Richland", snapshot.games.single { it.id == "game-pr" }.opponentName)
        assertEquals("Pine-Richland", snapshot.captureOrganizations["a.mov"]?.opponentName)
        assertEquals("Pine-Richland", snapshot.captureOrganizations["b.mov"]?.opponentName)
        // A team without that game merges to its most-used spelling.
        assertEquals("Pine Richland", snapshot.captureOrganizations["c.mov"]?.opponentName)
        assertEquals(listOf("Lions", "Pine-Richland"), captureOpponentOptions(snapshot, "Tigers", null))
        assertEquals(snapshot, snapshot.withCanonicalOpponents())
    }

    @Test
    fun cameraPlayerFollowsSelectedTeamsMyPlayer() = runTest {
        val repository = repository()
        repository.setCameraOrganization(CaptureOrganization("Tigers", playerId = null))
        assertNull(repository.snapshot.value.linkedCameraOrganization()?.playerId)
        repository.assignPlayer("Tigers", "player")
        assertEquals("player", repository.snapshot.value.linkedCameraOrganization()?.playerId)
        repository.setCameraOrganization(CaptureOrganization("Bears", playerId = null))
        assertNull(repository.snapshot.value.linkedCameraOrganization()?.playerId)
        repository.assignPlayer("Bears", "bear-player")
        assertEquals("bear-player", repository.snapshot.value.linkedCameraOrganization()?.playerId)
        repository.setCameraOrganization(null)
        assertNull(repository.snapshot.value.linkedCameraOrganization())
    }

    @Test
    fun opponentEditorOffersScheduleAndSavedOpponentsAsTheUserTypes() = runTest {
        val repository = repository()
        repository.saveGames(listOf(
            game.copy(id = "game-2", opponentName = "Hawks", scheduledStartMs = 300, scheduledEndMs = 400),
            game.copy(id = "game-3", opponentName = "Blackhawks", scheduledStartMs = 500, scheduledEndMs = 600)
        ))
        repository.organizeCapture("video.mov", CaptureOrganization("Tigers", opponentName = "Eagles"))
        val snapshot = repository.snapshot.value
        val options = captureOpponentOptions(snapshot, "Tigers", null)
        assertEquals(listOf("Blackhawks", "Eagles", "Hawks", "Lions"), options)
        assertEquals(listOf("Lions"), captureOpponentOptions(snapshot, "Tigers", "season").filter { it == "Lions" })
        assertEquals(emptyList(), captureOpponentOptions(snapshot, "Missing", null))
        assertEquals(listOf("Hawks", "Blackhawks"), matchingOpponentOptions(options, "haw"))
        assertEquals(emptyList(), matchingOpponentOptions(options, "lions"))
        assertEquals(options.take(2), matchingOpponentOptions(options, " ", limit = 2))
        assertEquals(emptyList(), matchingOpponentOptions(options, "zzz"))
        assertEquals(listOf("Blackhawks", "Eagles", "Lions"), opponentSuggestions(options, "Hawks", filtering = false))
        assertEquals(listOf("Hawks", "Blackhawks"), opponentSuggestions(options, "haw", filtering = true))
        assertEquals(options, opponentSuggestions(options, "", filtering = true))
    }

    @Test
    fun seasonFilterUsesYearsAndPlayerFilterUsesNames() {
        val seasons = listOf(
            SportSeasonProfile("s1", "c", "Hockey", "2026 Season", "Tigers", 0, 0),
            SportSeasonProfile("s2", "c", "Hockey", "Spring 2026", "Bears", 0, 0),
            SportSeasonProfile("s3", "c", "Hockey", "2025 Season", "Tigers", 0, 0)
        )
        val snapshot = TeamWorkspaceSnapshot(seasons = seasons)
        assertEquals("2026", snapshot.captureSeasonYear(seasons[0]))
        assertEquals("2026", snapshot.captureSeasonYear(seasons[1]))
        assertTrue(snapshot.matchesSeasonYear("s2", "s1"))
        assertFalse(snapshot.matchesSeasonYear("s3", "s1"))
        assertFalse(snapshot.matchesSeasonYear(null, "s1"))
        assertTrue(snapshot.matchesSeasonYear(null, null))
        assertFalse(snapshot.matchesPlayerName("someone", "missing"))
    }

    @Test
    fun importNeedsTeamOpponentAndAnyAvailablePlayerAndSeason() = runTest {
        val repository = repository()
        val draft = com.playerid.app.ui.memory.CaptureDetailsDraft("a.mov", null, "Tigers", null, null, null, "", null)
        assertFalse(draft.isCompleteForImport(repository.snapshot.value))
        draft.opponent = "Lions"
        // No My Player yet, so only the season is still needed.
        assertFalse(draft.isCompleteForImport(repository.snapshot.value))
        draft.season = "season"
        assertTrue(draft.isCompleteForImport(repository.snapshot.value))
        val withMyPlayer = repository.snapshot.value.copy(assignedPlayerIds = mapOf("Tigers" to "player"))
        assertFalse(draft.isCompleteForImport(withMyPlayer))
        draft.player = "player"
        assertTrue(draft.isCompleteForImport(withMyPlayer))
        draft.save(repository, "b.mov")
        assertEquals(CaptureOrganization("Tigers", "season", null, "player", "Lions"),
            repository.snapshot.value.captureOrganizations["b.mov"])
    }

    @Test
    fun choosingASeasonYearCreatesAnInactiveSeasonOnceAndReusesMatchingYears() = runTest {
        val repository = repository()
        val created = repository.ensureCaptureSeason("Tigers", 2024, 5)
        val season2024 = repository.snapshot.value.seasons.single { it.id == created }
        assertEquals("2024 Season", season2024.seasonLabel)
        assertEquals("Tigers", season2024.teamName)
        assertFalse(season2024.isActive)
        assertEquals("season", repository.findActiveSeasonForTeam("Tigers")?.id)
        assertEquals(created, repository.ensureCaptureSeason("Tigers", 2024, 6))
        assertEquals("season", repository.ensureCaptureSeason("Tigers", 2026, 7))
        assertNotEquals(created, repository.ensureCaptureSeason("Bears", 2024, 8))
        repository.organizeCapture("old.mov", CaptureOrganization("Tigers", created, opponentName = "Lions"))
        assertEquals(created, repository.snapshot.value.captureOrganizations["old.mov"]?.seasonId)
        assertFailsWith<IllegalArgumentException> { repository.ensureCaptureSeason("Missing", 2024, 9) }
    }

    @Test
    fun clearingATagKeepsOtherDetails() = runTest {
        val repository = repository()
        repository.organizeCapture("photo.jpg", organization)
        repository.tagCapture("photo.jpg", "Hat Trick", rememberCustom = true)
        repository.clearCaptureTag("photo.jpg")
        val snapshot = repository.snapshot.value
        assertNull(snapshot.captureMomentTags["photo.jpg"])
        assertEquals(organization, snapshot.captureOrganizations["photo.jpg"])
        assertEquals(listOf("Hat Trick"), snapshot.customCaptureMomentTags)
    }

    @Test
    fun filtersResetInvalidSelectionsAfterTeamOrScheduleChanges() = runTest {
        val snapshot = repository().snapshot.value
        val filter = CaptureLibraryFilter("Bears", "season", "Lions", "player")
        assertEquals(CaptureLibraryFilter("Bears", opponent = "Lions"), normalizeCaptureFilter(filter, snapshot))
        assertEquals(CaptureLibraryFilter(), normalizeCaptureFilter(
            CaptureLibraryFilter("Missing", "Missing", "Missing", "Missing"), snapshot
        ))
    }

    @Test
    fun repeatedOpponentsAcrossGamesAndTeamsMatchWithoutCaseSensitivity() = runTest {
        val repository = repository()
        repository.saveGames(listOf(
            game.copy(id = "rematch", opponentName = "LIONS"),
            game.copy(id = "bear-game", sportSeasonId = "bear-season", opponentName = "lions")
        ))
        val rematch = photo.copy(identifier = "file:///media/rematch.jpg")
        repository.organizeCapture("photo.jpg", organization)
        repository.organizeCapture("rematch.jpg", organization.copy(gameId = "rematch"))
        repository.organizeCapture("video.mov", CaptureOrganization("Bears", "bear-season", "bear-game"))
        val captures = listOf(photo, rematch, video)
        assertEquals(captures, filterCaptures(captures, repository.snapshot.value, CaptureLibraryFilter(opponent = "Lions")))
        assertEquals(listOf(photo, rematch), filterCaptures(
            captures, repository.snapshot.value, CaptureLibraryFilter(teamName = "Tigers", opponent = "Lions")
        ))
    }

    @Test
    fun scheduleUpdatesCannotPersistBrokenAssignmentReferences() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.organizeCapture("photo.jpg", organization)
        val before = storage.contents
        assertFailsWith<IllegalArgumentException> {
            repository.saveGames(listOf(game.copy(sportSeasonId = "bear-season")))
        }
        assertFailsWith<IllegalArgumentException> {
            repository.saveSeason(season.copy(teamName = "Bears"))
        }
        assertEquals(before, storage.contents)
        assertEquals(organization, LocalTeamRepository.open(storage).snapshot.value.captureOrganizations["photo.jpg"])
    }

    @Test
    fun cameraSetupPersistsWithoutChangingExistingCaptures() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.organizeCapture("photo.jpg", CaptureOrganization("Bears"))
        repository.setCameraOrganization(organization, "Away")
        val restored = LocalTeamRepository.open(storage).snapshot.value
        assertEquals(organization, restored.cameraOrganization)
        assertEquals("Away", restored.cameraJerseyLabel)
        assertEquals(CaptureOrganization("Bears"), restored.captureOrganizations["photo.jpg"])
        repository.setCameraOrganization(null)
        assertNull(LocalTeamRepository.open(storage).snapshot.value.cameraOrganization)
    }

    @Test
    fun unscheduledOpponentPersistsAndFiltersWithoutAGame() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        val unscheduled = CaptureOrganization("Tigers", opponentName = "Falcons")
        repository.setCameraOrganization(unscheduled)
        repository.organizeCapture("photo.jpg", unscheduled)
        val restored = LocalTeamRepository.open(storage).snapshot.value
        val filter = CaptureLibraryFilter(teamName = "Tigers", opponent = "falcons")
        assertEquals(unscheduled, restored.cameraOrganization)
        assertEquals(filter, normalizeCaptureFilter(filter, restored))
        assertEquals(listOf(photo), filterCaptures(listOf(photo, video), restored, filter))
    }

    @Test
    fun cameraSetupDeletionCleanupAndFailedWritesAreSafe() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.setCameraOrganization(organization)
        repository.deleteGame("game")
        repository.deletePlayer("player")
        val cleaned = organization.copy(gameId = null, playerId = null)
        assertEquals(cleaned, LocalTeamRepository.open(storage).snapshot.value.cameraOrganization)
        val before = storage.contents
        storage.failWrites = true
        assertFailsWith<TeamStorageException> { repository.setCameraOrganization(CaptureOrganization("Bears")) }
        assertEquals(cleaned, repository.snapshot.value.cameraOrganization)
        assertEquals(before, storage.contents)
    }

    @Test
    fun savedMediaAssignmentFailuresExplicitlyPreserveCaptureRecovery() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.organizeSavedCapture(photo, organization)
        assertEquals(organization, LocalTeamRepository.open(storage).snapshot.value.captureOrganizations["photo.jpg"])
        storage.failWrites = true
        val error = assertFailsWith<CameraCaptureException> { repository.organizeSavedCapture(video, organization) }
        assertTrue(error.message!!.contains("capture is saved"))
        assertTrue(error.message!!.contains("Edit details in Memories"))
        assertNull(repository.snapshot.value.captureOrganizations["video.mov"])
        storage.failWrites = false
        repository.deletePlayer("player")
        assertFailsWith<CameraCaptureException> { repository.organizeSavedCapture(video, organization) }
        val before = storage.writes
        repository.organizeSavedCapture(video, null)
        assertEquals(before, storage.writes)
    }

    @Test
    fun invalidCameraJerseyAndOpponentAreRejectedWithoutPublishing() = runTest {
        val repository = repository()
        assertFailsWith<IllegalArgumentException> { repository.setCameraOrganization(organization, "Invalid") }
        assertFailsWith<IllegalArgumentException> { repository.setCameraOrganization(organization.copy(opponentName = " ")) }
        assertNull(repository.snapshot.value.cameraOrganization)
    }

    @Test
    fun renamedScheduledOpponentsDoNotLeaveStaleFilters() = runTest {
        val repository = repository()
        repository.organizeCapture("photo.jpg", organization.copy(opponentName = "Old name"))
        repository.saveGames(listOf(game.copy(opponentName = "New name")))
        val snapshot = repository.snapshot.value
        assertEquals("New name", snapshot.captureOpponentName(snapshot.captureOrganizations.getValue("photo.jpg")))
        assertNull(normalizeCaptureFilter(CaptureLibraryFilter(opponent = "Old name"), snapshot).opponent)
        assertEquals(listOf(photo), filterCaptures(listOf(photo), snapshot, CaptureLibraryFilter(opponent = "New name")))
    }

    @Test
    fun snapshotsFromBeforeCameraSetupRetainExistingAssignments() = runTest {
        val storage = TestTeamStorage()
        val repository = repository(storage)
        repository.organizeCapture("photo.jpg", organization)
        storage.contents = storage.contents!!
            .replace(",\"opponentName\":null", "")
            .replace(",\"cameraOrganization\":null", "")
            .replace(",\"cameraJerseyLabel\":null", "")
        val before = storage.contents
        val restored = LocalTeamRepository.open(storage).snapshot.value
        assertEquals(organization, restored.captureOrganizations["photo.jpg"])
        assertNull(restored.cameraOrganization)
        assertNull(restored.cameraJerseyLabel)
        assertEquals(before, storage.contents)
    }

    @Test
    fun playerMemoryCountsIgnoreReelsOtherTeamsAndUnassignedCaptures() {
        val snapshot = TeamWorkspaceSnapshot(
            captureOrganizations = mapOf(
                "a.mov" to CaptureOrganization("Tigers", playerId = "p1"),
                "b.mov" to CaptureOrganization("Tigers", playerId = "p1"),
                "c.jpg" to CaptureOrganization("Tigers", playerId = "p2"),
                "d.mov" to CaptureOrganization("Tigers"),
                "e.mov" to CaptureOrganization("Bears", playerId = "p1"),
                "reel.mov" to CaptureOrganization("Tigers", playerId = "p1")
            ),
            captureReels = mapOf("reel.mov" to com.playerid.app.domain.memory.MemoryReel("Reel", listOf("a.mov", "b.mov")))
        )
        assertEquals(mapOf("p1" to 2, "p2" to 1), playerMemoryCounts(snapshot, "Tigers"))
    }
}
