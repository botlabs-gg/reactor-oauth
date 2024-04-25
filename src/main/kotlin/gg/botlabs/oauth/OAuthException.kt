package gg.botlabs.oauth

import org.json.JSONObject
import java.lang.IllegalStateException
import java.lang.RuntimeException

class OAuthException(override val message: String, val rfcError: String? = null) : RuntimeException(message) {
    override fun fillInStackTrace() = this

    companion object {
        fun onInvalidJson(body: String): Nothing {
            throw OAuthException("Failed to get grant. Invalid JSON response: $body")
        }

        fun onError(json: JSONObject): Nothing {
            val error = json.optString("error") ?: throw IllegalStateException("Failed to get grant: $json")
            throw OAuthException("Failed to get grant: $error", error)
        }
    }

}