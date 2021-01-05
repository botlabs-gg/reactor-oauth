package gg.botlabs.oauth

import org.json.JSONObject
import java.lang.IllegalStateException
import java.lang.RuntimeException

class OAuthException(override val message: String, val rfcError: String? = null) : RuntimeException(message) {
    override fun fillInStackTrace() = this


    companion object {
        fun onInvalidJson(id: Any?, body: String): Nothing {
            throw OAuthException("Failed to get grant for $id. Invalid JSON response: $body")
        }

        fun onError(id: Any?, json: JSONObject): Nothing {
            val error = json.optString("error") ?: throw IllegalStateException("Failed to get grant for $id: $json")
            throw OAuthException("Failed to get grant for $id: $error", error)
        }
    }

}