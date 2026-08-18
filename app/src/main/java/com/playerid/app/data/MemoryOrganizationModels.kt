package com.playerid.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "child_profiles",
    indices = [Index(value = ["displayName"], unique = true)]
)
data class ChildProfile(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(
    tableName = "sport_seasons",
    indices = [
        Index(value = ["childId", "sportName", "seasonLabel"], unique = true),
        Index(value = ["teamName"])
    ]
)
data class SportSeason(
    @PrimaryKey
    val id: String,
    val childId: String,
    val sportName: String,
    val seasonLabel: String,
    val teamName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(
    tableName = "game_schedules",
    indices = [
        Index(value = ["sportSeasonId"]),
        Index(value = ["scheduledStartMs"]),
        Index(value = ["locationLat", "locationLng"])
    ]
)
data class GameSchedule(
    @PrimaryKey
    val id: String,
    val sportSeasonId: String,
    val opponentName: String,
    val gameLabel: String,
    val scheduledStartMs: Long,
    val scheduledEndMs: Long,
    val locationName: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val source: String = "manual",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "memory_items",
    indices = [
        Index(value = ["contentUri"], unique = true),
        Index(value = ["mediaStoreId"]),
        Index(value = ["dateTakenMs"]),
        Index(value = ["sportSeasonId"]),
        Index(value = ["gameScheduleId"]),
        Index(value = ["needsReview", "autoScore"])
    ]
)
data class MemoryItem(
    @PrimaryKey
    val id: String,
    // Media stays on phone. We only keep a pointer to MediaStore content.
    val contentUri: String,
    val mediaStoreId: Long,
    val mimeType: String,
    val displayName: String,
    val dateTakenMs: Long,
    val dateAddedMs: Long,
    val bucketName: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val durationMs: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val sportSeasonId: String? = null,
    val gameScheduleId: String? = null,
    val categorizationSource: String = "unassigned",
    val autoScore: Double = 0.0,
    val needsReview: Boolean = true,
    val reviewedAtMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "media_ingestion_state")
data class MediaIngestionState(
    @PrimaryKey
    val key: String = "default",
    val lastScannedDateAddedMs: Long = 0L,
    val lastScannedAtMs: Long = System.currentTimeMillis()
)
