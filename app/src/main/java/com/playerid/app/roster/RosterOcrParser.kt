package com.playerid.app.roster

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.max
import kotlin.math.roundToInt

private val leadingNumberPattern = Regex("^\\s*#?(\\d{1,2})\\s*[\\-:\\.]?\\s*(.+)$")
private val trailingNumberPattern = Regex("^\\s*(.+?)\\s*[\\-:\\.]?\\s*#?(\\d{1,2})\\s*$")
private val middleNumberPattern = Regex("^\\s*(.+?)\\s+#?(\\d{1,2})\\s*(?:[-]\\s*(.+))?$")
private val jerseyNumberPattern = Regex("\\b#?(\\d{1,2})\\b")
private val headerTokens = setOf("name", "player", "number", "no", "jersey", "#")
private val academicYearPattern = Regex("\\b(freshman|sophomore|junior|senior|fr|so|jr|sr)\\b", RegexOption.IGNORE_CASE)
private val positionTokens = setOf(
    "g", "gk", "goalie", "goalkeeper",
    "d", "def", "defense",
    "m", "mid", "midfield",
    "a", "att", "attack", "attach",
    "lsm", "fo", "fogo",
    "m/fogo", "d/lsm", "a/m", "att/mid", "attack/midfield", "attach/midfield"
)
private val noiseTokens = setOf(
    // Minimal set - just obvious UI/navigation terms
    "home", "menu", "settings", "filter", "search", "sort", 
    "players", "roster", "team", "schedule", "account",
    "view", "edit", "add", "next", "back", "done", "cancel"
)

// Common English words that are unlikely to be names
private val commonWords = setOf(
    "the", "and", "for", "with", "from", "have", "this", "that",
    "will", "your", "all", "can", "get", "just", "now", "like",
    "our", "out", "see", "some", "time", "use", "way", "more"
)

suspend fun extractRosterCandidates(context: Context, imageUri: Uri): RosterOcrResult {
    val bitmap = loadBitmapFromUri(context, imageUri)
    val scaledBitmap = scaleBitmap(bitmap, 2048)
    return extractRosterCandidates(scaledBitmap)
}

suspend fun extractRosterCandidates(bitmap: Bitmap): RosterOcrResult {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    val image = InputImage.fromBitmap(bitmap, 0)
    val result = recognizer.process(image).await()
    recognizer.close()

    val lines = result.textBlocks.flatMap { block ->
        block.lines.map { it.text }
    }
    val blockLines = result.textBlocks.map { block ->
        block.lines.map { it.text }
    }

    val directCandidates = lines.mapNotNull { parseRosterLine(it) }
    val adjacentCandidates = parseAdjacentLinePairs(lines)
    val blockCandidates = blockLines.flatMap { parseBlockLinePairs(it) }
    
    // Combine and deduplicate
    val allCandidates = (directCandidates + adjacentCandidates + blockCandidates)
        .distinctBy { it.number + "|" + it.name.lowercase() }
    
    // ONLY include candidates that have jersey numbers
    // This eliminates advertising/food text which rarely has numbers
    val numberedCandidates = allCandidates.filter { it.number != "na" }
    
    return RosterOcrResult(candidates = numberedCandidates, rawLines = lines)
}

data class RosterOcrResult(
    val candidates: List<RosterCandidate>,
    val rawLines: List<String>
)

data class RosterCandidate(
    val number: String,
    val name: String,
    val academicYear: String? = null,
    val position: String? = null
)

private fun parseRosterLine(line: String): RosterCandidate? {
    val cleaned = line.replace("\\s+".toRegex(), " ").trim()
    if (cleaned.isBlank()) return null
    if (!cleaned.any { it.isDigit() } || !cleaned.any { it.isLetter() }) return null

    val leadingMatch = leadingNumberPattern.find(cleaned)
    val trailingMatch = trailingNumberPattern.find(cleaned)
    val middleMatch = middleNumberPattern.find(cleaned)
    var inlinePosition: String? = null

    val (number, name) = when {
        leadingMatch != null -> {
            val num = leadingMatch.groupValues[1]
            val rawName = leadingMatch.groupValues[2]
            num to rawName
        }
        trailingMatch != null -> {
            val rawName = trailingMatch.groupValues[1]
            val num = trailingMatch.groupValues[2]
            num to rawName
        }
        middleMatch != null -> {
            val rawName = middleMatch.groupValues[1]
            val num = middleMatch.groupValues[2]
            inlinePosition = middleMatch.groupValues.getOrNull(3)?.trim().orEmpty()
            num to rawName
        }
        else -> return null
    }

    val normalizedName = cleanName(name)
    if (normalizedName.length < 2) return null
    if (normalizedName.any { it.isDigit() }) return null

    val (academicYear, cleanedName) = extractAcademicYear(normalizedName)
    val finalName = if (cleanedName.length >= 2) cleanedName else normalizedName
    if (finalName.any { it.isDigit() }) return null
    val normalizedToken = finalName.lowercase().replace("[\\W_]".toRegex(), "")
    if (normalizedToken == "am" || normalizedToken == "pm") return null

    val headerCheck = finalName.lowercase().replace("[\\W_]".toRegex(), "")
    if (headerCheck in headerTokens) return null
    if (looksLikePositionOnly(finalName)) return null
    if (!looksLikeNameLine(finalName)) return null

    val parsedNumber = number.toIntOrNull() ?: return null
    if (parsedNumber < 0 || parsedNumber > 99) return null

    return RosterCandidate(
        number = parsedNumber.toString(),
        name = finalName,
        academicYear = academicYear,
        position = inlinePosition?.let { normalizePosition(it) }
    )
}

private fun parseAdjacentLinePairs(lines: List<String>): List<RosterCandidate> {
    val filtered = lines.map { it.trim().replace("\\s+".toRegex(), " ") }
        .filter { it.isNotBlank() }
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
        // Handle name + position (without number)
        if (looksLikeNameLine(first) && looksLikeDetailLineWithoutNumber(second)) {
            results.add(
                RosterCandidate(
                    number = "na",
                    name = cleanName(first),
                    position = extractPosition(second)
                )
            )
            index += 2
            continue
        }
        if (looksLikeDetailLine(first) && looksLikeNameLine(second)) {
            val prevWasName = index > 0 && looksLikeNameLine(filtered[index - 1])
            val nextIsDetail = index + 2 < filtered.size && looksLikeDetailLine(filtered[index + 2])
            if (!prevWasName) {
                if (!nextIsDetail) {
                    results.addAll(tryPairLines(first, second))
                    index += 2
                    continue
                }
            }
        }
        index += 1
    }
    return results
}

private fun parseBlockLinePairs(lines: List<String>): List<RosterCandidate> {
    val filtered = lines.map { it.trim().replace("\\s+".toRegex(), " ") }
        .filter { it.isNotBlank() }
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
            val prevWasName = index > 0 && looksLikeNameLine(filtered[index - 1])
            val nextIsDetail = index + 2 < filtered.size && looksLikeDetailLine(filtered[index + 2])
            if (!prevWasName) {
                if (!nextIsDetail) {
                    results.addAll(tryPairLines(first, second))
                    index += 2
                    continue
                }
            }
        }
        index += 1
    }
    return results
}

private fun tryPairLines(first: String, second: String): List<RosterCandidate> {
    val results = mutableListOf<RosterCandidate>()
    if (looksLikeNameLine(first) && looksLikeDetailLine(second)) {
        val number = extractNumber(second) ?: return results
        results.add(
            RosterCandidate(
                number = number,
                name = cleanName(first),
                position = extractPosition(second)
            )
        )
        return results
    }
    if (looksLikeDetailLine(first) && looksLikeNameLine(second)) {
        val number = extractNumber(first) ?: return results
        results.add(
            RosterCandidate(
                number = number,
                name = cleanName(second),
                position = extractPosition(first)
            )
        )
    }
    if (results.isEmpty() && looksLikeNameLine(first) && looksLikeDetailLineWithoutNumber(second)) {
        results.add(
            RosterCandidate(
                number = "na",
                name = cleanName(first),
                position = extractPosition(second)
            )
        )
    }
    return results
}

private fun looksLikeNameLine(text: String): Boolean {
    if (text.length < 2) return false
    if (text.any { it.isDigit() }) return false
    // Filter out ALL CAPS UI text (names are typically title case)
    val letterCount = text.count { it.isLetter() }
    val upperCount = text.count { it.isUpperCase() }
    if (letterCount > 0 && upperCount == letterCount && text.contains(" ")) return false
    val headerCheck = text.lowercase().replace("[\\W_]".toRegex(), "")
    if (headerCheck in headerTokens) return false
    if (looksLikePositionOnly(text)) return false
    val tokens = text.split(" ")
        .map { it.replace("[^A-Za-z']".toRegex(), "") }
        .filter { it.isNotBlank() }
    if (tokens.size < 2 || tokens.size > 4) return false
    if (tokens.any { it.length < 2 }) return false
    if (tokens.any { it.lowercase() in noiseTokens }) return false
    // Filter out common English words
    if (tokens.any { it.lowercase() in commonWords }) return false
    // At least one token should be properly capitalized (like a name)
    val hasProperCapitalization = tokens.any { token ->
        token.isNotEmpty() && token[0].isUpperCase() && token.drop(1).any { it.isLowerCase() }
    }
    if (!hasProperCapitalization) return false
    // Name parts should look uncommon (have vowel-consonant patterns, not all consonants)
    val hasReasonableTokens = tokens.all { token ->
        val lower = token.lowercase()
        val hasVowel = lower.any { it in "aeiou" }
        val consonantCount = lower.count { it in "bcdfghjklmnpqrstvwxyz" }
        val vowelCount = lower.count { it in "aeiou" }
        // Reject if all consonants or looks like abbreviation
        hasVowel && consonantCount <= token.length - 1
    }
    if (!hasReasonableTokens) return false
    return text.any { it.isLetter() }
}

private fun looksLikePositionOnly(text: String): Boolean {
    val normalized = text.lowercase().replace("[\\W_]".toRegex(), "")
    return normalized in positionTokens
}

private fun looksLikeDetailLine(text: String): Boolean {
    val hasNumber = extractNumber(text) != null
    if (!hasNumber) return false
    val hasPosition = extractPosition(text) != null
    val hasDash = text.contains("-")
    return hasPosition || hasDash || text.contains("#")
}

private fun looksLikeDetailLineWithoutNumber(text: String): Boolean {
    if (extractNumber(text) != null) return false
    val hasPosition = extractPosition(text) != null
    val hasDash = text.contains("-")
    return hasPosition || hasDash
}

private fun parseNameOnlyLines(lines: List<String>): List<RosterCandidate> {
    val filtered = lines.map { it.trim().replace("\\s+".toRegex(), " ") }
        .filter { it.isNotBlank() }
    if (filtered.isEmpty()) return emptyList()
    val results = mutableListOf<RosterCandidate>()
    for (index in filtered.indices) {
        val current = filtered[index]
        if (!looksLikeNameLine(current)) continue
        val prev = filtered.getOrNull(index - 1)
        val next = filtered.getOrNull(index + 1)
        // Only skip if there's a number paired with this name (handled by parseAdjacentLinePairs)
        if (prev != null && looksLikeDetailLine(prev) && extractNumber(prev) != null) continue
        if (next != null && looksLikeDetailLine(next) && extractNumber(next) != null) continue
        results.add(
            RosterCandidate(
                number = "na",
                name = cleanName(current)
            )
        )
    }
    return results
}

private fun cleanName(text: String): String {
    return text
        .replace("(C)", "", ignoreCase = true)
        .replace("\\s+".toRegex(), " ")
        .trim()
}

private fun extractNumber(text: String): String? {
    val match = jerseyNumberPattern.find(text) ?: return null
    return match.groupValues[1].toIntOrNull()?.toString()
}

private fun extractPosition(text: String): String? {
    val tokens = text.uppercase().split(Regex("[^A-Z/]+"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
    for (token in tokens) {
        val normalized = normalizePosition(token)
        if (normalized != null) return normalized
    }
    return null
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
    val cleaned = text.replace(academicYearPattern, " ").replace("\\s+".toRegex(), " ").trim()
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

private fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    } else {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}

private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
    val maxSide = max(bitmap.width, bitmap.height)
    if (maxSide <= maxDimension) return bitmap

    val scale = maxDimension.toFloat() / maxSide.toFloat()
    val targetWidth = (bitmap.width * scale).roundToInt()
    val targetHeight = (bitmap.height * scale).roundToInt()

    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}
