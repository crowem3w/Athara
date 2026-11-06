package soc.athara.app.shared.payments.ble

import java.util.Base64

/**
 * What the two phones say over Bluetooth LE. Short identifiers only: no amount, balance or card data
 * (design section 1), and never a full token (a legacy advert carries about 31 bytes).
 *
 *   merchant beacon: [0x11][handle 6 bytes][nonce 8 bytes]   = 15 bytes
 *   payer beacon:    [0x12][beacon id 12 bytes]              = 13 bytes
 *
 * Carried as manufacturer-specific data under [COMPANY_ID]. The server and the apps pass the same bytes around as
 * unpadded URL-safe base64 (8, 11 and 16 characters), so the API never sees raw bytes.
 */
object BeaconCodec {
    /** 0xFFFF is the Bluetooth SIG value reserved for testing. Replace it with a registered company id (or switch to a service UUID) before release. */
    const val COMPANY_ID = 0xFFFF

    const val TYPE_MERCHANT: Byte = 0x11
    const val TYPE_PAYER: Byte = 0x12

    private const val HANDLE_BYTES = 6
    private const val NONCE_BYTES = 8
    private const val BEACON_BYTES = 12

    /** What a scan found. [id] is the text form the server uses. */
    sealed interface Heard {
        /** A merchant's rotating beacon: who (handle) and the current nonce. */
        data class Merchant(val handle: String, val nonce: String) : Heard {
            /** One merchant phone = one handle, however many nonces it has sent. */
            val key: String get() = handle
        }

        /** A payer's single-use beacon id. */
        data class Payer(val beaconId: String) : Heard {
            val key: String get() = beaconId
        }
    }

    fun merchantPayload(handle: String, nonce: String): ByteArray? {
        val h = decode(handle, HANDLE_BYTES) ?: return null
        val n = decode(nonce, NONCE_BYTES) ?: return null
        return byteArrayOf(TYPE_MERCHANT) + h + n
    }

    fun payerPayload(beaconId: String): ByteArray? {
        val b = decode(beaconId, BEACON_BYTES) ?: return null
        return byteArrayOf(TYPE_PAYER) + b
    }

    /** Null for anything that is not exactly one of our two beacons. Scan data is untrusted input. */
    fun parse(payload: ByteArray?): Heard? {
        if (payload == null || payload.isEmpty()) return null
        return when (payload[0]) {
            TYPE_MERCHANT -> {
                if (payload.size != 1 + HANDLE_BYTES + NONCE_BYTES) return null
                Heard.Merchant(
                    handle = encode(payload.copyOfRange(1, 1 + HANDLE_BYTES)),
                    nonce = encode(payload.copyOfRange(1 + HANDLE_BYTES, payload.size)),
                )
            }
            TYPE_PAYER -> {
                if (payload.size != 1 + BEACON_BYTES) return null
                Heard.Payer(encode(payload.copyOfRange(1, payload.size)))
            }
            else -> null
        }
    }

    // java.util.Base64 (not android.util) so this file runs in plain JVM unit tests.
    internal fun encode(bytes: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    /** Null unless [text] decodes to exactly [size] bytes. */
    internal fun decode(text: String, size: Int): ByteArray? = try {
        Base64.getUrlDecoder().decode(text).takeIf { it.size == size }
    } catch (e: IllegalArgumentException) {
        null
    }
}
