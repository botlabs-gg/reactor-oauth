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
    ).toGrantMono(handler, false)

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
    ).toGrantMono(handler, true)

    /** Returns immediately if the bearer has not expired. Otherwise calls [refreshGrant] */
    fun <T> getFreshGrant(handler: RefreshHandler<T>, grant: TokenGrant, toleranceSeconds: Long = 300): Mono<T> {
        val expiry = grant.expires.minusSeconds(toleranceSeconds)
        if (Instant.now().isBefore(expiry)) return Mono.just(handler.onUnchanged()!!)
        return refreshGrant(handler, grant)
    }

    private fun <T> Request.toGrantMono(handler: GrantHandler<T>, isRefresh: Boolean): Mono<T> = header("Accept", "application/json")
        .monoResponse()
        .flatMap { res ->
            val bodyStr = res.data.decodeToString()
            val json: JSONObject
            try {
                json = JSONObject(bodyStr)
            } catch (e: Exception) {
                OAuthException.onInvalidJson(bodyStr)
            }

            if (!res.isSuccessful) {
                if (!isRefresh) OAuthException.onError(json)

                val refreshHandler = handler as RefreshHandler
                @Suppress("UNCHECKED_CAST") // Type cast safe as Mono<Void> returns empty
                return@flatMap refreshHandler.onFailure(res) as Mono<T>
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