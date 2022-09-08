package gg.botlabs.oauth

import org.json.JSONObject
import java.time.Instant

data class TokenGrant(
    val bearerToken: String,
    val refreshToken: String,
    val scope: List<String>?,
    val expires: Instant,
    val guild: JSONObject
)

