package gg.botlabs.oauth

import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono

interface GrantHandler<T> {
    /** Invoked when the server grants us new tokens. */
    fun handleTokenGrant(grant: TokenGrant): Mono<T>
}

interface RefreshHandler<T> : GrantHandler<T> {
    /** Invoked when checking for staleness and we decide not to refresh */
    fun onUnchanged(): T

    /** Invoked when the server rejects our refresh attempt, requiring reauthentication */
    fun onInvalidGrant(response: ResponseEntity<String>): Mono<Void>
}