package com.playerid.app.ui.ai

import org.junit.Assert.assertTrue
import org.junit.Test

class DesignSystemAnalyzerTest {
    private val analyzer = DesignSystemAnalyzer()

    @Test
    fun extractsTokensAndSpotrComponents() {
        val snapshot = analyzer.analyzeSources(
            colorSource = """
                val SpotrPrimaryBlue = Color(0xFF2563EB)
                val SpotrSuccessGreen = Color(0xFF22C55E)
            """.trimIndent(),
            typeSource = """
                headlineMedium = TextStyle(fontSize = 24.sp)
                bodyLarge = TextStyle(fontSize = 16.sp)
            """.trimIndent(),
            componentSources = listOf(
                "fun SpotrScreenHeader() {}",
                "fun SpotrPlayerCard() {}"
            )
        )

        assertTrue(snapshot.colorTokens.contains("SpotrPrimaryBlue"))
        assertTrue(snapshot.typographyTokens.contains("headlineMedium"))
        assertTrue(snapshot.spotrComponents.contains("SpotrScreenHeader"))
    }
}
