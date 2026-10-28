package com.playerid.app.domain.team

import com.playerid.app.roster.RosterCandidate
import com.playerid.app.roster.parseRosterText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TeamWorkspaceTest {
    private var nextId = 0
    private fun workspace(repository: LocalTeamRepository) =
        TeamWorkspace(repository, newId = { "id-${nextId++}" }, now = { 1_790_000_000_000L })

    @Test
    fun onboardingAppearsOnlyForNewUncompletedWorkspaces() {
        assertTrue(TeamWorkspaceSnapshot().requiresOnboarding)
        assertEquals(false, TeamWorkspaceSnapshot(teams = listOf(testTeam())).requiresOnboarding)
        assertEquals(false, TeamWorkspaceSnapshot(onboardingCompleted = true).requiresOnboarding)
    }

    @Test
    fun onboardingCompletionPersistsWithoutChangingTeamData() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        repository.savePlayer(testPlayer())
        val before = repository.snapshot.value
        repository.completeOnboarding()
        assertEquals(before.copy(onboardingCompleted = true), LocalTeamRepository.open(storage).snapshot.value)
    }

    @Test
    fun oldWorkspaceWithoutCompletionFieldLoadsWithoutLosingData() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        storage.contents = storage.contents!!.replace(",\"onboardingCompleted\":false", "")
        assertEquals(repository.snapshot.value, LocalTeamRepository.open(storage).snapshot.value)
    }

    @Test
    fun failedOnboardingCompletionRemainsIncomplete() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        storage.failWrites = true
        assertFailsWith<TeamStorageException> { repository.completeOnboarding() }
        assertEquals(false, repository.snapshot.value.onboardingCompleted)
    }

    @Test
    fun onboardingJoinReplacesSubscriptionsAndAssignsPlayerInOneCommit() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        repository.saveTeam(testTeam(id = "other", name = "Lions"))
        repository.savePlayer(testPlayer())
        repository.subscribe(TeamSubscription(LOCAL_TEAM_USER, "Lions", 1))
        val beforeWrites = storage.writes
        workspace(repository).joinFromOnboarding(testTeam(), testPlayer())
        assertEquals(beforeWrites + 1, storage.writes)
        val restored = LocalTeamRepository.open(storage)
        assertEquals(listOf("Tigers"), restored.observeSubscribedTeams(LOCAL_TEAM_USER).first().map { it.name })
        assertEquals("player", restored.snapshot.value.assignedPlayerIds["Tigers"])
    }

    @Test
    fun failedOnboardingJoinDoesNotChangeSubscriptionOrAssignment() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        repository.savePlayer(testPlayer())
        val before = repository.snapshot.value
        storage.failWrites = true
        assertFailsWith<TeamStorageException> { workspace(repository).joinFromOnboarding(testTeam(), testPlayer()) }
        assertEquals(before, repository.snapshot.value)
        assertEquals(before, LocalTeamRepository.open(storage).snapshot.value)
    }

    @Test
    fun createAndEditTeamPersistWithSubscriptionInSingleCommit() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        val workspace = workspace(repository)
        val submission = TeamCreationSubmission(" Tigers ", "Soccer", "#112233", "#FFFFFF")
        val team = workspace.createTeam(submission)
        assertEquals("Tigers", team.name)
        assertEquals(1, storage.writes)
        assertEquals(listOf(team), repository.observeSubscribedTeams(LOCAL_TEAM_USER).first())
        workspace.updateTeam(team, submission.copy(teamName = "Tigers", sport = "Hockey"))
        assertEquals("Hockey", LocalTeamRepository.open(storage).findTeam("Tigers")?.sport)
    }

    @Test
    fun renamingTeamMovesEveryLinkedRecordAndRejectsDuplicates() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        val workspace = workspace(repository)
        val submission = TeamCreationSubmission("Tigers", "Soccer", "#112233", "#FFFFFF")
        val team = workspace.createTeam(submission)
        workspace.createTeam(submission.copy(teamName = "Lions"))
        val player = PlayerProfile("p1", "7", "Alex Smith", "", "Tigers", "", createdAt = 0, updatedAt = 0)
        repository.savePlayer(player)
        repository.assignPlayer("Tigers", "p1")
        repository.organizeCapture("clip.mov", CaptureOrganization(teamName = "Tigers", playerId = "p1"))
        repository.setCameraOrganization(CaptureOrganization(teamName = "Tigers"))

        assertFailsWith<IllegalArgumentException> { workspace.updateTeam(team, submission.copy(teamName = "lions")) }
        val renamed = workspace.updateTeam(team, submission.copy(teamName = " Tigers U12 ", sport = "Hockey"))

        val saved = LocalTeamRepository.open(storage).snapshot.value
        assertEquals("Tigers U12", renamed.name)
        assertEquals(listOf("Lions", "Tigers U12"), saved.teams.map { it.name }.sorted())
        assertEquals("Hockey", saved.teams.first { it.id == team.id }.sport)
        assertEquals("Tigers U12", saved.players.single().teamName)
        assertTrue(saved.subscriptions.any { it.teamName == "Tigers U12" })
        assertTrue(saved.subscriptions.none { it.teamName == "Tigers" })
        assertEquals(mapOf("Tigers U12" to "p1"), saved.assignedPlayerIds)
        assertEquals("Tigers U12", saved.captureOrganizations.getValue("clip.mov").teamName)
        assertEquals("Tigers U12", saved.cameraOrganization?.teamName)
    }

    @Test
    fun failedCreateDoesNotLeaveUnsubscribedTeam() = runTest {
        val storage = TestTeamStorage().apply { failWrites = true }
        val repository = LocalTeamRepository.open(storage)
        assertFailsWith<TeamStorageException> {
            workspace(repository).createTeam(TeamCreationSubmission("Tigers", "Soccer", "#112233", "#FFFFFF"))
        }
        assertTrue(repository.snapshot.value.teams.isEmpty())
    }

    @Test
    fun addEditDeletePlayerUsesSharedServiceAndPersistsAcrossReopen() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        val workspace = workspace(repository)
        val form = PlayerFormSubmission(" Alex Smith ", " 7 ", " Goalie ", "Tigers", "Junior")
        workspace.savePlayer(form)
        val player = repository.snapshot.value.players.single()
        assertEquals("Alex Smith", player.name)
        assertEquals(LOCAL_TEAM_USER, player.addedBy)
        workspace.savePlayer(form.copy(name = "Alex Updated"), player)
        assertEquals(player.id, repository.snapshot.value.players.single().id)
        assertEquals("Alex Updated", LocalTeamRepository.open(storage).snapshot.value.players.single().name)
        workspace.deletePlayer(player.id)
        assertTrue(LocalTeamRepository.open(storage).snapshot.value.players.isEmpty())
    }

    @Test
    fun rosterImportUpdatesExistingNumberAndPreservesIdentity() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        repository.savePlayer(testPlayer())
        val workspace = workspace(repository)
        val candidates = parseRosterText(listOf("7 Alex Updated", "12 Jordan Lee")).candidates
        val beforeWrites = storage.writes
        val result = workspace.importRoster("Tigers", candidates)
        assertEquals(RosterImportSummary(1, 1), result)
        assertEquals(beforeWrites + 1, storage.writes)
        assertEquals("player", repository.findPlayer("Tigers", "7")?.id)
        assertEquals("Goalie", repository.findPlayer("Tigers", "7")?.position)
        assertEquals(2, LocalTeamRepository.open(storage).snapshot.value.players.size)
    }

    @Test
    fun rosterImportRejectsDuplicateNumbersInsteadOfSilentlyOverwriting() = runTest {
        val repository = LocalTeamRepository.open(TestTeamStorage())
        repository.saveTeam(testTeam())
        val candidates = listOf(
            RosterCandidate("Alex Smith", "7", "Goalie", null, null),
            RosterCandidate("Jordan Lee", "7", "Forward", null, null)
        )
        assertFailsWith<IllegalArgumentException> { workspace(repository).importRoster("Tigers", candidates) }
        assertTrue(repository.snapshot.value.players.isEmpty())
    }

    @Test
    fun scheduleImportPersistsHierarchyAndSkipsRepeatGames() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        repository.saveTeam(testTeam(id = "other", name = "Lions"))
        val workspace = workspace(repository)
        val entries = parseScheduleText(
            "date,start,end,opponent,location\n2026-10-20,18:00,19:30,Bears,Main Field",
            "America/New_York"
        )
        assertEquals(1, workspace.importSchedule(testTeam(), entries))
        assertEquals(0, workspace.importSchedule(testTeam(), entries))
        val restored = LocalTeamRepository.open(storage)
        val games = restored.observeGamesForTeam("Tigers").first()
        assertEquals(1, games.size)
        assertEquals("Bears", games.single().opponentName)
        assertTrue(restored.observeGamesForTeam("Lions").first().isEmpty())
        assertEquals(1, restored.snapshot.value.children.size)
        assertEquals(1, restored.snapshot.value.seasons.size)
    }

    @Test
    fun invalidScheduleDoesNotPartiallyCreateHierarchy() = runTest {
        val repository = LocalTeamRepository.open(TestTeamStorage())
        repository.saveTeam(testTeam())
        val invalid = ScheduleImportEntry("vs Bears", "Bears", 20, 10, null, null, null)
        assertFailsWith<IllegalArgumentException> { workspace(repository).importSchedule(testTeam(), listOf(invalid)) }
        assertTrue(repository.snapshot.value.children.isEmpty())
        assertTrue(repository.snapshot.value.seasons.isEmpty())
        assertTrue(repository.snapshot.value.games.isEmpty())
    }

    @Test
    fun duplicateScheduleLabelsAreNormalizedBeforeImport() = runTest {
        val repository = LocalTeamRepository.open(TestTeamStorage())
        repository.saveTeam(testTeam())
        val entry = ScheduleImportEntry("", "Bears", 10, 20, null, null, null)
        val result = workspace(repository).importSchedule(testTeam(), listOf(entry, entry.copy(gameLabel = "vs Bears")))
        assertEquals(1, result)
        assertEquals(1, repository.snapshot.value.games.size)
    }

    @Test
    fun manualGameCanBeAddedEditedAndDeletedDurably() = runTest {
        val storage = TestTeamStorage()
        val repository = LocalTeamRepository.open(storage)
        repository.saveTeam(testTeam())
        val workspace = workspace(repository)
        val form = ScheduleGameForm("Bears", "2026-10-20", "18:00", "19:30", "Main Field")
        workspace.saveGame(testTeam(), form)
        val game = repository.snapshot.value.games.single()
        assertEquals("manual", game.source)
        workspace.saveGame(testTeam(), form.copy(opponent = "Lions", label = "JV vs Lions"), game)
        val saved = LocalTeamRepository.open(storage).snapshot.value.games.single()
        assertEquals("Lions", saved.opponentName)
        assertEquals("JV vs Lions", saved.gameLabel)
        assertEquals(game.id, saved.id)
        repository.deleteGame(saved.id)
        assertTrue(LocalTeamRepository.open(storage).snapshot.value.games.isEmpty())
    }

    @Test
    fun gameFormUsesSpecifiedTimeZoneAndTrimsValues() {
        val entry = ScheduleGameForm(" Bears ", "2026-10-20", "18:00", "19:30", " Field ")
            .entry(TimeZone.of("America/New_York"))
        assertEquals(Instant.parse("2026-10-20T22:00:00Z").toEpochMilliseconds(), entry.startMs)
        assertEquals("Bears", entry.opponent)
        assertEquals("Field", entry.locationName)
        assertEquals(90 * 60_000L, entry.endMs - entry.startMs)
    }

    @Test
    fun gameFormRejectsInvalidInput() {
        val valid = ScheduleGameForm("Bears", "2026-10-20", "18:00", "19:30")
        listOf(
            valid.copy(opponent = ""),
            valid.copy(date = "2026-02-30"),
            valid.copy(startTime = "25:00"),
            valid.copy(endTime = "17:00"),
            valid.copy(endTime = "18:00")
        ).forEach { form ->
            assertFailsWith<IllegalArgumentException> { form.entry(TimeZone.UTC) }
        }
    }

    @Test
    fun gameFormRejectsTimeSkippedByDaylightSavingChange() {
        assertFailsWith<IllegalArgumentException> {
            ScheduleGameForm("Bears", "2026-03-08", "02:30", "04:00")
                .entry(TimeZone.of("America/New_York"))
        }
    }
}
