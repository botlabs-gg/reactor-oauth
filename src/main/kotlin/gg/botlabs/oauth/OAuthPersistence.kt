package gg.botlabs.oauth

import reactor.core.publisher.Mono

interface OAuthPersistence<ID> {
    fun get(id: ID): Mono<TokenGrant<ID>>
    fun put(tokenGrant: TokenGrant<ID>): Mono<*>
    fun delete(id: ID): Mono<*>
}