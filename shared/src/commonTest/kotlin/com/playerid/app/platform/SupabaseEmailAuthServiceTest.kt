package com.playerid.app.platform

import com.playerid.app.domain.auth.AccountException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.OutgoingContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import kotlin.test.*

class SupabaseEmailAuthServiceTest {
    @Test
    fun usesOnlyAuthEndpointsAndThePublishableKeyForEmailOtp() = runTest {
        val paths = mutableListOf<String>()
        val payloads = mutableListOf<JsonObject>()
        val engine = MockEngine { request ->
            paths += request.url.encodedPath
            assertEquals(PlayloomCloud.PUBLISHABLE_KEY, request.headers["apikey"])
            assertNull(request.headers[HttpHeaders.Authorization])
            val body = request.body as OutgoingContent.ByteArrayContent
            payloads += Json.parseToJsonElement(body.bytes().decodeToString()).jsonObject
            if (request.url.encodedPath.endsWith("token")) {
                assertEquals("refresh_token", request.url.parameters["grant_type"])
            }
            respond(
                if (request.url.encodedPath.endsWith("otp")) "{}" else
                    """{"access_token":"access","refresh_token":"refresh","expires_in":3600,"user":{"id":"user-1","email":"family@example.com"}}""",
                HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = configureAccountClient(HttpClient(engine))
        try {
            val service = SupabaseEmailAuthService(client) { 1000 }
            service.requestCode("family@example.com")
            val session = service.verifyCode("family@example.com", "123456")
            assertEquals(4600, session.expiresAtSeconds)
            service.refreshSession("refresh")
            assertEquals(listOf("/auth/v1/otp", "/auth/v1/verify", "/auth/v1/token"), paths)
            assertEquals(JsonPrimitive(true), payloads[0]["create_user"])
            assertEquals(JsonPrimitive("email"), payloads[1]["type"])
            assertEquals(JsonPrimitive("123456"), payloads[1]["token"])
            assertEquals(JsonPrimitive("refresh"), payloads[2]["refresh_token"])
        } finally {
            client.close()
        }
    }

    @Test
    fun rateLimitsAreExplicitAndServerDetailsAreNotEchoed() = runTest {
        val client = configureAccountClient(HttpClient(MockEngine {
            respond("""{"msg":"secret-server-detail"}""", HttpStatusCode.TooManyRequests)
        }))
        try {
            val failure = assertFailsWith<AccountException> {
                SupabaseEmailAuthService(client).requestCode("family@example.com")
            }
            assertTrue(failure.message.orEmpty().contains("Too many"))
            assertFalse(failure.message.orEmpty().contains("secret-server-detail"))
        } finally {
            client.close()
        }
    }

    @Test
    fun invalidCodeAndMalformedResponseFailExplicitly() = runTest {
        for (status in listOf(HttpStatusCode.BadRequest, HttpStatusCode.OK)) {
            val client = configureAccountClient(HttpClient(MockEngine {
                respond("{}", status, headersOf(HttpHeaders.ContentType, "application/json"))
            }))
            try {
                assertFailsWith<AccountException> {
                    SupabaseEmailAuthService(client).verifyCode("family@example.com", "123456")
                }
            } finally {
                client.close()
            }
        }
    }
}
