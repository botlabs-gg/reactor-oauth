package gg.botlabs.oauth

import reactor.core.publisher.Mono

interface OAuthPersistence<ID> {
    fun put(tokenGrant: TokenGrant<ID>): Mono<TokenGrant<ID>>
    fun delete(id: ID): Mono<Void>
}
