package com.playerid.app.ui.ai

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Secure OpenAI API client for ChatGPT integration.
 *
 * API key is stored in EncryptedSharedPreferences and never committed to source control.
 * Set the key once via [saveApiKey], then call [chatCompletion] to query ChatGPT.
 */
class OpenAIClient(private val context: Context) {

    companion object {
        private const val TAG = "OpenAIClient"
        private const val PREFS_FILE = "openai_secure_prefs"
        private const val KEY_API_KEY = "openai_api_key"
        private const val BASE_URL = "https://api.openai.com/v1/chat/completions"
        private const val MODEL = "gpt-4-turbo"
        private const val MAX_RETRIES = 3
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Persist the API key securely; call once during onboarding. */
    fun saveApiKey(apiKey: String) {
        encryptedPrefs.edit().putString(KEY_API_KEY, apiKey).apply()
    }

    /** Returns true if an API key has been saved. */
    fun hasApiKey(): Boolean = encryptedPrefs.getString(KEY_API_KEY, null).isNullOrBlank().not()

    /** Remove the stored API key. */
    fun clearApiKey() {
        encryptedPrefs.edit().remove(KEY_API_KEY).apply()
    }

    /**
     * Send a list of messages to ChatGPT and return the assistant's reply.
     *
     * @param messages List of [ChatMessage] objects forming the conversation history.
     * @param systemPrompt Optional system-level instruction prepended to the conversation.
     * @return The assistant's response text, or an error description on failure.
     */
    suspend fun chatCompletion(
        messages: List<ChatMessage>,
        systemPrompt: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = encryptedPrefs.getString(KEY_API_KEY, null)
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("OpenAI API key not configured. Please add it in Settings.")
            )
        }

        val messagesArray = JSONArray()
        systemPrompt?.let {
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", it)
            })
        }
        messages.forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", msg.role.value)
                put("content", msg.content)
            })
        }

        val requestBody = JSONObject().apply {
            put("model", MODEL)
            put("messages", messagesArray)
            put("max_tokens", 2048)
            put("temperature", 0.7)
        }.toString().toRequestBody("application/json".toMediaType())

        val authHeader = buildString {
            append("Bearer ")
            append(apiKey)
        }

        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                val request = Request.Builder()
                    .url(BASE_URL)
                    .addHeader("Authorization", authHeader)
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build()

                val response = httpClient.newCall(request).execute()
                val bodyString = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    Log.w(TAG, "HTTP ${response.code} on attempt $attempt")
                    lastException = RuntimeException("API error ${response.code}")
                    return@repeat
                }

                val jsonResponse = JSONObject(bodyString)
                val content = jsonResponse
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")

                return@withContext Result.success(content.trim())
            } catch (e: Exception) {
                Log.e(TAG, "Request failed on attempt $attempt", e)
                lastException = e
            }
        }
        Result.failure(lastException ?: RuntimeException("Unknown error"))
    }
}

/** Represents a single message in a ChatGPT conversation. */
data class ChatMessage(
    val role: Role,
    val content: String
) {
    enum class Role(val value: String) {
        USER("user"),
        ASSISTANT("assistant"),
        SYSTEM("system")
    }
}
