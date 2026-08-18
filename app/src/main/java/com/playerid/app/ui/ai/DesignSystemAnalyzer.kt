package com.playerid.app.ui.ai

/**
 * Analyzes the Player-ID design system and returns a reference document
 * that can be injected into ChatGPT prompts so generated code is consistent
 * with the Spotr / Material 3 design language.
 */
object DesignSystemAnalyzer {

    /**
     * Returns a concise design-system reference string suitable for inclusion
     * in a ChatGPT system prompt.
     */
    fun buildDesignReference(): String = buildString {
        appendLine("=== SPOTR / PLAYER-ID DESIGN SYSTEM REFERENCE ===")
        appendLine()
        appendLine("## Colors (ui/theme/Color.kt)")
        appendLine("- SpotrPrimaryBlue   = #2563EB  (primary actions, buttons)")
        appendLine("- SpotrSuccessGreen  = #22C55E  (success states, live indicators)")
        appendLine("- SpotrHighlightOrange = #F97316 (warnings, highlights)")
        appendLine("- SpotrDarkSurface   = #111827  (dark backgrounds)")
        appendLine("- SpotrLightBackground = #F5F6FA (light backgrounds)")
        appendLine("- SpotrText          = #1F2937  (primary text on light)")
        appendLine("- SpotrSurfaceLight  = #FFFFFF  (card/surface on light)")
        appendLine("- SpotrSurfaceDark   = #0F172A  (card/surface on dark)")
        appendLine("- SpotrOutline       = #E5E7EB  (borders on light)")
        appendLine("- SpotrOutlineDark   = #273047  (borders on dark)")
        appendLine("- ErrorRed           = #EF4444  (errors, delete)")
        appendLine()
        appendLine("## Typography (ui/theme/Type.kt)")
        appendLine("- displayLarge  : Oswald Bold  40sp / 46sp")
        appendLine("- headlineLarge : Oswald Bold  32sp / 38sp")
        appendLine("- headlineMedium: Oswald Bold  28sp / 34sp")
        appendLine("- titleLarge    : Inter SemiBold 22sp / 28sp")
        appendLine("- titleMedium   : Inter SemiBold 18sp / 24sp")
        appendLine("- bodyLarge     : Inter Regular 16sp / 24sp")
        appendLine("- bodyMedium    : Inter Regular 14sp / 20sp")
        appendLine("- labelLarge    : Inter Medium  13sp / 18sp")
        appendLine("- labelSmall    : Inter Medium  11sp / 16sp")
        appendLine()
        appendLine("## Reusable Components (ui/components/)")
        appendLine("- SpotrCards    : PlayerCard, TeamCard — use for list items")
        appendLine("- SpotrHeaders  : Screen top-bar with back navigation")
        appendLine("- SpotrBottomNav: Bottom navigation bar (Camera/Validate/Team/Settings)")
        appendLine()
        appendLine("## Design Rules")
        appendLine("1. Always reference MaterialTheme.colorScheme tokens, not hardcoded colors.")
        appendLine("2. Use MaterialTheme.typography styles, not raw TextStyle.")
        appendLine("3. Round corners: large cards 16.dp, dialogs 28.dp, chips 8.dp.")
        appendLine("4. Prefer existing Spotr components before creating new ones.")
        appendLine("5. Padding: screen edges 16.dp, card content 12–16.dp.")
        appendLine("6. Use Scaffold + TopAppBar for full screens.")
        appendLine("7. Support both light and dark themes.")
        appendLine("8. All screens must be @Composable functions.")
        appendLine()
        appendLine("## Tech Stack")
        appendLine("- Jetpack Compose + Material 3")
        appendLine("- Single-activity architecture with Compose Navigation")
        appendLine("- State via ViewModel + StateFlow / collectAsState()")
        appendLine("- Kotlin coroutines for async work")
        appendLine("- minSdk 26, targetSdk 35, Kotlin 2.1")
        appendLine()
        appendLine("=== END OF DESIGN REFERENCE ===")
    }

    /**
     * Returns a ChatGPT system prompt that primes the model to act as a
     * Jetpack Compose UI designer for the Spotr / Player-ID app.
     */
    fun buildSystemPrompt(): String = buildString {
        appendLine("You are an expert Jetpack Compose UI designer for the Spotr / Player-ID Android app.")
        appendLine("When generating UI code:")
        appendLine("- Always produce complete, compilable @Composable functions.")
        appendLine("- Follow the Spotr design system documented below.")
        appendLine("- Use Material 3 components from androidx.compose.material3.*")
        appendLine("- Wrap code in a single ```kotlin ... ``` fenced block.")
        appendLine("- After the code, briefly explain the key design decisions.")
        appendLine()
        append(buildDesignReference())
    }
}
