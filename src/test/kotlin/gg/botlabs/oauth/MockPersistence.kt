package gg.botlabs.oauth

import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

class MockPersistence<ID> : OAuthPersistence<ID> {
    val store = mutableMapOf<ID, TokenGrant<ID>>()

    override fun put(tokenGrant: TokenGrant<ID>): Mono<TokenGrant<ID>> {
        store[tokenGrant.id] = tokenGrant
        return tokenGrant.toMono()
    }

    override fun delete(id: ID): Mono<Void> {
        store.remove(id)
        return Mono.empty()
    }
}