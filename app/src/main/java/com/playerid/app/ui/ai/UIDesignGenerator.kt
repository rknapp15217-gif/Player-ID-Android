package com.playerid.app.ui.ai

import android.content.Context

/**
 * High-level façade that takes a natural-language UI requirement and returns
 * production-ready Jetpack Compose code by calling ChatGPT with the Spotr
 * design system context injected automatically.
 */
class UIDesignGenerator(context: Context) {

    private val client = OpenAIClient(context)
    private val systemPrompt = DesignSystemAnalyzer.buildSystemPrompt()

    /**
     * Generate Compose UI code from a natural-language [requirement].
     *
     * @param requirement  Plain-English description of the screen or component to create.
     * @param history      Optional prior messages for multi-turn refinement.
     * @return [Result] containing the raw ChatGPT response (includes code block + explanation).
     */
    suspend fun generate(
        requirement: String,
        history: List<ChatMessage> = emptyList()
    ): Result<String> {
        val messages = history + ChatMessage(
            role = ChatMessage.Role.USER,
            content = requirement
        )
        return client.chatCompletion(messages, systemPrompt)
    }

    /** Expose underlying client so the UI layer can check / save the API key. */
    val openAIClient: OpenAIClient get() = client
}
