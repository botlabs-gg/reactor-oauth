package gg.botlabs.oauth

import com.github.kittinunf.fuel.core.Method
import net.wussmann.kenneth.mockfuel.MockFuelStore
import net.wussmann.kenneth.mockfuel.data.MockResponse
import net.wussmann.kenneth.mockfuel.junit.MockFuelExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URLDecoder
import java.time.Instant

@ExtendWith(MockFuelExtension::class)
class Tests {
    private val app = OAuthApplication(
        "http://nohost/token",
        "id",
        "secret",
        "http://redirect.url"
    )

    @Test
    fun exchangeCode(mock: MockFuelStore) {
        val code = Math.random().toString()
        val handler = TestGrantHandler()
        val bearer = "bearer token"
        val refresh = "refresh token"
        val scope = listOf("one", "two", "three")

        mock.accessTokenResponse(bearer, refresh, scope)

        app.exchangeCode(handler, code, scope)
            .verifier()
            .assertNext {
                assertEquals(scope, it.scope)
                assertTrue(it.expires.isAfter(Instant.now()))
            }.verifyComplete()

        mock.verifyRequest {
            assertMethod(Method.POST)
            assertPath("/token")
            decodeWwwUrlEncoded(request.body()!!).apply {
                assertEquals(app.clientId, get("client_id"))
                assertEquals(app.clientSecret, get("client_secret"))
                assertEquals(app.redirectUri, get("redirect_uri"))
                assertEquals("authorization_code", get("grant_type"))
                assertEquals(code, get("code"))
                assertEquals(scope, get("scope")?.split(" "))
            }
        }
    }

    @Test
    fun refreshToken(mock: MockFuelStore) {
        val oldGrant = TokenGrant(
            bearerToken = "old bearer, ignored",
            refreshToken = "old refresh",
            scope = listOf("one", "two", "three"),
            expires = Instant.now()
        )
        val handler = TestRefreshHandler(oldGrant)
        val newBearer = "new bearer token"
        val newRefresh = "new refresh token"

        mock.accessTokenResponse(newBearer, newRefresh, oldGrant.scope!!)

        app.getFreshGrant(handler, oldGrant)
            .verifier()
            .assertNext {
                assertEquals(oldGrant.scope, it.scope)
                assertEquals(newBearer, it.bearerToken)
                assertEquals(newRefresh, it.refreshToken)
                assertTrue(it.expires.isAfter(Instant.now()))
            }.verifyComplete()

        mock.verifyRequest {
            assertMethod(Method.POST)
            assertPath("/token")
            decodeWwwUrlEncoded(request.body()!!).apply {
                assertEquals(app.clientId, get("client_id"))
                assertEquals(app.clientSecret, get("client_secret"))
                assertEquals(app.redirectUri, get("redirect_uri"))
                assertEquals("refresh_token", get("grant_type"))
                assertEquals(oldGrant.refreshToken, get("refresh_token"))
            }
        }
    }

    @Test
    fun refreshTokenRejection(mock: MockFuelStore) {
        val grant = TokenGrant(
            bearerToken = "",
            refreshToken = "",
            scope = listOf("one", "two", "three"),
            expires = Instant.now()
        )
        val handler = TestRefreshHandler(grant)

        mock.enqueue(MockResponse(400, body = """{"error": "invalid_grant"}""".toByteArray()))

        app.getFreshGrant(handler, grant)
            .expectCompleteEmpty()
    }

    @Test
    fun getGrantNoRefresh() {
        val grant = TokenGrant(
            bearerToken = "",
            refreshToken = "",
            scope = listOf("one", "two", "three"),
            expires = Instant.now().plusSeconds(60)
        )
        val handler = TestRefreshHandler(grant)

        app.getFreshGrant(handler, grant, toleranceSeconds = 30)
            .verifier()
            .assertNext {
                assertEquals(grant, it)
            }.verifyComplete()
    }

    private fun decodeWwwUrlEncoded(string: String): Map<String, String> =
        string.split("&").associate {
            val bothSides = it.split("=")
            URLDecoder.decode(bothSides.first(), "UTF-8") to
                    URLDecoder.decode(bothSides.last(), "UTF-8")
        }

    private fun MockFuelStore.accessTokenResponse(bearer: String, refresh: String, scope: List<String>) {
        on {
            MockResponse(
                200, """
                {
                    "access_token": "$bearer",
                    "refresh_token": "$refresh",
                    "token_type": "Bearer",
                    "expires_in": 604800,
                    "scope": "${scope.joinToString(" ")}"
                }
            """.trimIndent().toByteArray()
            )
        }
    }
}