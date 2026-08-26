package com.playerid.app.data.repositories

import com.playerid.app.data.Player
import com.playerid.app.data.Team
import com.playerid.app.data.UserTeamSubscription
import com.playerid.app.data.teamsnap.TeamSnapSyncStatus
import com.playerid.app.domain.team.PlayerProfile
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