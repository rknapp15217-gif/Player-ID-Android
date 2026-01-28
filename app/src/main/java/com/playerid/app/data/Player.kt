package com.playerid.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "players")
data class Player(
    @PrimaryKey
    val id: String = "",
    val number: String,
    val name: String,
    val position: String,
    val team: String,
    val academicYear: String, // "Freshman", "Sophomore", "Junior", "Senior"
    val addedBy: String = "Unknown", // Track who added this player for crowd-sourcing
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // TeamSnap integration fields
    val teamSnapId: String? = null,
    val teamSnapTeamId: String? = null,
    val lastSyncDate: Long? = null,
    val syncStatus: com.playerid.app.data.teamsnap.TeamSnapSyncStatus = com.playerid.app.data.teamsnap.TeamSnapSyncStatus.NOT_SYNCED
) : Parcelable

enum class AcademicYear(val displayName: String) {
    FRESHMAN("Freshman"),
    SOPHOMORE("Sophomore"), 
    JUNIOR("Junior"),
    SENIOR("Senior")
}

@Parcelize
@Entity(
    tableName = "teams",
    indices = [androidx.room.Index(value = ["name"], unique = true)]
)
data class Team(
    @PrimaryKey
    val id: String = "",
    val name: String,
    val description: String = "",
    val color: String = "",
    val createdBy: String = "Unknown", // Track who created the team
    val isActive: Boolean = true,
    val isVerified: Boolean = false, // Community verification badge
    val isArchived: Boolean = false, // Hidden from main search but preserved
    val lastActivityAt: Long = System.currentTimeMillis(), // Last player addition/update
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val reportCount: Int = 0, // Community reports for duplicates/issues
    val verificationCount: Int = 0 // Community verifications
) : Parcelable

data class TeamWithStats(
    val team: Team,
    val playerCount: Int,
    val contributors: List<String>, // List of users who added players
    val lastActivity: Long // Most recent player addition/update
)

@Entity(
    tableName = "user_team_subscriptions",
    primaryKeys = arrayOf("userId", "teamName"),
    foreignKeys = [
        ForeignKey(
            entity = Team::class,
            parentColumns = ["name"],
            childColumns = ["teamName"], 
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["teamName"])]
)
data class UserTeamSubscription(
    val userId: String,
    val teamName: String,
    val subscribedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
