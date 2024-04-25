package gg.botlabs.oauth

import org.json.JSONObject
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
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

    private val webClient = WebClient.create()

    /** Exchanges the code for a new grant of type authorization_code */
    fun <T> exchangeCode(handler: GrantHandler<T>, code: String, scope: List<String>? = null): Mono<T> =
        webClient.post().uri(buildUrl(tokenUrl,
            listOfNotNull(
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to redirectUri,
                scope?.let { "scope" to it.joinToString(" ") }
            )
        )).toGrantMono(handler)

    /** Attempts to refresh a grant */
    fun <T> refreshGrant(handler: RefreshHandler<T>, grant: TokenGrant): Mono<T> =
        refreshGrant(handler, grant.refreshToken)

    /** Attempts to refresh a grant */
    fun <T> refreshGrant(handler: RefreshHandler<T>, refreshToken: String): Mono<T> = webClient.post().uri(
        buildUrl(
            tokenUrl,
            listOfNotNull(
                "client_id" to clientId,
                "client_secret" to clientSecret,
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken,
                "redirect_uri" to redirectUri
            )
        )
    ).toGrantMono(handler)

    fun refresh(refreshToken: String): Mono<TokenGrant> = webClient.post()
        .uri(
            buildUrl(
                tokenUrl,
                listOfNotNull(
                    "client_id" to clientId,
                    "client_secret" to clientSecret,
                    "grant_type" to "refresh_token",
                    "refresh_token" to refreshToken,
                    "redirect_uri" to redirectUri
                )
            )
        ).toGrantMonoNoHandler()

    fun revoke(token: String): Mono<Unit> {
        if (revocationUrl == null) throw IllegalStateException("Revocation URL not provided")
        return webClient.post().uri(
            buildUrl(
                revocationUrl,
                listOf(
                    "token" to token,
                    "client_id" to clientId,
                    "client_secret" to clientSecret
                )
            )
        ).retrieve().toBodilessEntity().thenReturn(Unit)
    }

    /** Returns immediately if the bearer has not expired. Otherwise calls [refreshGrant] */
    fun <T> getFreshGrant(handler: RefreshHandler<T>, grant: TokenGrant, toleranceSeconds: Long = 300): Mono<T> {
        val expiry = grant.expires.minusSeconds(toleranceSeconds)
        if (Instant.now().isBefore(expiry)) return Mono.just(handler.onUnchanged()!!)
        return refreshGrant(handler, grant)
    }

    private fun <T> WebClient.RequestHeadersSpec<*>.toGrantMono(handler: GrantHandler<T>): Mono<T> =
        header("Accept", "application/json")
            .retrieve()
            .toEntity(String::class.java)
            .flatMap { res ->
                val json: JSONObject
                try {
                    json = JSONObject(res.body!!)
                } catch (e: Exception) {
                    OAuthException.onInvalidJson(res.body ?: "<Empty body>")
                }

                if (json.optString("error") == "invalid_grant") {
                    val refreshHandler = handler as RefreshHandler
                    @Suppress("UNCHECKED_CAST") // Type cast safe as Mono<Void> returns empty
                    return@flatMap refreshHandler.onInvalidGrant(res) as Mono<T>
                }

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

    private fun WebClient.RequestHeadersSpec<*>.toGrantMonoNoHandler(): Mono<TokenGrant> =
        header("Accept", "application/json")
            .retrieve()
            .toEntity(String::class.java)
            .map { res ->
                val json: JSONObject
                try {
                    json = JSONObject(res.body)
                } catch (e: Exception) {
                    OAuthException.onInvalidJson(res.body ?: "<empty body>")
                }

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

    private fun buildUrl(baseUri: String, arguments: List<Pair<String, String>>): String = buildString {
        append(baseUri)
        arguments.forEachIndexed { i, pair ->
            if (i == 0) append("?")
            else append("&")
            append(pair.first)
            append("=")
            append(pair.second)
        }
    }

}