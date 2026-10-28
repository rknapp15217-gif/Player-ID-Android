package com.playerid.app.domain.voice

import com.playerid.app.domain.team.PlayerProfile
import com.playerid.app.domain.team.LibraryActionRequest
import com.playerid.app.domain.team.answerRosterQuestion
import com.playerid.app.domain.team.parseRosterQuestion
import kotlin.math.max

sealed class VoiceCommandDecision {
    data class Capture(val message: String = "Moment captured!") : VoiceCommandDecision()
    data class SwitchTeam(val teamName: String, val message: String) : VoiceCommandDecision()
    data class Match(
        val message: String,
        val speech: String,
        val players: List<PlayerProfile>
    ) : VoiceCommandDecision()
    data class Error(val message: String) : VoiceCommandDecision()
    data class Answer(val message: String, val speech: String = message) : VoiceCommandDecision()
}

class VoiceCommandProcessor {
    fun process(
        spokenTexts: List<String>,
        teamNames: List<String>,
        selectedTeam: String?,
        players: List<PlayerProfile>
    ): VoiceCommandDecision {
        val originalText = spokenTexts.firstOrNull()?.lowercase()?.trim().orEmpty()
        if (CAPTURE_WORDS.any(originalText::contains)) return VoiceCommandDecision.Capture()

        val cleanInputs = spokenTexts
            .map { it.lowercase().trim().replace(WAKE_WORDS_REGEX, "").trim() }
            .filter(String::isNotBlank)
        val cleanText = cleanInputs.firstOrNull().orEmpty()
        if (TEAM_WORDS.any(cleanText::contains)) {
            teamNames.firstOrNull { cleanText.contains(it.lowercase()) }?.let { teamName ->
                return VoiceCommandDecision.SwitchTeam(teamName, "Switched to $teamName")
            }
        }
        if (selectedTeam == null) return VoiceCommandDecision.Error("Please select a team first")

        cleanInputs.firstOrNull()?.let { text ->
            parseRosterQuestion(text)?.let { query ->
                return VoiceCommandDecision.Answer(
                    message = answerRosterQuestion(
                        LibraryActionRequest.Roster(query), players, selectedTeam,
                        compact = true,
                        includeCount = Regex("\\b(how many|count|number of)\\b").containsMatchIn(text)
                    ),
                    speech = answerRosterQuestion(
                        LibraryActionRequest.Roster(query), players, selectedTeam,
                        compact = true,
                        includeCount = Regex("\\b(how many|count|number of)\\b").containsMatchIn(text),
                        summarizeLargeList = true
                    )
                )
            }
        }
        return lookupRoster(cleanInputs, players.filter { it.teamName == selectedTeam })
    }

    fun lookupRoster(
        queries: List<String>,
        players: List<PlayerProfile>,
        allowApproximateNames: Boolean = true
    ): VoiceCommandDecision {
        val roster = players.filter { it.isActive }
        val processedInputs = queries.map { preprocessVoiceQuery(it.lowercase()) }.filter(String::isNotBlank)
        val jerseyNumber = processedInputs.asSequence()
            .firstOrNull { it.matches(Regex("#?[0-9]{1,3}")) }?.removePrefix("#")
        if (jerseyNumber != null) {
            val jerseyMatches = roster.filter { it.number == jerseyNumber }
            if (jerseyMatches.size == 1) {
                val player = jerseyMatches.single()
                return VoiceCommandDecision.Match(
                    message = "#$jerseyNumber ${player.name}",
                    speech = "#$jerseyNumber ${player.name}",
                    players = jerseyMatches
                )
            }
            if (jerseyMatches.size > 1) {
                val message = "I found ${jerseyMatches.size} players with #$jerseyNumber"
                return VoiceCommandDecision.Match(message, message, jerseyMatches)
            }
            return VoiceCommandDecision.Error("No active roster player has #$jerseyNumber.")
        }

        val fullNameMatches = roster.filter { player ->
            processedInputs.any { normalize(it) == normalize(player.name) }
        }
        val exactMatches = fullNameMatches.ifEmpty { roster.filter { player ->
            processedInputs.any { query ->
                val tokens = normalize(query).split(' ').filter(String::isNotBlank)
                val nameTokens = normalize(player.name).split(' ')
                tokens.isNotEmpty() && tokens.all { it in nameTokens }
            } }
        }
        if (exactMatches.isNotEmpty() || !allowApproximateNames) {
            return if (exactMatches.isEmpty()) VoiceCommandDecision.Error("No active roster player matches that name.")
            else VoiceCommandDecision.Match(
                if (exactMatches.size == 1) "Number #${exactMatches.single().number} ${exactMatches.single().name}"
                    else exactMatches.joinToString("\n") { "${it.name} #${it.number}" },
                if (exactMatches.size == 1) "Number ${exactMatches.single().number}"
                    else "I found multiple players with that name",
                exactMatches
            )
        }

        val scored = processedInputs.mapNotNull { query ->
            val matches = roster.map { it to scoreNameMatch(query, it.name) }
                .sortedByDescending { it.second }
            matches.firstOrNull()?.let { query to matches }
        }.maxByOrNull { it.second.first().second }?.second.orEmpty()
        val bestScore = scored.firstOrNull()?.second ?: 0.0
        val matches = scored
            .filter { it.second >= 0.62 && bestScore - it.second <= 0.08 }
            .map { it.first }
        return when {
            matches.size == 1 -> {
                val player = matches.single()
                VoiceCommandDecision.Match(
                    message = "Number #${player.number} ${player.name}",
                    speech = "Number ${player.number}",
                    players = matches
                )
            }
            matches.size > 1 -> {
                val names = matches.joinToString(" and ") { "${it.name} #${it.number}" }
                VoiceCommandDecision.Match(
                    message = "I found ${matches.size} players: $names",
                    speech = "I found multiple players with that name",
                    players = matches
                )
            }
            else -> VoiceCommandDecision.Error("Sorry no roster match")
        }
    }

    private fun preprocessVoiceQuery(raw: String): String {
        var processed = raw.trim().trimEnd('?', '.', '!').replace(Regex("\\s+"), " ")
        FILLER_WORDS.forEach { word -> processed = processed.replace("\\b$word\\b".toRegex(), "").trim() }
        processed = processed.replace(Regex("\\s+"), " ").replace(Regex("#\\s+"), "#")
        NUMBER_WORDS[processed]?.let { return it }
        val parts = processed.replace('-', ' ').split(' ').filter(String::isNotBlank)
        val tens = mapOf("twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
            "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90)
        tens[parts.firstOrNull()]?.let { ten ->
            if (parts.size == 1) return ten.toString()
            val units = NUMBER_WORDS[parts.getOrNull(1)]?.toIntOrNull()
            if (parts.size == 2 && units != null && units in 1..9) return (ten + units).toString()
        }
        return processed
    }

    private fun scoreNameMatch(queryRaw: String, playerNameRaw: String): Double {
        val query = normalize(queryRaw)
        val playerName = normalize(playerNameRaw)
        if (query.isBlank() || playerName.isBlank()) return 0.0
        if (query == playerName) return 1.0
        if (playerName.contains(query) || query.contains(playerName)) return 0.92
        val queryTokens = query.split(" ").filter(String::isNotBlank)
        val playerTokens = playerName.split(" ").filter(String::isNotBlank)
        var exact = 0
        var phonetic = 0
        queryTokens.forEach { token ->
            if (token in playerTokens) exact++
            else if (soundex(token).let { code -> code.isNotEmpty() && playerTokens.any { soundex(it) == code } }) phonetic++
        }
        val denominator = max(1, max(queryTokens.size, playerTokens.size))
        val similarity = normalizedSimilarity(query, playerName)
        return max(similarity, exact.toDouble() / denominator * 0.60 + phonetic.toDouble() / denominator * 0.28 + similarity * 0.12)
    }

    private fun normalize(value: String): String = value.lowercase()
        .replace("'", "").replace("-", " ").replace("[^a-z0-9 ]".toRegex(), " ")
        .replace("\\s+".toRegex(), " ").trim()

    private fun normalizedSimilarity(first: String, second: String): Double {
        val maxLength = max(first.length, second.length)
        if (maxLength == 0) return 1.0
        return (1.0 - levenshteinDistance(first, second).toDouble() / maxLength).coerceIn(0.0, 1.0)
    }

    private fun levenshteinDistance(first: String, second: String): Int {
        val distances = IntArray(second.length + 1) { it }
        for (firstIndex in 1..first.length) {
            var diagonal = distances[0]
            distances[0] = firstIndex
            for (secondIndex in 1..second.length) {
                val previous = distances[secondIndex]
                distances[secondIndex] = minOf(
                    distances[secondIndex] + 1,
                    distances[secondIndex - 1] + 1,
                    diagonal + if (first[firstIndex - 1] == second[secondIndex - 1]) 0 else 1
                )
                diagonal = previous
            }
        }
        return distances[second.length]
    }

    private fun soundex(input: String): String {
        val letters = input.uppercase().filter { it in 'A'..'Z' }
        if (letters.isEmpty()) return ""
        val result = StringBuilder()
        var previous = '0'
        letters.drop(1).forEach { letter ->
            val code = when (letter) {
                'B', 'F', 'P', 'V' -> '1'
                'C', 'G', 'J', 'K', 'Q', 'S', 'X', 'Z' -> '2'
                'D', 'T' -> '3'
                'L' -> '4'
                'M', 'N' -> '5'
                'R' -> '6'
                else -> '0'
            }
            if (code != previous && code != '0') result.append(code)
            previous = code
        }
        return (letters.first() + result.toString()).padEnd(4, '0').take(4)
    }

    private companion object {
        val CAPTURE_WORDS = listOf("capture", "stop", "finish", "done")
        val TEAM_WORDS = listOf("team", "switch", "select")
        val FILLER_WORDS = listOf("number", "jersey", "player", "who is", "what is", "is", "the", "find", "identify")
        val NUMBER_WORDS = mapOf(
            "zero" to "0", "one" to "1", "two" to "2", "three" to "3", "four" to "4",
            "five" to "5", "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9", "ten" to "10",
            "eleven" to "11", "twelve" to "12", "thirteen" to "13", "fourteen" to "14",
            "fifteen" to "15", "sixteen" to "16", "seventeen" to "17", "eighteen" to "18", "nineteen" to "19",
            "double zero" to "00", "zero zero" to "00"
        )
        val WAKE_WORDS_REGEX = Regex("(?i)^(spotter|spottr|spotr|sport|spot|potter|hey spotter|hey spotr|hey spottr)\\b")
    }
}