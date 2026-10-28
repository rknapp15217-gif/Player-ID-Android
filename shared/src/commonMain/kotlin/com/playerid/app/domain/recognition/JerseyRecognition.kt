package com.playerid.app.domain.recognition

import com.playerid.app.domain.team.PlayerProfile
import com.playerid.app.domain.team.TeamProfile
import kotlin.math.sqrt

/** One jersey number read from one video frame, with the shirt color sampled around it. */
data class JerseyObservation(
    val number: String,
    val colorHex: String?,
    val confidence: Float,
    val timeMs: Long
)

data class JerseyPlayerMatch(
    val player: PlayerProfile,
    val confidence: Float,
    val frames: Int,
    val colorVerified: Boolean
)

data class JerseyMatchSettings(
    val minFrames: Int = 2,
    val minConfidence: Float = 0.5f,
    val maxColorDistance: Double = 110.0
)

/** Reads a jersey number, fixing common letter/digit confusions and dropping leading zeros so "07" equals "7". */
fun normalizeJerseyNumber(raw: String): String? {
    val digits = raw.trim().map {
        when (it) {
            'O', 'o' -> '0'
            'I', 'l' -> '1'
            else -> it
        }
    }.joinToString("")
    if (digits.isEmpty() || digits.length > 2 || !digits.all { it in '0'..'9' }) return null
    return digits.trimStart('0').ifEmpty { "0" }
}

fun parseHexColor(hex: String): Triple<Int, Int, Int>? {
    val clean = hex.trim().removePrefix("#")
    if (clean.length != 6) return null
    val value = clean.toLongOrNull(16) ?: return null
    return Triple(((value shr 16) and 0xFF).toInt(), ((value shr 8) and 0xFF).toInt(), (value and 0xFF).toInt())
}

/** Weighted RGB distance that tracks perceived difference; 0 is identical, roughly 765 is black versus white. */
fun jerseyColorDistance(a: String, b: String): Double? {
    val (r1, g1, b1) = parseHexColor(a) ?: return null
    val (r2, g2, b2) = parseHexColor(b) ?: return null
    val meanRed = (r1 + r2) / 2.0
    val dr = (r1 - r2).toDouble()
    val dg = (g1 - g2).toDouble()
    val db = (b1 - b2).toDouble()
    return sqrt((2 + meanRed / 256) * dr * dr + 4 * dg * dg + (2 + (255 - meanRed) / 256) * db * db)
}

/** The team's jersey colors, home first. Duplicates and invalid values are removed. */
fun teamJerseyColors(team: TeamProfile?): List<String> =
    listOfNotNull(team?.homeJerseyColor, team?.awayJerseyColor).filter { parseHexColor(it) != null }.distinct()

/**
 * Matches numbers read from a clip against a roster. A number counts only when it was read in enough frames; when
 * the sampled shirt color is known it must also be close to one of the team's jersey colors, so the same number on
 * the opposing team is not credited to the roster player.
 */
fun matchJerseyPlayers(
    observations: List<JerseyObservation>,
    roster: List<PlayerProfile>,
    team: TeamProfile?,
    settings: JerseyMatchSettings = JerseyMatchSettings()
): List<JerseyPlayerMatch> {
    val colors = teamJerseyColors(team)
    val byNumber = roster.filter { it.isActive }.groupBy { normalizeJerseyNumber(it.number) }
    val matches = mutableListOf<JerseyPlayerMatch>()
    observations.filter { it.confidence >= settings.minConfidence }
        .mapNotNull { obs -> normalizeJerseyNumber(obs.number)?.let { it to obs } }
        .groupBy({ it.first }, { it.second })
        .forEach { (number, reads) ->
            // A duplicated number on the roster is ambiguous, so no player is suggested.
            val player = byNumber[number]?.singleOrNull() ?: return@forEach
            val teamReads = if (colors.isEmpty()) reads else reads.filter { read ->
                read.colorHex == null || colors.any { (jerseyColorDistance(read.colorHex, it) ?: Double.MAX_VALUE) <= settings.maxColorDistance }
            }
            if (teamReads.size < settings.minFrames) return@forEach
            val verified = colors.isNotEmpty() && teamReads.any { it.colorHex != null }
            val averageConfidence = teamReads.map { it.confidence }.average().toFloat()
            // Agreement across frames matters more than any single read, so reward consistency up to a cap.
            val consistency = (teamReads.size.toFloat() / (settings.minFrames * 3)).coerceAtMost(1f)
            matches += JerseyPlayerMatch(
                player = player,
                confidence = (averageConfidence * (0.6f + 0.4f * consistency)).coerceAtMost(1f),
                frames = teamReads.size,
                colorVerified = verified
            )
        }
    return matches.sortedByDescending { it.confidence }
}
