package com.playerid.app.domain.camera

import com.playerid.app.domain.team.CaptureOrganization
import com.playerid.app.domain.team.LocalTeamRepository
import com.playerid.app.domain.team.TeamStorageException
import com.playerid.app.domain.team.captureFileKey
import com.playerid.app.domain.team.captureDate
import com.playerid.app.platform.MediaReference
import kotlinx.datetime.Clock

suspend fun LocalTeamRepository.organizeSavedCapture(media: MediaReference, details: CaptureOrganization?) {
    if (details == null) return
    try {
        transaction { staged ->
            val year = captureDate(media, staged.snapshot.value)?.year
            val seasonId = details.seasonId
                ?: staged.snapshot.value.games.firstOrNull { it.id == details.gameId }?.sportSeasonId
                ?: year?.let { staged.ensureCaptureSeason(details.teamName, it, Clock.System.now().toEpochMilliseconds()) }
            staged.organizeCapture(captureFileKey(media), details.copy(seasonId = seasonId))
        }
    } catch (failure: TeamStorageException) {
        throw CameraCaptureException("Your capture is saved, but its game details could not be saved. Edit details in Memories. ${failure.message.orEmpty()}")
    } catch (failure: IllegalArgumentException) {
        throw CameraCaptureException("Your capture is saved, but its selected team/player/game changed. Edit details in Memories.")
    }
}
