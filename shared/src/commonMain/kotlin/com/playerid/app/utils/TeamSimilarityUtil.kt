package com.playerid.app.utils

import kotlin.math.max
import kotlin.math.min

/**
 * Utility class for detecting similar team names to prevent duplicates.
 */
class TeamSimilarityUtil {
    companion object {
        private const val SIMILARITY_THRESHOLD = 0.8f
        private const val HIGH_SIMILARITY_THRESHOLD = 0.9f

        private fun levenshteinDistance(str1: String, str2: String): Int {
            val len1 = str1.length
            val len2 = str2.length
            val distances = Array(len1 + 1) { IntArray(len2 + 1) }

            for (index in 0..len1) distances[index][0] = index
            for (index in 0..len2) distances[0][index] = index

            for (firstIndex in 1..len1) {
                for (secondIndex in 1..len2) {
                    val cost = if (str1[firstIndex - 1] == str2[secondIndex - 1]) 0 else 1
                    distances[firstIndex][secondIndex] = min(
                        min(
                            distances[firstIndex - 1][secondIndex] + 1,
                            distances[firstIndex][secondIndex - 1] + 1
                        ),
                        distances[firstIndex - 1][secondIndex - 1] + cost
                    )
                }
            }

            return distances[len1][len2]
        }

        private fun calculateSimilarity(str1: String, str2: String): Float {
            val normalized1 = normalizeTeamName(str1)
            val normalized2 = normalizeTeamName(str2)
            val maxLength = max(normalized1.length, normalized2.length)
            if (maxLength == 0) return 1.0f

            val distance = levenshteinDistance(normalized1, normalized2)
            return 1.0f - (distance.toFloat() / maxLength)
        }

        private fun normalizeTeamName(name: String): String {
            return name.lowercase()
                .replace(Regex("\\b(team|club|fc|athletic|sports?)\\b"), "")
                .replace(Regex("\\b(high school|hs|middle school|ms|elementary|elem)\\b"), "school")
                .replace(Regex("\\b(lacrosse|lax)\\b"), "lacrosse")
                .replace(Regex("\\b(soccer|football)\\b"), "soccer")
                .replace(Regex("\\b(basketball|bball)\\b"), "basketball")
                .replace(Regex("\\b(baseball|ball)\\b"), "baseball")
                .replace(Regex("\\b(volleyball|vball)\\b"), "volleyball")
                .replace(Regex("\\b(u\\d+|under \\d+)\\b"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        }

        fun areTeamsSimilar(name1: String, name2: String): Boolean {
            return calculateSimilarity(name1, name2) >= SIMILARITY_THRESHOLD
        }

        fun areTeamsHighlySimilar(name1: String, name2: String): Boolean {
            return calculateSimilarity(name1, name2) >= HIGH_SIMILARITY_THRESHOLD
        }

        fun findSimilarTeams(newTeamName: String, existingTeams: List<String>): List<SimilarTeam> {
            return existingTeams.mapNotNull { existingName ->
                val similarity = calculateSimilarity(newTeamName, existingName)
                if (similarity >= SIMILARITY_THRESHOLD) {
                    SimilarTeam(existingName, similarity)
                } else {
                    null
                }
            }.sortedByDescending { it.similarity }
        }

        fun generateTeamNameSuggestions(teamName: String): List<String> {
            val suggestions = mutableListOf<String>()

            if (teamName.contains("HS", ignoreCase = true)) {
                suggestions.add(teamName.replace(Regex("\\bHS\\b", RegexOption.IGNORE_CASE), "High School"))
            }
            if (teamName.contains("MS", ignoreCase = true)) {
                suggestions.add(teamName.replace(Regex("\\bMS\\b", RegexOption.IGNORE_CASE), "Middle School"))
            }
            if (teamName.contains("LAX", ignoreCase = true)) {
                suggestions.add(teamName.replace(Regex("\\bLAX\\b", RegexOption.IGNORE_CASE), "Lacrosse"))
            }

            return suggestions.distinct()
        }
    }

    data class SimilarTeam(
        val name: String,
        val similarity: Float
    )
}
