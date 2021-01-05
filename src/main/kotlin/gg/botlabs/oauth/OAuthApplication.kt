package gg.botlabs.oauth

import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.isSuccessful
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.fuel.reactor.monoResponse
import org.json.JSONObject
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.Instant

@Suppress("MemberVisibilityCanBePrivate", "unused")
class OAuthApplication<ID>(
    val tokenUrl: String,
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val oauthPersistence: OAuthPersistence<ID>
) {

    /** Exchanges the code for a new grant of type authorization_code and persists it */
    fun exchangeCode(id: ID, code: String, scope: List<String>? = null): Mono<TokenGrant<ID>> = tokenUrl.httpPost(
        listOfNotNull(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to redirectUri,
            scope?.let { "scope" to it.joinToString(" ") }
        )
    ).toGrantMono(id).flatMap { oauthPersistence.put(it) }

    /** Attempts to refresh a grant, or delete it if it has expired */
    fun refreshGrant(grant: TokenGrant<ID>): Mono<TokenGrant<ID>> = tokenUrl.httpPost(
        listOfNotNull(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "grant_type" to "refresh_token",
            "refresh_token" to grant.refreshToken,
            "redirect_uri" to redirectUri,
            grant.scope?.let { "scope" to it.joinToString(" ") }
        )
    ).toGrantMono(grant.id).flatMap { oauthPersistence.put(it) }

    /** Returns immediately if the bearer has not expired. Otherwise calls [refreshGrant] */
    fun getFreshGrant(grant: TokenGrant<ID>, toleranceSeconds: Long = 10): Mono<TokenGrant<ID>> {
        if (grant.expires.minusSeconds(toleranceSeconds).isBefore(Instant.now())) return grant.toMono()
        return refreshGrant(grant)
    }

    private fun Request.toGrantMono(id: ID): Mono<TokenGrant<ID>> = header("Accept", "application/json")
        .monoResponse()
        .map { res ->
            val bodyStr = res.data.decodeToString()
            val json: JSONObject
            try {
                json = JSONObject()
            } catch (e: Exception) {
                OAuthException.onInvalidJson(id, bodyStr)
            }

            if (!res.isSuccessful) OAuthException.onError(id, json)

            json.run {
                TokenGrant(
                    id,
                    getString("access_token"),
                    getString("refresh_token"),
                    optString("scope")?.split(' '),
                    Instant.now().plusSeconds(getLong("expires_in"))
                )
            }
        }

}