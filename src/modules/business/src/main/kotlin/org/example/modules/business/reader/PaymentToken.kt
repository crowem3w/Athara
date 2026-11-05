package org.example.modules.business.reader

import org.json.JSONException
import org.json.JSONObject

/**
 * What a customer's phone shows: `{ "session_id": "...", "nonce": "..." }` (plan section 5). No amount or balance:
 * those live on the server.
 */
internal data class PaymentToken(val sessionId: String, val nonce: String)

internal object PaymentTokenParser {
    private const val MAX_LENGTH = 512
    private val SAFE_ID = Regex("^[A-Za-z0-9_-]{8,128}$")

    /** Null when [raw] is not a well-formed token. Anything read from a tag or a QR code is untrusted input. */
    fun parse(raw: String?): PaymentToken? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty() || text.length > MAX_LENGTH) return null
        return try {
            val json = JSONObject(text)
            val sessionId = json.optString("session_id")
            val nonce = json.optString("nonce")
            if (SAFE_ID.matches(sessionId) && SAFE_ID.matches(nonce)) PaymentToken(sessionId, nonce) else null
        } catch (e: JSONException) {
            null
        }
    }
}
