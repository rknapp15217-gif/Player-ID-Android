package com.playerid.app.platform

import com.playerid.app.domain.auth.AccountException
import com.playerid.app.domain.auth.AccountSession
import com.playerid.app.domain.auth.AccountUser
import com.playerid.app.domain.auth.EmailAuthService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.JsonConvertException
import io.ktor.utils.io.errors.IOException
import kotlinx.datetime.Clock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException

// Publishable client configuration only. Never put service-role/secret keys here.
object PlayloomCloud {
    const val URL = "https://uupsnlpwwbpcbivihdub.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_z3Dzjwf6L2TVrSpM1Sn0-w_ag1x_glP"
}

class SupabaseEmailAuthService(
    private val client: HttpClient,
    private val nowSeconds: () -> Long = { Clock.System.now().epochSeconds }
) : EmailAuthService {
    override suspend fun requestCode(email: String) {
        post("otp", CodeRequest(email))
    }

    override suspend fun verifyCode(email: String, code: String): AccountSession =
        session(post("verify", VerifyRequest(email, code)))

    override suspend fun refreshSession(refreshToken: String): AccountSession =
        session(post("token?grant_type=refresh_token", RefreshRequest(refreshToken)))

    private suspend inline fun <reified T : Any> post(path: String, payload: T): HttpResponse {
        val response = try {
            client.post("${PlayloomCloud.URL}/auth/v1/$path") {
                header("apikey", PlayloomCloud.PUBLISHABLE_KEY)
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
        } catch (failure: HttpRequestTimeoutException) {
            throw AccountException("Sign-in timed out. Check your connection and try again.", failure)
        } catch (failure: IOException) {
            throw AccountException("Cannot reach sign-in. Check your connection and try again.", failure)
        }
        if (!response.status.isSuccess()) {
            // Do not echo server bodies: they can contain email addresses or session details.
            val message = when (response.status.value) {
                429 -> "Too many sign-in attempts. Wait a little before trying again."
                400, 401, 403, 422 -> "Sign-in was not accepted. Check your email/code or request a new code."
                else -> "Sign-in is unavailable (HTTP ${response.status.value}). Try again later."
            }
            throw AccountException(message)
        }
        return response
    }

    private suspend fun session(response: HttpResponse): AccountSession {
        val result = try {
            response.body<TokenResponse>()
        } catch (failure: SerializationException) {
            throw AccountException("The sign-in response could not be read. Please try again.", failure)
        } catch (failure: JsonConvertException) {
            throw AccountException("The sign-in response could not be read. Please try again.", failure)
        } catch (failure: IOException) {
            throw AccountException("The connection was interrupted during sign-in. Please try again.", failure)
        }
        return AccountSession(
            result.accessToken, result.refreshToken,
            result.expiresAt ?: (nowSeconds() + result.expiresIn),
            AccountUser(result.user.id, result.user.email)
        )
    }
}

@Serializable
private data class CodeRequest(val email: String, val create_user: Boolean = true)

@Serializable
private data class VerifyRequest(val email: String, val token: String, val type: String = "email")

@Serializable
private data class RefreshRequest(val refresh_token: String)

@Serializable
private data class TokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("expires_in") val expiresIn: Long,
    @SerialName("expires_at") val expiresAt: Long? = null,
    val user: TokenUser
)

@Serializable
private data class TokenUser(val id: String, val email: String)
