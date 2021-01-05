package gg.botlabs.oauth

import reactor.core.publisher.Mono

@Suppress("MemberVisibilityCanBePrivate", "unused")
class OAuthApplication<ID>(
    val tokenUrl: String,
    val clientId: String,
    val clientSecret: String,
    val redirectUri: String,
    val oauthPersistence: OAuthPersistence<ID>
) {

    /** Exchanges the code for a new grant of type authorization_code */
    fun exchangeCode(id: ID, code: String, scope: List<String>): Mono<TokenGrant<ID>> = TODO()

    /** Attempts to refresh a grant, or delete it if it has expired */
    fun refreshGrant(grant: TokenGrant<ID>): Mono<TokenGrant<ID>> = TODO()

    /** Returns immediately if the bearer has not expired. Otherwise calls [refreshGrant] */
    fun getFreshGrant(grant: TokenGrant<ID>): Mono<TokenGrant<ID>> = TODO()

}