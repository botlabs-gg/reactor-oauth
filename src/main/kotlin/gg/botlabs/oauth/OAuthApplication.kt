package gg.botlabs.oauth

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.reactor.monoResponse
import com.github.kittinunf.fuel.reactor.monoUnit
import org.json.JSONObject
import reactor.core.publisher.Mono
import java.lang.IllegalStateException
import java.time.Instant

@Suppress("MemberVisibilityCanBePrivate", "unused")
class OAuthApplication(
    val tokenUrl: String,
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val revocationUrl: String? = null
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
    fun <T> refreshGrant(handler: RefreshHandler<T>, grant: TokenGrant): Mono<T> = refreshGrant(handler, grant.refreshToken)

    /** Attempts to refresh a grant */
    fun <T> refreshGrant(handler: RefreshHandler<T>, refreshToken: String): Mono<T> = tokenUrl.httpPost(
        listOfNotNull(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
            "redirect_uri" to redirectUri
        )
    ).toGrantMono(handler, true)

    fun refresh(refreshToken: String): Mono<TokenGrant> = tokenUrl.httpPost(
        listOfNotNull(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "grant_type" to "refresh_token",
            "refresh_token" to refreshToken,
            "redirect_uri" to redirectUri
        )
    ).toGrantMonoNoHandler()

    fun revoke(token: String): Mono<Unit> {
        if (revocationUrl == null) throw IllegalStateException("Revocation URL not provided")
        return revocationUrl.httpPost(
            listOf(
                "token" to token,
                "client_id" to clientId,
                "client_secret" to clientSecret
            )
        ).monoUnit()
    }

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

            if (json.optString("error") == "invalid_grant") {
                val refreshHandler = handler as RefreshHandler
                @Suppress("UNCHECKED_CAST") // Type cast safe as Mono<Void> returns empty
                return@flatMap refreshHandler.onInvalidGrant(res) as Mono<T>
            }

            if (!res.isSuccessful) OAuthException.onError(json)

            handler.handleTokenGrant(json.run {
                TokenGrant(
                    getString("access_token"),
                    getString("refresh_token"),
                    optString("scope")?.split(' '),
                    Instant.now().plusSeconds(getLong("expires_in")),
                    this
                )
            })
        }

    private fun Request.toGrantMonoNoHandler(): Mono<TokenGrant> = header("Accept", "application/json")
        .monoResponse()
        .map { res ->
            val bodyStr = res.data.decodeToString()
            val json: JSONObject
            try {
                json = JSONObject(bodyStr)
            } catch (e: Exception) {
                OAuthException.onInvalidJson(bodyStr)
            }

            if (!res.isSuccessful) OAuthException.onError(json)

            json.run {
                TokenGrant(
                    getString("access_token"),
                    getString("refresh_token"),
                    optString("scope")?.split(' '),
                    Instant.now().plusSeconds(getLong("expires_in")),
                    this
                )
            }
        }

}