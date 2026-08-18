package com.playerid.app.ui.ai

object DesignSystemPrompts {
    const val SYSTEM_PROMPT = """
You are the Player-ID Android UI designer.
Generate Jetpack Compose code only.
Follow Material 3 and existing Spotr conventions.
Prefer MaterialTheme.colorScheme + documented Spotr tokens.
Prefer existing Spotr* reusable components before creating new ones.
Return Kotlin Compose screen code with clear composable boundaries.
"""

    fun designPrompt(requirements: String, designSystem: DesignSystemSnapshot): String {
        return buildString {
            appendLine("Screen requirements:")
            appendLine(requirements)
            appendLine()
            appendLine("Available color tokens: ${designSystem.colorTokens.joinToString()}")
            appendLine("Available typography tokens: ${designSystem.typographyTokens.joinToString()}")
            appendLine("Available Spotr components: ${designSystem.spotrComponents.joinToString()}")
            appendLine()
            appendLine("Constraints:")
            appendLine("1) Use Material 3 composables.")
            appendLine("2) Avoid hardcoded hex colors when a token exists.")
            appendLine("3) Include accessibility content descriptions where relevant.")
        }
    }
}
