package com.playerid.app.domain.team

import kotlinx.serialization.Serializable

@Serializable
data class TeamProfile(
    val id: String = "",
    val name: String,
    val description: String = "",
    val sport: String = "Soccer",
    val color: String = "",
    val awayColor: String = "#FFFFFF",
    val homeJerseyColor: String = "#1976D2",
    val awayJerseyColor: String = "#FFFFFF",
    val createdBy: String = "Unknown",
    val isActive: Boolean = true,
    val isVerified: Boolean = false,
    val isArchived: Boolean = false,
    val lastActivityAt: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val reportCount: Int = 0,
    val verificationCount: Int = 0
)

@Serializable
data class PlayerProfile(
    val id: String = "",
    val number: String,
    val name: String,
    val position: String,
    val teamName: String,
    val academicYear: String,
    val addedBy: String = "Unknown",
    val isActive: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
    val teamSnapId: String? = null,
    val teamSnapTeamId: String? = null,
    val lastSyncDate: Long? = null,
    val syncStatus: String = "NOT_SYNCED"
)

@Serializable
data class TeamSubscription(
    val userId: String,
    val teamName: String,
    val subscribedAt: Long,
    val isActive: Boolean = true
)