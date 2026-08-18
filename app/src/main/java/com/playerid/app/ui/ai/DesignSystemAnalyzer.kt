package com.playerid.app.ui.ai

class DesignSystemAnalyzer {
    fun analyzeSources(
        colorSource: String,
        typeSource: String,
        componentSources: List<String>
    ): DesignSystemSnapshot {
        val colorTokens = COLOR_TOKEN_REGEX.findAll(colorSource)
            .map { it.groupValues[1] }
            .distinct()
            .toList()

        val typographyTokens = TYPE_TOKEN_REGEX.findAll(typeSource)
            .map { it.groupValues[1] }
            .distinct()
            .toList()

        val components = componentSources
            .flatMap { source -> COMPONENT_REGEX.findAll(source).map { it.groupValues[1] }.toList() }
            .distinct()

        return DesignSystemSnapshot(
            colorTokens = colorTokens,
            typographyTokens = typographyTokens,
            spotrComponents = components
        )
    }

    fun defaultSnapshot(): DesignSystemSnapshot = DesignSystemSnapshot(
        colorTokens = listOf(
            "SpotrPrimaryBlue",
            "SpotrSuccessGreen",
            "SpotrHighlightOrange",
            "SpotrLightBackground",
            "SpotrDarkSurface",
            "ErrorRed"
        ),
        typographyTokens = listOf(
            "displayLarge",
            "headlineLarge",
            "headlineMedium",
            "titleLarge",
            "titleMedium",
            "bodyLarge",
            "bodyMedium",
            "labelLarge",
            "labelSmall"
        ),
        spotrComponents = listOf(
            "SpotrPlayerCard",
            "SpotrActionCard",
            "SpotrStatsCard",
            "SpotrScreenHeader",
            "SpotrBottomNavigationBar"
        )
    )

    private companion object {
        val COLOR_TOKEN_REGEX = Regex(
            pattern = """^\s*val\s+([A-Z][A-Za-z0-9_]+)\s*=\s*Color\(""",
            option = RegexOption.MULTILINE
        )
        val TYPE_TOKEN_REGEX = Regex("""([A-Za-z]+)\s*=\s*TextStyle\(""")
        val COMPONENT_REGEX = Regex("""fun\s+(Spotr[A-Za-z0-9_]+)\(""")
    }
}

data class DesignSystemSnapshot(
    val colorTokens: List<String>,
    val typographyTokens: List<String>,
    val spotrComponents: List<String>
)
