package com.playerid.app.domain.team

enum class MemoryQuestionKind { COUNT, OPPONENTS, FIRST }

data class MemoryQuestion(val kind: MemoryQuestionKind, val criteria: String, val asksLocation: Boolean = false)

/** Questions become metadata queries; answers are computed only from the resulting records. */
fun parseMemoryQuestion(text: String): MemoryQuestion? {
    if (parseRosterQuestion(text) != null) return null
    val query = text.trim().trimEnd('?', '.', '!')
        .replace(Regex("^(?:(?:can|could|would) you\\s+)?(?:please\\s+)?(?:tell me\\s+)?", RegexOption.IGNORE_CASE), "")
    fun criteria(value: String) = value
        .replace(Regex("\\b(did|does|has|have|had|do|there|are|is|were|was|record|recorded|capture|captured)\\b",
            RegexOption.IGNORE_CASE), " ")
        .replace(Regex("\\s+"), " ").trim()
    val count = Regex("^(?:how many|count|what is the number of|what['’]s the number of)\\s+(.+)$",
        RegexOption.IGNORE_CASE).matchEntire(query)
    if (count != null) return criteria(count.groupValues[1]).takeIf(String::isNotBlank)
        ?.let { MemoryQuestion(MemoryQuestionKind.COUNT, it) }
    val opponents = Regex("^(?:(?:which|what)\\s+(?:teams|opponents)|who)\\s+(.+?)\\s+(?:against|vs|versus)(?:\\s+(.+))?$",
        RegexOption.IGNORE_CASE).matchEntire(query)
    if (opponents != null) return MemoryQuestion(MemoryQuestionKind.OPPONENTS,
        criteria("${opponents.groupValues[1]} ${opponents.groupValues[2]}")
            .replace(Regex("\\b(his|her|their)\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ").trim())
    val first = Regex("^(what|when|where|where\\s+and\\s+when|when\\s+and\\s+where)\\s+(?:was|is)\\s+(.+\\b(?:first|earliest)\\b.+)$",
        RegexOption.IGNORE_CASE).matchEntire(query)
    if (first != null) return MemoryQuestion(MemoryQuestionKind.FIRST,
        criteria(first.groupValues[2]), asksLocation = first.groupValues[1].contains("where", true))
    return null
}

data class MemoryQuestionAnswer(val title: String, val detail: String)

fun answerMemoryQuestion(
    query: AskPlayloomQuery,
    count: Int,
    reels: Boolean,
    opponents: List<String> = emptyList(),
    firstDate: String? = null,
    firstLocation: String? = null
): MemoryQuestionAnswer? {
    val question = query.question ?: return null
    if (query.issue != null) return null
    if (count == 0 && (reels || question.kind != MemoryQuestionKind.COUNT)) {
        return MemoryQuestionAnswer("No matching saved ${if (reels) "reels" else "memories"}", "")
    }
    if (reels) return MemoryQuestionAnswer("$count saved ${if (count == 1) "reel" else "reels"}",
        "Matching reels are shown below. This is not a count of individual plays.")
    val singular = when (query.filters.momentTag) {
        "Score" -> "goal"
        "Faceoff Win" -> "faceoff win"
        "Save" -> "save"
        "Assist" -> "assist"
        "Defensive Stop" -> "defensive stop"
        "Ground Ball" -> "ground ball"
        else -> "memory"
    }
    val event = if (count == 1) singular else if (singular == "memory") "memories" else "${singular}s"
    if (count == 0) return MemoryQuestionAnswer("0 recorded $event", "")
    val countAnswer = MemoryQuestionAnswer("$count recorded $event",
        "Matching memories are shown below. Recorded moments, not complete game statistics.")
    return when (question.kind) {
        MemoryQuestionKind.COUNT -> countAnswer
        MemoryQuestionKind.FIRST -> when {
            query.filters.resultSelection != MemoryResultSelection.FIRST -> countAnswer
            firstDate == null -> MemoryQuestionAnswer("Recorded $singular",
                "Its capture date isn't saved, so we can't determine whether it was first.")
            else -> MemoryQuestionAnswer("First recorded $singular",
                if (question.asksLocation) "$firstDate · ${firstLocation?.takeIf(String::isNotBlank) ?: "Location not saved"}"
                else "$firstDate · Earliest matching dated memory. Open it below to view it.")
        }
        MemoryQuestionKind.OPPONENTS -> {
            if (query.filters.resultSelection == MemoryResultSelection.FIRST && firstDate == null) {
                return MemoryQuestionAnswer("First recorded $singular not determined",
                    "Capture date not saved.")
            }
            val names = opponents.filter(String::isNotBlank).distinctBy(::opponentKey).sortedBy { it.lowercase() }
            if (query.filters.resultSelection == MemoryResultSelection.FIRST) {
                return MemoryQuestionAnswer(
                    names.singleOrNull() ?: if (names.isEmpty()) "Opponent not saved" else names.joinToString(" · "),
                    "First recorded $singular · $firstDate"
                )
            }
            MemoryQuestionAnswer(
                if (names.isEmpty()) "Opponent not saved" else "${names.size} recorded ${if (names.size == 1) "opponent" else "opponents"}",
                (if (names.isEmpty()) "Matching memories have no saved opponent." else names.take(5).joinToString(" · ") +
                    if (names.size > 5) " · ${names.size - 5} more in the matching memories" else "") +
                    "\nNot complete game statistics. Memories without saved opponents aren't listed."
            )
        }
    }
}
