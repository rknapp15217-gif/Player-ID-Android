package com.playerid.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "videos")
data class VideoClip(
    @PrimaryKey
    val id: String = "",
    val filePath: String,
    val duration: Long, // milliseconds
    val createdAt: Long = System.currentTimeMillis(),
    val gameDate: String,
    val gameTitle: String = "",
    val thumbnailPath: String? = null,
    val bubbleMetadata: String = "", // JSON string of time-coded bubble data
    val isExported: Boolean = false,
    val exportedPath: String? = null
) : Parcelable

@Parcelize
data class BubbleMetadata(
    val playerId: String,
    val playerName: String,
    val playerNumber: Int,
    val team: String,
    val startTime: Long, // milliseconds from video start
    val endTime: Long,
    val position: BubblePosition,
    val isVisible: Boolean = true
) : Parcelable

@Parcelize
data class BubblePosition(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
) : Parcelable

@Parcelize
data class Overlay(
    val id: String,
    val type: OverlayType,
    val text: String,
    val timestamp: Long, // when to show in video
    val duration: Long = 3000, // how long to show (3 seconds default)
    val position: OverlayPosition = OverlayPosition.BOTTOM_CENTER,
    val isEnabled: Boolean = true
) : Parcelable

enum class OverlayType(val displayName: String) {
    WINNING_GOAL("Winning Goal"),
    GREAT_SAVE("Great Save"),
    TEAM_SPIRIT("Team Spirit"),
    FIRST_GAME("First Game"),
    AMAZING_PLAY("Amazing Play"),
    GOOD_DEFENSE("Good Defense"),
    CUSTOM("Custom")
}

enum class OverlayPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

@Parcelize
@Entity(tableName = "rosters")
data class Roster(
    @PrimaryKey
    val id: String = "",
    val teamName: String,
    val qrCode: String, // For team sharing
    val createdBy: String, // Parent who created it
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
) : Parcelable

@Parcelize
data class ParentConsent(
    val parentId: String,
    val playerId: String,
    val hasConsent: Boolean = true,
    val canRecordVideo: Boolean = true,
    val canShareHighlights: Boolean = true,
    val consentDate: Long = System.currentTimeMillis()
) : Parcelable