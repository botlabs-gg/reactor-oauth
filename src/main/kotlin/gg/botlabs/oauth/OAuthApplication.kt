package gg.botlabs.oauth

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.reactor.monoResponse
import org.json.JSONObject
import reactor.core.publisher.Mono
import java.time.Instant

@Suppress("MemberVisibilityCanBePrivate", "unused")
class OAuthApplication(
    val tokenUrl: String,
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String
) {

    /** Exchanges the code for a new grant of type authorization_code */
    fun <T> exchangeCode(handler: GrantHandler<T>, code: String, scope: List<String>? = null): Mono<T> = tokenUrl.httpPost(
        listOfNotNull(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to redirectUri,
            scope?.let { "scope" to it.joinToString(" ") }
        )
    ).toGrantMono(handler)

    /** Attempts to refresh a grant */
    fun <T> refreshGrant(handler: RefreshHandler<T>, grant: TokenGrant): Mono<T> = tokenUrl.httpPost(
        listOfNotNull(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "grant_type" to "refresh_token",
            "refresh_token" to grant.refreshToken,
            "redirect_uri" to redirectUri,
            grant.scope?.let { "scope" to it.joinToString(" ") }
        )
    ).toGrantMono(handler)

    /** Returns immediately if the bearer has not expired. Otherwise calls [refreshGrant] */
    fun <T> getFreshGrant(handler: RefreshHandler<T>, grant: TokenGrant, toleranceSeconds: Long = 10): Mono<T> {
        if (grant.expires.minusSeconds(toleranceSeconds).isBefore(Instant.now())) return Mono.just(handler.onUnchanged())
        return refreshGrant(handler, grant)
    }

    private fun <T> Request.toGrantMono(handler: GrantHandler<T>): Mono<T> = header("Accept", "application/json")
        .monoResponse()
        .flatMap { res ->
            val bodyStr = res.data.decodeToString()
            val json: JSONObject
            try {
                json = JSONObject()
            } catch (e: Exception) {
                OAuthException.onInvalidJson(bodyStr)
            }

            if (!res.isSuccessful && handler is RefreshHandler) {
                @Suppress("UNCHECKED_CAST") // Type cast safe as Mono<Void> returns empty
                return@flatMap handler.onFailure(res) as Mono<T>
            } else if (!res.isSuccessful) {
                OAuthException.onError(json)
            }

            handler.handleTokenGrant(json.run {
                TokenGrant(
                    getString("access_token"),
                    getString("refresh_token"),
                    optString("scope")?.split(' '),
                    Instant.now().plusSeconds(getLong("expires_in"))
                )
            })
        }

}