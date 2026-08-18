package com.playerid.app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.playerid.app.ui.ai.DesignSystemAnalyzer
import com.playerid.app.ui.ai.DesignValidator
import com.playerid.app.ui.ai.OpenAIClient
import com.playerid.app.ui.ai.UIDesignGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DesignSystemExplorerUiState(
    val requirements: String = "Create a player profile screen with stats and recent clips",
    val generatedCode: String = "",
    val analysisSummary: String = "",
    val validationSummary: String = "",
    val isGenerating: Boolean = false
)

class DesignSystemExplorerViewModel(
    private val analyzer: DesignSystemAnalyzer,
    private val validator: DesignValidator,
    private val generator: UIDesignGenerator
) : ViewModel() {

    private val _uiState = MutableStateFlow(DesignSystemExplorerUiState())
    val uiState: StateFlow<DesignSystemExplorerUiState> = _uiState.asStateFlow()

    fun updateRequirements(requirements: String) {
        _uiState.update { it.copy(requirements = requirements) }
    }

    fun analyzeDesignSystem() {
        val snapshot = analyzer.defaultSnapshot()
        val summary = buildString {
            appendLine("Colors: ${snapshot.colorTokens.joinToString()}")
            appendLine("Typography: ${snapshot.typographyTokens.joinToString()}")
            appendLine("Components: ${snapshot.spotrComponents.joinToString()}")
        }
        _uiState.update { it.copy(analysisSummary = summary) }
    }

    fun generateUi() {
        if (_uiState.value.isGenerating) return
        _uiState.update { it.copy(isGenerating = true) }

        viewModelScope.launch {
            val requirements = _uiState.value.requirements
            val result = generator.generateComposeScreen(requirements)
            val code = result.fold(
                onSuccess = { it.composeCode },
                onFailure = { "Generation failed: ${it.message}" }
            )
            _uiState.update { it.copy(generatedCode = code, isGenerating = false) }
        }
    }

    fun validateGeneratedUi() {
        val generatedCode = _uiState.value.generatedCode
        if (generatedCode.isBlank()) return

        val result = validator.validate(generatedCode, analyzer.defaultSnapshot())
        val summary = buildString {
            appendLine("Valid: ${result.isValid}")
            if (result.warnings.isNotEmpty()) {
                appendLine("Warnings:")
                result.warnings.forEach { appendLine("• $it") }
            }
            if (result.suggestions.isNotEmpty()) {
                appendLine("Suggestions:")
                result.suggestions.forEach { appendLine("• $it") }
            }
        }
        _uiState.update { it.copy(validationSummary = summary) }
    }

    class Factory(
        private val analyzer: DesignSystemAnalyzer = DesignSystemAnalyzer(),
        private val validator: DesignValidator = DesignValidator(),
        private val generator: UIDesignGenerator = UIDesignGenerator(OpenAIClient())
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DesignSystemExplorerViewModel::class.java)) {
                return DesignSystemExplorerViewModel(analyzer, validator, generator) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
