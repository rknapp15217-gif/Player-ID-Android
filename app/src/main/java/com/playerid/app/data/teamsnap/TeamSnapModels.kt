package com.playerid.app.data.teamsnap

import com.google.gson.annotations.SerializedName

/**
 * TeamSnap API Response Models
 */

data class TeamSnapResponse<T>(
    @SerializedName("collection")
    val collection: TeamSnapCollection<T>
)

data class TeamSnapCollection<T>(
    @SerializedName("items")
    val items: List<T>,
    @SerializedName("total")
    val total: Int? = null
)

/**
 * TeamSnap Team Model
 */
data class TeamSnapTeam(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("sport_id")
    val sportId: Long? = null,
    @SerializedName("season_name")
    val seasonName: String? = null,
    @SerializedName("team_photo_url")
    val teamPhotoUrl: String? = null,
    @SerializedName("location_country")
    val locationCountry: String? = null,
    @SerializedName("location_postal_code")
    val locationPostalCode: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
)

/**
 * TeamSnap Member (Player) Model
 */
data class TeamSnapMember(
    @SerializedName("id")
    val id: Long,
    @SerializedName("team_id")
    val teamId: Long,
    @SerializedName("first_name")
    val firstName: String?,
    @SerializedName("last_name")
    val lastName: String?,
    @SerializedName("jersey_number")
    val jerseyNumber: Int? = null,
    @SerializedName("position")
    val position: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("phone_number")
    val phoneNumber: String? = null,
    @SerializedName("birthday")
    val birthday: String? = null,
    @SerializedName("is_non_player")
    val isNonPlayer: Boolean = false,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null
) {
    val fullName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ")
}

/**
 * TeamSnap Authentication Models
 */
data class TeamSnapAuthRequest(
    @SerializedName("email")
    val email: String,
    @SerializedName("password")
    val password: String
)

data class TeamSnapAuthResponse(
    @SerializedName("access_token")
    val accessToken: String,
    @SerializedName("token_type")
    val tokenType: String,
    @SerializedName("expires_in")
    val expiresIn: Long,
    @SerializedName("refresh_token")
    val refreshToken: String? = null
)

/**
 * TeamSnap Error Response
 */
data class TeamSnapError(
    @SerializedName("message")
    val message: String,
    @SerializedName("errors")
    val errors: Map<String, List<String>>? = null
)

/**
 * Local models for integration
 */
data class TeamSnapImportResult(
    val team: TeamSnapTeam,
    val members: List<TeamSnapMember>,
    val importedCount: Int,
    val skippedCount: Int,
    val errors: List<String> = emptyList()
)

enum class TeamSnapSyncStatus {
    NOT_SYNCED,
    SYNCING,
    SYNCED,
    SYNC_ERROR,
    SYNC_OUTDATED
}