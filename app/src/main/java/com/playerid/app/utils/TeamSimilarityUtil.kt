package com.playerid.app.utils

import kotlin.math.max
import kotlin.math.min

/**
 * Utility class for detecting similar team names to prevent duplicates
 * Uses fuzzy matching algorithms to identify potential duplicates
 */
class TeamSimilarityUtil {
    
    companion object {
        private const val SIMILARITY_THRESHOLD = 0.8f // 80% similarity threshold
        private const val HIGH_SIMILARITY_THRESHOLD = 0.9f // 90% for high confidence matches
        
        /**
         * Calculate Levenshtein distance between two strings
         */
        private fun levenshteinDistance(str1: String, str2: String): Int {
            val len1 = str1.length
            val len2 = str2.length
            val dp = Array(len1 + 1) { IntArray(len2 + 1) }
            
            for (i in 0..len1) dp[i][0] = i
            for (j in 0..len2) dp[0][j] = j
            
            for (i in 1..len1) {
                for (j in 1..len2) {
                    val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                    dp[i][j] = min(
                        min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                    )
                }
            }
            
            return dp[len1][len2]
        }
        
        /**
         * Calculate similarity ratio between two strings (0.0 - 1.0)
         */
        private fun calculateSimilarity(str1: String, str2: String): Float {
            val normalized1 = normalizeTeamName(str1)
            val normalized2 = normalizeTeamName(str2)
            
            val maxLen = max(normalized1.length, normalized2.length)
            if (maxLen == 0) return 1.0f
            
            val distance = levenshteinDistance(normalized1, normalized2)
            return 1.0f - (distance.toFloat() / maxLen)
        }
        
        /**
         * Normalize team name for comparison by:
         * - Converting to lowercase
         * - Removing common words and abbreviations
         * - Trimming whitespace
         * - Standardizing common terms
         */
        private fun normalizeTeamName(name: String): String {
            return name.lowercase()
                .replace(Regex("\\b(team|club|fc|athletic|sports?)\\b"), "")
                .replace(Regex("\\b(high school|hs|middle school|ms|elementary|elem)\\b"), "school")
                .replace(Regex("\\b(lacrosse|lax)\\b"), "lacrosse")
                .replace(Regex("\\b(soccer|football)\\b"), "soccer")
                .replace(Regex("\\b(basketball|bball)\\b"), "basketball")
                .replace(Regex("\\b(baseball|ball)\\b"), "baseball")
                .replace(Regex("\\b(volleyball|vball)\\b"), "volleyball")
                .replace(Regex("\\b(u\\d+|under \\d+)\\b"), "") // Remove age groups for similarity
                .replace(Regex("\\s+"), " ") // Normalize whitespace
                .trim()
        }
        
        /**
         * Check if two team names are similar enough to be potential duplicates
         */
        fun areTeamsSimilar(name1: String, name2: String): Boolean {
            return calculateSimilarity(name1, name2) >= SIMILARITY_THRESHOLD
        }
        
        /**
         * Check if two team names are highly similar (likely duplicates)
         */
        fun areTeamsHighlySimilar(name1: String, name2: String): Boolean {
            return calculateSimilarity(name1, name2) >= HIGH_SIMILARITY_THRESHOLD
        }
        
        /**
         * Find similar teams from a list of existing team names
         */
        fun findSimilarTeams(newTeamName: String, existingTeams: List<String>): List<SimilarTeam> {
            return existingTeams.mapNotNull { existingName ->
                val similarity = calculateSimilarity(newTeamName, existingName)
                if (similarity >= SIMILARITY_THRESHOLD) {
                    SimilarTeam(existingName, similarity)
                } else null
            }.sortedByDescending { it.similarity }
        }
        
        /**
         * Generate suggestions for team name improvements
         */
        fun generateTeamNameSuggestions(teamName: String): List<String> {
            val suggestions = mutableListOf<String>()
            val normalized = normalizeTeamName(teamName)
            
            // Suggest full names for common abbreviations
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