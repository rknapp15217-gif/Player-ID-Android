package com.playerid.app.ui.ai

class UIDesignGenerator(
    private val openAIClient: OpenAIClient,
    private val designSystemAnalyzer: DesignSystemAnalyzer = DesignSystemAnalyzer()
) {
    suspend fun generateComposeScreen(requirements: String): Result<GeneratedUiDesign> {
        val snapshot = designSystemAnalyzer.defaultSnapshot()
        val prompt = DesignSystemPrompts.designPrompt(requirements, snapshot)

        return openAIClient.generateChatCompletion(
            systemPrompt = DesignSystemPrompts.SYSTEM_PROMPT,
            userPrompt = prompt
        ).map { content ->
            GeneratedUiDesign(
                composeCode = content,
                appliedColorTokens = snapshot.colorTokens,
                referencedComponents = snapshot.spotrComponents
            )
        }
    }
}

data class GeneratedUiDesign(
    val composeCode: String,
    val appliedColorTokens: List<String>,
    val referencedComponents: List<String>
)
