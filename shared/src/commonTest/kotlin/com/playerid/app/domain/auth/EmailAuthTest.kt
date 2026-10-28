package com.playerid.app.domain.auth

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.*

class EmailAuthTest {
    private val session = AccountSession("access", "refresh", 2000, AccountUser("user-1", "family@example.com"))

    private class Storage : SecureSessionStorage {
        var value: String? = null
        var failWrites = false
        var failClear = false
        override suspend fun read(): String? = value
        override suspend fun write(value: String) {
            if (failWrites) throw AccountException("Storage full")
            this.value = value
        }
        override suspend fun clear() {
            if (failClear) throw AccountException("Storage unavailable")
            value = null
        }
    }

    private class Service(var result: AccountSession) : EmailAuthService {
        val requested = mutableListOf<String>()
        var verified: Pair<String, String>? = null
        var refreshed: String? = null
        var failure: Exception? = null
        override suspend fun requestCode(email: String) {
            failure?.let { throw it }
            requested += email
        }
        override suspend fun verifyCode(email: String, code: String): AccountSession {
            failure?.let { throw it }
            verified = email to code
            return result
        }
        override suspend fun refreshSession(refreshToken: String): AccountSession {
            failure?.let { throw it }
            refreshed = refreshToken
            return result
        }
    }

    @Test
    fun requestsAndVerifiesCodeForTheRequestedEmail() = runTest {
        val service = Service(session)
        val storage = Storage()
        val auth = EmailAuth(service, storage) { 1000 }
        auth.restore()
        assertFalse(auth.state.value.loading)
        assertNull(auth.state.value.user)
        auth.requestCode(" family@example.com ")
        assertEquals(listOf("family@example.com"), service.requested)
        assertEquals("family@example.com", auth.state.value.pendingEmail)
        auth.verifyCode("12345678")
        assertEquals("family@example.com" to "12345678", service.verified)
        assertEquals(session.user, auth.state.value.user)
        assertNull(auth.state.value.pendingEmail)
        assertEquals(session, Json.decodeFromString<AccountSession>(storage.value!!))
    }

    @Test
    fun invalidInputsDoNotCallTheServer() = runTest {
        val service = Service(session)
        val auth = EmailAuth(service, Storage()) { 1000 }
        auth.requestCode("not-an-email")
        assertTrue(service.requested.isEmpty())
        assertNotNull(auth.state.value.error)
        auth.verifyCode("123456")
        assertNull(service.verified)
        auth.requestCode("family@example.com")
        auth.verifyCode("abc123")
        assertNull(service.verified)
        assertNotNull(auth.state.value.error)
    }

    @Test
    fun changingEmailClearsThePendingChallenge() = runTest {
        val auth = EmailAuth(Service(session), Storage()) { 1000 }
        auth.requestCode("family@example.com")
        auth.changeEmail()
        assertNull(auth.state.value.pendingEmail)
        assertNull(auth.state.value.message)
    }

    @Test
    fun sessionIsNotPublishedUntilSecureStorageSucceeds() = runTest {
        val storage = Storage().apply { failWrites = true }
        val auth = EmailAuth(Service(session), storage) { 1000 }
        auth.requestCode("family@example.com")
        auth.verifyCode("123456")
        assertNull(auth.state.value.user)
        assertNull(storage.value)
        assertEquals("Storage full", auth.state.value.error)
    }

    @Test
    fun restoresValidSessionWithoutNetworkAndRefreshesNearExpiry() = runTest {
        val service = Service(session)
        val storage = Storage().apply { value = Json.encodeToString(session) }
        val auth = EmailAuth(service, storage) { 1000 }
        auth.restore()
        assertEquals(session.user, auth.state.value.user)
        assertNull(service.refreshed)
        val rotated = session.copy(accessToken = "new-access", refreshToken = "new-refresh", expiresAtSeconds = 4000)
        service.result = rotated
        val later = EmailAuth(service, storage) { 1990 }
        later.restore()
        assertEquals("refresh", service.refreshed)
        assertEquals(rotated, Json.decodeFromString<AccountSession>(storage.value!!))
        assertEquals("new-access", later.accessToken())
    }

    @Test
    fun refreshFailureIsVisibleAndDoesNotEraseTheSavedSession() = runTest {
        val original = Json.encodeToString(session)
        val storage = Storage().apply { value = original }
        val service = Service(session).apply { failure = AccountException("Offline") }
        val auth = EmailAuth(service, storage) { 1990 }
        auth.restore()
        assertNull(auth.state.value.user)
        assertEquals("Offline", auth.state.value.error)
        assertEquals(original, storage.value)
    }

    @Test
    fun accessTokenRefreshesBeforeUseAndSavesTheRotatedRefreshToken() = runTest {
        var now = 1000L
        val service = Service(session)
        val storage = Storage().apply { value = Json.encodeToString(session) }
        val auth = EmailAuth(service, storage) { now }
        auth.restore()
        now = 1990
        service.result = session.copy(accessToken = "new-access", refreshToken = "new-refresh", expiresAtSeconds = 4000)
        assertEquals("new-access", auth.accessToken())
        assertEquals("new-refresh", Json.decodeFromString<AccountSession>(storage.value!!).refreshToken)
    }

    @Test
    fun malformedSavedSessionCanBeExplicitlyCleared() = runTest {
        val storage = Storage().apply { value = "broken" }
        val auth = EmailAuth(Service(session), storage) { 1000 }
        auth.restore()
        assertNotNull(auth.state.value.error)
        assertEquals("broken", storage.value)
        auth.signOut()
        assertNull(storage.value)
        assertNull(auth.state.value.error)
    }

    @Test
    fun incompleteServerSessionIsRejected() = runTest {
        val auth = EmailAuth(Service(session.copy(accessToken = "")), Storage()) { 1000 }
        auth.requestCode("family@example.com")
        auth.verifyCode("123456")
        assertNull(auth.state.value.user)
        assertNotNull(auth.state.value.error)
    }

    @Test
    fun signOutOnlySucceedsWhenSecureStorageIsCleared() = runTest {
        val storage = Storage().apply { value = Json.encodeToString(session) }
        val auth = EmailAuth(Service(session), storage) { 1000 }
        auth.restore()
        storage.failClear = true
        auth.signOut()
        assertNotNull(auth.state.value.error)
        assertEquals(session.user, auth.state.value.user)
        storage.failClear = false
        auth.signOut()
        assertNull(storage.value)
        assertNull(auth.state.value.user)
        assertFailsWith<AccountException> { auth.accessToken() }
    }

    @Test
    fun cancellationIsNotPresentedAsSuccess() = runTest {
        val service = Service(session).apply { failure = CancellationException("Cancelled") }
        val auth = EmailAuth(service, Storage()) { 1000 }
        assertFailsWith<CancellationException> { auth.requestCode("family@example.com") }
        assertFalse(auth.state.value.loading)
        assertNull(auth.state.value.pendingEmail)
    }

    @Test
    fun sessionStringDoesNotExposeTokens() {
        assertFalse(session.toString().contains(session.accessToken))
        assertFalse(session.toString().contains(session.refreshToken))
    }
}
