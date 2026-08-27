package com.playerid.app.domain.team

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class MemoryGameMatch(
    val game: GameScheduleProfile,
    val score: Double
)

class MemoryGameMatcher {
    fun findBestMatch(
        dateTakenMs: Long,
        latitude: Double?,
        longitude: Double?,
        games: List<GameScheduleProfile>
    ): MemoryGameMatch? = games
        .map { game ->
            MemoryGameMatch(
                game = game,
                score = score(dateTakenMs, latitude, longitude, game)
            )
        }
        .maxByOrNull(MemoryGameMatch::score)

    fun score(
        dateTakenMs: Long,
        latitude: Double?,
        longitude: Double?,
        game: GameScheduleProfile
    ): Double {
        val preWindowMs = 1000L * 60L * 60L * 3L
        val postWindowMs = 1000L * 60L * 60L * 4L
        val inWindow = dateTakenMs in
            (game.scheduledStartMs - preWindowMs)..(game.scheduledEndMs + postWindowMs)

        val timeScore = if (inWindow) {
            val midpoint = (game.scheduledStartMs + game.scheduledEndMs) / 2L
            val deltaHours = abs(dateTakenMs - midpoint) / (1000.0 * 60.0 * 60.0)
            (1.0 - (deltaHours / 6.0)).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

        val locationScore = if (
            latitude != null && longitude != null &&
            game.locationLat != null && game.locationLng != null
        ) {
            when (haversineKm(latitude, longitude, game.locationLat, game.locationLng)) {
                in 0.0..1.0 -> 1.0
                in 1.0..5.0 -> 0.8
                in 5.0..15.0 -> 0.55
                in 15.0..30.0 -> 0.35
                else -> 0.0
            }
        } else {
            0.0
        }

        return (timeScore * 0.75) + (locationScore * 0.25)
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadiusKm = 6371.0
        val latitudeDelta = degreesToRadians(lat2 - lat1)
        val longitudeDelta = degreesToRadians(lon2 - lon1)
        val startLatitude = degreesToRadians(lat1)
        val endLatitude = degreesToRadians(lat2)
        val distance = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2) *
            cos(startLatitude) * cos(endLatitude)
        return earthRadiusKm * 2 * atan2(sqrt(distance), sqrt(1 - distance))
    }

    private fun degreesToRadians(degrees: Double): Double = degrees * kotlin.math.PI / 180.0
}