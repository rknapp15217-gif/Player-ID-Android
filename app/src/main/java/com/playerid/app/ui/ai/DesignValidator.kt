package com.playerid.app.ui.ai

class DesignValidator {
    fun validate(
        composeSource: String,
        designSystem: DesignSystemSnapshot
    ): ValidationResult {
        val warnings = mutableListOf<String>()
        val suggestions = mutableListOf<String>()

        if (!composeSource.contains("MaterialTheme")) {
            warnings += "Use MaterialTheme typography and colorScheme for consistency."
        }

        HEX_COLOR_REGEX.findAll(composeSource).forEach { match ->
            warnings += "Hardcoded color found: ${match.value}. Prefer Spotr token or MaterialTheme colorScheme."
        }

        val missingTokens = designSystem.colorTokens.filterNot { composeSource.contains(it) }
        if (missingTokens.size == designSystem.colorTokens.size) {
            suggestions += "No known Spotr color tokens detected. Consider using: ${designSystem.colorTokens.joinToString()}"
        }

        if (!composeSource.contains("contentDescription")) {
            suggestions += "Add contentDescription values to interactive icons for accessibility."
        }

        return ValidationResult(
            isValid = warnings.isEmpty(),
            warnings = warnings,
            suggestions = suggestions
        )
    }

    private companion object {
        val HEX_COLOR_REGEX = Regex("""Color\(0[xX][0-9A-Fa-f]{8}\)""")
    }
}

data class ValidationResult(
    val isValid: Boolean,
    val warnings: List<String>,
    val suggestions: List<String>
)
