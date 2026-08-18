package com.playerid.app.ui.ai

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DesignValidatorTest {
    private val validator = DesignValidator()
    private val snapshot = DesignSystemAnalyzer().defaultSnapshot()

    @Test
    fun flagsHardcodedHexColors() {
        val source = """
            @Composable
            fun Test() {
                Text("Hello", color = Color(0xFF1976D2))
            }
        """.trimIndent()

        val result = validator.validate(source, snapshot)

        assertFalse(result.isValid)
        assertTrue(result.warnings.any { it.contains("Hardcoded color") })
    }

    @Test
    fun allowsMaterialThemeUsageWithoutWarnings() {
        val source = """
            @Composable
            fun Test() {
                Text(
                    text = "Hello",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        """.trimIndent()

        val result = validator.validate(source, snapshot)

        assertTrue(result.isValid)
    }
}
