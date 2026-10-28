package com.playerid.app.domain.team

import com.playerid.app.roster.RosterCandidate
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

const val LOCAL_TEAM_USER = "local-user"

class TeamWorkspace(
    val repository: LocalTeamRepository,
    private val newId: () -> String,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    suspend fun createTeam(submission: TeamCreationSubmission): TeamProfile {
        require(submission.teamName.isNotBlank()) { "Enter a team name." }
        val timestamp = now()
        val team = TeamProfile(
            id = newId(),
            name = submission.teamName.trim(),
            sport = submission.sport,
            color = submission.homeJerseyColor,
            awayColor = submission.awayJerseyColor,
            homeJerseyColor = submission.homeJerseyColor,
            awayJerseyColor = submission.awayJerseyColor,
            createdBy = LOCAL_TEAM_USER,
            lastActivityAt = timestamp,
            createdAt = timestamp,
            updatedAt = timestamp
        )
        repository.transaction { staged ->
            staged.saveTeam(team)
            staged.subscribe(TeamSubscription(LOCAL_TEAM_USER, team.name, timestamp))
        }
        return team
    }

    /** Saves team settings, renaming the team (and everything linked to it) when the name changed. Returns the saved team. */
    suspend fun updateTeam(team: TeamProfile, submission: TeamCreationSubmission): TeamProfile {
        val name = submission.teamName.trim()
        val updated = team.copy(
            name = name,
            sport = submission.sport,
            color = submission.homeJerseyColor,
            awayColor = submission.awayJerseyColor,
            homeJerseyColor = submission.homeJerseyColor,
            awayJerseyColor = submission.awayJerseyColor,
            updatedAt = now()
        )
        repository.transaction { staged ->
            if (name != team.name) staged.renameTeam(team.id, name)
            staged.saveTeam(updated)
        }
        return updated
    }

    suspend fun joinTeam(teamName: String) {
        repository.subscribe(TeamSubscription(LOCAL_TEAM_USER, teamName, now()))
    }

    suspend fun leaveTeam(teamName: String) {
        repository.unsubscribe(LOCAL_TEAM_USER, teamName)
    }

    suspend fun joinFromOnboarding(team: TeamProfile, player: PlayerProfile?) {
        repository.transaction { staged ->
            TeamSubscriptionService(staged).replaceWithTeam(LOCAL_TEAM_USER, team.name, now())
            staged.assignPlayer(team.name, player?.id)
        }
    }

    suspend fun savePlayer(submission: PlayerFormSubmission, existing: PlayerProfile? = null) {
        require(submission.name.isNotBlank() && submission.number.isNotBlank() && submission.position.isNotBlank()) {
            "Enter the player's name, jersey number, and position."
        }
        val timestamp = now()
        val player = (existing ?: PlayerProfile(
            id = "",
            name = "",
            number = "",
            position = "",
            teamName = submission.teamName,
            academicYear = submission.academicYear,
            createdAt = timestamp,
            updatedAt = timestamp
        )).copy(
            name = submission.name.trim(),
            number = submission.number.trim(),
            position = submission.position.trim(),
            academicYear = submission.academicYear
        )
        val service = TeamRosterService(repository)
        if (existing == null) {
            service.addPlayer(player, newId(), LOCAL_TEAM_USER, timestamp)
        } else {
            service.updatePlayer(player, timestamp)
        }
    }

    suspend fun deletePlayer(playerId: String) = TeamRosterService(repository).deletePlayer(playerId)

    suspend fun importRoster(teamName: String, candidates: List<RosterCandidate>): RosterImportSummary {
        require(candidates.isNotEmpty()) { "No players were recognized. Check the text and preview again." }
        require(candidates.map { it.number }.distinct().size == candidates.size) {
            "More than one player has the same jersey number. Correct the text before importing."
        }
        return repository.transaction { staged ->
            TeamRosterService(staged).importRoster(
                teamName, candidates, LOCAL_TEAM_USER, candidates.map { newId() }, now()
            )
        }
    }

    suspend fun importSchedule(team: TeamProfile, entries: List<ScheduleImportEntry>): Int {
        require(entries.isNotEmpty()) { "No games were recognized. Check the dates and times and preview again." }
        require(entries.all { it.opponent.isNotBlank() && it.endMs > it.startMs }) {
            "Every game needs an opponent and an end time after its start time."
        }
        return repository.transaction { staged ->
            val existing = staged.snapshot.value
            val seasonIds = existing.seasons.filter { it.teamName == team.name }.map { it.id }.toSet()
            val unique = entries.distinctBy {
                Triple(it.startMs, it.opponent.lowercase(), it.gameLabel.ifBlank { "vs ${it.opponent}" }.lowercase())
            }
            val fresh = unique.filter { entry ->
                existing.games.none {
                    it.sportSeasonId in seasonIds && it.scheduledStartMs == entry.startMs &&
                        it.opponentName.equals(entry.opponent, ignoreCase = true) &&
                        it.gameLabel.equals(entry.gameLabel.ifBlank { "vs ${entry.opponent}" }, ignoreCase = true)
                }
            }
            ScheduleImportService(staged).import(
                team.name, team.sport, fresh, fresh.map { newId() },
                Instant.fromEpochMilliseconds(now())
                    .toLocalDateTime(TimeZone.currentSystemDefault()).year, now()
            )
        }
    }

    suspend fun saveGame(team: TeamProfile, form: ScheduleGameForm, existing: GameScheduleProfile? = null) {
        val entry = form.entry()
        if (existing != null) {
            repository.saveGames(listOf(existing.copy(
                opponentName = entry.opponent,
                gameLabel = entry.gameLabel,
                scheduledStartMs = entry.startMs,
                scheduledEndMs = entry.endMs,
                locationName = entry.locationName,
                updatedAt = now()
            )))
        } else {
            repository.transaction { staged ->
                val id = newId()
                ScheduleImportService(staged).import(
                    team.name, team.sport, listOf(entry), listOf(id),
                    Instant.fromEpochMilliseconds(now())
                        .toLocalDateTime(TimeZone.currentSystemDefault()).year, now()
                )
                val game = staged.snapshot.value.games.first { it.id == id }
                staged.saveGames(listOf(game.copy(source = "manual")))
            }
        }
    }
}

data class ScheduleGameForm(
    val opponent: String = "",
    val date: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val location: String = "",
    val label: String = ""
) {
    fun entry(timeZone: TimeZone = TimeZone.currentSystemDefault()): ScheduleImportEntry {
        require(opponent.isNotBlank()) { "Enter the opponent's name." }
        val day = try {
            LocalDate.parse(date.trim())
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("Enter a valid date in YYYY-MM-DD format.", error)
        }
        fun instant(value: String, label: String): Long {
            val time = try {
                LocalTime.parse(value.trim())
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("Enter a valid $label time in 24-hour HH:MM format.", error)
            }
            val local = LocalDateTime(day, time)
            val instant = local.toInstant(timeZone)
            require(instant.toLocalDateTime(timeZone) == local) {
                "The $label time does not exist in ${timeZone.id} because of a clock change. Choose another time."
            }
            return instant.toEpochMilliseconds()
        }
        val start = instant(startTime, "start")
        val end = instant(endTime, "end")
        require(end > start) { "End time must be after start time on the same date." }
        return ScheduleImportEntry(
            gameLabel = label.trim().ifBlank { "vs ${opponent.trim()}" },
            opponent = opponent.trim(),
            startMs = start,
            endMs = end,
            locationName = location.trim().ifBlank { null },
            latitude = null,
            longitude = null
        )
    }
}
