package com.playerid.app.ui.ai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.playerid.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OpenAIClient(
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val gson: Gson = Gson(),
    private val apiKeyProvider: () -> String = { BuildConfig.OPENAI_API_KEY }
) {
    suspend fun generateChatCompletion(
        systemPrompt: String,
        userPrompt: String,
        model: String = "gpt-4o-mini",
        temperature: Double = 0.2,
        maxTokens: Int = 1200
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!BuildConfig.DEBUG) {
            return@withContext Result.failure(
                IllegalStateException("OpenAI client is debug-only. Use a backend proxy for release builds.")
            )
        }

        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("OpenAI API key is missing"))
        }

        val payload = ChatCompletionRequest(
            model = model,
            temperature = temperature,
            maxTokens = maxTokens,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            )
        )

        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", authHeader(apiKey))
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(payload).toRequestBody(JSON))
            .build()

        runCatching {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("OpenAI request failed (${response.code}): $body")
                }

                val completion = gson.fromJson(body, ChatCompletionResponse::class.java)
                completion.choices.firstOrNull()?.message?.content
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: error("OpenAI response did not include message content")
            }
        }
    }

    private companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val BEARER_PREFIX = "Bearer "

        fun authHeader(apiKey: String): String = BEARER_PREFIX + apiKey
    }
}

data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double,
    @SerializedName("max_tokens") val maxTokens: Int
)

data class ChatMessage(
    val role: String,
    val content: String
)

data class ChatCompletionResponse(
    val choices: List<ChatChoice> = emptyList()
)

data class ChatChoice(
    val message: ChatMessage = ChatMessage(role = "assistant", content = "")
)
