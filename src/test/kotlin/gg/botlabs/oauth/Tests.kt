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

        mock.on {
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

    private fun decodeWwwUrlEncoded(string: String): Map<String, String> =
        string.split("&").associate {
            val bothSides = it.split("=")
            URLDecoder.decode(bothSides.first(), "UTF-8") to
                    URLDecoder.decode(bothSides.last(), "UTF-8")
        }
}