package gg.botlabs.oauth

import java.time.Instant

data class TokenGrant<ID>(
    val id: ID,
    val bearerToken: String,
    val refreshToken: String,
    val scope: List<String>?,
    val expires: Instant
)
