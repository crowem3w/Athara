package soc.athara.app.modules.personal.nfc

import org.json.JSONObject

/** What a business's reader gets from this phone: `{ "session_id": "...", "nonce": "..." }` (the shape the Business reader parses). */
internal data class PaymentToken(val sessionId: String, val nonce: String) {
    fun toJsonBytes(): ByteArray =
        JSONObject().put("session_id", sessionId).put("nonce", nonce).toString().toByteArray(Charsets.UTF_8)
}

/** Lifetime of one nonce on the card. The server issues a fresh one every time the card asks (see NfcCard). */
internal const val TokenLifetimeSeconds = 15

/**
 * Bridge between the card on screen and [AtharaHceService] (which Android starts on its own when a reader taps).
 *
 * The token comes from the server and is only a pointer to a payment: it holds no amount and no balance. The
 * server decides what a token is worth, accepts a nonce once, and drops it after [TokenLifetimeSeconds] seconds
 * (replaced by [set] with the next one), so reading it more than once here is harmless.
 * There is a token only while the card is open ([set] .. [clear]).
 */
internal object PaymentTokenHost {
    @Volatile
    private var token: PaymentToken? = null

    fun set(value: PaymentToken) {
        token = value
    }

    fun clear() {
        token = null
    }

    /** Bytes for the reader, or null when the card is not open. */
    fun current(): ByteArray? = token?.toJsonBytes()
}
