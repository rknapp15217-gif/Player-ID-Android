package com.playerid.app.data.repositories

import com.playerid.app.data.Player
import com.playerid.app.data.GameSchedule
import com.playerid.app.data.ChildProfile
import com.playerid.app.data.SportSeason
import com.playerid.app.data.MemoryItem
import com.playerid.app.data.Team
import com.playerid.app.data.UserTeamSubscription
import com.playerid.app.data.teamsnap.TeamSnapSyncStatus
import com.playerid.app.domain.team.PlayerProfile
import com.playerid.app.domain.team.GameScheduleProfile
import com.playerid.app.domain.team.ChildProfileRecord
import com.playerid.app.domain.team.SportSeasonProfile
import com.playerid.app.domain.team.MemoryItemProfile
import com.playerid.app.platform.MediaKind
import com.playerid.app.platform.MediaReference
import com.playerid.app.domain.team.TeamProfile
import com.playerid.app.domain.team.TeamSubscription

fun Team.toProfile() = TeamProfile(
    id = id,
    name = name,
    description = description,
    sport = sport,
    color = color,
    awayColor = awayColor,
    homeJerseyColor = homeJerseyColor,
    awayJerseyColor = awayJerseyColor,
    createdBy = createdBy,
    isActive = isActive,
    isVerified = isVerified,
    isArchived = isArchived,
    lastActivityAt = lastActivityAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    reportCount = reportCount,
    verificationCount = verificationCount
)

fun TeamProfile.toEntity() = Team(
    id = id,
    name = name,
    description = description,
    sport = sport,
    color = color,
    awayColor = awayColor,
    homeJerseyColor = homeJerseyColor,
    awayJerseyColor = awayJerseyColor,
    createdBy = createdBy,
    isActive = isActive,
    isVerified = isVerified,
    isArchived = isArchived,
    lastActivityAt = lastActivityAt,
    createdAt = createdAt,
    updatedAt = updatedAt,
    reportCount = reportCount,
    verificationCount = verificationCount
)

fun Player.toProfile() = PlayerProfile(
    id = id,
    number = number,
    name = name,
    position = position,
    teamName = team,
    academicYear = academicYear,
    addedBy = addedBy,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    teamSnapId = teamSnapId,
    teamSnapTeamId = teamSnapTeamId,
    lastSyncDate = lastSyncDate,
    syncStatus = syncStatus.name
)

fun PlayerProfile.toEntity() = Player(
    id = id,
    number = number,
    name = name,
    position = position,
    team = teamName,
    academicYear = academicYear,
    addedBy = addedBy,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    teamSnapId = teamSnapId,
    teamSnapTeamId = teamSnapTeamId,
    lastSyncDate = lastSyncDate,
    syncStatus = TeamSnapSyncStatus.entries.find { it.name == syncStatus }
        ?: TeamSnapSyncStatus.NOT_SYNCED
)

fun UserTeamSubscription.toProfile() = TeamSubscription(
    userId = userId,
    teamName = teamName,
    subscribedAt = subscribedAt,
    isActive = isActive
)

fun TeamSubscription.toEntity() = UserTeamSubscription(
    userId = userId,
    teamName = teamName,
    subscribedAt = subscribedAt,
    isActive = isActive
)

fun GameSchedule.toProfile() = GameScheduleProfile(
    id = id,
    sportSeasonId = sportSeasonId,
    opponentName = opponentName,
    gameLabel = gameLabel,
    scheduledStartMs = scheduledStartMs,
    scheduledEndMs = scheduledEndMs,
    locationName = locationName,
    locationLat = locationLat,
    locationLng = locationLng,
    source = source,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun GameScheduleProfile.toEntity() = GameSchedule(
    id = id,
    sportSeasonId = sportSeasonId,
    opponentName = opponentName,
    gameLabel = gameLabel,
    scheduledStartMs = scheduledStartMs,
    scheduledEndMs = scheduledEndMs,
    locationName = locationName,
    locationLat = locationLat,
    locationLng = locationLng,
    source = source,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ChildProfile.toProfile() = ChildProfileRecord(
    id = id,
    displayName = displayName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isActive = isActive
)

fun ChildProfileRecord.toEntity() = ChildProfile(
    id = id,
    displayName = displayName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isActive = isActive
)

fun SportSeason.toProfile() = SportSeasonProfile(
    id = id,
    childId = childId,
    sportName = sportName,
    seasonLabel = seasonLabel,
    teamName = teamName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isActive = isActive
)

fun SportSeasonProfile.toEntity() = SportSeason(
    id = id,
    childId = childId,
    sportName = sportName,
    seasonLabel = seasonLabel,
    teamName = teamName,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isActive = isActive
)

fun MemoryItem.toProfile() = MemoryItemProfile(
    id = id,
    media = MediaReference(
        identifier = contentUri,
        kind = if (mimeType.startsWith("video/")) MediaKind.VIDEO else MediaKind.IMAGE,
        mimeType = mimeType
    ),
    platformMediaId = mediaStoreId,
    displayName = displayName,
    dateTakenMs = dateTakenMs,
    dateAddedMs = dateAddedMs,
    bucketName = bucketName,
    width = width,
    height = height,
    durationMs = durationMs,
    latitude = latitude,
    longitude = longitude,
    sportSeasonId = sportSeasonId,
    gameScheduleId = gameScheduleId,
    categorizationSource = categorizationSource,
    autoScore = autoScore,
    needsReview = needsReview,
    reviewedAtMs = reviewedAtMs,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MemoryItemProfile.toEntity() = MemoryItem(
    id = id,
    contentUri = media.identifier,
    mediaStoreId = platformMediaId,
    mimeType = media.mimeType.orEmpty(),
    displayName = displayName,
    dateTakenMs = dateTakenMs,
    dateAddedMs = dateAddedMs,
    bucketName = bucketName,
    width = width,
    height = height,
    durationMs = durationMs,
    latitude = latitude,
    longitude = longitude,
    sportSeasonId = sportSeasonId,
    gameScheduleId = gameScheduleId,
    categorizationSource = categorizationSource,
    autoScore = autoScore,
    needsReview = needsReview,
    reviewedAtMs = reviewedAtMs,
    createdAt = createdAt,
    updatedAt = updatedAt
)