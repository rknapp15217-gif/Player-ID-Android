package com.playerid.app.domain.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AccountException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Serializable
data class AccountUser(val id: String, val email: String)

@Serializable
data class AccountSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresAtSeconds: Long,
    val user: AccountUser
) {
    override fun toString(): String = "AccountSession(redacted)"
}

interface SecureSessionStorage {
    suspend fun read(): String?
    suspend fun write(value: String)
    suspend fun clear()
}

interface EmailAuthService {
    suspend fun requestCode(email: String)
    suspend fun verifyCode(email: String, code: String): AccountSession
    suspend fun refreshSession(refreshToken: String): AccountSession
}

data class AccountState(
    val loading: Boolean = true,
    val user: AccountUser? = null,
    val pendingEmail: String? = null,
    val message: String? = null,
    val error: String? = null
)

class EmailAuth(
    private val service: EmailAuthService,
    private val storage: SecureSessionStorage,
    private val nowSeconds: () -> Long = { Clock.System.now().epochSeconds }
) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val mutableState = MutableStateFlow(AccountState())
    val state: StateFlow<AccountState> = mutableState.asStateFlow()
    private var session: AccountSession? = null

    suspend fun restore() = action {
        val stored = storage.read()
        if (stored != null) {
            val saved = try {
                json.decodeFromString<AccountSession>(stored)
            } catch (failure: SerializationException) {
                throw AccountException("The saved sign-in could not be read. Sign out on this device, then sign in again.", failure)
            }
            validateSession(saved)
            val restored = if (saved.expiresAtSeconds <= nowSeconds() + 60) {
                service.refreshSession(saved.refreshToken)
            } else saved
            saveSession(restored)
        }
    }

    suspend fun requestCode(rawEmail: String) = action {
        val email = rawEmail.trim()
        if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email)) {
            throw AccountException("Enter a valid email address.")
        }
        service.requestCode(email)
        mutableState.value = mutableState.value.copy(
            pendingEmail = email,
            message = "Check your email for a verification code."
        )
    }

    fun changeEmail() {
        if (!state.value.loading) {
            mutableState.value = mutableState.value.copy(pendingEmail = null, message = null, error = null)
        }
    }

    suspend fun verifyCode(rawCode: String) = action {
        val email = state.value.pendingEmail ?: throw AccountException("Request an email code first.")
        val code = rawCode.trim()
        if (code.length !in 6..10 || !code.all { it in '0'..'9' }) {
            throw AccountException("Enter the verification code from your email.")
        }
        saveSession(service.verifyCode(email, code))
        mutableState.value = mutableState.value.copy(pendingEmail = null, message = "Signed in. Team data is still local on this device.")
    }

    suspend fun signOut() = action {
        withContext(NonCancellable) {
            storage.clear()
            session = null
            mutableState.value = AccountState(loading = true, message = "Signed out on this device. Your local teams and media have not changed.")
        }
    }

    /** Use at the sync boundary, not a cached token: refresh tokens can rotate. */
    suspend fun accessToken(): String = mutex.withLock {
        val current = session ?: throw AccountException("Sign in to continue.")
        if (current.expiresAtSeconds <= nowSeconds() + 60) saveSession(service.refreshSession(current.refreshToken))
        session?.accessToken ?: throw AccountException("Sign in to continue.")
    }

    private fun validateSession(value: AccountSession) {
        if (value.accessToken.isBlank() || value.refreshToken.isBlank() ||
            value.user.id.isBlank() || value.user.email.isBlank() || value.expiresAtSeconds <= 0
        ) throw AccountException("The server returned an incomplete sign-in. Please sign in again.")
    }

    private suspend fun saveSession(value: AccountSession) {
        validateSession(value)
        withContext(NonCancellable) {
            storage.write(json.encodeToString(value))
            session = value
            mutableState.value = mutableState.value.copy(user = value.user)
        }
    }

    private suspend fun action(block: suspend () -> Unit) = mutex.withLock {
        mutableState.value = mutableState.value.copy(loading = true, error = null, message = null)
        try {
            block()
        } catch (failure: CancellationException) {
            throw failure
        } catch (failure: AccountException) {
            mutableState.value = mutableState.value.copy(error = failure.message)
        } finally {
            mutableState.value = mutableState.value.copy(loading = false)
        }
    }
}
