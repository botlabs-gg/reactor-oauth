@file:Suppress("MemberVisibilityCanBePrivate")

package gg.botlabs.oauth

import com.github.kittinunf.fuel.core.Response
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono


open class TestGrantHandler() : GrantHandler<TokenGrant> {
    var grant: TokenGrant? = null
        private set

    override fun handleTokenGrant(grant: TokenGrant): Mono<TokenGrant> {
        this.grant = grant
        return grant.toMono()
    }
}

class TestRefreshHandler(val original: TokenGrant) : RefreshHandler<TokenGrant>, TestGrantHandler() {
    override fun onUnchanged() = original
    var failureResponse: Response? = null
        private set

    override fun onInvalidGrant(response: Response): Mono<Void> {
        failureResponse = response
        return Mono.empty()
    }
}