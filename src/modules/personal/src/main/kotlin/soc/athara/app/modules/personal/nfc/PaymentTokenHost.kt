package soc.athara.app.modules.personal.nfc

import android.util.Base64
import java.security.SecureRandom
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject

/** What a payer's phone reads from ours: `{ "session_id": "...", "nonce": "..." }` (same shape the Business reader parses). */
internal data class PaymentToken(val sessionId: String, val nonce: String) {
    fun toJsonBytes(): ByteArray =
        JSONObject().put("session_id", sessionId).put("nonce", nonce).toString().toByteArray(Charsets.UTF_8)
}

/** Lifetime of one token. The card UI shows a line shrinking over this time, then a fresh token replaces it. */
internal const val TokenLifetimeSeconds = 15

/**
 * Bridge between the card on screen and [AtharaHceService] (which Android starts on its own when a reader taps).
 * Both run in the app's main thread, so plain fields are enough; [active] is volatile only as a safeguard.
 *
 * Rules that keep a captured token useless:
 * - There is a token only while the card is open ([start] .. [stop]).
 * - It is replaced every [TokenLifetimeSeconds] seconds ([rotate]).
 * - It is handed out once: after a reader gets it, further reads are refused until the next token.
 *
 * TODO: the server must create the session and confirm the payment (`POST /sessions`, then a push or sync
 * result). Until those endpoints exist, "a reader received the token" is what counts as a scan.
 */
internal object PaymentTokenHost {
    private val random = SecureRandom()

    private class Active(val token: PaymentToken, var consumed: Boolean = false)

    @Volatile
    private var active: Active? = null

    private val _delivered = MutableSharedFlow<PaymentToken>(extraBufferCapacity = 1)

    /** Emits once per token, the moment a reader successfully receives it. */
    val delivered: SharedFlow<PaymentToken> = _delivered.asSharedFlow()

    /** Starts serving a fresh token. Call when the card opens. */
    fun start(): PaymentToken = rotate()

    /** Replaces the token with a new one. */
    fun rotate(): PaymentToken {
        val token = PaymentToken(randomId(), randomId())
        active = Active(token)
        return token
    }

    /** Stops serving. Call when the card closes. */
    fun stop() {
        active = null
    }

    /** Bytes for the reader, or null when nothing is being served or the token was already taken. */
    fun take(): ByteArray? {
        val current = active ?: return null
        if (current.consumed) return null
        current.consumed = true
        _delivered.tryEmit(current.token)
        return current.token.toJsonBytes()
    }

    /** 18 random bytes as 24 URL-safe characters, inside the reader's accepted id pattern. */
    private fun randomId(): String {
        val bytes = ByteArray(18)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
