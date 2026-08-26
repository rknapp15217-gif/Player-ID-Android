package com.playerid.app.roster

private val leadingNumberPattern = Regex("^\\s*#?(\\d{1,2})\\s*[\\-:\\.]?\\s*(.+)$")
private val trailingNumberPattern = Regex("^\\s*(.+?)\\s*[\\-:\\.]?\\s*#?(\\d{1,2})\\s*$")
private val middleNumberPattern = Regex("^\\s*(.+?)\\s+#?(\\d{1,2})\\s*(?:[-]\\s*(.+))?$")
private val jerseyNumberPattern = Regex("\\b#?(\\d{1,2})\\b")
private val headerTokens = setOf("name", "player", "number", "no", "jersey", "#")
private val academicYearPattern = Regex(
    "\\b(freshman|sophomore|junior|senior|fr|so|jr|sr)\\b",
    RegexOption.IGNORE_CASE
)
private val positionTokens = setOf(
    "g", "gk", "goalie", "goalkeeper",
    "d", "def", "defense",
    "m", "mid", "midfield",
    "a", "att", "attack", "attach",
    "lsm", "fo", "fogo",
    "m/fogo", "d/lsm", "a/m", "att/mid", "attack/midfield", "attach/midfield"
)
private val noiseTokens = setOf(
    "home", "menu", "settings", "filter", "search", "sort",
    "players", "roster", "team", "schedule", "account",
    "view", "edit", "add", "next", "back", "done", "cancel"
)
private val commonWords = setOf(
    "the", "and", "for", "with", "from", "have", "this", "that",
    "will", "your", "all", "can", "get", "just", "now", "like",
    "our", "out", "see", "some", "time", "use", "way", "more"
)

fun parseRosterText(
    lines: List<String>,
    blockLines: List<List<String>> = emptyList()
): RosterOcrResult {
    val directCandidates = lines.mapNotNull(::parseRosterLine)
    val adjacentCandidates = parseAdjacentLinePairs(lines)
    val blockCandidates = blockLines.flatMap(::parseBlockLinePairs)
    val candidates = (directCandidates + adjacentCandidates + blockCandidates)
        .distinctBy { it.number + "|" + it.name.lowercase() }
        .filter { it.number != "na" }

    return RosterOcrResult(candidates = candidates, rawLines = lines)
}

private fun parseRosterLine(line: String): RosterCandidate? {
    val cleaned = line.replace(Regex("\\s+"), " ").trim()
    if (cleaned.isBlank()) return null
    if (!cleaned.any(Char::isDigit) || !cleaned.any(Char::isLetter)) return null

    val leadingMatch = leadingNumberPattern.find(cleaned)
    val trailingMatch = trailingNumberPattern.find(cleaned)
    val middleMatch = middleNumberPattern.find(cleaned)
    var inlinePosition: String? = null

    val (number, name) = when {
        leadingMatch != null -> leadingMatch.groupValues[1] to leadingMatch.groupValues[2]
        trailingMatch != null -> trailingMatch.groupValues[2] to trailingMatch.groupValues[1]
        middleMatch != null -> {
            inlinePosition = middleMatch.groupValues.getOrNull(3)?.trim().orEmpty()
            middleMatch.groupValues[2] to middleMatch.groupValues[1]
        }
        else -> return null
    }

    val normalizedName = cleanName(name)
    if (normalizedName.length < 2 || normalizedName.any(Char::isDigit)) return null

    val (academicYear, cleanedName) = extractAcademicYear(normalizedName)
    val finalName = cleanedName.takeIf { it.length >= 2 } ?: normalizedName
    if (finalName.any(Char::isDigit)) return null
    val normalizedToken = finalName.lowercase().replace(Regex("[\\W_]"), "")
    if (normalizedToken == "am" || normalizedToken == "pm") return null
    if (normalizedToken in headerTokens || looksLikePositionOnly(finalName) || !looksLikeNameLine(finalName)) {
        return null
    }

    val parsedNumber = number.toIntOrNull() ?: return null
    if (parsedNumber !in 0..99) return null

    return RosterCandidate(
        name = finalName,
        number = parsedNumber.toString(),
        position = inlinePosition?.let(::normalizePosition).orEmpty(),
        graduationYear = null,
        academicYear = academicYear
    )
}

private fun parseAdjacentLinePairs(lines: List<String>): List<RosterCandidate> {
    val filtered = normalizedLines(lines)
    if (filtered.size < 2) return emptyList()
    val results = mutableListOf<RosterCandidate>()
    var index = 0
    while (index < filtered.size - 1) {
        val first = filtered[index]
        val second = filtered[index + 1]
        if (looksLikeNameLine(first) && looksLikeDetailLine(second)) {
            results.addAll(tryPairLines(first, second))
            index += 2
            continue
        }
        if (looksLikeNameLine(first) && looksLikeDetailLineWithoutNumber(second)) {
            results.add(candidateWithoutNumber(first, second))
            index += 2
            continue
        }
        if (looksLikeDetailLine(first) && looksLikeNameLine(second)) {
            val previousWasName = index > 0 && looksLikeNameLine(filtered[index - 1])
            val nextIsDetail = index + 2 < filtered.size && looksLikeDetailLine(filtered[index + 2])
            if (!previousWasName && !nextIsDetail) {
                results.addAll(tryPairLines(first, second))
                index += 2
                continue
            }
        }
        index += 1
    }
    return results
}

private fun parseBlockLinePairs(lines: List<String>): List<RosterCandidate> {
    val filtered = normalizedLines(lines)
    if (filtered.size < 2) return emptyList()
    val results = mutableListOf<RosterCandidate>()
    var index = 0
    while (index < filtered.size - 1) {
        val first = filtered[index]
        val second = filtered[index + 1]
        if (looksLikeNameLine(first) && looksLikeDetailLine(second)) {
            results.addAll(tryPairLines(first, second))
            index += 2
            continue
        }
        if (looksLikeDetailLine(first) && looksLikeNameLine(second)) {
            val previousWasName = index > 0 && looksLikeNameLine(filtered[index - 1])
            val nextIsDetail = index + 2 < filtered.size && looksLikeDetailLine(filtered[index + 2])
            if (!previousWasName && !nextIsDetail) {
                results.addAll(tryPairLines(first, second))
                index += 2
                continue
            }
        }
        index += 1
    }
    return results
}

private fun tryPairLines(first: String, second: String): List<RosterCandidate> {
    if (looksLikeNameLine(first) && looksLikeDetailLine(second)) {
        val number = extractNumber(second) ?: return emptyList()
        return listOf(candidate(first, number, second))
    }
    if (looksLikeDetailLine(first) && looksLikeNameLine(second)) {
        val number = extractNumber(first) ?: return emptyList()
        return listOf(candidate(second, number, first))
    }
    if (looksLikeNameLine(first) && looksLikeDetailLineWithoutNumber(second)) {
        return listOf(candidateWithoutNumber(first, second))
    }
    return emptyList()
}

private fun candidate(name: String, number: String, details: String) = RosterCandidate(
    name = cleanName(name),
    number = number,
    position = extractPosition(details).orEmpty(),
    graduationYear = null,
    academicYear = null
)

private fun candidateWithoutNumber(name: String, details: String) = candidate(name, "na", details)

private fun normalizedLines(lines: List<String>) = lines
    .map { it.trim().replace(Regex("\\s+"), " ") }
    .filter(String::isNotBlank)

private fun looksLikeNameLine(text: String): Boolean {
    if (text.length < 2 || text.any(Char::isDigit)) return false
    val letterCount = text.count(Char::isLetter)
    val uppercaseCount = text.count(Char::isUpperCase)
    if (letterCount > 0 && uppercaseCount == letterCount && text.contains(" ")) return false
    val headerCheck = text.lowercase().replace(Regex("[\\W_]"), "")
    if (headerCheck in headerTokens || looksLikePositionOnly(text)) return false

    val tokens = text.split(" ")
        .map { it.replace(Regex("[^A-Za-z']"), "") }
        .filter(String::isNotBlank)
    if (tokens.size !in 2..4 || tokens.any { it.length < 2 }) return false
    if (tokens.any { it.lowercase() in noiseTokens || it.lowercase() in commonWords }) return false
    if (tokens.none { token ->
            token.firstOrNull()?.isUpperCase() == true && token.drop(1).any(Char::isLowerCase)
        }) return false

    return tokens.all { token ->
        val lower = token.lowercase()
        val hasVowel = lower.any { it in "aeiou" }
        val consonantCount = lower.count { it in "bcdfghjklmnpqrstvwxyz" }
        hasVowel && consonantCount <= token.length - 1
    }
}

private fun looksLikePositionOnly(text: String): Boolean {
    return text.lowercase().replace(Regex("[\\W_]"), "") in positionTokens
}

private fun looksLikeDetailLine(text: String): Boolean {
    if (extractNumber(text) == null) return false
    return extractPosition(text) != null || text.contains("-") || text.contains("#")
}

private fun looksLikeDetailLineWithoutNumber(text: String): Boolean {
    if (extractNumber(text) != null) return false
    return extractPosition(text) != null || text.contains("-")
}

private fun cleanName(text: String): String {
    return text.replace("(C)", "", ignoreCase = true).replace(Regex("\\s+"), " ").trim()
}

private fun extractNumber(text: String): String? {
    val match = jerseyNumberPattern.find(text) ?: return null
    return match.groupValues[1].toIntOrNull()?.toString()
}

private fun extractPosition(text: String): String? {
    val tokens = text.uppercase().split(Regex("[^A-Z/]+"))
        .map(String::trim)
        .filter(String::isNotBlank)
    return tokens.firstNotNullOfOrNull(::normalizePosition)
}

private fun normalizePosition(raw: String): String? {
    return when (raw.trim().uppercase()) {
        "G", "GK", "GOALIE", "GOALKEEPER" -> "Goalie"
        "D", "DEF", "DEFENSE" -> "Defense"
        "M", "MID", "MIDFIELD" -> "Midfield"
        "A", "ATT", "ATTACK", "ATTACH" -> "Attack"
        "LSM" -> "LSM"
        "FO", "FOGO" -> "FOGO"
        "M/FOGO" -> "Midfield/FOGO"
        "D/LSM" -> "Defense/LSM"
        "A/M", "ATT/MID", "ATTACK/MIDFIELD", "ATTACH/MIDFIELD" -> "Attack/Midfield"
        else -> null
    }
}

private fun extractAcademicYear(text: String): Pair<String?, String> {
    val match = academicYearPattern.find(text) ?: return null to text
    val normalized = normalizeAcademicYear(match.value) ?: return null to text
    val cleaned = text.replace(academicYearPattern, " ").replace(Regex("\\s+"), " ").trim()
    return normalized to cleaned
}

private fun normalizeAcademicYear(raw: String): String? {
    return when (raw.trim().lowercase()) {
        "freshman", "fr" -> "Freshman"
        "sophomore", "so" -> "Sophomore"
        "junior", "jr" -> "Junior"
        "senior", "sr" -> "Senior"
        else -> null
    }
}
